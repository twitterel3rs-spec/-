package com.example.sync

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.CloudCategory
import com.example.data.model.CloudMenuItem
import com.example.data.model.CloudOrderDoc
import com.example.data.model.CloudShiftDoc
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.Shift
import com.example.data.model.TerminalDoc
import com.example.data.repository.FirestoreCloudRepository
import com.example.data.repository.OrderPlacementResult
import com.example.data.repository.RestaurantRepository
import com.example.printer.PrinterConfig
import com.example.printer.PrinterResult
import com.example.util.BackgroundNotificationHelper
import com.example.util.CloudAuthManager
import com.example.util.SoundAlertUtil
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AppSyncCoordinator private constructor(private val application: Application) {

    companion object {
        private const val TAG = "AppSyncCoordinator"
        const val SELLER_CASHIER_ACK_TIMEOUT_MS = 12_000L
        const val OFFLINE_AUTO_PRINT_EXPIRY_MS = 30_000L

        const val SYNC_STATE_SYNCED = "SYNCED"
        const val SYNC_STATE_PENDING_OFFLINE = "PENDING_OFFLINE"
        const val SYNC_STATE_WAITING_CASHIER_ACK = "WAITING_CASHIER_ACK"
        const val SYNC_STATE_PRINT_CONFIRMED = "PRINT_CONFIRMED"
        const val SYNC_STATE_PRINT_ERROR = "PRINT_ERROR"

        @Volatile
        private var INSTANCE: AppSyncCoordinator? = null

        fun getInstance(application: Application): AppSyncCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSyncCoordinator(application).also { INSTANCE = it }
            }
        }
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db: AppDatabase = AppDatabase.getInstance(application)
    val repository = RestaurantRepository(db, application)
    val cloudRepository = FirestoreCloudRepository(application)

    private val terminalPrefs = application.getSharedPreferences("sennara_terminal_prefs", Context.MODE_PRIVATE)

    val deviceRole = MutableStateFlow(terminalPrefs.getString("device_role", "CASHIER") ?: "CASHIER")
    val activeSellerName = MutableStateFlow(
        terminalPrefs.getString(
            "active_seller_name",
            if ((terminalPrefs.getString("device_role", "CASHIER") ?: "CASHIER") == "SELLER") "بائع صالة" else "كاشير رئيسي"
        ) ?: "كاشير رئيسي"
    )
    val backgroundServiceEnabled = MutableStateFlow(
        terminalPrefs.getBoolean("background_service_enabled", true)
    )

    val cloudUser = MutableStateFlow<FirebaseUser?>(null)
    val isCloudConnected = MutableStateFlow(false)
    val isCloudSyncing = MutableStateFlow(false)
    val lastCloudSyncTime = MutableStateFlow<String?>(null)
    val isRestoringBackupSyncingCloud = MutableStateFlow(false)
    val nextOrderNumberSynced = MutableStateFlow<Int?>(null)

    private val _remoteSyncedPrinters = MutableStateFlow<List<PrinterConfig>>(emptyList())
    val remoteSyncedPrinters: StateFlow<List<PrinterConfig>> = _remoteSyncedPrinters.asStateFlow()

    val isDeviceOnline = MutableStateFlow(checkConnectivityNow())

    val pendingSyncOrdersCount: StateFlow<Int> = repository.pendingSyncOrdersCount
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val snackbarEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val printDiagnosticEvents = MutableSharedFlow<Pair<String?, Long>>(replay = 0, extraBufferCapacity = 32)
    val isPrintingFromCloud = MutableStateFlow(false)
    val orderVisibilityEvents = MutableSharedFlow<Long?>(replay = 0, extraBufferCapacity = 16)

    // Internal sync tracking & queues
    private var cloudMenuJob: Job? = null
    private var cloudMenuTombstonesJob: Job? = null
    private var cloudCategoriesJob: Job? = null
    private var cloudOrdersJob: Job? = null
    private var cloudShiftsJob: Job? = null
    private var cloudTerminalsJob: Job? = null
    private var cloudOrderCounterJob: Job? = null
    private var cloudRestoreEpochJob: Job? = null
    private var cloudObserversBootstrapJob: Job? = null

    private val inFlightCloudOrderIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val sellerAckWatchJobs = ConcurrentHashMap<String, Job>()
    private val offlineFlushMutex = Mutex()
    private val tempSeqLock = Any()
    private var offlineFlushRetryJob: Job? = null

    val recentLocalArchiveOverrides = ConcurrentHashMap<String, Pair<Boolean, Long>>()
    val pendingArchiveTargets = ConcurrentHashMap<String, Pair<Boolean, Long>>()
    private val pendingArchiveMutex = Mutex()
    @Volatile private var pendingArchiveLoaded = false
    @Volatile private var lastPendingFlushMs = 0L

    val localMenuTombstones = ConcurrentHashMap<String, Long>()
    val pendingMenuDeletes = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val menuDeleteMutex = Mutex()
    @Volatile private var menuDeleteLoaded = false
    @Volatile private var lastObservedTombstonesJson: String? = null
    @Volatile var lastObservedCloudMenuItems: List<CloudMenuItem> = emptyList()

    @Volatile var activeObservedCloudUid: String? = null
    @Volatile var isCloudSyncPausedForRestore: Boolean = false

    @Volatile private var networkCallbackRegistered = false
    @Volatile private var firebaseAuthHooked = false

    init {
        registerConnectivityMonitor()
        ensureFirebaseAuthHooked()
    }

    fun ensureStarted() {
        isDeviceOnline.value = checkConnectivityNow()
        registerConnectivityMonitor()
        ensureFirebaseAuthHooked()
        val activeUser = cloudUser.value
        if (activeUser != null && !isCloudSyncPausedForRestore && activeObservedCloudUid != activeUser.uid) {
            startCloudSyncObservers(activeUser.uid)
        }
    }

    private fun ensureFirebaseAuthHooked() {
        if (firebaseAuthHooked) return
        if (FirebaseApp.getApps(application).isEmpty()) return
        synchronized(this) {
            if (firebaseAuthHooked) return
            try {
                val auth = Firebase.auth
                cloudUser.value = auth.currentUser
                isCloudConnected.value = (auth.currentUser != null)
                auth.addAuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    cloudUser.value = user
                    isCloudConnected.value = (user != null)
                    if (user != null) {
                        if (!isCloudSyncPausedForRestore && activeObservedCloudUid != user.uid) {
                            startCloudSyncObservers(user.uid)
                        }
                    } else {
                        stopCloudSyncObservers()
                    }
                }
                CloudAuthManager.attemptAutoSignIn(
                    context = application,
                    scope = scope,
                    onSuccess = { user ->
                        cloudUser.value = user
                        isCloudConnected.value = true
                    }
                )
                firebaseAuthHooked = true
            } catch (t: Throwable) {
                Log.w(TAG, "FirebaseAuth not ready yet: ${t.message}")
            }
        }
    }

    fun checkConnectivityNow(): Boolean {
        return try {
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                hasTransport && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnected == true
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun registerConnectivityMonitor() {
        if (networkCallbackRegistered) return
        try {
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        val wasOffline = !isDeviceOnline.value
                        isDeviceOnline.value = true
                        if (wasOffline) {
                            Log.i(TAG, "Connectivity restored! Flushing pending offline queues.")
                        }
                        val uid = cloudUser.value?.uid
                        if (uid != null && !isCloudSyncPausedForRestore) {
                            scope.launch {
                                flushPendingCloudArchives(uid)
                                flushPendingMenuDeletesAndCounter(uid)
                                flushPendingOfflineSellerOrders(uid)
                                registerTerminalWithPrinters(uid)
                            }
                            scheduleOfflineFlushRetryIfNeeded(uid)
                        }
                    }

                    override fun onLost(network: Network) {
                        isDeviceOnline.value = checkConnectivityNow()
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        val hasInet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        val wasOffline = !isDeviceOnline.value
                        isDeviceOnline.value = hasInet
                        if (hasInet && wasOffline) {
                            val uid = cloudUser.value?.uid
                            if (uid != null && !isCloudSyncPausedForRestore) {
                                scope.launch {
                                    flushPendingCloudArchives(uid)
                                    flushPendingMenuDeletesAndCounter(uid)
                                    flushPendingOfflineSellerOrders(uid)
                                }
                                scheduleOfflineFlushRetryIfNeeded(uid)
                            }
                        }
                    }
                }
                cm.registerDefaultNetworkCallback(callback)
                networkCallbackRegistered = true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "NetworkCallback registration skipped: ${t.message}")
        }
    }

    fun nextPendingTempOrderNumber(): String {
        synchronized(tempSeqLock) {
            val next = (terminalPrefs.getInt("pending_temp_order_seq", 0) + 1).coerceAtLeast(1)
            terminalPrefs.edit().putInt("pending_temp_order_seq", next).apply()
            return "معلق-$next"
        }
    }

    fun emitSnackbar(message: String) {
        snackbarEvents.tryEmit(message)
    }

    fun postOrderNotification(key: String, title: String, body: String) {
        BackgroundNotificationHelper.notifyOrderAlert(application, key, title, body)
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        backgroundServiceEnabled.value = enabled
        terminalPrefs.edit().putBoolean("background_service_enabled", enabled).apply()
    }

    fun applyDeviceRole(newRole: String, requestedName: String = "") {
        deviceRole.value = newRole
        if (requestedName.isNotBlank()) {
            activeSellerName.value = requestedName.trim()
        }
        terminalPrefs.edit()
            .putString("device_role", newRole)
            .putString("active_seller_name", activeSellerName.value)
            .apply()
        val user = cloudUser.value
        if (user != null && !isCloudSyncPausedForRestore) {
            scope.launch {
                registerTerminalWithPrinters(user.uid)
                if (newRole == "CASHIER") {
                    startCloudSyncObservers(user.uid, forceRestart = true)
                }
            }
        }
    }

    fun setDeviceRole(newRole: String) {
        deviceRole.value = newRole
        terminalPrefs.edit().putString("device_role", newRole).apply()
        val user = cloudUser.value
        if (user != null && !isCloudSyncPausedForRestore) {
            scope.launch {
                registerTerminalWithPrinters(user.uid)
                if (newRole == "CASHIER") {
                    startCloudSyncObservers(user.uid, forceRestart = true)
                }
            }
        }
    }

    fun setActiveSellerName(newName: String) {
        val sanitized = newName.trim().ifBlank { if (deviceRole.value == "SELLER") "بائع صالة" else "كاشير رئيسي" }
        activeSellerName.value = sanitized
        terminalPrefs.edit().putString("active_seller_name", sanitized).apply()
        val user = cloudUser.value
        if (user != null && !isCloudSyncPausedForRestore) {
            scope.launch {
                registerTerminalWithPrinters(user.uid)
            }
        }
    }

    fun buildFailedPrintersLabel(failedNames: List<String>, totalAttempted: Int): String {
        val cleanNames = failedNames.filter { it.isNotBlank() }.distinct()
        if (cleanNames.isEmpty()) return ""
        val joined = cleanNames.joinToString("، ")
        return when {
            cleanNames.size == 2 && totalAttempted == 2 -> "الطابعتين ($joined)"
            cleanNames.size == 2 -> "الطابعتين: $joined"
            cleanNames.size > 2 -> "(${cleanNames.size} طابعات): $joined"
            else -> joined
        }
    }

    private fun extractPrinterErrorMessage(result: PrinterResult?): String? {
        return when (result) {
            is PrinterResult.Success -> null
            is PrinterResult.Error -> result.errorMessage
            is PrinterResult.PermissionRequired -> "يلزم منح إذن البلوتوث/الـ USB للطابعة"
            null -> "انتهت مهلة الاتصال بالطابعة"
        }
    }

    private fun serializePrinterSummariesToJson(printers: List<PrinterConfig>): String {
        val array = JSONArray()
        for (p in printers.filter { it.isEnabled }) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("displayName", p.displayName)
                put("role", p.role)
                put("isEnabled", p.isEnabled)
                put("isCashierDefault", p.isCashierDefault)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializePrinterSummariesFromJson(jsonStr: String): List<PrinterConfig> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<PrinterConfig>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PrinterConfig(
                        id = obj.optString("id", "MAIN"),
                        displayName = obj.optString("displayName", "طابعة"),
                        role = obj.optString("role", "CUSTOMER_INVOICE"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        isCashierDefault = obj.optBoolean("isCashierDefault", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list.distinctBy { it.id.removePrefix("p_").trim() }
    }

    suspend fun registerTerminalWithPrinters(userId: String) {
        try {
            val localPrinters = if (deviceRole.value == "CASHIER") {
                repository.getAllPrinterConfigs().filter { it.isEnabled }
            } else emptyList()
            val terminal = TerminalDoc(
                id = "term_${Build.MODEL.replace(" ", "_")}_${deviceRole.value}",
                name = activeSellerName.value.ifBlank { if (deviceRole.value == "SELLER") "بائع صالة" else "كاشير رئيسي" },
                isMaster = (deviceRole.value == "CASHIER"),
                canPrint = (deviceRole.value == "CASHIER"),
                printerListJson = serializePrinterSummariesToJson(localPrinters)
            )
            cloudRepository.registerTerminal(userId, terminal)

            if (deviceRole.value == "CASHIER") {
                val localNext = repository.getNextOrderNumber()
                cloudRepository.syncMaxOrderNumber(userId, localNext)
                val currentActiveShift = repository.getActiveShiftDirect()
                if (currentActiveShift != null) {
                    cloudRepository.upsertShift(userId, currentActiveShift.toCloudShiftDoc())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerTerminalWithPrinters error", e)
        }
    }

    fun Shift.toCloudShiftDoc(): CloudShiftDoc {
        return CloudShiftDoc(
            id = "shift_${this.shiftNumber}",
            shiftNumber = this.shiftNumber,
            cashierName = this.cashierName,
            openedAtMs = this.openedAt,
            closedAtMs = this.closedAt ?: 0L,
            openingBalance = this.openingBalance,
            totalCashSales = this.totalCashSales,
            totalWalletSales = this.totalWalletSales,
            totalExpenses = this.totalExpenses,
            actualDrawerCash = this.actualDrawerCash,
            expectedDrawerCash = this.expectedDrawerCash,
            difference = this.difference,
            ordersCount = this.ordersCount,
            isClosed = this.isClosed,
            notes = this.notes
        )
    }

    suspend fun ensurePendingArchiveLoaded() {
        if (pendingArchiveLoaded) return
        pendingArchiveMutex.withLock {
            if (pendingArchiveLoaded) return
            try {
                val raw = repository.getSetting("pending_cloud_archive_map_json")
                if (!raw.isNullOrBlank()) {
                    val obj = JSONObject(raw)
                    val nowMs = System.currentTimeMillis()
                    val maxAgeMs = 7L * 24L * 60L * 60L * 1000L
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val cid = keys.next()
                        val entry = obj.optJSONObject(cid) ?: continue
                        val ts = entry.optLong("ts", 0L)
                        if (ts > 0L && (nowMs - ts) <= maxAgeMs) {
                            pendingArchiveTargets[cid] = entry.optBoolean("archived", true) to ts
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ensurePendingArchiveLoaded error", e)
            } finally {
                pendingArchiveLoaded = true
            }
        }
    }

    private suspend fun persistPendingArchiveLocked() {
        try {
            val nowMs = System.currentTimeMillis()
            val maxAgeMs = 7L * 24L * 60L * 60L * 1000L
            val root = JSONObject()
            val iterator = pendingArchiveTargets.entries.iterator()
            while (iterator.hasNext()) {
                val (cid, pair) = iterator.next()
                if (nowMs - pair.second > maxAgeMs) {
                    iterator.remove()
                } else {
                    root.put(cid, JSONObject().apply {
                        put("archived", pair.first)
                        put("ts", pair.second)
                    })
                }
            }
            repository.saveSetting("pending_cloud_archive_map_json", root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "persistPendingArchiveLocked error", e)
        }
    }

    suspend fun enqueuePendingCloudArchive(entries: Map<String, Boolean>) {
        if (entries.isEmpty()) return
        ensurePendingArchiveLoaded()
        val nowMs = System.currentTimeMillis()
        pendingArchiveMutex.withLock {
            for ((cid, archived) in entries) {
                if (cid.isNotBlank()) {
                    pendingArchiveTargets[cid] = archived to nowMs
                    recentLocalArchiveOverrides[cid] = archived to nowMs
                }
            }
            persistPendingArchiveLocked()
        }
    }

    suspend fun clearPendingCloudArchive(cloudIds: Collection<String>) {
        if (cloudIds.isEmpty()) return
        ensurePendingArchiveLoaded()
        var changed = false
        pendingArchiveMutex.withLock {
            for (cid in cloudIds) {
                if (pendingArchiveTargets.remove(cid) != null) {
                    changed = true
                }
            }
            if (changed) persistPendingArchiveLocked()
        }
    }

    suspend fun clearAllSyncQueuesForSupremeRestore() {
        ensurePendingArchiveLoaded()
        pendingArchiveMutex.withLock {
            pendingArchiveTargets.clear()
            recentLocalArchiveOverrides.clear()
            repository.saveSetting("pending_cloud_archive_map_json", "{}")
        }
        clearAllMenuTombstonesLocally()
        repository.saveSetting("pending_cloud_order_counter_override", "")
        repository.saveSetting("pending_cloud_supreme_restore", "")
    }

    suspend fun flushPendingCloudArchives(userId: String) {
        ensurePendingArchiveLoaded()
        val snapshot = pendingArchiveTargets.toMap()
        if (snapshot.isEmpty()) return
        lastPendingFlushMs = System.currentTimeMillis()

        val toArchive = snapshot.filterValues { it.first }.keys.toList()
        val toUnarchive = snapshot.filterValues { !it.first }.keys.toList()

        if (toArchive.isNotEmpty()) {
            val transientFailed = withTimeoutOrNull(15_000L) {
                cloudRepository.batchArchiveCloudOrdersDetailed(userId, toArchive, true)
            } ?: toArchive.toSet()
            val succeeded = toArchive.filterNot { it in transientFailed }
            if (succeeded.isNotEmpty()) clearPendingCloudArchive(succeeded)
        }

        if (toUnarchive.isNotEmpty()) {
            val transientFailed = withTimeoutOrNull(15_000L) {
                cloudRepository.batchArchiveCloudOrdersDetailed(userId, toUnarchive, false)
            } ?: toUnarchive.toSet()
            val succeeded = toUnarchive.filterNot { it in transientFailed }
            if (succeeded.isNotEmpty()) clearPendingCloudArchive(succeeded)
        }
    }

    suspend fun ensureMenuDeleteStateLoaded() {
        val currentRaw = try {
            repository.getSetting("local_menu_tombstones_json")
        } catch (_: Exception) {
            null
        }
        if (menuDeleteLoaded && currentRaw == lastObservedTombstonesJson) return
        menuDeleteMutex.withLock {
            val lockedRaw = try {
                repository.getSetting("local_menu_tombstones_json")
            } catch (_: Exception) {
                currentRaw
            }
            if (menuDeleteLoaded && lockedRaw == lastObservedTombstonesJson) return
            try {
                val nowMs = System.currentTimeMillis()
                val maxAgeMs = 30L * 24L * 60L * 60L * 1000L
                if (!lockedRaw.isNullOrBlank()) {
                    val obj = JSONObject(lockedRaw)
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val cid = keys.next()
                        val ts = obj.optLong(cid, 0L)
                        if (cid.isNotBlank() && ts > 0L && (nowMs - ts) <= maxAgeMs) {
                            localMenuTombstones[cid] = ts
                        }
                    }
                }
                lastObservedTombstonesJson = lockedRaw
                if (!menuDeleteLoaded) {
                    val rawPending = repository.getSetting("pending_menu_deletes_json")
                    if (!rawPending.isNullOrBlank()) {
                        val arr = JSONArray(rawPending)
                        for (i in 0 until arr.length()) {
                            val cid = arr.optString(i, "")
                            if (cid.isNotBlank()) pendingMenuDeletes.add(cid)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ensureMenuDeleteStateLoaded error", e)
            } finally {
                menuDeleteLoaded = true
            }
        }
    }

    private suspend fun persistMenuDeleteStateLocked() {
        try {
            val nowMs = System.currentTimeMillis()
            val maxAgeMs = 30L * 24L * 60L * 60L * 1000L
            val bounded = localMenuTombstones.entries
                .filter { (nowMs - it.value) <= maxAgeMs }
                .sortedByDescending { it.value }
                .take(500)
            localMenuTombstones.clear()
            val tObj = JSONObject()
            for ((cid, ts) in bounded) {
                localMenuTombstones[cid] = ts
                tObj.put(cid, ts)
            }
            val tJson = tObj.toString()
            repository.saveSetting("local_menu_tombstones_json", tJson)
            lastObservedTombstonesJson = tJson

            val pArr = JSONArray()
            synchronized(pendingMenuDeletes) {
                for (cid in pendingMenuDeletes) pArr.put(cid)
            }
            repository.saveSetting("pending_menu_deletes_json", pArr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "persistMenuDeleteStateLocked error", e)
        }
    }

    suspend fun recordMenuItemTombstones(cloudIds: Collection<String>, enqueueCloudDelete: Boolean) {
        val cleanIds = cloudIds.filter { it.isNotBlank() && it != "item_unknown" }.distinct()
        if (cleanIds.isEmpty()) return
        ensureMenuDeleteStateLoaded()
        val nowMs = System.currentTimeMillis()
        menuDeleteMutex.withLock {
            for (cid in cleanIds) {
                localMenuTombstones[cid] = nowMs
                if (enqueueCloudDelete) pendingMenuDeletes.add(cid)
            }
            persistMenuDeleteStateLocked()
        }
    }

    suspend fun clearPendingMenuDeletes(cloudIds: Collection<String>) {
        if (cloudIds.isEmpty()) return
        ensureMenuDeleteStateLoaded()
        menuDeleteMutex.withLock {
            var changed = false
            for (cid in cloudIds) {
                if (pendingMenuDeletes.remove(cid)) changed = true
            }
            if (changed) persistMenuDeleteStateLocked()
        }
    }

    suspend fun clearAllMenuTombstonesLocally() {
        ensureMenuDeleteStateLoaded()
        menuDeleteMutex.withLock {
            localMenuTombstones.clear()
            pendingMenuDeletes.clear()
            persistMenuDeleteStateLocked()
        }
    }

    suspend fun purgeLocalItemsMatchingTombstones() {
        ensureMenuDeleteStateLoaded()
        if (localMenuTombstones.isEmpty()) return
        val localItems = repository.getAllMenuItemsIncludingArchivedList()
        for (item in localItems) {
            if (item.cloudId.isNotBlank() && localMenuTombstones.containsKey(item.cloudId)) {
                repository.deleteMenuItem(item.id)
            }
        }
    }

    suspend fun flushPendingMenuDeletesAndCounter(userId: String) {
        ensureMenuDeleteStateLoaded()
        val toDelete = synchronized(pendingMenuDeletes) { pendingMenuDeletes.toList() }
        if (toDelete.isNotEmpty()) {
            val failed = withTimeoutOrNull(15_000L) {
                cloudRepository.deleteMenuItemsWithTombstone(userId, toDelete)
            } ?: toDelete.toSet()
            val succeeded = toDelete.filterNot { it in failed }
            if (succeeded.isNotEmpty()) {
                clearPendingMenuDeletes(succeeded)
            }
        }

        val pendingCounter = repository.getSetting("pending_cloud_order_counter_override")?.toIntOrNull()
        if (pendingCounter != null && pendingCounter > 0 && deviceRole.value == "CASHIER") {
            val res = withTimeoutOrNull(10_000L) {
                cloudRepository.setCloudOrderNumberOverride(userId, pendingCounter)
            }
            if (res?.isSuccess == true) {
                repository.saveSetting("pending_cloud_order_counter_override", "")
            }
        }
    }

    /**
     * Event-driven 12-second Cashier ACK monitor for Seller terminals (zero heartbeat polling!).
     * If Master Cashier does not claim or respond within timeoutMs, marks order WAITING_CASHIER_ACK and alerts Seller.
     */
    fun watchSellerOrderCashierAck(
        userId: String,
        cloudOrderId: String,
        localOrderId: Long,
        orderNumberDisplay: String,
        timeoutMs: Long = SELLER_CASHIER_ACK_TIMEOUT_MS
    ) {
        if (cloudOrderId.isBlank() || deviceRole.value != "SELLER") return
        sellerAckWatchJobs.remove(cloudOrderId)?.cancel()
        val job = scope.launch {
            try {
                delay(timeoutMs)
                val localOrd = repository.getOrderByCloudIdDirect(cloudOrderId)?.order
                    ?: repository.getOrderByCloudId(cloudOrderId)?.order
                if (localOrd != null && (
                        localOrd.syncState == SYNC_STATE_PRINT_CONFIRMED ||
                            localOrd.syncState == SYNC_STATE_PRINT_ERROR ||
                            localOrd.syncState == SYNC_STATE_PENDING_OFFLINE
                        )
                ) {
                    return@launch
                }
                if (!isDeviceOnline.value) {
                    // Device itself went offline before receiving ACK
                    return@launch
                }
                repository.updateOrderSyncState(
                    orderId = localOrd?.id ?: localOrderId,
                    syncState = SYNC_STATE_WAITING_CASHIER_ACK
                )
                val msg = "⏳ تليفونك (البائع) متصل بالنت، لكن جهاز الكاشير الرئيسي لم يستجب للأوردر ($orderNumberDisplay) — قد يكون الكاشير أوفلاين أو مغلقاً؛ الأوردر معلق حالياً"
                emitSnackbar(msg)
                postOrderNotification(
                    key = "ack_timeout_$cloudOrderId",
                    title = "الكاشير الرئيسي لم يستجب ⏳",
                    body = msg
                )
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Log.w(TAG, "watchSellerOrderCashierAck error: ${e.message}")
            } finally {
                sellerAckWatchJobs.remove(cloudOrderId)
            }
        }
        sellerAckWatchJobs[cloudOrderId] = job
    }

    private fun scheduleOfflineFlushRetryIfNeeded(userId: String) {
        if (deviceRole.value != "SELLER") return
        offlineFlushRetryJob?.cancel()
        offlineFlushRetryJob = scope.launch {
            val retryDelays = listOf(2_500L, 6_000L, 12_000L)
            for (waitMs in retryDelays) {
                delay(waitMs)
                if (!isDeviceOnline.value || isCloudSyncPausedForRestore || deviceRole.value != "SELLER") break
                val remaining = repository.getPendingOfflineOrders()
                if (remaining.isEmpty()) break
                flushPendingOfflineSellerOrders(userId)
            }
        }
    }

    /**
     * Automatically flushes held Seller offline orders as soon as internet connectivity is restored.
     * Allocates official sequential order numbers from the Master/Cloud order counter BEFORE uploading.
     * If more than 30 seconds elapsed while offline, uploads as SAVED_ONLY without auto-printing.
     */
    suspend fun flushPendingOfflineSellerOrders(userId: String) {
        if (deviceRole.value != "SELLER" || isCloudSyncPausedForRestore || !isDeviceOnline.value) return
        offlineFlushMutex.withLock {
            try {
                val pendingList = repository.getPendingOfflineOrders()
                if (pendingList.isEmpty()) return

                for (orderWithItems in pendingList) {
                    if (!isDeviceOnline.value || isCloudSyncPausedForRestore) break
                    val ord = orderWithItems.order
                    val action = ord.pendingPrintAction.ifBlank { "NEW_ORDER" }
                    val requestTimeMs = ord.pendingPrintRequestedAtMs.takeIf { it > 0L } ?: ord.createdAt
                    val needsNumberAllocation = !ord.orderNumber.startsWith("#") || ord.cloudOrderId.isNullOrBlank()

                    var officialNumInt = ord.orderNumber.replace("#", "").trim().toIntOrNull() ?: 0
                    var officialNumStr = ord.orderNumber
                    var cloudId = ord.cloudOrderId?.takeIf { it.isNotBlank() } ?: ""

                    if (needsNumberAllocation) {
                        val fallbackLocal = repository.getNextOrderNumber()
                        val allocRes = withTimeoutOrNull(8_000L) {
                            cloudRepository.tryAllocateOnlineOrderNumber(userId, fallbackLocal)
                        } ?: Result.failure(Exception("انتهت مهلة تحديد الرقم من السحابة"))

                        if (allocRes.isFailure) {
                            Log.w(TAG, "Offline flush paused: cannot reach cloud counter yet (${allocRes.exceptionOrNull()?.message})")
                            break
                        }
                        officialNumInt = allocRes.getOrThrow()
                        officialNumStr = "#$officialNumInt"
                        cloudId = "order_${ord.createdAt.coerceAtLeast(1L)}_${officialNumInt}_${UUID.randomUUID().toString().take(4)}"
                        repository.updateOrderSyncMetadata(
                            orderId = ord.id,
                            orderNumber = officialNumStr,
                            cloudOrderId = cloudId,
                            syncState = SYNC_STATE_PENDING_OFFLINE,
                            pendingPrintAction = action,
                            pendingPrintRequestedAtMs = requestTimeMs
                        )
                        nextOrderNumberSynced.value = (officialNumInt + 1).coerceAtLeast(1)
                    }

                    val activeShiftNum = repository.getActiveShiftDirect()?.shiftNumber ?: 0
                    val updatedItemsJson = serializeOrderItemsToJson(orderWithItems.items)
                    val sellerLabel = ord.sellerName.ifBlank { activeSellerName.value }
                    val elapsedOfflineMs = (System.currentTimeMillis() - requestTimeMs).coerceAtLeast(0L)
                    val allowAutoPrint = elapsedOfflineMs <= OFFLINE_AUTO_PRINT_EXPIRY_MS

                    var skippedExpiredReprint = false
                    val uploadSuccess = when {
                        action == "NEW_ORDER" || action == "NEW_ORDER_SAVED_ONLY" -> {
                            val printStatus = if (action == "NEW_ORDER" && allowAutoPrint) "PENDING_PRINT" else "SAVED_ONLY"
                            val doc = CloudOrderDoc(
                                id = cloudId,
                                orderNumber = officialNumInt.coerceAtLeast(1),
                                customerName = ord.customerName,
                                customerPhone = ord.customerPhone,
                                customerSecondaryPhone = ord.customerSecondaryPhone.orEmpty(),
                                customerArea = ord.customerArea.orEmpty(),
                                deliveryAddressTitle = ord.deliveryAddressTitle.orEmpty(),
                                deliveryAddressFull = ord.deliveryAddressFull.orEmpty(),
                                orderType = ord.orderType,
                                status = ord.status.ifBlank { "NEW" },
                                printStatus = printStatus,
                                requestedPrinterTarget = if (printStatus == "PENDING_PRINT") "ALL" else "",
                                isArchived = ord.isArchived,
                                shiftNumber = activeShiftNum,
                                itemsJson = updatedItemsJson,
                                subtotal = ord.subtotal,
                                deliveryFee = ord.deliveryFee,
                                discount = ord.discount,
                                total = ord.totalAmount,
                                paymentMethod = ord.paymentMethod,
                                notes = ord.notes,
                                driverName = ord.driverName.orEmpty(),
                                createdByTerminal = sellerLabel,
                                printedByTerminal = "",
                                createdAtMs = ord.createdAt.coerceAtLeast(0L),
                                printRequestedAtMs = requestTimeMs,
                                hasFullDetails = true
                            )
                            val res = withTimeoutOrNull(8_000L) {
                                cloudRepository.createOrder(userId, doc)
                            }
                            res?.isSuccess == true
                        }
                        action == "EDIT_PRINT" || action == "EDIT_SAVED_ONLY" -> {
                            val printStatus = if (action == "EDIT_PRINT" && allowAutoPrint) "PRINT_REQUESTED" else "SAVED_ONLY"
                            val doc = CloudOrderDoc(
                                id = cloudId,
                                orderNumber = officialNumInt.coerceAtLeast(1),
                                customerName = ord.customerName,
                                customerPhone = ord.customerPhone,
                                customerSecondaryPhone = ord.customerSecondaryPhone.orEmpty(),
                                customerArea = ord.customerArea.orEmpty(),
                                deliveryAddressTitle = ord.deliveryAddressTitle.orEmpty(),
                                deliveryAddressFull = ord.deliveryAddressFull.orEmpty(),
                                orderType = ord.orderType,
                                status = ord.status.ifBlank { "NEW" },
                                printStatus = printStatus,
                                requestedPrinterTarget = if (printStatus == "PRINT_REQUESTED") "ALL" else "",
                                reprintReason = if (printStatus == "PRINT_REQUESTED") "نسخة معدلة" else "",
                                isArchived = ord.isArchived,
                                shiftNumber = activeShiftNum,
                                itemsJson = updatedItemsJson,
                                subtotal = ord.subtotal,
                                deliveryFee = ord.deliveryFee,
                                discount = ord.discount,
                                total = ord.totalAmount,
                                paymentMethod = ord.paymentMethod,
                                notes = ord.notes,
                                driverName = ord.driverName.orEmpty(),
                                createdByTerminal = sellerLabel,
                                printedByTerminal = "",
                                createdAtMs = ord.createdAt.coerceAtLeast(0L),
                                printRequestedAtMs = requestTimeMs,
                                hasFullDetails = true
                            )
                            val res = withTimeoutOrNull(8_000L) {
                                cloudRepository.syncEditedOrderToCloud(userId, doc)
                            }
                            res?.isSuccess == true
                        }
                        action.startsWith("REPRINT|") -> {
                            if (!allowAutoPrint && !needsNumberAllocation) {
                                skippedExpiredReprint = true
                                true
                            } else if (!allowAutoPrint) {
                                skippedExpiredReprint = true
                                val doc = CloudOrderDoc(
                                    id = cloudId,
                                    orderNumber = officialNumInt.coerceAtLeast(1),
                                    customerName = ord.customerName,
                                    customerPhone = ord.customerPhone,
                                    customerSecondaryPhone = ord.customerSecondaryPhone.orEmpty(),
                                    customerArea = ord.customerArea.orEmpty(),
                                    deliveryAddressTitle = ord.deliveryAddressTitle.orEmpty(),
                                    deliveryAddressFull = ord.deliveryAddressFull.orEmpty(),
                                    orderType = ord.orderType,
                                    status = ord.status.ifBlank { "NEW" },
                                    printStatus = "SAVED_ONLY",
                                    requestedPrinterTarget = "",
                                    reprintReason = "",
                                    isArchived = ord.isArchived,
                                    shiftNumber = activeShiftNum,
                                    itemsJson = updatedItemsJson,
                                    subtotal = ord.subtotal,
                                    deliveryFee = ord.deliveryFee,
                                    discount = ord.discount,
                                    total = ord.totalAmount,
                                    paymentMethod = ord.paymentMethod,
                                    notes = ord.notes,
                                    driverName = ord.driverName.orEmpty(),
                                    createdByTerminal = sellerLabel,
                                    printedByTerminal = "",
                                    createdAtMs = ord.createdAt.coerceAtLeast(0L),
                                    printRequestedAtMs = requestTimeMs,
                                    hasFullDetails = true
                                )
                                val res = withTimeoutOrNull(8_000L) {
                                    cloudRepository.createOrder(userId, doc)
                                }
                                res?.isSuccess == true
                            } else {
                                val parts = action.split("|", limit = 3)
                                val targetPrinter = parts.getOrNull(1)?.ifBlank { "ALL" } ?: "ALL"
                                val reason = parts.getOrNull(2).orEmpty()
                                val doc = CloudOrderDoc(
                                    id = cloudId,
                                    orderNumber = officialNumInt.coerceAtLeast(1),
                                    customerName = ord.customerName,
                                    customerPhone = ord.customerPhone,
                                    customerSecondaryPhone = ord.customerSecondaryPhone.orEmpty(),
                                    customerArea = ord.customerArea.orEmpty(),
                                    deliveryAddressTitle = ord.deliveryAddressTitle.orEmpty(),
                                    deliveryAddressFull = ord.deliveryAddressFull.orEmpty(),
                                    orderType = ord.orderType,
                                    status = ord.status.ifBlank { "NEW" },
                                    printStatus = "PRINT_REQUESTED",
                                    requestedPrinterTarget = targetPrinter,
                                    reprintReason = reason,
                                    isArchived = ord.isArchived,
                                    shiftNumber = activeShiftNum,
                                    itemsJson = updatedItemsJson,
                                    subtotal = ord.subtotal,
                                    deliveryFee = ord.deliveryFee,
                                    discount = ord.discount,
                                    total = ord.totalAmount,
                                    paymentMethod = ord.paymentMethod,
                                    notes = ord.notes,
                                    driverName = ord.driverName.orEmpty(),
                                    createdByTerminal = sellerLabel,
                                    printedByTerminal = "",
                                    createdAtMs = ord.createdAt.coerceAtLeast(0L),
                                    printRequestedAtMs = requestTimeMs,
                                    hasFullDetails = true
                                )
                                val res = withTimeoutOrNull(8_000L) {
                                    cloudRepository.syncEditedOrderToCloud(userId, doc)
                                }
                                res?.isSuccess == true
                            }
                        }
                        else -> false
                    }

                    if (!uploadSuccess) {
                        Log.w(TAG, "Offline flush failed uploading $cloudId ($officialNumStr); will retry on next network pulse.")
                        break
                    }

                    // Push stock updates for newly synced order
                    try {
                        val soldMenuIds = orderWithItems.items.mapNotNull { it.menuItemId }.toSet()
                        if (soldMenuIds.isNotEmpty()) {
                            val allIngs = repository.getAllMenuItemsIncludingArchivedList()
                            val affected = allIngs.filter { it.id in soldMenuIds }
                            if (affected.isNotEmpty()) {
                                val cloudItems = affected.map { item ->
                                    val cId = resolveCanonicalCloudId(item)
                                    if (item.id > 0L && item.cloudId != cId) {
                                        repository.updateMenuItemCloudId(item.id, cId)
                                    }
                                    CloudMenuItem(
                                        id = cId,
                                        name = item.name,
                                        category = item.category,
                                        price = item.basePrice,
                                        costPrice = item.costPrice,
                                        stockQuantity = item.currentStock,
                                        minStockAlert = item.minStockAlert,
                                        unit = item.unit,
                                        targetPrinter = item.targetPrinter,
                                        kitchenName = item.kitchenName,
                                        supportsPreparation = item.supportsPreparation,
                                        isArchived = item.isArchived,
                                        isActive = item.isAvailable,
                                        displayOrder = item.displayOrder,
                                        isPlateWeightEnabled = item.isPlateWeightEnabled,
                                        plateWeightKg = item.plateWeightKg,
                                        pricePerKg = item.pricePerKg
                                    )
                                }
                                withTimeoutOrNull(6_000L) {
                                    cloudRepository.batchUpsertMenuItems(userId, cloudItems)
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    repository.updateOrderSyncMetadata(
                        orderId = ord.id,
                        orderNumber = officialNumStr,
                        cloudOrderId = cloudId,
                        syncState = SYNC_STATE_SYNCED,
                        pendingPrintAction = "",
                        pendingPrintRequestedAtMs = 0L
                    )

                    val expectsAutoPrint = allowAutoPrint &&
                        (action == "NEW_ORDER" || action == "EDIT_PRINT" || action.startsWith("REPRINT|"))
                    val expiredAutoPrintNotice = !allowAutoPrint &&
                        (action == "NEW_ORDER" || action == "EDIT_PRINT" || skippedExpiredReprint)

                    val infoMsg = when {
                        expectsAutoPrint ->
                            "☁️ عاد الاتصال — تم تحديد الرقم الرسمي ($officialNumStr) وإرسال الأوردر للكاشير للطباعة"
                        skippedExpiredReprint ->
                            "⏱️ مرّ أكثر من 30 ثانية على طلب طباعة ($officialNumStr) أثناء انقطاع النت — لم يُطبع تلقائياً، يمكنك الضغط على طباعة الآن إذا أردت"
                        expiredAutoPrintNotice ->
                            "☁️ عاد الاتصال — تم مزامنة الأوردر ($officialNumStr) وحفظه عند الكاشير فقط بدون طباعة تلقائية (مرّ أكثر من 30 ثانية) ✓"
                        else ->
                            "☁️ عاد الاتصال — تمت مزامنة الأوردر بالرقم الرسمي ($officialNumStr) ✓"
                    }
                    emitSnackbar(infoMsg)
                    postOrderNotification(
                        key = "flushed_$cloudId",
                        title = "مزامنة الأوردر المعلق ($officialNumStr) ☁️",
                        body = infoMsg
                    )

                    if (expectsAutoPrint) {
                        watchSellerOrderCashierAck(
                            userId = userId,
                            cloudOrderId = cloudId,
                            localOrderId = ord.id,
                            orderNumberDisplay = officialNumStr
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "flushPendingOfflineSellerOrders error", e)
            }
        }
    }

    fun startCloudSyncObservers(userId: String, forceRestart: Boolean = false) {
        if (isCloudSyncPausedForRestore) return
        if (!forceRestart && activeObservedCloudUid == userId &&
            (cloudOrdersJob?.isActive == true || cloudObserversBootstrapJob?.isActive == true)
        ) {
            return
        }
        stopCloudSyncObservers()
        activeObservedCloudUid = userId

        cloudObserversBootstrapJob = scope.launch {
            try {
                val pendingSupremeRestore = repository.getSetting("pending_cloud_supreme_restore") == "1"
                if (pendingSupremeRestore && deviceRole.value == "CASHIER") {
                    isCloudSyncPausedForRestore = true
                    isRestoringBackupSyncingCloud.value = true
                    try {
                        val pushRes = withTimeoutOrNull(120_000L) {
                            pushRestoredDatabaseToCloud(userId)
                        } ?: Result.failure(Exception("انتهت مهلة الرفع للسحابة (120 ثانية)"))
                        if (pushRes.isSuccess) {
                            repository.saveSetting("pending_cloud_supreme_restore", "")
                            emitSnackbar("☁️ تم فرض النسخة الاحتياطية المستعادة على القاعدة المشتركة بنجاح ✓")
                        } else {
                            val errMsg = pushRes.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"
                            emitSnackbar("⚠️ تعذر فرض النسخة المستعادة على السحابة: $errMsg")
                            return@launch
                        }
                    } finally {
                        isRestoringBackupSyncingCloud.value = false
                        isCloudSyncPausedForRestore = false
                    }
                }

                if (activeObservedCloudUid != userId || isCloudSyncPausedForRestore) return@launch
                attachCloudSyncObserversInternal(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "cloudObserversBootstrapJob error", e)
            }
        }
    }

    private fun attachCloudSyncObserversInternal(userId: String) {
        scope.launch {
            flushPendingCloudArchives(userId)
            flushPendingMenuDeletesAndCounter(userId)
            flushPendingOfflineSellerOrders(userId)
            registerTerminalWithPrinters(userId)
        }

        cloudRestoreEpochJob = scope.launch {
            try {
                cloudRepository.observeRestoreEpoch(userId).collect { cloudEpoch ->
                    if (isCloudSyncPausedForRestore || cloudEpoch == null || cloudEpoch <= 0L) return@collect
                    val localEpoch = repository.getSetting("local_restore_epoch")?.toLongOrNull() ?: 0L
                    if (cloudEpoch > localEpoch) {
                        if (deviceRole.value == "SELLER") {
                            isCloudSyncPausedForRestore = true
                            try {
                                clearAllSyncQueuesForSupremeRestore()
                                repository.clearSyncedCloudDataForRemoteRestore()
                                repository.saveSetting("local_restore_epoch", cloudEpoch.toString())
                            } finally {
                                isCloudSyncPausedForRestore = false
                            }
                            emitSnackbar("🔄 تم تحديث قاعدة البيانات تلقائياً وفقاً للنسخة الاحتياطية المستعادة من الكاشير الرئيسي ✓")
                            startCloudSyncObservers(userId, forceRestart = true)
                        } else if (repository.getSetting("pending_cloud_supreme_restore") != "1") {
                            repository.saveSetting("local_restore_epoch", cloudEpoch.toString())
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "observeRestoreEpoch error", e)
            }
        }

        cloudTerminalsJob = scope.launch {
            try {
                cloudRepository.observeTerminals(userId).collect { terminals ->
                    val masterPrinters = terminals
                        .filter { it.isMaster && it.printerListJson.isNotBlank() }
                        .flatMap { deserializePrinterSummariesFromJson(it.printerListJson) }
                        .distinctBy { it.id.removePrefix("p_").trim() }
                    _remoteSyncedPrinters.value = masterPrinters
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "observeTerminals error", e)
            }
        }

        cloudOrderCounterJob = scope.launch {
            try {
                cloudRepository.observeCloudOrderCounter(userId).collect { lastOrderNum ->
                    if (isCloudSyncPausedForRestore || lastOrderNum == null) return@collect
                    val pendingOverride = repository.getSetting("pending_cloud_order_counter_override")?.toIntOrNull()
                    if (pendingOverride != null && pendingOverride > 0) return@collect
                    val cloudNext = (lastOrderNum + 1L).toInt().coerceAtLeast(1)
                    val currentLocalNext = repository.getNextOrderNumber()
                    if (currentLocalNext != cloudNext) {
                        repository.resetOrderCounter(cloudNext)
                    }
                    nextOrderNumberSynced.value = cloudNext
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "observeCloudOrderCounter error", e)
            }
        }

        cloudCategoriesJob = scope.launch {
            try {
                cloudRepository.observeCategories(userId).collect { cloudCats ->
                    if (cloudCats.isNotEmpty() && !isCloudSyncPausedForRestore) {
                        syncCloudCategoriesToLocal(cloudCats)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "observeCategories error", e)
            }
        }

        cloudMenuTombstonesJob = scope.launch {
            try {
                ensureMenuDeleteStateLoaded()
                cloudRepository.observeDeletedMenuItemIds(userId).collect { cloudTombstones ->
                    if (isCloudSyncPausedForRestore || cloudTombstones.isEmpty()) return@collect
                    recordMenuItemTombstones(cloudTombstones.keys, enqueueCloudDelete = false)
                    purgeLocalItemsMatchingTombstones()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "observeDeletedMenuItemIds error", e)
            }
        }

        cloudMenuJob = scope.launch {
            try {
                ensureMenuDeleteStateLoaded()
                cloudRepository.observeMenuItems(userId).collect { cloudItems ->
                    if (!isCloudSyncPausedForRestore) {
                        lastObservedCloudMenuItems = cloudItems
                        purgeLocalItemsMatchingTombstones()
                        if (cloudItems.isNotEmpty()) {
                            syncCloudMenuItemsToLocal(cloudItems)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "observeMenuItems error", e)
            }
        }

        cloudShiftsJob = scope.launch {
            try {
                cloudRepository.observeShifts(userId).collect { shiftsList ->
                    if (isCloudSyncPausedForRestore || shiftsList.isEmpty()) return@collect
                    for (cShift in shiftsList.sortedBy { it.shiftNumber }) {
                        if (!cShift.isClosed) {
                            repository.forceOpenOrSyncShift(
                                shiftNumber = cShift.shiftNumber,
                                cashierName = cShift.cashierName,
                                openedAt = cShift.openedAtMs,
                                openingBalance = cShift.openingBalance
                            )
                        } else {
                            repository.syncCloseShiftFromCloud(
                                shiftNumber = cShift.shiftNumber,
                                closedAtMs = cShift.closedAtMs,
                                actualDrawerCash = cShift.actualDrawerCash,
                                totalExpenses = cShift.totalExpenses,
                                notes = cShift.notes,
                                cashierName = cShift.cashierName,
                                openedAtMs = cShift.openedAtMs,
                                openingBalance = cShift.openingBalance
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "observeShifts error", e)
            }
        }

        cloudOrdersJob = scope.launch {
            try {
                ensurePendingArchiveLoaded()
                var isInitialOrdersBatch = true
                cloudRepository.observeCloudOrderChanges(userId).collect { changes ->
                    if (isCloudSyncPausedForRestore || changes.isEmpty()) return@collect
                    ensurePendingArchiveLoaded()
                    val nowMs = System.currentTimeMillis()
                    val healToArchivedIds = mutableMapOf<String, Boolean>()
                    var shouldPlayNewSellerOrderChime = false

                    for (change in changes) {
                        if (change.changeType == "REMOVED") continue
                        val doc = change.order

                        val pendingTarget = pendingArchiveTargets[doc.id]
                        val archOverride = recentLocalArchiveOverrides[doc.id]
                        val effectiveArchived = when {
                            pendingTarget != null -> pendingTarget.first
                            archOverride != null && (nowMs - archOverride.second) < 15_000L -> {
                                if (doc.isArchived == archOverride.first) {
                                    recentLocalArchiveOverrides.remove(doc.id)
                                }
                                archOverride.first
                            }
                            else -> {
                                if (archOverride != null) recentLocalArchiveOverrides.remove(doc.id)
                                doc.isArchived
                            }
                        }
                        val effectiveDoc = if (effectiveArchived != doc.isArchived) doc.copy(isArchived = effectiveArchived) else doc

                        if (deviceRole.value == "CASHIER" && !effectiveDoc.isArchived && effectiveDoc.unarchiveTimestamp == 0L) {
                            val localOrd = repository.getOrderByCloudIdDirect(effectiveDoc.id)?.order
                            if (localOrd != null && localOrd.isArchived) {
                                healToArchivedIds[effectiveDoc.id] = true
                            }
                        }

                        // Seller ACK status tracking: notify Seller when Cashier responds!
                        if (deviceRole.value == "SELLER") {
                            handleSellerCloudStatusFeedback(effectiveDoc, isInitialOrdersBatch, change.changeType)
                        }

                        val isActionablePrint = deviceRole.value == "CASHIER" &&
                            (effectiveDoc.printStatus == "PENDING_PRINT" || effectiveDoc.printStatus == "PRINT_REQUESTED")

                        if (isActionablePrint) {
                            scope.launch {
                                handleIncomingCloudOrder(userId, effectiveDoc)
                            }
                        } else {
                            val existedLocally = repository.getOrderByCloudIdDirect(effectiveDoc.id) != null
                            val items = deserializeOrderItemsFromJson(effectiveDoc.itemsJson)
                            repository.upsertCloudOrderToLocal(effectiveDoc, items)

                            if (!isInitialOrdersBatch &&
                                deviceRole.value == "CASHIER" &&
                                change.changeType == "ADDED" &&
                                !existedLocally &&
                                !effectiveDoc.isArchived &&
                                effectiveDoc.printedByTerminal != "نسخة مستعادة" &&
                                effectiveDoc.printStatus == "SAVED_ONLY"
                            ) {
                                shouldPlayNewSellerOrderChime = true
                            }
                        }
                    }
                    isInitialOrdersBatch = false

                    if (shouldPlayNewSellerOrderChime) {
                        SoundAlertUtil.playNewOrderAlert(application)
                    }

                    if (healToArchivedIds.isNotEmpty()) {
                        enqueuePendingCloudArchive(healToArchivedIds)
                        scope.launch {
                            flushPendingCloudArchives(userId)
                        }
                    } else if (pendingArchiveTargets.isNotEmpty() && (nowMs - lastPendingFlushMs) > 15_000L) {
                        scope.launch {
                            flushPendingCloudArchives(userId)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "observeCloudOrderChanges error", e)
            }
        }
    }

    private suspend fun handleSellerCloudStatusFeedback(
        cloudOrder: CloudOrderDoc,
        isInitialBatch: Boolean,
        changeType: String
    ) {
        if (cloudOrder.printStatus !in setOf("PRINTING", "PRINTED", "ERROR", "SAVED_ONLY")) return
        // Cashier responded! Cancel ACK timeout watcher if active.
        sellerAckWatchJobs.remove(cloudOrder.id)?.cancel()

        val local = repository.getOrderByCloudIdDirect(cloudOrder.id) ?: return
        val ord = local.order
        if (ord.syncState == SYNC_STATE_PENDING_OFFLINE) return

        val wasWaitingOrSynced = ord.syncState == SYNC_STATE_WAITING_CASHIER_ACK || ord.syncState == SYNC_STATE_SYNCED
        val targetSyncState = when (cloudOrder.printStatus) {
            "PRINTED" -> SYNC_STATE_PRINT_CONFIRMED
            "ERROR" -> SYNC_STATE_PRINT_ERROR
            "SAVED_ONLY", "PRINTING" -> if (ord.syncState == SYNC_STATE_WAITING_CASHIER_ACK) SYNC_STATE_SYNCED else ord.syncState
            else -> ord.syncState
        }

        if (targetSyncState != ord.syncState) {
            repository.updateOrderSyncState(ord.id, targetSyncState)
        }

        if (!isInitialBatch && changeType == "MODIFIED" && wasWaitingOrSynced) {
            when (cloudOrder.printStatus) {
                "PRINTED" -> {
                    val msg = "🖨️ استلم الكاشير الرئيسي الأوردر #${cloudOrder.orderNumber} وتمت طباعته بنجاح ✓"
                    emitSnackbar(msg)
                    postOrderNotification("printed_${cloudOrder.id}", "تمت الطباعة عند الكاشير ✓", msg)
                }
                "ERROR" -> {
                    val msg = "⚠️ استلم الكاشير الأوردر #${cloudOrder.orderNumber} لكن تعذرت الطباعة — يرجى مراجعة الطابعات على الكاشير"
                    emitSnackbar(msg)
                    postOrderNotification("print_err_${cloudOrder.id}", "تنبيه فشل طباعة على الكاشير ⚠️", msg)
                }
                "SAVED_ONLY" -> {
                    if (ord.syncState == SYNC_STATE_WAITING_CASHIER_ACK) {
                        val msg = "✓ استلم الكاشير الرئيسي الأوردر المعلق #${cloudOrder.orderNumber} وحفظه"
                        emitSnackbar(msg)
                        postOrderNotification("saved_${cloudOrder.id}", "استلام أوردر معلق ✓", msg)
                    }
                }
            }
        }
    }

    fun stopCloudSyncObservers() {
        activeObservedCloudUid = null
        cloudObserversBootstrapJob?.cancel()
        cloudObserversBootstrapJob = null
        cloudRestoreEpochJob?.cancel()
        cloudRestoreEpochJob = null
        cloudMenuJob?.cancel()
        cloudMenuJob = null
        cloudMenuTombstonesJob?.cancel()
        cloudMenuTombstonesJob = null
        cloudCategoriesJob?.cancel()
        cloudCategoriesJob = null
        cloudOrdersJob?.cancel()
        cloudOrdersJob = null
        cloudShiftsJob?.cancel()
        cloudShiftsJob = null
        cloudTerminalsJob?.cancel()
        cloudTerminalsJob = null
        cloudOrderCounterJob?.cancel()
        cloudOrderCounterJob = null
        sellerAckWatchJobs.values.forEach { it.cancel() }
        sellerAckWatchJobs.clear()
    }

    private suspend fun syncCloudCategoriesToLocal(cloudCats: List<CloudCategory>) {
        try {
            val localCats = repository.getAllCategoriesList()
            for (cCat in cloudCats) {
                val existing = localCats.firstOrNull {
                    repository.normalizeArabicText(it.name) == repository.normalizeArabicText(cCat.name)
                }
                if (existing == null) {
                    repository.saveCategory(
                        MenuCategory(
                            name = cCat.name,
                            displayOrder = cCat.sortOrder,
                            isArchived = cCat.isArchived
                        )
                    )
                } else if (existing.displayOrder != cCat.sortOrder || existing.isArchived != cCat.isArchived) {
                    repository.saveCategory(
                        existing.copy(
                            displayOrder = cCat.sortOrder,
                            isArchived = cCat.isArchived
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncCloudCategoriesToLocal error", e)
        }
    }

    suspend fun syncCloudMenuItemsToLocal(cloudItems: List<CloudMenuItem>) {
        try {
            ensureMenuDeleteStateLoaded()
            purgeLocalItemsMatchingTombstones()

            val localList = repository.getAllMenuItemsIncludingArchivedList()
            val existingCats = repository.getAllCategoriesList().map { repository.normalizeArabicText(it.name) }.toMutableSet()
            var updatedCount = 0
            val nowMs = System.currentTimeMillis()

            val orderedCloudItems = cloudItems
                .filter { !localMenuTombstones.containsKey(it.id) }
                .sortedBy { it.updatedAt?.toDate()?.time ?: 0L }
            val matchedLocalIds = mutableSetOf<Long>()

            for (cloudItm in orderedCloudItems) {
                val catName = cloudItm.category.trim()
                if (catName.isNotBlank() && !existingCats.contains(repository.normalizeArabicText(catName))) {
                    repository.saveCategory(MenuCategory(name = catName, displayOrder = 0))
                    existingCats.add(repository.normalizeArabicText(catName))
                }

                val normCloudName = repository.normalizeArabicText(cloudItm.name)
                val normCloudCat = repository.normalizeArabicText(cloudItm.category)
                val existing = localList.firstOrNull {
                    it.cloudId.isNotBlank() && it.cloudId == cloudItm.id
                } ?: localList.firstOrNull {
                    it.id !in matchedLocalIds &&
                        it.cloudId.isBlank() &&
                        repository.normalizeArabicText(it.name) == normCloudName &&
                        repository.normalizeArabicText(it.category) == normCloudCat &&
                        it.unit.trim().equals(cloudItm.unit.ifBlank { it.unit }.trim(), ignoreCase = true) &&
                        kotlin.math.abs(it.basePrice - cloudItm.price) < 0.01
                }
                if (existing != null) matchedLocalIds.add(existing.id)

                val overrideEntry = recentLocalArchiveOverrides[cloudItm.id]
                    ?: existing?.cloudId?.takeIf { it.isNotBlank() }?.let { recentLocalArchiveOverrides[it] }
                val effectiveIsArchived = if (overrideEntry != null && (nowMs - overrideEntry.second) < 3500L) {
                    if (cloudItm.isArchived == overrideEntry.first) {
                        recentLocalArchiveOverrides.remove(cloudItm.id)
                    }
                    overrideEntry.first
                } else {
                    if (overrideEntry != null) recentLocalArchiveOverrides.remove(cloudItm.id)
                    cloudItm.isArchived
                }

                if (existing != null) {
                    val targetPrinter = cloudItm.targetPrinter.ifBlank { "MAIN" }
                    val needsUpdate = existing.name != cloudItm.name ||
                        existing.category != (cloudItm.category.ifBlank { existing.category }) ||
                        existing.unit != (cloudItm.unit.ifBlank { existing.unit }) ||
                        existing.basePrice != cloudItm.price ||
                        existing.costPrice != cloudItm.costPrice ||
                        existing.currentStock != cloudItm.stockQuantity ||
                        existing.minStockAlert != cloudItm.minStockAlert ||
                        existing.targetPrinter != targetPrinter ||
                        existing.kitchenName != cloudItm.kitchenName ||
                        existing.isArchived != effectiveIsArchived ||
                        existing.isAvailable != cloudItm.isActive ||
                        existing.supportsPreparation != cloudItm.supportsPreparation ||
                        existing.displayOrder != cloudItm.displayOrder ||
                        existing.isPlateWeightEnabled != cloudItm.isPlateWeightEnabled ||
                        existing.plateWeightKg != cloudItm.plateWeightKg ||
                        existing.pricePerKg != cloudItm.pricePerKg ||
                        existing.cloudId != cloudItm.id

                    if (needsUpdate) {
                        val updated = existing.copy(
                            name = cloudItm.name.ifBlank { existing.name },
                            category = cloudItm.category.ifBlank { existing.category },
                            unit = cloudItm.unit.ifBlank { existing.unit },
                            basePrice = cloudItm.price,
                            costPrice = if (cloudItm.costPrice > 0.0) cloudItm.costPrice else existing.costPrice,
                            currentStock = cloudItm.stockQuantity,
                            minStockAlert = cloudItm.minStockAlert,
                            targetPrinter = targetPrinter,
                            kitchenName = cloudItm.kitchenName,
                            isArchived = effectiveIsArchived,
                            supportsPreparation = cloudItm.supportsPreparation,
                            isAvailable = cloudItm.isActive,
                            cloudId = cloudItm.id,
                            displayOrder = cloudItm.displayOrder,
                            isPlateWeightEnabled = cloudItm.isPlateWeightEnabled,
                            plateWeightKg = cloudItm.plateWeightKg,
                            pricePerKg = cloudItm.pricePerKg
                        )
                        repository.saveMenuItem(updated)
                        updatedCount++
                    }
                } else {
                    val newItem = MenuItem(
                        name = cloudItm.name,
                        category = cloudItm.category.ifBlank { "عام" },
                        unit = cloudItm.unit.ifBlank { "كيلو" },
                        basePrice = cloudItm.price,
                        costPrice = cloudItm.costPrice,
                        currentStock = cloudItm.stockQuantity,
                        minStockAlert = cloudItm.minStockAlert,
                        targetPrinter = cloudItm.targetPrinter.ifBlank { "MAIN" },
                        kitchenName = cloudItm.kitchenName,
                        supportsPreparation = cloudItm.supportsPreparation,
                        isArchived = effectiveIsArchived,
                        isAvailable = cloudItm.isActive,
                        displayOrder = cloudItm.displayOrder,
                        isPlateWeightEnabled = cloudItm.isPlateWeightEnabled,
                        plateWeightKg = cloudItm.plateWeightKg,
                        pricePerKg = cloudItm.pricePerKg,
                        cloudId = cloudItm.id
                    )
                    repository.saveMenuItem(newItem)
                    updatedCount++
                }
            }
            if (updatedCount > 0) {
                lastCloudSyncTime.value = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault()).format(Date())
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncCloudMenuItemsToLocal error", e)
        }
    }

    private data class CloudPrintOutcome(
        val attempted: Int,
        val succeeded: Int,
        val noEnabledPrinters: Boolean,
        val firstError: String?,
        val succeededNames: List<String> = emptyList(),
        val failedNames: List<String> = emptyList()
    )

    private suspend fun dispatchLinkedPrintersSuspend(localOrderId: Long): CloudPrintOutcome {
        val orderWithItems = repository.getOrderByIdDirect(localOrderId)
            ?: return CloudPrintOutcome(0, 0, noEnabledPrinters = false, firstError = "الأوردر غير موجود محلياً")
        val allConfigs = repository.getAllPrinterConfigs().filter { it.isEnabled }
        if (allConfigs.isEmpty()) {
            return CloudPrintOutcome(0, 0, noEnabledPrinters = true, firstError = null)
        }

        val itemPrinters = orderWithItems.items.flatMap { it.targetPrinter.split(",") }
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
        val pendingCancels = repository.getPendingKitchenCancellations(localOrderId)

        val printersToPrint = mutableSetOf<String>()
        val invoicePrinters = allConfigs.filter { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" || it.isCashierDefault }
        for (p in invoicePrinters) printersToPrint.add(p.id)
        for (p in allConfigs) {
            if (p.role == "KITCHEN_ORDER" || p.role == "BOTH") {
                val hasItemForPrinter = itemPrinters.contains(p.id.uppercase()) || itemPrinters.contains("ALL") || itemPrinters.isEmpty()
                val hasCancelForPrinter = pendingCancels.any { repository.matchesKitchenPrinterTarget(it.targetPrinter, p.id) }
                if (hasItemForPrinter || hasCancelForPrinter) {
                    printersToPrint.add(p.id)
                }
            }
        }
        if (printersToPrint.isEmpty()) printersToPrint.add(allConfigs.first().id)

        var attempted = 0
        var succeeded = 0
        var firstError: String? = null
        val succeededNames = mutableListOf<String>()
        val failedNames = mutableListOf<String>()
        for (pid in printersToPrint) {
            val pConfig = allConfigs.firstOrNull { it.id == pid } ?: repository.getPrinterConfig(pid)
            attempted++
            val res = withTimeoutOrNull(30_000L) {
                repository.dispatchPrint(localOrderId, pid)
            }
            if (res is PrinterResult.Success) {
                succeeded++
                succeededNames.add(pConfig.displayName)
            } else {
                failedNames.add(pConfig.displayName)
                if (firstError == null) {
                    firstError = extractPrinterErrorMessage(res)
                }
            }
        }
        return CloudPrintOutcome(
            attempted = attempted,
            succeeded = succeeded,
            noEnabledPrinters = false,
            firstError = firstError,
            succeededNames = succeededNames,
            failedNames = failedNames
        )
    }

    internal suspend fun handleIncomingCloudOrder(
        userId: String,
        cloudOrder: CloudOrderDoc,
        skipCloudClaimForTest: Boolean = false
    ) {
        val lockKey = "${cloudOrder.id}_${cloudOrder.printStatus}_${cloudOrder.requestedPrinterTarget}"
        if (!inFlightCloudOrderIds.add(lockKey)) return

        try {
            val isRemoteReprint = cloudOrder.printStatus == "PRINT_REQUESTED"
            val claimed = if (skipCloudClaimForTest) {
                true
            } else {
                cloudRepository.tryClaimOrderForPrinting(
                    userId = userId,
                    orderId = cloudOrder.id,
                    expectedStatuses = setOf("PENDING_PRINT", "PRINT_REQUESTED")
                )
            }
            if (!claimed) return

            val items = deserializeOrderItemsFromJson(cloudOrder.itemsJson)
            val existingLocal = repository.getOrderByCloudIdDirect(cloudOrder.id)
            val orderTimeMs = if (cloudOrder.createdAtMs > 0L) {
                cloudOrder.createdAtMs
            } else {
                cloudOrder.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            }

            val localOrderId = if (existingLocal != null) {
                repository.upsertCloudOrderToLocal(cloudOrder, items)
            } else {
                if (items.isEmpty()) {
                    if (!skipCloudClaimForTest) {
                        cloudRepository.updateCloudOrderPrintState(userId, cloudOrder.id, "ERROR", activeSellerName.value)
                    }
                    return
                }
                val cleanNum = if (cloudOrder.orderNumber > 0) "#${cloudOrder.orderNumber}" else ""
                val matchedShift = if (cloudOrder.shiftNumber > 0) {
                    repository.getShiftByNumberDirect(cloudOrder.shiftNumber) ?: repository.getActiveShiftDirect()
                } else repository.getActiveShiftDirect()
                val isFromClosedShift = matchedShift != null && matchedShift.isClosed &&
                    (matchedShift.closedAt == null || orderTimeMs <= matchedShift.closedAt)
                val order = Order(
                    orderNumber = cleanNum,
                    orderType = cloudOrder.orderType,
                    customerName = cloudOrder.customerName.ifBlank { "زبون صالة" },
                    customerPhone = cloudOrder.customerPhone,
                    customerSecondaryPhone = cloudOrder.customerSecondaryPhone.trim().takeIf { it.isNotBlank() },
                    customerArea = cloudOrder.customerArea.trim().takeIf { it.isNotBlank() },
                    deliveryAddressTitle = cloudOrder.deliveryAddressTitle.trim().takeIf { it.isNotBlank() },
                    deliveryAddressFull = cloudOrder.deliveryAddressFull.trim().takeIf { it.isNotBlank() },
                    driverName = cloudOrder.driverName.trim().takeIf { it.isNotBlank() },
                    deliveryFee = cloudOrder.deliveryFee,
                    subtotal = cloudOrder.subtotal,
                    discount = cloudOrder.discount,
                    totalAmount = cloudOrder.total,
                    paymentMethod = cloudOrder.paymentMethod.ifBlank { "CASH" },
                    notes = cloudOrder.notes,
                    status = if (cloudOrder.status.isBlank() || cloudOrder.status == "PENDING") "NEW" else cloudOrder.status,
                    isArchived = cloudOrder.isArchived || (isFromClosedShift && cloudOrder.unarchiveTimestamp == 0L),
                    shiftId = matchedShift?.id,
                    sellerName = cloudOrder.createdByTerminal,
                    deviceRole = "SELLER",
                    cloudOrderId = cloudOrder.id,
                    createdAt = orderTimeMs
                )
                val res = repository.placeOrder(order, items, deductStock = false)
                if (res !is OrderPlacementResult.Success) {
                    if (!skipCloudClaimForTest) {
                        cloudRepository.updateCloudOrderPrintState(userId, cloudOrder.id, "ERROR", activeSellerName.value)
                    }
                    return
                }
                res.orderId
            }

            orderVisibilityEvents.tryEmit(localOrderId)

            if (!isRemoteReprint) {
                SoundAlertUtil.playNewOrderAlert(application)
            }
            executeCloudPrintJob(
                userId = userId,
                localOrderId = localOrderId,
                cloudOrder = cloudOrder,
                skipCloudWriteForTest = skipCloudClaimForTest
            )
        } catch (e: Exception) {
            Log.e(TAG, "handleIncomingCloudOrder error", e)
            if (!skipCloudClaimForTest) {
                try {
                    cloudRepository.updateCloudOrderPrintState(userId, cloudOrder.id, "ERROR", activeSellerName.value)
                } catch (_: Exception) {}
            }
        } finally {
            inFlightCloudOrderIds.remove(lockKey)
        }
    }

    private suspend fun executeCloudPrintJob(
        userId: String,
        localOrderId: Long,
        cloudOrder: CloudOrderDoc,
        skipCloudWriteForTest: Boolean = false
    ) {
        val isRemoteReprint = cloudOrder.printStatus == "PRINT_REQUESTED"
        val enabledConfigs = repository.getAllPrinterConfigs().filter { it.isEnabled }
        val reqTarget = cloudOrder.requestedPrinterTarget.trim().ifBlank { "ALL" }
        val reprintReason = cloudOrder.reprintReason.ifBlank { null }

        val printOutcome: CloudPrintOutcome = if (enabledConfigs.isEmpty()) {
            CloudPrintOutcome(attempted = 0, succeeded = 0, noEnabledPrinters = true, firstError = null)
        } else {
            when {
                reqTarget.equals("ALL", ignoreCase = true) -> {
                    dispatchLinkedPrintersSuspend(localOrderId)
                }
                reqTarget.equals("KITCHEN_ALL", ignoreCase = true) -> {
                    val kitchenConfigs = enabledConfigs
                        .filter { it.role == "KITCHEN_ORDER" || it.role == "BOTH" || it.id.contains("KITCHEN") }
                    if (kitchenConfigs.isEmpty()) {
                        CloudPrintOutcome(0, 0, noEnabledPrinters = true, firstError = "لا توجد طابعة مطبخ مفعّلة")
                    } else {
                        var attempted = 0
                        var succeeded = 0
                        var firstError: String? = null
                        val succeededNames = mutableListOf<String>()
                        val failedNames = mutableListOf<String>()
                        for (kc in kitchenConfigs) {
                            attempted++
                            val res = withTimeoutOrNull(30_000L) {
                                repository.dispatchPrint(
                                    orderId = localOrderId,
                                    printerTarget = kc.id,
                                    reprintReason = reprintReason ?: "إضافة تشغيل",
                                    targetDocumentType = "KITCHEN_ORDER"
                                )
                            }
                            if (res is PrinterResult.Success) {
                                succeeded++
                                succeededNames.add(kc.displayName)
                            } else {
                                failedNames.add(kc.displayName)
                                if (firstError == null) firstError = extractPrinterErrorMessage(res)
                            }
                        }
                        CloudPrintOutcome(attempted, succeeded, false, firstError, succeededNames, failedNames)
                    }
                }
                else -> {
                    val cleanReqId = reqTarget.removePrefix("p_").trim()
                    val exactMatch = enabledConfigs.firstOrNull {
                        it.id.removePrefix("p_").trim().equals(cleanReqId, ignoreCase = true)
                    }
                    val matchedConfig = exactMatch ?: run {
                        val wantsKitchen = cleanReqId.uppercase().contains("KITCHEN")
                        if (wantsKitchen) {
                            enabledConfigs.firstOrNull { it.role == "KITCHEN_ORDER" || it.role == "BOTH" }
                                ?: enabledConfigs.firstOrNull()
                        } else {
                            enabledConfigs.firstOrNull { it.isCashierDefault }
                                ?: enabledConfigs.firstOrNull { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" }
                                ?: enabledConfigs.firstOrNull()
                        }
                    }
                    if (matchedConfig == null) {
                        CloudPrintOutcome(0, 0, noEnabledPrinters = true, firstError = "الطابعة المطلوبة غير مفعّلة")
                    } else {
                        val explicitDocType = if (matchedConfig.role.startsWith("KITCHEN")) "KITCHEN_ORDER"
                        else if (matchedConfig.role == "CUSTOMER_INVOICE") "CUSTOMER_INVOICE" else null
                        val res = withTimeoutOrNull(30_000L) {
                            repository.dispatchPrint(
                                orderId = localOrderId,
                                printerTarget = matchedConfig.id,
                                reprintReason = reprintReason,
                                targetDocumentType = explicitDocType
                            )
                        }
                        val ok = res is PrinterResult.Success
                        CloudPrintOutcome(
                            attempted = 1,
                            succeeded = if (ok) 1 else 0,
                            noEnabledPrinters = false,
                            firstError = extractPrinterErrorMessage(res),
                            succeededNames = if (ok) listOf(matchedConfig.displayName) else emptyList(),
                            failedNames = if (!ok) listOf(matchedConfig.displayName) else emptyList()
                        )
                    }
                }
            }
        }

        val terminalName = activeSellerName.value
        val failedLabel = buildFailedPrintersLabel(printOutcome.failedNames, printOutcome.attempted)
        val statusMessage: String = when {
            printOutcome.noEnabledPrinters -> {
                val cloudState = if (isRemoteReprint) "ERROR" else "SAVED_ONLY"
                if (!skipCloudWriteForTest) {
                    cloudRepository.updateCloudOrderPrintState(userId, cloudOrder.id, cloudState, terminalName)
                }
                if (isRemoteReprint) {
                    "⚠️ تعذر تنفيذ طلب الطباعة من (${cloudOrder.createdByTerminal}) للأوردر #${cloudOrder.orderNumber}: لا توجد طابعة مفعّلة على جهاز الكاشير"
                } else {
                    "🔔 أوردر سحابي جديد من (${cloudOrder.createdByTerminal}) برقم #${cloudOrder.orderNumber} — تم استلامه وحفظه فقط (لا توجد طابعة مفعّلة)"
                }
            }
            printOutcome.attempted > 0 && printOutcome.succeeded == printOutcome.attempted -> {
                if (!skipCloudWriteForTest) {
                    cloudRepository.markOrderPrinted(userId, cloudOrder.id, terminalName)
                }
                val okNames = printOutcome.succeededNames.joinToString("+")
                if (isRemoteReprint) {
                    "🖨️ تم تنفيذ طلب طباعة من (${cloudOrder.createdByTerminal}) للأوردر #${cloudOrder.orderNumber} ($okNames) ✓"
                } else {
                    "🔔 أوردر سحابي #${cloudOrder.orderNumber} من (${cloudOrder.createdByTerminal}) تم استلامه وطباعته ($okNames) ✓"
                }
            }
            printOutcome.succeeded in 1 until printOutcome.attempted -> {
                if (!skipCloudWriteForTest) {
                    cloudRepository.updateCloudOrderPrintState(userId, cloudOrder.id, "ERROR", terminalName)
                }
                val okNames = printOutcome.succeededNames.joinToString("+")
                val errSuffix = printOutcome.firstError?.let { " — $it" } ?: ""
                val failPart = if (failedLabel.isNotBlank()) "تعذر على $failedLabel" else "فشل الباقي"
                "⚠️ أوردر #${cloudOrder.orderNumber} من (${cloudOrder.createdByTerminal}): طُبع على ($okNames) فقط و$failPart$errSuffix"
            }
            else -> {
                if (!skipCloudWriteForTest) {
                    cloudRepository.updateCloudOrderPrintState(userId, cloudOrder.id, "ERROR", terminalName)
                }
                val targetErr = if (failedLabel.isNotBlank()) " على $failedLabel" else ""
                val errSuffix = printOutcome.firstError?.let { " — $it" } ?: ""
                if (isRemoteReprint) {
                    "⚠️ تعذر الطباعة$targetErr للأوردر #${cloudOrder.orderNumber} من (${cloudOrder.createdByTerminal})$errSuffix"
                } else {
                    "🔔 أوردر سحابي #${cloudOrder.orderNumber} من (${cloudOrder.createdByTerminal}) — حُفظ لكن تعذر الطباعة$targetErr$errSuffix"
                }
            }
        }

        val diagStatus = when {
            printOutcome.noEnabledPrinters -> "حُفظ #${cloudOrder.orderNumber} (لا توجد طابعات مفعّلة)"
            printOutcome.attempted > 0 && printOutcome.succeeded == printOutcome.attempted ->
                "طُبع سحابياً #${cloudOrder.orderNumber} (${printOutcome.succeededNames.joinToString("+")}) ✓"
            failedLabel.isNotBlank() -> "تعذر الطباعة (#${cloudOrder.orderNumber}): $failedLabel"
            else -> "تعذر الطباعة (#${cloudOrder.orderNumber})"
        }
        printDiagnosticEvents.tryEmit(diagStatus to if (printOutcome.failedNames.isNotEmpty()) 8L else 4L)
        emitSnackbar(statusMessage)

        val notifTitle = if (printOutcome.failedNames.isNotEmpty() || (isRemoteReprint && printOutcome.noEnabledPrinters)) {
            "⚠️ مشكلة طباعة للأوردر #${cloudOrder.orderNumber}"
        } else {
            "🖨️ أوردر جديد من (${cloudOrder.createdByTerminal})"
        }
        postOrderNotification("cashier_order_${cloudOrder.id}", notifTitle, statusMessage)
    }

    fun resolveCanonicalCloudId(item: MenuItem): String {
        return if (item.cloudId.isNotBlank() && item.cloudId != "item_unknown") {
            item.cloudId
        } else {
            "item_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
    }

    fun resolveMenuItemMatch(
        menuItemId: Long?,
        itemName: String,
        unit: String,
        unitPrice: Double,
        allMenu: List<MenuItem>
    ): MenuItem? {
        if (menuItemId != null) {
            allMenu.firstOrNull { it.id == menuItemId }?.let { return it }
        }
        if (itemName.isBlank()) return null
        val normName = repository.normalizeArabicText(itemName)
        return allMenu.firstOrNull {
            repository.normalizeArabicText(it.name) == normName &&
                it.unit.trim().equals(unit.trim(), ignoreCase = true) &&
                kotlin.math.abs(it.basePrice - unitPrice) < 0.01
        } ?: allMenu.firstOrNull {
            repository.normalizeArabicText(it.name) == normName &&
                kotlin.math.abs(it.basePrice - unitPrice) < 0.01
        } ?: allMenu.firstOrNull {
            repository.normalizeArabicText(it.name) == normName &&
                it.unit.trim().equals(unit.trim(), ignoreCase = true)
        } ?: allMenu.firstOrNull {
            repository.normalizeArabicText(it.name) == normName
        }
    }

    suspend fun serializeOrderItemsToJson(items: List<OrderItem>): String {
        val localMenuById = repository.getAllMenuItemsIncludingArchivedList().associateBy { it.id }
        val array = JSONArray()
        for (item in items) {
            val matchedCloudId = item.menuItemId?.let { mid -> localMenuById[mid]?.cloudId }?.takeIf { it.isNotBlank() } ?: ""
            val obj = JSONObject().apply {
                put("id", item.id)
                put("orderId", item.orderId)
                put("menuItemId", item.menuItemId ?: -1L)
                put("menuItemCloudId", matchedCloudId)
                put("itemName", item.itemName)
                put("kitchenName", item.kitchenName)
                put("category", item.category)
                put("unit", item.unit)
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
                put("preparationId", item.preparationId ?: -1L)
                put("preparationName", item.preparationName ?: "")
                put("preparationPrice", item.preparationPrice)
                put("itemNotes", item.itemNotes)
                put("totalPrice", item.totalPrice)
                put("isCustomAdHoc", item.isCustomAdHoc)
                put("targetPrinter", item.targetPrinter)
                put("printedInKitchen", item.printedInKitchen)
                put("printedKitchenQuantity", item.printedKitchenQuantity)
            }
            array.put(obj)
        }
        return array.toString()
    }

    suspend fun deserializeOrderItemsFromJson(jsonStr: String): List<OrderItem> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<OrderItem>()
        val localMenu = repository.getAllMenuItemsIncludingArchivedList()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawItemId = obj.optLong("id", 0L)
                val rawMenuId = obj.optLong("menuItemId", -1L)
                val cloudMenuId = obj.optString("menuItemCloudId", "").trim()
                val rawPrepId = obj.optLong("preparationId", -1L)
                val itemName = obj.optString("itemName", "صنف")
                val unit = obj.optString("unit", "طبق")
                val unitPrice = obj.optDouble("unitPrice", 0.0)
                val isAdHoc = obj.optBoolean("isCustomAdHoc", false)

                val resolvedLocalMenuId = when {
                    isAdHoc -> null
                    cloudMenuId.isNotBlank() -> {
                        localMenu.firstOrNull { it.cloudId == cloudMenuId }?.id
                            ?: resolveMenuItemMatch(
                                menuItemId = if (rawMenuId > 0L) rawMenuId else null,
                                itemName = itemName,
                                unit = unit,
                                unitPrice = unitPrice,
                                allMenu = localMenu
                            )?.id
                            ?: if (rawMenuId > 0L) rawMenuId else null
                    }
                    localMenu.isNotEmpty() -> {
                        resolveMenuItemMatch(
                            menuItemId = null,
                            itemName = itemName,
                            unit = unit,
                            unitPrice = unitPrice,
                            allMenu = localMenu
                        )?.id ?: if (rawMenuId > 0L) rawMenuId else null
                    }
                    else -> if (rawMenuId > 0L) rawMenuId else null
                }

                list.add(
                    OrderItem(
                        id = if (rawItemId > 0L) rawItemId else 0L,
                        orderId = obj.optLong("orderId", 0L),
                        menuItemId = resolvedLocalMenuId,
                        itemName = itemName,
                        kitchenName = obj.optString("kitchenName", ""),
                        category = obj.optString("category", "عام"),
                        unit = unit,
                        quantity = obj.optDouble("quantity", 1.0),
                        unitPrice = unitPrice,
                        preparationId = if (rawPrepId > 0L) rawPrepId else null,
                        preparationName = obj.optString("preparationName", "").ifBlank { null },
                        preparationPrice = obj.optDouble("preparationPrice", 0.0),
                        itemNotes = obj.optString("itemNotes", ""),
                        totalPrice = obj.optDouble("totalPrice", 0.0),
                        isCustomAdHoc = isAdHoc,
                        targetPrinter = obj.optString("targetPrinter", "MAIN"),
                        printedInKitchen = obj.optBoolean("printedInKitchen", false),
                        printedKitchenQuantity = obj.optDouble("printedKitchenQuantity", 0.0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun sanitizeFirestoreDocId(rawId: String?, prefix: String, uniqueSuffix: String = ""): String {
        val cleaned = (rawId ?: "")
            .trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .trim('_')
            .take(120)
        return if (cleaned.isNotBlank() && cleaned != "item_unknown") {
            cleaned
        } else if (uniqueSuffix.isNotBlank()) {
            "${prefix}_${uniqueSuffix.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}".take(120)
        } else {
            "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
    }

    private fun parsePositiveOrderNumberForCloud(rawOrderNumber: String, fallbackOrderId: Long): Int {
        val normalized = buildString {
            for (ch in rawOrderNumber) {
                when (ch) {
                    in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
                    in '۰'..'۹' -> append(('0'.code + (ch.code - '۰'.code)).toChar())
                    in '0'..'9' -> append(ch)
                }
            }
        }
        val parsed = normalized.toIntOrNull() ?: 0
        return if (parsed > 0) parsed else fallbackOrderId.toInt().coerceAtLeast(1)
    }

    private fun normalizeCloudOrderStatus(rawStatus: String): String {
        val upper = rawStatus.trim().uppercase(Locale.ROOT)
        return when (upper) {
            "NEW", "PENDING", "PREPARING", "READY", "OUT_FOR_DELIVERY", "COMPLETED", "CANCELLED" -> upper
            "DONE", "FINISHED", "DELIVERED" -> "COMPLETED"
            "CANCELED" -> "CANCELLED"
            else -> "NEW"
        }
    }

    suspend fun pushRestoredDatabaseToCloud(userId: String): Result<Unit> {
        var currentStage = "تهيئة الرفع"
        return try {
            currentStage = "مسح البيانات القديمة على السحابة"
            cloudRepository.wipeCloudWorkspaceForRestore(userId).getOrThrow()
            val epochMs = System.currentTimeMillis()

            currentStage = "رفع الأقسام"
            val cats = repository.getAllCategoriesList()
            val cloudCats = cats.map { cat ->
                CloudCategory(
                    id = sanitizeFirestoreDocId("cat_${cat.id}", "cat", cat.id.toString()),
                    name = cat.name.trim().ifBlank { "قسم عام" }.take(100),
                    sortOrder = cat.displayOrder,
                    isArchived = cat.isArchived
                )
            }.distinctBy { it.id }
            cloudRepository.batchUpsertCategories(userId, cloudCats, skipExistingCheck = true).getOrThrow()

            currentStage = "رفع الأصناف"
            val allItems = repository.getAllMenuItemsIncludingArchivedList()
            val cloudItems = allItems.map { item ->
                val cid = sanitizeFirestoreDocId(resolveCanonicalCloudId(item), "item", item.id.toString())
                if (item.id > 0L && item.cloudId != cid) {
                    repository.updateMenuItemCloudId(item.id, cid)
                }
                CloudMenuItem(
                    id = cid,
                    name = item.name.trim().ifBlank { "صنف" }.take(150),
                    category = item.category.trim().ifBlank { "عام" }.take(100),
                    price = item.basePrice.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                    costPrice = item.costPrice.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                    stockQuantity = item.currentStock.takeIf { it.isFinite() } ?: 0.0,
                    minStockAlert = item.minStockAlert.takeIf { it.isFinite() } ?: 3.0,
                    unit = item.unit.trim().ifBlank { "وجبة" }.take(50),
                    targetPrinter = item.targetPrinter.trim().ifBlank { "MAIN" }.take(100),
                    kitchenName = item.kitchenName.trim().take(150),
                    supportsPreparation = item.supportsPreparation,
                    isArchived = item.isArchived,
                    isActive = item.isAvailable,
                    displayOrder = item.displayOrder,
                    isPlateWeightEnabled = item.isPlateWeightEnabled,
                    plateWeightKg = item.plateWeightKg.takeIf { it.isFinite() && it >= 0.0 } ?: 0.5,
                    pricePerKg = item.pricePerKg.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                )
            }.distinctBy { it.id }
            cloudRepository.batchUpsertMenuItems(userId, cloudItems, skipExistingCheck = true).getOrThrow()

            currentStage = "رفع الورديات"
            val shifts = repository.getRecentShiftsDirect(30)
            val shiftNumById = shifts.associate { it.id to it.shiftNumber.coerceAtLeast(1) }
            if (shifts.isNotEmpty()) {
                val cloudShifts = shifts.map { s ->
                    val doc = s.toCloudShiftDoc()
                    val safeNum = doc.shiftNumber.coerceAtLeast(1)
                    doc.copy(
                        id = sanitizeFirestoreDocId("shift_$safeNum", "shift", s.id.toString()),
                        shiftNumber = safeNum,
                        cashierName = doc.cashierName.trim().take(100),
                        openedAtMs = doc.openedAtMs.coerceAtLeast(0L),
                        closedAtMs = doc.closedAtMs.coerceAtLeast(0L),
                        notes = doc.notes.trim().take(1000)
                    )
                }.distinctBy { it.id }
                cloudRepository.batchUpsertShifts(userId, cloudShifts, skipExistingCheck = true).getOrThrow()
            }

            currentStage = "رفع الأوردرات"
            val allOrders = repository.getAllOrdersDirect()
                .filter { it.order.syncState != SYNC_STATE_PENDING_OFFLINE && it.order.orderNumber.startsWith("#") }
            val unarchivedOrders = allOrders.filter { !it.order.isArchived }
            val recentOrders = allOrders.sortedByDescending { it.order.createdAt }.take(150)
            val targetOrders = (unarchivedOrders + recentOrders).distinctBy { it.order.id }
            if (targetOrders.isNotEmpty()) {
                val cloudOrders = targetOrders.map { owi ->
                    val ord = owi.order
                    val cleanNum = parsePositiveOrderNumberForCloud(ord.orderNumber, ord.id)
                    val rawCid = ord.cloudOrderId?.takeIf { it.isNotBlank() }
                        ?: "order_${ord.createdAt.coerceAtLeast(0L)}_${cleanNum}_${ord.id}"
                    val cid = sanitizeFirestoreDocId(rawCid, "order", "${cleanNum}_${ord.id}")
                    if (ord.cloudOrderId != cid) {
                        repository.updateCloudOrderId(ord.id, cid)
                    }
                    val itemsJsonRaw = serializeOrderItemsToJson(owi.items)
                    val safeItemsJson = if (itemsJsonRaw.length <= 450_000) itemsJsonRaw else "[]"
                    CloudOrderDoc(
                        id = cid,
                        orderNumber = cleanNum,
                        customerName = ord.customerName.trim().take(150),
                        customerPhone = ord.customerPhone.trim().take(50),
                        customerSecondaryPhone = ord.customerSecondaryPhone.orEmpty().trim().take(50),
                        customerArea = ord.customerArea.orEmpty().trim().take(150),
                        deliveryAddressTitle = ord.deliveryAddressTitle.orEmpty().trim().take(150),
                        deliveryAddressFull = ord.deliveryAddressFull.orEmpty().trim().take(500),
                        orderType = ord.orderType.trim().ifBlank { "TAKEAWAY" }.take(50),
                        status = normalizeCloudOrderStatus(ord.status),
                        printStatus = "PRINTED",
                        requestedPrinterTarget = "",
                        reprintReason = "",
                        isArchived = ord.isArchived,
                        unarchiveTimestamp = if (!ord.isArchived) epochMs else 0L,
                        shiftNumber = ord.shiftId?.let { shiftNumById[it] } ?: 0,
                        itemsJson = safeItemsJson,
                        subtotal = ord.subtotal.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                        deliveryFee = ord.deliveryFee.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                        discount = ord.discount.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                        total = ord.totalAmount.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                        paymentMethod = ord.paymentMethod.trim().ifBlank { "CASH" }.take(50),
                        notes = ord.notes.trim().take(1000),
                        driverName = ord.driverName.orEmpty().trim().take(150),
                        createdByTerminal = ord.sellerName.trim().ifBlank { "كاشير رئيسي" }.take(100),
                        printedByTerminal = "نسخة مستعادة",
                        createdAtMs = ord.createdAt.coerceAtLeast(0L)
                    )
                }.distinctBy { it.id }
                cloudRepository.batchUpsertOrders(userId, cloudOrders, skipExistingCheck = true).getOrThrow()
            }

            currentStage = "تحديث العداد وبصمة الاستعادة"
            val maxNum = repository.getNextOrderNumber()
            cloudRepository.setCloudOrderNumberOverride(userId, maxNum).getOrThrow()
            repository.saveSetting("local_restore_epoch", epochMs.toString())
            repository.saveSetting("pending_cloud_supreme_restore", "")
            cloudRepository.publishRestoreEpoch(
                userId = userId,
                epochMs = epochMs,
                restoredBy = activeSellerName.value.ifBlank { "كاشير رئيسي" }.take(100)
            ).getOrThrow()
            registerTerminalWithPrinters(userId)

            lastCloudSyncTime.value = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault()).format(Date())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "pushRestoredDatabaseToCloud error at [$currentStage]", e)
            Result.failure(Exception("[$currentStage]: ${e.localizedMessage ?: e.message ?: "خطأ غير معروف"}", e))
        }
    }
}
