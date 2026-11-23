package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.CustomerAddress
import com.example.data.model.DeliveryArea
import com.example.data.model.DeliveryDriver
import com.example.data.model.DriverSettlement
import com.example.data.model.Employee
import com.example.data.model.EmployeeAttendance
import com.example.data.model.EmployeeTransaction
import com.example.data.model.InventoryMovement
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.model.TodayItemSalesStat
import com.example.data.model.TodaySalesSummary
import com.example.data.model.SalesBucketStat
import com.example.data.model.PreparationType
import com.example.data.model.AccountTransaction
import com.example.data.model.GeneralAccount
import com.example.data.model.MenuItemIngredient
import com.example.data.model.PrintLog
import com.example.data.model.Shift
import org.json.JSONObject
import com.example.data.repository.AdvancedSalesReport
import com.example.data.repository.DriverAccountStatement
import com.example.data.repository.EmployeeStatement
import com.example.data.repository.IngredientCheckResult
import com.example.data.repository.OrderPlacementResult
import com.example.data.repository.RestaurantRepository
import com.example.data.model.CloudCategory
import com.example.data.model.CloudMenuItem
import com.example.data.model.CloudOrderChange
import com.example.data.model.CloudOrderDoc
import com.example.data.model.CloudShiftDoc
import com.example.data.model.TerminalDoc
import com.example.data.repository.FirestoreCloudRepository
import com.example.sync.AppSyncCoordinator
import com.example.util.CloudAuthManager
import com.example.util.SoundAlertUtil
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.example.printer.DiscoveredPrinter
import com.example.printer.InvoiceTemplateConfig
import com.example.printer.KitchenOrderTemplateConfig
import com.example.printer.NetworkPrinterScanner
import com.example.printer.PrinterConfig
import com.example.printer.PrinterResult
import com.example.printer.ReceiptBitmapRenderer
import com.example.printer.ReceiptPrintDocumentAdapter
import com.example.receiver.CallStateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class NavScreen(val titleArabic: String) {
    POS("نقطة البيع"),
    CALLS_CUSTOMERS("العملاء"),
    PRICES_STOCK("الأسعار والمخزون"),
    ORDERS("الأوردرات والفواتير"),
    DRIVERS_STAFF("الحسابات العامة والورديات"),
    REPORTS_SETTINGS("لوحة الإعدادات الشاملة")
}

data class CartItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val menuItem: MenuItem?,
    val name: String,
    val kitchenName: String = "",
    val category: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val preparation: PreparationType? = null,
    val notes: String = "",
    val isCustomAdHoc: Boolean = false,
    val targetPrinter: String = "MAIN",
    val orderItemId: Long? = null,
    val printedKitchenQuantity: Double = 0.0
) {
    val effectivePrepPrice: Double get() {
        val basePrep = preparation?.additionalPricePerUnit ?: 0.0
        val item = menuItem
        return if (item != null && item.unit == "طبق" && item.isPlateWeightEnabled && item.plateWeightKg > 0.0) {
            basePrep * item.plateWeightKg
        } else {
            basePrep
        }
    }
    val prepPrice: Double get() = effectivePrepPrice
    val total: Double get() = (unitPrice + prepPrice) * quantity
}

data class LiveDrawerInfo(
    val openingBalance: Double = 0.0,
    val storeCashSales: Double = 0.0,
    val driverSettlementsCash: Double = 0.0,
    val totalReceipts: Double = 0.0,
    val totalStaffWithdrawals: Double = 0.0,
    val totalExpensesAndPayments: Double = 0.0,
    val walletSales: Double = 0.0,
    val currentDrawerBalance: Double = 0.0,
    val ordersCount: Int = 0
)

data class CallerLookupState(
    val phoneNumber: String,
    val customer: Customer?,
    val addresses: List<CustomerAddress>,
    val recentOrders: List<OrderWithItems>,
    val isVisible: Boolean = false
)

data class StockOverridePrompt(
    val itemName: String,
    val requested: Double,
    val available: Double,
    val pendingAction: () -> Unit
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val coordinator = AppSyncCoordinator.getInstance(application)
    private val repository = coordinator.repository
    val cloudRepository = coordinator.cloudRepository

    val cloudUser: StateFlow<FirebaseUser?> = coordinator.cloudUser
    val isCloudConnected: StateFlow<Boolean> = coordinator.isCloudConnected
    val isCloudSyncing: StateFlow<Boolean> = coordinator.isCloudSyncing
    val lastCloudSyncTime: StateFlow<String?> = coordinator.lastCloudSyncTime
    val isRestoringBackupSyncingCloud: StateFlow<Boolean> = coordinator.isRestoringBackupSyncingCloud
    val remoteSyncedPrinters: StateFlow<List<PrinterConfig>> = coordinator.remoteSyncedPrinters

    val backgroundServiceEnabled: StateFlow<Boolean> = coordinator.backgroundServiceEnabled
    val isDeviceOnline: StateFlow<Boolean> = coordinator.isDeviceOnline
    val pendingSyncOrdersCount: StateFlow<Int> = coordinator.pendingSyncOrdersCount

    private val recentLocalArchiveOverrides get() = coordinator.recentLocalArchiveOverrides
    private val localMenuTombstones get() = coordinator.localMenuTombstones
    private val lastObservedCloudMenuItems get() = coordinator.lastObservedCloudMenuItems
    private var isCloudSyncPausedForRestore: Boolean
        get() = coordinator.isCloudSyncPausedForRestore
        set(value) { coordinator.isCloudSyncPausedForRestore = value }

    val currentScreen = MutableStateFlow(NavScreen.POS)
    val selectedAccountsTab = MutableStateFlow(0)

    fun openAttendanceBoard() {
        selectedAccountsTab.value = 0
        currentScreen.value = NavScreen.DRIVERS_STAFF
    }

    // Data streams
    val categories: StateFlow<List<MenuCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategoriesIncludingArchived: StateFlow<List<MenuCategory>> = repository.allCategoriesIncludingArchived
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val menuItems: StateFlow<List<MenuItem>> = repository.allMenuItems
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allMenuItemsIncludingArchived: StateFlow<List<MenuItem>> = repository.allMenuItemsIncludingArchived
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val preparations: StateFlow<List<PreparationType>> = repository.allPreparations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPreparationsIncludingInactive: StateFlow<List<PreparationType>> = repository.allPreparationsIncludingInactive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderWithItems>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val showTodaySalesDialog = MutableStateFlow(false)

    val selectedSalesPeriod = MutableStateFlow(com.example.data.model.SalesPeriodType.TODAY)
    val customSalesStartDate = MutableStateFlow<Long?>(null)
    val customSalesEndDate = MutableStateFlow<Long?>(null)

    val todaySalesSummary: StateFlow<TodaySalesSummary> = combine(
        orders,
        selectedSalesPeriod,
        customSalesStartDate,
        customSalesEndDate
    ) { localOrders, period, customStart, customEnd ->
        calculateSalesSummaryForPeriod(localOrders, period, customStart, customEnd)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TodaySalesSummary()
    )

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drivers: StateFlow<List<DeliveryDriver>> = repository.allDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDriversIncludingArchived: StateFlow<List<DeliveryDriver>> = repository.allDriversIncludingArchived
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settlements: StateFlow<List<DriverSettlement>> = repository.allSettlements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEmployeesIncludingArchived: StateFlow<List<Employee>> = repository.allEmployeesIncludingArchived
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<EmployeeAttendance>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAttendance: StateFlow<List<EmployeeAttendance>> = repository.allAttendance
        .map { list ->
            val calToday = java.util.Calendar.getInstance()
            val curYear = calToday.get(java.util.Calendar.YEAR)
            val curDay = calToday.get(java.util.Calendar.DAY_OF_YEAR)
            val calRec = java.util.Calendar.getInstance()
            list.filter { att ->
                calRec.timeInMillis = att.date
                calRec.get(java.util.Calendar.YEAR) == curYear &&
                calRec.get(java.util.Calendar.DAY_OF_YEAR) == curDay
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allPrinters = MutableStateFlow<List<PrinterConfig>>(emptyList())
    val allPrinters: StateFlow<List<PrinterConfig>> = _allPrinters.asStateFlow()

    val employeeTransactions: StateFlow<List<EmployeeTransaction>> = repository.allEmployeeTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryMovements: StateFlow<List<InventoryMovement>> = repository.allInventoryMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val printLogs: StateFlow<List<PrintLog>> = repository.allPrintLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveryAreas: StateFlow<List<DeliveryArea>> = repository.allActiveDeliveryAreas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeliveryAreas: StateFlow<List<DeliveryArea>> = repository.allDeliveryAreas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShift: StateFlow<Shift?> = repository.activeShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentlyClosedShift = MutableStateFlow<Shift?>(null)

    fun clearRecentlyClosedShift() {
        recentlyClosedShift.value = null
    }

    val allShifts: StateFlow<List<Shift>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIngredients: StateFlow<List<MenuItemIngredient>> = repository.allIngredients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<GeneralAccount>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccountsIncludingArchived: StateFlow<List<GeneralAccount>> = repository.allAccountsIncludingArchived
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccountTransactions: StateFlow<List<AccountTransaction>> = repository.allAccountTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveDrawerInfo: StateFlow<LiveDrawerInfo> = combine(
        activeShift,
        orders,
        settlements,
        employeeTransactions,
        allAccountTransactions
    ) { shift, orderList, settlementList, staffTxList, accountTxList ->
        try {
            if (shift == null) {
                LiveDrawerInfo()
            } else {
                val shiftId = shift.id
                val startTime = shift.openedAt
                val endTime = shift.closedAt ?: Long.MAX_VALUE

                // 1. Shift orders
                val shiftOrders = orderList.filter {
                    (it.order.shiftId == shiftId || (it.order.shiftId == null && it.order.createdAt >= startTime && it.order.createdAt <= endTime)) &&
                    it.order.status != "CANCELLED"
                }
                val storeCashOrders = shiftOrders.filter { 
                    it.order.paymentMethod == "CASH" && 
                    it.order.orderType != "DELIVERY" &&
                    it.order.status == "COMPLETED"
                }
                val storeCash = storeCashOrders.sumOf { it.order.totalAmount }

                val walletOrders = shiftOrders.filter { 
                    (it.order.paymentMethod == "WALLET" || it.order.paymentMethod == "CARD") &&
                    (it.order.orderType == "DELIVERY" || it.order.status == "COMPLETED")
                }
                val walletSales = walletOrders.sumOf { it.order.totalAmount }

                // 2. Driver settlements
                val shiftSettlements = settlementList.filter {
                    (it.shiftId == shiftId || (it.shiftId == null && it.date >= startTime && it.date <= endTime))
                }
                val deliveryCash = shiftSettlements.sumOf {
                    if (it.actualHandoverAmount > 0.0) it.actualHandoverAmount else it.netAmountToHandover
                }

                // 3. Staff withdrawals
                val shiftStaffTxs = staffTxList.filter {
                    it.isDeductedFromDrawer &&
                    (it.shiftId == shiftId || (it.shiftId == null && it.date >= startTime && it.date <= endTime)) &&
                    (it.type == "ADVANCE" || it.type == "SALARY_PAYOUT")
                }
                val staffOut = shiftStaffTxs.sumOf { it.amount }

                // 4. Account transactions
                val shiftAccTxs = accountTxList.filter {
                    it.isPaidFromDrawer &&
                    (it.shiftId == shiftId || (it.shiftId == null && it.date >= startTime && it.date <= endTime))
                }
                val receipts = shiftAccTxs.filter { it.type == "RECEIPT" }.sumOf { it.amount }
                val payments = shiftAccTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }

                val totalIncoming = shift.openingBalance + storeCash + deliveryCash + receipts
                val totalOutgoing = staffOut + payments
                val currentBalance = totalIncoming - totalOutgoing

                LiveDrawerInfo(
                    openingBalance = shift.openingBalance,
                    storeCashSales = storeCash,
                    driverSettlementsCash = deliveryCash,
                    totalReceipts = receipts,
                    totalStaffWithdrawals = staffOut,
                    totalExpensesAndPayments = payments,
                    walletSales = walletSales,
                    currentDrawerBalance = currentBalance,
                    ordersCount = shiftOrders.size
                )
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainViewModel", "Error in liveDrawerInfo", t)
            LiveDrawerInfo()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LiveDrawerInfo())

    val advancedSalesReport = MutableStateFlow<AdvancedSalesReport?>(null)

    // Cart state (only TAKEAWAY and DELIVERY; no Dine-in, no Card)
    val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val editingOrderId = MutableStateFlow<Long?>(null)
    val editingOrderNumber = MutableStateFlow<String?>(null)
    val orderType = MutableStateFlow("TAKEAWAY") // "DELIVERY", "TAKEAWAY"
    val selectedCustomer = MutableStateFlow<Customer?>(null)
    val customerAddresses = MutableStateFlow<List<CustomerAddress>>(emptyList())
    val selectedAddress = MutableStateFlow<CustomerAddress?>(null)
    val selectedDriver = MutableStateFlow<DeliveryDriver?>(null)
    val deliveryFee = MutableStateFlow(20.0)
    val discount = MutableStateFlow(0.0)
    val isRoundToFiveEnabled = MutableStateFlow(false)
    val paymentMethod = MutableStateFlow("CASH") // "CASH", "WALLET"
    val orderNotes = MutableStateFlow("")
    val orderPaidAmount = MutableStateFlow(0.0) // المبلغ المدفوع من الزبون
    val orderIsPaid = MutableStateFlow(false) // هل الأوردر خالص بالكامل

    // Direct customer fields editable in order before printing
    val orderCustomerName = MutableStateFlow("")
    val orderCustomerPhone = MutableStateFlow("")
    val orderCustomerSecondaryPhone = MutableStateFlow("")
    val orderCustomerArea = MutableStateFlow("")
    val orderCustomerStreet = MutableStateFlow("")
    val saveCustomerPermanently = MutableStateFlow(true)

    // Caller ID state
    private val _callerState = MutableStateFlow<CallerLookupState?>(null)
    val callerState: StateFlow<CallerLookupState?> = _callerState.asStateFlow()

    // Alerts and Overrides
    private val _stockOverridePrompt = MutableStateFlow<StockOverridePrompt?>(null)
    val stockOverridePrompt: StateFlow<StockOverridePrompt?> = _stockOverridePrompt.asStateFlow()

    val userMessage = MutableStateFlow<String?>(null)
    val snackbarEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val dailyPriceReviewNeeded = MutableStateFlow(false)

    private fun postQueuedMessage(msg: String) {
        if (!snackbarEvents.tryEmit(msg)) {
            userMessage.value = msg
        }
    }

    internal fun buildFailedPrintersLabel(failedNames: List<String>, totalAttempted: Int): String {
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

    // Print Diagnostics State (Compact & Essential info only)
    val printDiagnosticStatus = MutableStateFlow<String?>(null)
    val isPrintingOrder = MutableStateFlow(false)
    val isSubmittingOrderState = MutableStateFlow(false)
    val isLandscapeMode = MutableStateFlow(false)
    private var isSavingOrderEdits = false
    private var isSubmittingOrder = false
    private var printDiagnosticJob: kotlinx.coroutines.Job? = null

    fun setPrintDiagnostic(status: String?, autoClearSeconds: Long = 4) {
        printDiagnosticJob?.cancel()
        printDiagnosticStatus.value = status
        if (status != null && autoClearSeconds > 0) {
            printDiagnosticJob = viewModelScope.launch {
                kotlinx.coroutines.delay(autoClearSeconds * 1000)
                printDiagnosticStatus.value = null
            }
        }
    }

    // Network Printer Auto-Discovery State
    val discoveredNetworkPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val isScanningNetworkPrinters = MutableStateFlow(false)

    // 🛡️ Local POS Terminal State (Synchronized via AppSyncCoordinator)
    val deviceRole: StateFlow<String> = coordinator.deviceRole
    val activeSellerName: StateFlow<String> = coordinator.activeSellerName
    val showRoleSwitcherDialog = MutableStateFlow(false)
    val cashierLeaseConflictDialogMessage = MutableStateFlow<String?>(null)
    val isPrimaryCashierActiveLeaseHolder = MutableStateFlow(true)

    fun hasActiveCashierAuthority(): Boolean = true
    fun applyDeviceRole(newRole: String, requestedName: String = "") {
        coordinator.applyDeviceRole(newRole, requestedName)
    }
    fun setDeviceRole(newRole: String) {
        coordinator.setDeviceRole(newRole)
    }
    fun dismissCashierLeaseConflictDialog() { cashierLeaseConflictDialogMessage.value = null }
    fun setActiveSellerName(newName: String) {
        coordinator.setActiveSellerName(newName)
    }
    fun toggleRoleSwitcherDialog() {
        showRoleSwitcherDialog.value = !showRoleSwitcherDialog.value
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        coordinator.setBackgroundServiceEnabled(enabled)
    }

    fun triggerManualOfflineFlush() {
        val uid = cloudUser.value?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            coordinator.flushPendingOfflineSellerOrders(uid)
        }
    }

    fun serializeOrderItemsToJson(items: List<OrderItem>): String {
        val localMenuById = menuItems.value.associateBy { it.id }
        val array = org.json.JSONArray()
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

    fun deserializeOrderItemsFromJson(jsonStr: String): List<OrderItem> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<OrderItem>()
        val localMenu = menuItems.value
        try {
            val array = org.json.JSONArray(jsonStr)
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

    fun serializeMenuItemToJson(item: MenuItem): String {
        val obj = JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("category", item.category)
            put("unit", item.unit)
            put("basePrice", item.basePrice)
            put("costPrice", item.costPrice)
            put("currentStock", item.currentStock)
            put("minStockAlert", item.minStockAlert)
            put("targetPrinter", item.targetPrinter)
            put("kitchenName", item.kitchenName)
            put("isAvailable", item.isAvailable)
            put("isArchived", item.isArchived)
            put("displayOrder", item.displayOrder)
            put("supportsPreparation", item.supportsPreparation)
            put("isPlateWeightEnabled", item.isPlateWeightEnabled)
            put("plateWeightKg", item.plateWeightKg)
            put("pricePerKg", item.pricePerKg)
            put("cloudId", item.cloudId)
        }
        return obj.toString()
    }

    fun deserializeMenuItemFromJson(jsonStr: String): MenuItem? {
        if (jsonStr.isBlank()) return null
        return try {
            val obj = JSONObject(jsonStr)
            MenuItem(
                id = obj.optLong("id", 0L),
                name = obj.optString("name", ""),
                category = obj.optString("category", "عام"),
                unit = obj.optString("unit", "كيلو"),
                basePrice = obj.optDouble("basePrice", 0.0),
                costPrice = obj.optDouble("costPrice", 0.0),
                currentStock = obj.optDouble("currentStock", 0.0),
                minStockAlert = obj.optDouble("minStockAlert", 3.0),
                targetPrinter = obj.optString("targetPrinter", "MAIN"),
                kitchenName = obj.optString("kitchenName", ""),
                isAvailable = obj.optBoolean("isAvailable", true),
                isArchived = obj.optBoolean("isArchived", false),
                displayOrder = obj.optInt("displayOrder", 0),
                supportsPreparation = obj.optBoolean("supportsPreparation", false),
                isPlateWeightEnabled = obj.optBoolean("isPlateWeightEnabled", false),
                plateWeightKg = obj.optDouble("plateWeightKg", 0.5),
                pricePerKg = obj.optDouble("pricePerKg", 0.0),
                cloudId = obj.optString("cloudId", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun serializePriceUpdatesToJson(updates: Map<Long, Double>): String {
        val obj = JSONObject()
        for ((id, price) in updates) {
            obj.put(id.toString(), price)
        }
        return obj.toString()
    }

    fun deserializePriceUpdatesFromJson(jsonStr: String): Map<Long, Double> {
        if (jsonStr.isBlank()) return emptyMap()
        val map = mutableMapOf<Long, Double>()
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = key.toLongOrNull() ?: continue
                map[id] = obj.optDouble(key, 0.0)
            }
        } catch (_: Exception) {}
        return map
    }

    // Last Submitted Order for post-order WhatsApp/Print modal
    private val _lastSubmittedOrder = MutableStateFlow<OrderWithItems?>(null)
    val lastSubmittedOrder: StateFlow<OrderWithItems?> = _lastSubmittedOrder.asStateFlow()

    fun dismissLastSubmittedOrder() {
        _lastSubmittedOrder.value = null
    }

    fun clearLastSubmittedOrder() {
        _lastSubmittedOrder.value = null
    }

    // Explicit Invoice / Print modal trigger for Orders Screen
    private val _requestedInvoiceOrder = MutableStateFlow<OrderWithItems?>(null)
    val requestedInvoiceOrder: StateFlow<OrderWithItems?> = _requestedInvoiceOrder.asStateFlow()

    fun openOrderInvoice(orderWithItems: OrderWithItems) {
        _requestedInvoiceOrder.value = orderWithItems
    }

    fun clearRequestedInvoiceOrder() {
        _requestedInvoiceOrder.value = null
    }

    // Highlighted Order ID for navigation from POS to Orders screen
    val highlightedOrderId = MutableStateFlow<Long?>(null)
    val orderVisibilityPulse = MutableStateFlow(0L)

    fun clearHighlightedOrder() {
        highlightedOrderId.value = null
    }

    // Owner WhatsApp Phone State
    val ownerWhatsAppPhone = MutableStateFlow("")

    fun clearPrintDiagnosticStatus() {
        printDiagnosticJob?.cancel()
        printDiagnosticStatus.value = null
    }

    // Active Statements State
    val activeEmployeeStatement = MutableStateFlow<EmployeeStatement?>(null)
    val activeDriverStatement = MutableStateFlow<DriverAccountStatement?>(null)

    // Google Drive & Daily Backup states
    val lastBackupInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val isAutoBackupEnabled = MutableStateFlow(true)
    val isBackingUp = MutableStateFlow(false)

    // Category Custom Colors (Key: Category Name, Value: Preset Theme ID e.g. "TEAL", "ORANGE", "BLUE", etc.)
    val categoryColors = MutableStateFlow<Map<String, String>>(emptyMap())

    fun loadCategoryColors() {
        viewModelScope.launch {
            val json = repository.getSetting("category_colors_json")
            if (!json.isNullOrBlank()) {
                try {
                    val obj = JSONObject(json)
                    val map = mutableMapOf<String, String>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = obj.getString(k)
                    }
                    categoryColors.value = map
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error parsing category colors", e)
                }
            }
        }
    }

    fun setCategoryColor(categoryName: String, themeId: String) {
        viewModelScope.launch {
            val updated = categoryColors.value.toMutableMap()
            updated[categoryName.trim()] = themeId
            categoryColors.value = updated
            try {
                val obj = JSONObject()
                for ((k, v) in updated) {
                    obj.put(k, v)
                }
                repository.saveSetting("category_colors_json", obj.toString())
                userMessage.value = "تم تحديد لون قسم \"$categoryName\" بنجاح ✓"
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error saving category colors", e)
            }
        }
    }

    init {
        viewModelScope.launch {
            try {
                repository.initializeDefaultDataIfEmpty()
                repository.deduplicateMenuItems()
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "initializeDefaultDataIfEmpty error", t)
            }
            try {
                repository.syncCustomerStats()
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "syncCustomerStats error", t)
            }
            try {
                repository.deduplicateOrders()
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "deduplicateOrders error", t)
            }
            try {
                loadNextOrderNumber()
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "loadNextOrderNumber error", t)
            }
            try {
                checkDailyPriceReminder()
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "checkDailyPriceReminder error", t)
            }
            try {
                loadOwnerWhatsAppPhone()
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "loadOwnerWhatsAppPhone error", t)
            }
            try {
                loadPrinters()
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "loadPrinters error", t)
            }
            try {
                loadCategoryColors()
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "loadCategoryColors error", t)
            }
            // Daily Backup check
            try {
                isAutoBackupEnabled.value = com.example.util.BackupManager.isAutoBackupEnabled(getApplication())
                lastBackupInfo.value = com.example.util.BackupManager.getLastBackupInfo(getApplication())
                val autoStats = com.example.util.BackupManager.checkAndPerformDailyAutoBackup(getApplication())
                if (autoStats != null) {
                    lastBackupInfo.value = com.example.util.BackupManager.getLastBackupInfo(getApplication())
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Backup check error", e)
            }
        }

        // Observe incoming phone calls
        viewModelScope.launch {
            try {
                CallStateReceiver.incomingCallFlow.collect { callEvent ->
                    if (callEvent != null) {
                        handleIncomingCall(callEvent.phoneNumber)
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "incomingCallFlow error", t)
            }
        }

        // Wire single app-scoped sync coordinator (zero duplicate Firestore listeners)
        coordinator.ensureStarted()

        viewModelScope.launch {
            coordinator.snackbarEvents.collect { msg ->
                postQueuedMessage(msg)
            }
        }
        viewModelScope.launch {
            coordinator.printDiagnosticEvents.collect { (diag, secs) ->
                setPrintDiagnostic(diag, autoClearSeconds = secs)
            }
        }
        viewModelScope.launch {
            coordinator.isPrintingFromCloud.collect { printing ->
                isPrintingOrder.value = printing
            }
        }
        viewModelScope.launch {
            coordinator.orderVisibilityEvents.collect { localId ->
                if (localId != null) {
                    highlightedOrderId.value = localId
                    orderVisibilityPulse.value += 1L
                }
            }
        }
        viewModelScope.launch {
            coordinator.nextOrderNumberSynced.collect { syncedNext ->
                if (syncedNext != null && syncedNext > 0) {
                    nextOrderNumber.value = syncedNext
                }
            }
        }
    }

    private suspend fun checkDailyPriceReminder() {
        val lastDate = repository.getSetting("lastPriceUpdateDate") ?: ""
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (lastDate != todayStr) {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour >= 7) {
                dailyPriceReviewNeeded.value = true
            }
        }
    }

    fun dismissDailyPriceReminder() {
        dailyPriceReviewNeeded.value = false
    }

    private fun Shift.toCloudShiftDoc(): CloudShiftDoc = with(coordinator) {
        toCloudShiftDoc()
    }

    private suspend fun enqueuePendingCloudArchive(entries: Map<String, Boolean>) =
        coordinator.enqueuePendingCloudArchive(entries)

    private suspend fun clearPendingCloudArchive(cloudIds: Collection<String>) =
        coordinator.clearPendingCloudArchive(cloudIds)

    private suspend fun clearAllSyncQueuesForSupremeRestore() =
        coordinator.clearAllSyncQueuesForSupremeRestore()

    private suspend fun ensureMenuDeleteStateLoaded() =
        coordinator.ensureMenuDeleteStateLoaded()

    private suspend fun recordMenuItemTombstones(cloudIds: Collection<String>, enqueueCloudDelete: Boolean) =
        coordinator.recordMenuItemTombstones(cloudIds, enqueueCloudDelete)

    private suspend fun clearPendingMenuDeletes(cloudIds: Collection<String>) =
        coordinator.clearPendingMenuDeletes(cloudIds)

    private suspend fun flushPendingMenuDeletesAndCounter(userId: String) =
        coordinator.flushPendingMenuDeletesAndCounter(userId)

    fun startCloudSyncObservers(userId: String, forceRestart: Boolean = false) =
        coordinator.startCloudSyncObservers(userId, forceRestart)

    fun stopCloudSyncObservers() =
        coordinator.stopCloudSyncObservers()

    internal suspend fun syncCloudMenuItemsToLocal(cloudItems: List<CloudMenuItem>) =
        coordinator.syncCloudMenuItemsToLocal(cloudItems)

    private fun extractPrinterErrorMessage(result: PrinterResult?): String? {
        return when (result) {
            is PrinterResult.Success -> null
            is PrinterResult.Error -> result.errorMessage
            is PrinterResult.PermissionRequired -> "يلزم منح إذن البلوتوث/الـ USB للطابعة"
            null -> "انتهت مهلة الاتصال بالطابعة"
        }
    }

    private fun resolveCanonicalCloudId(item: MenuItem): String =
        coordinator.resolveCanonicalCloudId(item)

    private fun resolveMenuItemMatch(
        menuItemId: Long?,
        itemName: String,
        unit: String,
        unitPrice: Double,
        allMenu: List<MenuItem>
    ): MenuItem? = coordinator.resolveMenuItemMatch(menuItemId, itemName, unit, unitPrice, allMenu)

    fun pushMenuItemToCloud(item: MenuItem) {
        val user = cloudUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ensureMenuDeleteStateLoaded()
                val cloudId = resolveCanonicalCloudId(item)
                if (localMenuTombstones.containsKey(cloudId)) {
                    if (item.id > 0L) repository.deleteMenuItem(item.id)
                    return@launch
                }
                if (item.id > 0L && item.cloudId != cloudId) {
                    repository.updateMenuItemCloudId(item.id, cloudId)
                }
                val cloudItem = CloudMenuItem(
                    id = cloudId,
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
                cloudRepository.upsertMenuItem(user.uid, cloudItem)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "pushMenuItemToCloud error", e)
            }
        }
    }

    fun pushCategoryToCloud(cat: MenuCategory) {
        val user = cloudUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cloudCat = CloudCategory(
                    id = "cat_${cat.id}",
                    name = cat.name,
                    sortOrder = cat.displayOrder,
                    isArchived = cat.isArchived
                )
                cloudRepository.upsertCategory(user.uid, cloudCat)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "pushCategoryToCloud error", e)
            }
        }
    }

    fun syncAllLocalMenuToCloud() {
        val user = cloudUser.value
        if (user == null) {
            userMessage.value = "يرجى ربط وتسجيل الدخول بحساب المطعم أولاً لتمكين المزامنة"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            coordinator.isCloudSyncing.value = true
            try {
                if (deviceRole.value == "CASHIER" && repository.getSetting("pending_cloud_supreme_restore") == "1") {
                    coordinator.isRestoringBackupSyncingCloud.value = true
                    val pushRes = try {
                        withTimeoutOrNull(120_000L) {
                            pushRestoredDatabaseToCloud(user.uid)
                        } ?: Result.failure(Exception("انتهت مهلة الرفع للسحابة (120 ثانية)"))
                    } finally {
                        coordinator.isRestoringBackupSyncingCloud.value = false
                    }
                    withContext(Dispatchers.Main) {
                        if (pushRes.isSuccess) {
                            userMessage.value = "☁️ تم فرض النسخة الاحتياطية المستعادة على القاعدة المشتركة بالكامل ✓"
                        } else {
                            val errMsg = pushRes.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"
                            userMessage.value = "⚠️ تعذر فرض النسخة المستعادة على السحابة: $errMsg"
                        }
                    }
                    if (pushRes.isSuccess) {
                        startCloudSyncObservers(user.uid, forceRestart = true)
                    }
                    return@launch
                }
                flushPendingMenuDeletesAndCounter(user.uid)
                val cloudTombstones = cloudRepository.getDeletedMenuItemIdsOnce(user.uid)
                if (cloudTombstones.isNotEmpty()) {
                    recordMenuItemTombstones(cloudTombstones.keys, enqueueCloudDelete = false)
                }
                coordinator.purgeLocalItemsMatchingTombstones()

                val currentItems = repository.getAllMenuItemsIncludingArchivedList()
                val cloudItems = mutableListOf<CloudMenuItem>()
                for (item in currentItems) {
                    val cloudId = resolveCanonicalCloudId(item)
                    if (localMenuTombstones.containsKey(cloudId)) continue
                    if (item.id > 0L && item.cloudId != cloudId) {
                        repository.updateMenuItemCloudId(item.id, cloudId)
                    }
                    cloudItems.add(
                        CloudMenuItem(
                            id = cloudId,
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
                    )
                }
                cloudRepository.batchUpsertMenuItems(user.uid, cloudItems)

                val cats = repository.getAllCategoriesList()
                val cloudCats = cats.map { cat ->
                    CloudCategory(
                        id = "cat_${cat.id}",
                        name = cat.name,
                        sortOrder = cat.displayOrder,
                        isArchived = cat.isArchived
                    )
                }
                cloudRepository.batchUpsertCategories(user.uid, cloudCats)

                val localNext = repository.getNextOrderNumber()
                cloudRepository.syncMaxOrderNumber(user.uid, localNext)

                withContext(Dispatchers.Main) {
                    coordinator.lastCloudSyncTime.value = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault()).format(Date())
                    userMessage.value = "تمت مزامنة ورفع ${cloudItems.size} صنف و${cats.size} قسم والرقم التسلسلي للأوردرات إلى السحابة بنجاح ✓"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    userMessage.value = "خطأ في المزامنة: ${e.localizedMessage ?: "فشل الرفع للسحابة"}"
                }
            } finally {
                coordinator.isCloudSyncing.value = false
            }
        }
    }

    fun handleIncomingCall(phone: String) {
        viewModelScope.launch {
            try {
                if (phone.isBlank()) return@launch
                val (cust, recentOrders) = repository.lookupCustomerByPhone(phone)
                val addrs = if (cust != null) repository.getAddressesForCustomer(cust.id) else emptyList()
                _callerState.value = CallerLookupState(
                    phoneNumber = phone,
                    customer = cust,
                    addresses = addrs,
                    recentOrders = recentOrders,
                    isVisible = true
                )
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "handleIncomingCall error", t)
            }
        }
    }

    fun dismissCallerDialog() {
        _callerState.value = null
        CallStateReceiver.clearCallEvent()
    }

    fun startNewOrderForCaller(customer: Customer?, phone: String, address: CustomerAddress?) {
        selectedCustomer.value = customer
        orderCustomerName.value = customer?.name ?: ""
        orderCustomerPhone.value = customer?.primaryPhone ?: phone
        orderCustomerArea.value = address?.area ?: ""
        orderCustomerStreet.value = address?.streetDetails ?: ""
        if (customer != null) {
            viewModelScope.launch {
                val addrs = repository.getAddressesForCustomer(customer.id)
                customerAddresses.value = addrs
                selectedAddress.value = address ?: addrs.firstOrNull { it.isDefault } ?: addrs.firstOrNull()
                if (selectedAddress.value != null) {
                    deliveryFee.value = selectedAddress.value!!.deliveryFee
                }
            }
            orderType.value = "DELIVERY"
        } else {
            selectedAddress.value = null
            customerAddresses.value = emptyList()
            orderType.value = "TAKEAWAY"
        }
        currentScreen.value = NavScreen.POS
        dismissCallerDialog()
    }

    // Cart operations
    fun addToCart(
        menuItem: MenuItem,
        quantity: Double,
        preparation: PreparationType?,
        customNotes: String = ""
    ) {
        val current = cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst {
            it.menuItem?.id == menuItem.id && it.preparation?.id == preparation?.id && it.notes == customNotes
        }
        if (existingIndex >= 0) {
            val old = current[existingIndex]
            current[existingIndex] = old.copy(quantity = old.quantity + quantity)
        } else {
            current.add(
                CartItem(
                    menuItem = menuItem,
                    name = menuItem.name,
                    kitchenName = menuItem.kitchenName,
                    category = menuItem.category,
                    unit = menuItem.unit,
                    quantity = quantity,
                    unitPrice = menuItem.basePrice,
                    preparation = if (menuItem.supportsPreparation) preparation else null,
                    notes = customNotes,
                    targetPrinter = menuItem.targetPrinter
                )
            )
        }
        cartItems.value = current
    }

    fun addAdHocCustomItem(
        name: String,
        unit: String,
        quantity: Double,
        unitPrice: Double,
        notes: String,
        targetPrinter: String = "MAIN"
    ) {
        val current = cartItems.value.toMutableList()
        current.add(
            CartItem(
                menuItem = null,
                name = name,
                category = "خاص واستثنائي",
                unit = unit,
                quantity = quantity,
                unitPrice = unitPrice,
                notes = notes,
                isCustomAdHoc = true,
                targetPrinter = targetPrinter
            )
        )
        cartItems.value = current
    }

    fun addExternalFishPreparationItem(
        fishName: String,
        prepName: String,
        weight: Double,
        prepPricePerKg: Double,
        notes: String
    ) {
        val current = cartItems.value.toMutableList()
        val title = "تجهيز سمك خارجي: $fishName ($prepName)"
        current.add(
            CartItem(
                menuItem = null,
                name = title,
                category = "تجهيز خارجي",
                unit = "كيلو",
                quantity = weight,
                unitPrice = prepPricePerKg,
                notes = notes,
                isCustomAdHoc = true,
                targetPrinter = "MAIN,KITCHEN_1"
            )
        )
        cartItems.value = current
        userMessage.value = "تمت إضافة تجهيز سمك خارجي ($weight كجم) للسلة بنجاح"
    }

    fun updateCartItemQuantity(cartItemId: String, newQty: Double) {
        if (newQty <= 0) {
            cartItems.value = cartItems.value.filter { it.id != cartItemId }
        } else {
            cartItems.value = cartItems.value.map {
                if (it.id == cartItemId) it.copy(quantity = newQty) else it
            }
        }
    }

    fun updateCartItem(
        cartItemId: String,
        name: String,
        unitPrice: Double,
        quantity: Double,
        targetPrinter: String,
        notes: String
    ) {
        cartItems.value = cartItems.value.map {
            if (it.id == cartItemId) {
                it.copy(
                    name = name,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    targetPrinter = targetPrinter,
                    notes = notes
                )
            } else it
        }
    }

    fun updateCartItemFull(
        cartItemId: String,
        name: String,
        unitPrice: Double,
        quantity: Double,
        unit: String,
        preparation: PreparationType?,
        notes: String,
        targetPrinter: String? = null
    ) {
        cartItems.value = cartItems.value.map {
            if (it.id == cartItemId) {
                it.copy(
                    name = name,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    unit = unit,
                    preparation = preparation,
                    notes = notes,
                    targetPrinter = targetPrinter ?: it.targetPrinter
                )
            } else it
        }
        userMessage.value = "تم تعديل الصنف في السلة بنجاح ✓"
    }

    fun addItemToCartDirect(
        menuItem: MenuItem?,
        name: String,
        category: String,
        unit: String,
        quantity: Double,
        unitPrice: Double,
        preparation: PreparationType?,
        notes: String,
        isCustomAdHoc: Boolean = false,
        targetPrinter: String = "MAIN"
    ) {
        val current = cartItems.value.toMutableList()
        current.add(
            CartItem(
                menuItem = menuItem,
                name = name,
                category = category,
                unit = unit,
                quantity = quantity,
                unitPrice = unitPrice,
                preparation = preparation,
                notes = notes,
                isCustomAdHoc = isCustomAdHoc,
                targetPrinter = targetPrinter
            )
        )
        cartItems.value = current
        userMessage.value = "تمت إضافة $name للسلة بنجاح ✓"
    }

    fun removeCartItem(cartItemId: String) {
        cartItems.value = cartItems.value.filter { it.id != cartItemId }
    }

    fun clearCart() {
        cartItems.value = emptyList()
        editingOrderId.value = null
        editingOrderNumber.value = null
        discount.value = 0.0
        isRoundToFiveEnabled.value = false
        orderPaidAmount.value = 0.0
        orderIsPaid.value = false
        orderNotes.value = ""
        orderCustomerName.value = ""
        orderCustomerPhone.value = ""
        orderCustomerSecondaryPhone.value = ""
        orderCustomerArea.value = ""
        orderCustomerStreet.value = ""
        selectedCustomer.value = null
        selectedAddress.value = null
        customerAddresses.value = emptyList()
    }

    fun startEditingOrder(orderWithItems: OrderWithItems) {
        val order = orderWithItems.order
        val items = orderWithItems.items
        val allMenu = menuItems.value
        val allPreps = preparations.value

        editingOrderId.value = order.id
        editingOrderNumber.value = order.orderNumber

        orderType.value = order.orderType
        orderCustomerName.value = order.customerName
        orderCustomerPhone.value = order.customerPhone
        orderCustomerSecondaryPhone.value = order.customerSecondaryPhone ?: ""
        orderCustomerArea.value = order.customerArea ?: ""
        orderCustomerStreet.value = order.deliveryAddressFull ?: ""
        deliveryFee.value = order.deliveryFee
        discount.value = order.discount
        orderNotes.value = order.notes
        paymentMethod.value = order.paymentMethod
        orderPaidAmount.value = order.paidAmount
        orderIsPaid.value = order.isPaid || (order.paidAmount >= order.totalAmount && order.totalAmount > 0.0)

        selectedDriver.value = drivers.value.firstOrNull { it.id == order.driverId }
        selectedCustomer.value = customers.value.firstOrNull { it.id == order.customerId }

        val mappedCartItems = items.map { ordItem ->
            val mItem = resolveMenuItemMatch(
                menuItemId = ordItem.menuItemId,
                itemName = ordItem.itemName,
                unit = ordItem.unit,
                unitPrice = ordItem.unitPrice,
                allMenu = allMenu
            )

            val prep = allPreps.firstOrNull { it.id == ordItem.preparationId }
                ?: if (!ordItem.preparationName.isNullOrBlank()) {
                    PreparationType(id = ordItem.preparationId ?: 0L, name = ordItem.preparationName, additionalPricePerUnit = ordItem.preparationPrice)
                } else null

            val effectiveIsCustom = ordItem.isCustomAdHoc && (mItem == null)
            val effectiveCategory = if (mItem != null && (ordItem.category.isBlank() || ordItem.category == "عام" || ordItem.category == "استثنائي" || ordItem.category == "خاص واستثنائي")) {
                mItem.category
            } else {
                ordItem.category
            }

            CartItem(
                id = "order_item_${ordItem.id}",
                menuItem = mItem,
                name = ordItem.itemName,
                kitchenName = ordItem.kitchenName.ifBlank { mItem?.kitchenName ?: "" },
                category = effectiveCategory,
                unit = ordItem.unit,
                quantity = ordItem.quantity,
                unitPrice = ordItem.unitPrice,
                preparation = prep,
                notes = ordItem.itemNotes,
                isCustomAdHoc = effectiveIsCustom,
                targetPrinter = ordItem.targetPrinter,
                orderItemId = ordItem.id,
                printedKitchenQuantity = ordItem.printedKitchenQuantity
            )
        }

        cartItems.value = mappedCartItems
        currentScreen.value = NavScreen.POS
        userMessage.value = "تم فتح الأوردر رقم ${order.orderNumber} في شاشة البيع للتعديل ✏️"
    }

    fun cancelEditingOrder() {
        val num = editingOrderNumber.value
        editingOrderId.value = null
        editingOrderNumber.value = null
        clearCart()
        // Do NOT force navigate to ORDERS if currently on POS (so user stays in POS with an empty cart to take new orders)
        if (currentScreen.value != NavScreen.POS) {
            currentScreen.value = NavScreen.ORDERS
        }
        if (!num.isNullOrBlank()) {
            userMessage.value = "تم إلغاء تعديل الأوردر $num"
        }
    }

    fun saveOrderEdits(
        autoPrint: Boolean = true,
        navigateToOrders: Boolean = false
    ) {
        if (isSavingOrderEdits || isSubmittingOrder) return
        val oId = editingOrderId.value ?: return
        val oNum = editingOrderNumber.value ?: ""

        if (cartItems.value.isEmpty()) {
            userMessage.value = "لا يمكن حفظ أوردر بدون أصناف. إذا كنت تريد إلغاء الأوردر بالكامل استخدم زر الإلغاء."
            return
        }

        isSavingOrderEdits = true
        isSubmittingOrderState.value = true

        val items = cartItems.value
        val subtotal = items.sumOf { it.total }
        val fee = if (orderType.value == "DELIVERY") deliveryFee.value else 0.0
        val total = (subtotal + fee - discount.value).coerceAtLeast(0.0)

        val inputName = orderCustomerName.value.trim()
        val inputPhone = orderCustomerPhone.value.trim()
        val inputArea = orderCustomerArea.value.trim()
        val inputStreet = orderCustomerStreet.value.trim()
        val customer = selectedCustomer.value
        val address = selectedAddress.value
        val driver = selectedDriver.value

        val effectiveName = if (inputName.isNotBlank()) inputName else (customer?.name ?: "زبون مباشر")
        val effectivePhone = if (inputPhone.isNotBlank()) inputPhone else (customer?.primaryPhone ?: "")
        val effectiveArea = if (inputArea.isNotBlank()) inputArea else (address?.area ?: "")
        val effectiveStreet = if (inputStreet.isNotBlank()) inputStreet else (address?.streetDetails ?: "")
        val fullAddress = if (effectiveStreet.isNotBlank()) {
            if (effectiveArea.isNotBlank()) "$effectiveArea - $effectiveStreet" else effectiveStreet
        } else null

        val secPhone = orderCustomerSecondaryPhone.value.trim().ifBlank { customer?.secondaryPhone }

        val paidAmt = orderPaidAmount.value
        val isPaidFinal = orderIsPaid.value || (paidAmt >= total && total > 0.0)
        val changeAmt = if (paidAmt > total) (paidAmt - total) else 0.0

        val order = Order(
            id = oId,
            orderNumber = oNum,
            orderType = orderType.value,
            customerId = customer?.id,
            customerName = effectiveName,
            customerPhone = effectivePhone,
            customerSecondaryPhone = secPhone,
            customerArea = effectiveArea,
            deliveryAddressTitle = address?.title,
            deliveryAddressFull = fullAddress,
            deliveryFee = fee,
            driverId = if (orderType.value == "DELIVERY") driver?.id else null,
            driverName = if (orderType.value == "DELIVERY") driver?.name else null,
            driverCommission = if (orderType.value == "DELIVERY" && driver?.calculationType == "COMMISSION") {
                if (fee > 0) fee else driver.ratePerOrder
            } else 0.0,
            subtotal = subtotal,
            discount = discount.value,
            totalAmount = total,
            paymentMethod = paymentMethod.value,
            notes = orderNotes.value,
            paidAmount = paidAmt,
            changeAmount = changeAmt,
            isPaid = isPaidFinal
        )

        val allMenu = menuItems.value
        val orderItems = items.map { cart ->
            val resolvedMenuItemId = cart.menuItem?.id
                ?: resolveMenuItemMatch(
                    menuItemId = null,
                    itemName = cart.name,
                    unit = cart.unit,
                    unitPrice = cart.unitPrice,
                    allMenu = allMenu
                )?.id

            OrderItem(
                id = cart.orderItemId ?: 0L,
                orderId = oId,
                menuItemId = resolvedMenuItemId,
                itemName = cart.name,
                kitchenName = cart.kitchenName.ifBlank { cart.menuItem?.kitchenName ?: "" },
                category = if (cart.menuItem != null && (cart.category.isBlank() || cart.category == "عام" || cart.category == "استثنائي")) cart.menuItem.category else cart.category,
                unit = cart.unit,
                quantity = cart.quantity,
                unitPrice = cart.unitPrice,
                preparationId = cart.preparation?.id,
                preparationName = cart.preparation?.name,
                preparationPrice = cart.prepPrice,
                itemNotes = cart.notes,
                totalPrice = cart.total,
                isCustomAdHoc = cart.isCustomAdHoc && (resolvedMenuItemId == null),
                targetPrinter = cart.targetPrinter,
                printedInKitchen = cart.quantity <= cart.printedKitchenQuantity,
                printedKitchenQuantity = cart.printedKitchenQuantity
            )
        }

        viewModelScope.launch {
            try {
                val beforeEditOrder = repository.getOrderByIdDirect(oId)
                val success = repository.updateOrderFromPos(oId, order, orderItems)
                if (success) {
                    val isSeller = deviceRole.value == "SELLER"
                    val cUser = cloudUser.value
                    val updatedWithItems = repository.getOrderByIdDirect(oId)
                    var heldOfflineForSeller = false
                    var sentEditOnlineCloudId: String? = null
                    val editRequestTimeMs = System.currentTimeMillis()

                    if (cUser != null && updatedWithItems != null) {
                        val uOrd = updatedWithItems.order
                        val hasOfficialNumber = uOrd.orderNumber.startsWith("#") && !uOrd.cloudOrderId.isNullOrBlank()
                        val onlineNow = coordinator.checkConnectivityNow().also { coordinator.isDeviceOnline.value = it }

                        if (isSeller && (!onlineNow || uOrd.syncState == AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE || !hasOfficialNumber)) {
                            heldOfflineForSeller = true
                            val pendingAction = if (autoPrint) "EDIT_PRINT" else "EDIT_SAVED_ONLY"
                            repository.updateOrderSyncMetadata(
                                orderId = oId,
                                orderNumber = uOrd.orderNumber,
                                cloudOrderId = uOrd.cloudOrderId,
                                syncState = AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE,
                                pendingPrintAction = pendingAction,
                                pendingPrintRequestedAtMs = editRequestTimeMs
                            )
                        } else {
                            val cleanNum = uOrd.orderNumber.replace("#", "").trim().toIntOrNull() ?: 0
                            val cloudId = uOrd.cloudOrderId?.takeIf { it.isNotBlank() }
                                ?: "order_${uOrd.createdAt}_${cleanNum}".also { repository.updateCloudOrderId(oId, it) }
                            val doc = CloudOrderDoc(
                                id = cloudId,
                                orderNumber = cleanNum,
                                customerName = uOrd.customerName,
                                customerPhone = uOrd.customerPhone,
                                customerSecondaryPhone = uOrd.customerSecondaryPhone.orEmpty(),
                                customerArea = uOrd.customerArea.orEmpty(),
                                deliveryAddressTitle = uOrd.deliveryAddressTitle.orEmpty(),
                                deliveryAddressFull = uOrd.deliveryAddressFull.orEmpty(),
                                orderType = uOrd.orderType,
                                status = uOrd.status.ifBlank { "NEW" },
                                printStatus = if (isSeller && autoPrint) "PRINT_REQUESTED" else "SAVED_ONLY",
                                requestedPrinterTarget = if (isSeller && autoPrint) "ALL" else "",
                                reprintReason = if (isSeller && autoPrint) "نسخة معدلة" else "",
                                isArchived = uOrd.isArchived,
                                shiftNumber = activeShift.value?.shiftNumber ?: 0,
                                itemsJson = serializeOrderItemsToJson(updatedWithItems.items),
                                subtotal = uOrd.subtotal,
                                deliveryFee = uOrd.deliveryFee,
                                discount = uOrd.discount,
                                total = uOrd.totalAmount,
                                paymentMethod = uOrd.paymentMethod,
                                notes = uOrd.notes,
                                driverName = uOrd.driverName.orEmpty(),
                                createdByTerminal = uOrd.sellerName.ifBlank { activeSellerName.value },
                                printedByTerminal = "",
                                createdAtMs = uOrd.createdAt.coerceAtLeast(0L),
                                printRequestedAtMs = editRequestTimeMs,
                                hasFullDetails = true
                            )
                            val syncRes = withContext(Dispatchers.IO) {
                                val res = withTimeoutOrNull(8_000L) {
                                    cloudRepository.syncEditedOrderToCloud(cUser.uid, doc)
                                }
                                if (!isSeller || res?.isSuccess == true) {
                                    val touchedMenuIds = (
                                        (beforeEditOrder?.items?.mapNotNull { it.menuItemId } ?: emptyList()) +
                                            updatedWithItems.items.mapNotNull { it.menuItemId }
                                        ).toSet()
                                    val ingIds = allIngredients.value.filter { it.menuItemId in touchedMenuIds }.map { it.ingredientItemId }.toSet()
                                    val affectedIds = touchedMenuIds + ingIds
                                    val updatedMenuList = repository.getAllMenuItemsIncludingArchivedList().filter { it.id in affectedIds }
                                    if (updatedMenuList.isNotEmpty()) {
                                        val cloudUpdatedItems = updatedMenuList.map { item ->
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
                                            cloudRepository.batchUpsertMenuItems(cUser.uid, cloudUpdatedItems)
                                        }
                                    }
                                }
                                res
                            }
                            if (isSeller && syncRes?.isSuccess != true) {
                                heldOfflineForSeller = true
                                val pendingAction = if (autoPrint) "EDIT_PRINT" else "EDIT_SAVED_ONLY"
                                repository.updateOrderSyncMetadata(
                                    orderId = oId,
                                    orderNumber = uOrd.orderNumber,
                                    cloudOrderId = cloudId,
                                    syncState = AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE,
                                    pendingPrintAction = pendingAction,
                                    pendingPrintRequestedAtMs = editRequestTimeMs
                                )
                            } else if (isSeller) {
                                sentEditOnlineCloudId = cloudId
                                repository.updateOrderSyncMetadata(
                                    orderId = oId,
                                    orderNumber = uOrd.orderNumber,
                                    cloudOrderId = cloudId,
                                    syncState = AppSyncCoordinator.SYNC_STATE_SYNCED,
                                    pendingPrintAction = "",
                                    pendingPrintRequestedAtMs = 0L
                                )
                            }
                        }
                    }

                    editingOrderId.value = null
                    editingOrderNumber.value = null
                    clearCart()
                    highlightedOrderId.value = oId
                    orderVisibilityPulse.value += 1L
                    if (navigateToOrders && !isLandscapeMode.value) {
                        currentScreen.value = NavScreen.ORDERS
                    }
                    if (!isSeller && autoPrint) {
                        printModifiedOrderAtCashier(oId)
                    } else if (isSeller && heldOfflineForSeller) {
                        val offlineMsg = "📴 تليفونك (البائع) غير متصل بالإنترنت — تم تعليق تعديلات الأوردر ($oNum) وسيتم إرسالها للكاشير فور عودة النت ⏳"
                        postQueuedMessage(offlineMsg)
                        coordinator.postOrderNotification("offline_edit_$oId", "تعديل أوردر معلق أوفلاين 📴", offlineMsg)
                    } else {
                        if (isSeller && autoPrint && cUser != null && sentEditOnlineCloudId != null) {
                            coordinator.watchSellerOrderCashierAck(cUser.uid, sentEditOnlineCloudId, oId, oNum)
                        }
                        postQueuedMessage(
                            when {
                                isSeller && autoPrint -> "تم حفظ تعديلات الأوردر $oNum وإرسالها للكاشير والطباعة ☁️🖨️"
                                isSeller -> "تم حفظ تعديلات الأوردر $oNum في السحابة بدون طباعة ✓"
                                else -> "تم حفظ تعديلات الأوردر $oNum بنجاح ✓"
                            }
                        )
                    }
                } else {
                    userMessage.value = "تعذر تحديث الأوردر، يرجى المحاولة مرة أخرى"
                }
            } finally {
                isSavingOrderEdits = false
                isSubmittingOrderState.value = false
            }
        }
    }

    // Checkout
    fun submitOrder(
        autoPrint: Boolean = true,
        navigateToOrders: Boolean = false,
        highlightOrder: Boolean = false,
        managerPasswordOverride: String? = null
    ) {
        if (isSubmittingOrder || isSubmittingOrderState.value) {
            return
        }

        if (editingOrderId.value != null) {
            saveOrderEdits(autoPrint = autoPrint, navigateToOrders = navigateToOrders)
            return
        }

        if (cartItems.value.isEmpty()) {
            userMessage.value = "السلة فارغة، يرجى إضافة أصناف أولاً"
            return
        }

        isSubmittingOrder = true
        isSubmittingOrderState.value = true

        val isSeller = deviceRole.value == "SELLER"
        if (!isSeller && autoPrint) {
            printDiagnosticStatus.value = "تم الضغط على زر تأكيد وطباعة الأوردر ✓"
            isPrintingOrder.value = true
        }

        val items = cartItems.value
        val subtotal = items.sumOf { it.total }
        val fee = if (orderType.value == "DELIVERY") deliveryFee.value else 0.0
        val rawBeforeRounding = (subtotal + fee - discount.value).coerceAtLeast(0.0)
        val roundingDiscount = if (isRoundToFiveEnabled.value && rawBeforeRounding > 0.0) {
            val rounded = kotlin.math.floor(rawBeforeRounding / 5.0) * 5.0
            (rawBeforeRounding - rounded).coerceAtLeast(0.0)
        } else 0.0
        val total = (rawBeforeRounding - roundingDiscount).coerceAtLeast(0.0)
        val totalDiscount = discount.value + roundingDiscount

        val customer = selectedCustomer.value
        val address = selectedAddress.value
        val driver = selectedDriver.value

        val inputName = orderCustomerName.value.trim()
        val inputPhone = orderCustomerPhone.value.trim()
        val inputArea = orderCustomerArea.value.trim()
        val inputStreet = orderCustomerStreet.value.trim()

        val effectiveName = if (inputName.isNotBlank()) inputName else (customer?.name ?: "زبون مباشر")
        val effectivePhone = if (inputPhone.isNotBlank()) inputPhone else (customer?.primaryPhone ?: "")
        val effectiveArea = if (inputArea.isNotBlank()) inputArea else (address?.area ?: "")
        val effectiveStreet = if (inputStreet.isNotBlank()) inputStreet else (address?.streetDetails ?: "")
        val fullAddress = if (effectiveStreet.isNotBlank()) {
            if (effectiveArea.isNotBlank()) "$effectiveArea - $effectiveStreet" else effectiveStreet
        } else null

        val secPhone = orderCustomerSecondaryPhone.value.trim().ifBlank { customer?.secondaryPhone }

        val paidAmt = orderPaidAmount.value
        val isPaidFinal = orderIsPaid.value || (paidAmt >= total && total > 0.0)
        val changeAmt = if (paidAmt > total) (paidAmt - total) else 0.0

        val effectiveSeller = activeSellerName.value.ifBlank {
            if (isSeller) "بائع صالة" else "كاشير رئيسي"
        }

        val order = Order(
            orderNumber = "",
            orderType = orderType.value,
            customerId = customer?.id,
            customerName = effectiveName,
            customerPhone = effectivePhone,
            customerSecondaryPhone = secPhone,
            customerArea = effectiveArea,
            deliveryAddressTitle = address?.title,
            deliveryAddressFull = fullAddress,
            deliveryFee = fee,
            driverId = if (orderType.value == "DELIVERY") driver?.id else null,
            driverName = if (orderType.value == "DELIVERY") driver?.name else null,
            driverCommission = if (orderType.value == "DELIVERY" && driver?.calculationType == "COMMISSION") {
                if (fee > 0) fee else driver.ratePerOrder
            } else 0.0,
            subtotal = subtotal,
            discount = totalDiscount,
            totalAmount = total,
            paymentMethod = paymentMethod.value,
            notes = orderNotes.value,
            paidAmount = paidAmt,
            changeAmount = changeAmt,
            isPaid = isPaidFinal,
            sellerName = effectiveSeller,
            deviceRole = deviceRole.value
        )

        val allMenu = menuItems.value
        val orderItems = items.map { cart ->
            val resolvedMenuItemId = cart.menuItem?.id
                ?: resolveMenuItemMatch(
                    menuItemId = null,
                    itemName = cart.name,
                    unit = cart.unit,
                    unitPrice = cart.unitPrice,
                    allMenu = allMenu
                )?.id

            OrderItem(
                menuItemId = resolvedMenuItemId,
                itemName = cart.name,
                kitchenName = cart.kitchenName.ifBlank { cart.menuItem?.kitchenName ?: "" },
                category = if (cart.menuItem != null && (cart.category.isBlank() || cart.category == "عام" || cart.category == "استثنائي")) cart.menuItem.category else cart.category,
                unit = cart.unit,
                quantity = cart.quantity,
                unitPrice = cart.unitPrice,
                preparationId = cart.preparation?.id,
                preparationName = cart.preparation?.name,
                preparationPrice = cart.prepPrice,
                itemNotes = cart.notes,
                totalPrice = cart.total,
                isCustomAdHoc = cart.isCustomAdHoc && (resolvedMenuItemId == null),
                targetPrinter = cart.targetPrinter
            )
        }

        val itemCount = orderItems.size
        printDiagnosticStatus.value = "بدء تجهيز بيانات الأوردر ($itemCount أصناف)..."

        viewModelScope.launch {
            try {
                var linkedCustId = customer?.id
                if (saveCustomerPermanently.value && effectivePhone.isNotBlank() && effectiveName.isNotBlank() && effectiveName != "زبون مباشر") {
                    val custToSave = Customer(
                        id = customer?.id ?: 0L,
                        name = effectiveName,
                        primaryPhone = effectivePhone,
                        secondaryPhone = secPhone,
                        notes = orderNotes.value,
                        totalOrdersCount = customer?.totalOrdersCount ?: 0,
                        totalSpent = customer?.totalSpent ?: 0.0
                    )
                    val addrToSave = if (effectiveStreet.isNotBlank()) {
                        CustomerAddress(
                            customerId = customer?.id ?: 0L,
                            title = address?.title ?: "الرئيسي",
                            area = effectiveArea,
                            streetDetails = effectiveStreet,
                            deliveryFee = fee,
                            isDefault = true
                        )
                    } else null
                    linkedCustId = repository.saveCustomerWithAddress(custToSave, addrToSave)
                }

                val cUser = cloudUser.value
                val fallbackNum = repository.getNextOrderNumber()
                val requestNowMs = System.currentTimeMillis()
                val activeShiftVal = activeShift.value

                var isSellerHeldOffline = false
                var allocatedOrderNum = 0
                var orderNumberLabel = ""
                var preCloudOrderId: String? = null
                var syncStateForInsert = AppSyncCoordinator.SYNC_STATE_SYNCED
                var pendingActionForInsert = ""
                var pendingRequestedAtForInsert = 0L

                if (isSeller && cUser != null) {
                    val onlineNow = coordinator.checkConnectivityNow().also { coordinator.isDeviceOnline.value = it }
                    val allocRes = if (onlineNow) {
                        withTimeoutOrNull(6_000L) {
                            cloudRepository.tryAllocateOnlineOrderNumber(cUser.uid, fallbackNum)
                        } ?: Result.failure(Exception("انتهت مهلة الاتصال بالخادم"))
                    } else {
                        Result.failure(Exception("الجهاز غير متصل بالإنترنت"))
                    }

                    if (allocRes.isSuccess) {
                        allocatedOrderNum = allocRes.getOrThrow()
                        orderNumberLabel = "#$allocatedOrderNum"
                        preCloudOrderId = "order_${requestNowMs}_${allocatedOrderNum}_${java.util.UUID.randomUUID().toString().take(4)}"
                    } else {
                        // Offline Seller Rule 1: DO NOT assign an official number! Assign temporary marker & hold in queue
                        isSellerHeldOffline = true
                        orderNumberLabel = coordinator.nextPendingTempOrderNumber()
                        preCloudOrderId = null
                        syncStateForInsert = AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE
                        pendingActionForInsert = if (autoPrint) "NEW_ORDER" else "NEW_ORDER_SAVED_ONLY"
                        pendingRequestedAtForInsert = requestNowMs
                    }
                } else {
                    allocatedOrderNum = if (cUser != null) {
                        withTimeoutOrNull(8_000L) {
                            cloudRepository.getAndIncrementOrderNumber(cUser.uid, fallbackNum)
                        } ?: fallbackNum
                    } else {
                        fallbackNum
                    }
                    orderNumberLabel = "#$allocatedOrderNum"
                    preCloudOrderId = "order_${requestNowMs}_${allocatedOrderNum}_${java.util.UUID.randomUUID().toString().take(4)}"
                }

                val finalOrder = (if (linkedCustId != null && linkedCustId > 0) order.copy(customerId = linkedCustId) else order).copy(
                    orderNumber = orderNumberLabel,
                    cloudOrderId = preCloudOrderId,
                    shiftId = activeShiftVal?.id,
                    status = "NEW",
                    createdAt = requestNowMs,
                    syncState = syncStateForInsert,
                    pendingPrintAction = pendingActionForInsert,
                    pendingPrintRequestedAtMs = pendingRequestedAtForInsert
                )

                val result = repository.placeOrder(finalOrder, orderItems, managerPasswordOverride, deductStock = true)
                when (result) {
                    is OrderPlacementResult.Success -> {
                        val orderNum = result.orderNumber
                        val orderId = result.orderId
                        nextOrderNumber.value = repository.getNextOrderNumber()
                        clearCart()

                        _lastSubmittedOrder.value = null
                        _requestedInvoiceOrder.value = null

                        // Immediately reveal & highlight the saved order BEFORE cloud stock sync & printer I/O
                        highlightedOrderId.value = orderId
                        orderVisibilityPulse.value += 1L
                        if (navigateToOrders && !isLandscapeMode.value) {
                            currentScreen.value = NavScreen.ORDERS
                        }

                        var uploadSucceeded = false
                        // Push order & stock deduction to central cloud shared database
                        if (cUser != null && !isSellerHeldOffline && preCloudOrderId != null) {
                            val cloudOrder = CloudOrderDoc(
                                id = preCloudOrderId,
                                orderNumber = allocatedOrderNum,
                                customerName = finalOrder.customerName,
                                customerPhone = finalOrder.customerPhone,
                                customerSecondaryPhone = finalOrder.customerSecondaryPhone.orEmpty(),
                                customerArea = finalOrder.customerArea.orEmpty(),
                                deliveryAddressTitle = finalOrder.deliveryAddressTitle.orEmpty(),
                                deliveryAddressFull = finalOrder.deliveryAddressFull.orEmpty(),
                                orderType = finalOrder.orderType,
                                status = "NEW",
                                printStatus = if (isSeller && autoPrint) "PENDING_PRINT" else "SAVED_ONLY",
                                requestedPrinterTarget = if (isSeller && autoPrint) "ALL" else "",
                                isArchived = false,
                                shiftNumber = activeShiftVal?.shiftNumber ?: 0,
                                itemsJson = serializeOrderItemsToJson(orderItems),
                                subtotal = finalOrder.subtotal,
                                deliveryFee = finalOrder.deliveryFee,
                                discount = finalOrder.discount,
                                total = finalOrder.totalAmount,
                                paymentMethod = finalOrder.paymentMethod,
                                notes = finalOrder.notes,
                                driverName = finalOrder.driverName.orEmpty(),
                                createdByTerminal = effectiveSeller,
                                printedByTerminal = "",
                                createdAtMs = finalOrder.createdAt.coerceAtLeast(0L),
                                printRequestedAtMs = requestNowMs,
                                hasFullDetails = true
                            )

                            val createRes = withContext(Dispatchers.IO) {
                                val res = withTimeoutOrNull(6_000L) {
                                    cloudRepository.createOrder(cUser.uid, cloudOrder)
                                }
                                if (res?.isSuccess == true || !isSeller) {
                                    val soldMenuIds = orderItems.mapNotNull { it.menuItemId }.toSet()
                                    val ingIds = allIngredients.value.filter { it.menuItemId in soldMenuIds }.map { it.ingredientItemId }.toSet()
                                    val affectedIds = soldMenuIds + ingIds
                                    val updatedItems = repository.getAllMenuItemsIncludingArchivedList().filter { it.id in affectedIds }
                                    if (updatedItems.isNotEmpty()) {
                                        val cloudUpdatedItems = updatedItems.map { item ->
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
                                            cloudRepository.batchUpsertMenuItems(cUser.uid, cloudUpdatedItems)
                                        }
                                    }
                                }
                                res
                            }
                            uploadSucceeded = createRes?.isSuccess == true
                            if (isSeller && !uploadSucceeded) {
                                // Upload dropped midway: hold in offline queue so ConnectivityManager flushes cleanly when back online
                                isSellerHeldOffline = true
                                repository.updateOrderSyncMetadata(
                                    orderId = orderId,
                                    orderNumber = orderNum,
                                    cloudOrderId = preCloudOrderId,
                                    syncState = AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE,
                                    pendingPrintAction = if (autoPrint) "NEW_ORDER" else "NEW_ORDER_SAVED_ONLY",
                                    pendingPrintRequestedAtMs = requestNowMs
                                )
                            }
                        }

                        if (!isSeller && autoPrint) {
                            isPrintingOrder.value = true
                            setPrintDiagnostic("جارٍ الطباعة للأوردر $orderNum للطابعات المربوطة...")

                            val allConfigs = repository.getAllPrinterConfigs().filter { it.isEnabled }
                            val printersToPrint = mutableSetOf<String>()

                            val invoicePrinters = allConfigs.filter { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" || it.isCashierDefault }
                            for (p in invoicePrinters) {
                                printersToPrint.add(p.id)
                            }

                            val itemPrinters = orderItems.flatMap { it.targetPrinter.split(",") }
                                .map { it.trim().uppercase() }
                                .filter { it.isNotBlank() }

                            for (p in allConfigs) {
                                if (p.role == "KITCHEN_ORDER" || p.role == "BOTH") {
                                    if (itemPrinters.contains(p.id.uppercase()) || itemPrinters.contains("ALL") || itemPrinters.isEmpty()) {
                                        printersToPrint.add(p.id)
                                    }
                                }
                            }

                            if (printersToPrint.isEmpty() && allConfigs.isNotEmpty()) {
                                printersToPrint.add(allConfigs.first().id)
                            }

                            if (printersToPrint.isNotEmpty()) {
                                val successNames = mutableListOf<String>()
                                val errorNames = mutableListOf<String>()
                                var firstFailReason: String? = null
                                for (pid in printersToPrint) {
                                    val pConfig = allConfigs.firstOrNull { it.id == pid } ?: repository.getPrinterConfig(pid)
                                    val printRes = withTimeoutOrNull(30_000L) {
                                        repository.dispatchPrint(orderId, pid)
                                    } ?: PrinterResult.Error("انتهت مهلة الاتصال بالطابعة")
                                    if (printRes is PrinterResult.Success) {
                                        successNames.add(pConfig.displayName)
                                    } else {
                                        errorNames.add(pConfig.displayName)
                                        if (firstFailReason == null) {
                                            firstFailReason = extractPrinterErrorMessage(printRes)
                                        }
                                    }
                                }
                                isPrintingOrder.value = false
                                if (errorNames.isEmpty()) {
                                    val namesStr = successNames.joinToString("+")
                                    setPrintDiagnostic("طُبع أوردر $orderNum ($namesStr) ✓")
                                    postQueuedMessage("تم حفظ وطباعة الأوردر $orderNum بنجاح على ($namesStr) ✓")
                                    if (cUser != null && successNames.isNotEmpty() && preCloudOrderId != null) {
                                        viewModelScope.launch(Dispatchers.IO) {
                                            cloudRepository.markOrderPrinted(cUser.uid, preCloudOrderId, effectiveSeller)
                                        }
                                    }
                                } else {
                                    val errLabel = buildFailedPrintersLabel(errorNames, printersToPrint.size)
                                    val reasonSuffix = firstFailReason?.let { " — $it" } ?: ""
                                    setPrintDiagnostic("تعذر الطباعة: $errLabel", autoClearSeconds = 8)
                                    val partialSuccess = if (successNames.isNotEmpty()) " (طُبع على ${successNames.joinToString("+")} فقط)" else ""
                                    postQueuedMessage("تم حفظ الأوردر $orderNum$partialSuccess — تعذر الطباعة على $errLabel$reasonSuffix")
                                    if (cUser != null && preCloudOrderId != null) {
                                        viewModelScope.launch(Dispatchers.IO) {
                                            cloudRepository.updateCloudOrderPrintState(cUser.uid, preCloudOrderId, "ERROR", effectiveSeller)
                                        }
                                    }
                                }
                            } else {
                                isPrintingOrder.value = false
                                setPrintDiagnostic("حُفظ $orderNum (لا توجد طابعات مفعلة)")
                                postQueuedMessage("تم حفظ الأوردر $orderNum بنجاح ✓")
                            }

                            if (highlightOrder) {
                                highlightedOrderId.value = orderId
                                orderVisibilityPulse.value += 1L
                            }
                        } else {
                            isPrintingOrder.value = false
                            if (highlightOrder) {
                                highlightedOrderId.value = orderId
                                orderVisibilityPulse.value += 1L
                            }
                            if (isSeller && isSellerHeldOffline) {
                                val offlineMsg = if (!orderNum.startsWith("#")) {
                                    "📴 تليفونك (البائع) غير متصل بالإنترنت — تم تعليق الأوردر ($orderNum) بدون رقم رسمي حتى عودة النت (لو مرّ >30 ثانية يُحفظ بدون طباعة تلقائية) ⏳"
                                } else {
                                    "📴 تعذر رفع الأوردر ($orderNum) من تليفونك — تم تعليقه وسيُرسل للكاشير تلقائياً فور عودة النت ⏳"
                                }
                                postQueuedMessage(offlineMsg)
                                coordinator.postOrderNotification("offline_new_$orderId", "أوردر معلق أوفلاين 📴", offlineMsg)
                            } else {
                                if (isSeller && autoPrint && cUser != null && preCloudOrderId != null && uploadSucceeded) {
                                    coordinator.watchSellerOrderCashierAck(cUser.uid, preCloudOrderId, orderId, orderNum)
                                }
                                postQueuedMessage(
                                    when {
                                        isSeller && autoPrint -> "تم إرسال الأوردر ($orderNum) للكاشير والطباعة ☁️🖨️"
                                        isSeller -> "تم تأكيد وحفظ الأوردر ($orderNum) في السحابة المشتركة فقط ✓"
                                        navigateToOrders -> "تم تأكيد وحفظ الأوردر $orderNum والانتقال لقائمة الأوردرات ✓"
                                        else -> "تم تسجيل وحفظ الأوردر $orderNum بنجاح ✓"
                                    }
                                )
                            }
                        }
                    }
                    is OrderPlacementResult.StockError -> {
                        isPrintingOrder.value = false
                        printDiagnosticStatus.value = "عجز مخزون: ${result.itemName}"
                        _stockOverridePrompt.value = StockOverridePrompt(
                            itemName = result.itemName,
                            requested = result.requested,
                            available = result.available,
                            pendingAction = {}
                        )
                    }
                    is OrderPlacementResult.Error -> {
                        isPrintingOrder.value = false
                        printDiagnosticStatus.value = "فشل: ${result.message}"
                        userMessage.value = result.message
                    }
                }
            } catch (e: Exception) {
                isPrintingOrder.value = false
                printDiagnosticStatus.value = "خطأ: ${e.localizedMessage ?: "حدث خطأ غير متوقع"}"
                userMessage.value = "خطأ: ${e.localizedMessage}"
            } finally {
                isSubmittingOrder = false
                isSubmittingOrderState.value = false
            }
        }
    }

    private fun buildFullCloudOrderDoc(
        orderWithItems: OrderWithItems,
        cloudId: String,
        printStatus: String = "SAVED_ONLY",
        requestedPrinterTarget: String = "",
        reprintReason: String = "",
        printRequestedAtMs: Long = 0L
    ): CloudOrderDoc {
        val ord = orderWithItems.order
        val cleanNum = ord.orderNumber.replace("#", "").trim().toIntOrNull() ?: 0
        return CloudOrderDoc(
            id = cloudId,
            orderNumber = cleanNum,
            customerName = ord.customerName,
            customerPhone = ord.customerPhone,
            customerSecondaryPhone = ord.customerSecondaryPhone.orEmpty(),
            customerArea = ord.customerArea.orEmpty(),
            deliveryAddressTitle = ord.deliveryAddressTitle.orEmpty(),
            deliveryAddressFull = ord.deliveryAddressFull.orEmpty(),
            orderType = ord.orderType,
            status = ord.status.ifBlank { "NEW" },
            printStatus = printStatus,
            requestedPrinterTarget = requestedPrinterTarget,
            reprintReason = reprintReason,
            isArchived = ord.isArchived,
            shiftNumber = activeShift.value?.shiftNumber ?: 0,
            itemsJson = serializeOrderItemsToJson(orderWithItems.items),
            subtotal = ord.subtotal,
            deliveryFee = ord.deliveryFee,
            discount = ord.discount,
            total = ord.totalAmount,
            paymentMethod = ord.paymentMethod,
            notes = ord.notes,
            driverName = ord.driverName.orEmpty(),
            createdByTerminal = ord.sellerName.ifBlank { activeSellerName.value },
            createdAtMs = ord.createdAt.coerceAtLeast(0L),
            printRequestedAtMs = printRequestedAtMs,
            hasFullDetails = true
        )
    }

    private suspend fun pushFullOrderUpdateToCloud(orderId: Long) {
        val cUser = cloudUser.value ?: return
        val orderWithItems = repository.getOrderByIdDirect(orderId) ?: return
        val ord = orderWithItems.order
        if (ord.syncState == AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE || !ord.orderNumber.startsWith("#")) return
        val cleanNum = ord.orderNumber.replace("#", "").trim().toIntOrNull() ?: 0
        val cloudId = ord.cloudOrderId?.takeIf { it.isNotBlank() }
            ?: "order_${ord.createdAt}_${cleanNum}".also { repository.updateCloudOrderId(ord.id, it) }
        val doc = buildFullCloudOrderDoc(orderWithItems, cloudId, printStatus = "SAVED_ONLY")
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(8_000L) {
                cloudRepository.syncEditedOrderToCloud(cUser.uid, doc)
            }
        }
    }

    private suspend fun sendRemotePrintRequestToCashier(
        orderId: Long,
        targetPrinter: String,
        reprintReason: String?,
        targetLabel: String
    ) {
        val user = cloudUser.value
        if (user == null) {
            userMessage.value = "⚠️ يرجى ربط جهاز البائع بالسحابة المشتركة أولاً لإرسال أمر الطباعة للكاشير"
            return
        }
        val orderWithItems = repository.getOrderByIdDirect(orderId)
        if (orderWithItems == null) {
            userMessage.value = "الأوردر غير موجود في قاعدة البيانات"
            return
        }
        val ord = orderWithItems.order
        val existingCloudId = ord.cloudOrderId?.takeIf { it.isNotBlank() }
        val onlineNow = coordinator.checkConnectivityNow().also { coordinator.isDeviceOnline.value = it }
        val reqNowMs = System.currentTimeMillis()
        val needsNumberAllocation = !ord.orderNumber.startsWith("#") || existingCloudId == null
        val pendingActionCode = if (needsNumberAllocation) {
            "NEW_ORDER"
        } else {
            "REPRINT|$targetPrinter|${reprintReason ?: ""}"
        }

        if (!onlineNow) {
            repository.updateOrderSyncMetadata(
                orderId = ord.id,
                orderNumber = ord.orderNumber,
                cloudOrderId = existingCloudId,
                syncState = AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE,
                pendingPrintAction = pendingActionCode,
                pendingPrintRequestedAtMs = reqNowMs
            )
            val msg = "📴 تعذر الطباعة لأن تليفونك (البائع) غير متصل بالإنترنت الآن — تم تعليق طلب الطباعة ($targetLabel) للأوردر (${ord.orderNumber}) ⏳"
            setPrintDiagnostic("تليفونك أوفلاين 📴", autoClearSeconds = 6)
            postQueuedMessage(msg)
            coordinator.postOrderNotification("offline_reprint_${ord.id}", "تليفونك أوفلاين — طلب معلق 📴", msg)
            return
        }

        isPrintingOrder.value = true
        setPrintDiagnostic("جارٍ إرسال أمر الطباعة إلى الكاشير ($targetLabel)...", autoClearSeconds = 4)

        var resolvedOrderNumInt = ord.orderNumber.replace("#", "").trim().toIntOrNull() ?: 0
        var resolvedOrderNumStr = ord.orderNumber
        var cloudId = existingCloudId ?: ""

        if (needsNumberAllocation) {
            val fallbackLocal = repository.getNextOrderNumber()
            val allocRes = withTimeoutOrNull(8_000L) {
                cloudRepository.tryAllocateOnlineOrderNumber(user.uid, fallbackLocal)
            } ?: Result.failure(Exception("انتهت مهلة تحديد الرقم من السحابة"))

            if (allocRes.isFailure) {
                isPrintingOrder.value = false
                repository.updateOrderSyncMetadata(
                    orderId = ord.id,
                    orderNumber = ord.orderNumber,
                    cloudOrderId = existingCloudId,
                    syncState = AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE,
                    pendingPrintAction = pendingActionCode,
                    pendingPrintRequestedAtMs = reqNowMs
                )
                val msg = "📴 تعذر وصول تليفونك (البائع) للسحابة لتحديد رقم الأوردر (${ord.orderNumber}) — لا يزال معلقاً لحين استقرار النت ⏳"
                setPrintDiagnostic("تعذر الاتصال بالسحابة 📴", autoClearSeconds = 6)
                postQueuedMessage(msg)
                return
            }
            resolvedOrderNumInt = allocRes.getOrThrow()
            resolvedOrderNumStr = "#$resolvedOrderNumInt"
            cloudId = "order_${ord.createdAt.coerceAtLeast(1L)}_${resolvedOrderNumInt}_${java.util.UUID.randomUUID().toString().take(4)}"
            repository.updateOrderSyncMetadata(
                orderId = ord.id,
                orderNumber = resolvedOrderNumStr,
                cloudOrderId = cloudId,
                syncState = AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE,
                pendingPrintAction = pendingActionCode,
                pendingPrintRequestedAtMs = reqNowMs
            )
            coordinator.nextOrderNumberSynced.value = (resolvedOrderNumInt + 1).coerceAtLeast(1)
        }

        val updatedOrderWithItems = repository.getOrderByIdDirect(ord.id) ?: orderWithItems
        val isBrandNewCloudUpload = needsNumberAllocation || ord.pendingPrintAction == "NEW_ORDER"
        val res = withTimeoutOrNull(10_000L) {
            val doc = buildFullCloudOrderDoc(
                orderWithItems = updatedOrderWithItems,
                cloudId = cloudId,
                printStatus = if (isBrandNewCloudUpload) "PENDING_PRINT" else "PRINT_REQUESTED",
                requestedPrinterTarget = targetPrinter,
                reprintReason = if (isBrandNewCloudUpload) "" else (reprintReason ?: ""),
                printRequestedAtMs = reqNowMs
            )
            if (isBrandNewCloudUpload) {
                cloudRepository.createOrder(user.uid, doc)
            } else {
                cloudRepository.syncEditedOrderToCloud(user.uid, doc)
            }
        }
        isPrintingOrder.value = false
        if (res?.isSuccess == true) {
            repository.updateOrderSyncMetadata(
                orderId = ord.id,
                orderNumber = resolvedOrderNumStr,
                cloudOrderId = cloudId,
                syncState = AppSyncCoordinator.SYNC_STATE_SYNCED,
                pendingPrintAction = "",
                pendingPrintRequestedAtMs = 0L
            )
            coordinator.watchSellerOrderCashierAck(user.uid, cloudId, ord.id, resolvedOrderNumStr)
            setPrintDiagnostic("تم إرسال أمر الطباعة للكاشير ✓")
            postQueuedMessage("تم إرسال أمر طباعة ($targetLabel) للأوردر $resolvedOrderNumStr إلى الكاشير الرئيسي ✓")
        } else {
            repository.updateOrderSyncMetadata(
                orderId = ord.id,
                orderNumber = resolvedOrderNumStr,
                cloudOrderId = cloudId.ifBlank { null },
                syncState = AppSyncCoordinator.SYNC_STATE_PENDING_OFFLINE,
                pendingPrintAction = pendingActionCode,
                pendingPrintRequestedAtMs = reqNowMs
            )
            val msg = "📴 تعذر اتصال تليفونك (البائع) بالسحابة — تم تعليق طلب الطباعة ($targetLabel) للأوردر $resolvedOrderNumStr ⏳"
            setPrintDiagnostic("تعذر الاتصال بالسحابة 📴", autoClearSeconds = 6)
            postQueuedMessage(msg)
            coordinator.postOrderNotification("offline_reprint_${ord.id}", "تعذر إرسال الطباعة 📴", msg)
        }
    }

    /**
     * Prints order to all printers that the items in this order are linked to, plus cashier invoice.
     * On SELLER devices, delegates remotely via Firestore to the Master Cashier.
     */
    fun printOrderToAllLinkedPrinters(orderId: Long) {
        if (deviceRole.value == "SELLER") {
            viewModelScope.launch {
                sendRemotePrintRequestToCashier(orderId, "ALL", null, "طباعة للكل")
            }
            return
        }
        viewModelScope.launch {
            try {
                val orderWithItems = repository.getOrderByIdDirect(orderId)
                if (orderWithItems == null) {
                    userMessage.value = "الأوردر غير موجود في قاعدة البيانات"
                    return@launch
                }
                val allConfigs = repository.getAllPrinterConfigs().filter { it.isEnabled }
                if (allConfigs.isEmpty()) {
                    userMessage.value = "لا توجد طابعات مفعلة في الإعدادات، يرجى ضبط الطابعات أولاً"
                    return@launch
                }

                val orderItems = orderWithItems.items
                val itemPrinters = orderItems.flatMap { it.targetPrinter.split(",") }.map { it.trim().uppercase() }.filter { it.isNotBlank() }
                val pendingCancels = repository.getPendingKitchenCancellations(orderId)

                val printersToPrint = mutableSetOf<String>()
                val invoicePrinters = allConfigs.filter { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" || it.isCashierDefault }
                for (p in invoicePrinters) {
                    printersToPrint.add(p.id)
                }
                for (p in allConfigs) {
                    if (p.role == "KITCHEN_ORDER" || p.role == "BOTH") {
                        val hasItemForPrinter = itemPrinters.contains(p.id.uppercase()) || itemPrinters.contains("ALL") || itemPrinters.isEmpty()
                        val hasCancelForPrinter = pendingCancels.any { repository.matchesKitchenPrinterTarget(it.targetPrinter, p.id) }
                        if (hasItemForPrinter || hasCancelForPrinter) {
                            printersToPrint.add(p.id)
                        }
                    }
                }
                if (printersToPrint.isEmpty() && allConfigs.isNotEmpty()) {
                    printersToPrint.add(allConfigs.first().id)
                }

                printDiagnosticStatus.value = "جارٍ إرسال الطباعة للأوردر #${orderWithItems.order.orderNumber} للطابعات المربوطة..."
                isPrintingOrder.value = true

                val successNames = mutableListOf<String>()
                val errorPrinterNames = mutableListOf<String>()
                val errorDetails = mutableListOf<String>()
                for (pid in printersToPrint) {
                    val pConfig = allConfigs.firstOrNull { it.id == pid } ?: repository.getPrinterConfig(pid)
                    val res = withTimeoutOrNull(30_000L) {
                        repository.dispatchPrint(orderId, pid)
                    } ?: PrinterResult.Error("انتهت مهلة الاتصال بالطابعة")
                    if (res is PrinterResult.Success) {
                        successNames.add(pConfig.displayName)
                    } else {
                        errorPrinterNames.add(pConfig.displayName)
                        val msg = extractPrinterErrorMessage(res) ?: "خطأ غير معروف"
                        errorDetails.add("${pConfig.displayName}: $msg")
                    }
                }
                isPrintingOrder.value = false
                if (errorPrinterNames.isEmpty()) {
                    val namesStr = successNames.joinToString("+")
                    setPrintDiagnostic("طُبع أوردر #${orderWithItems.order.orderNumber} ($namesStr) ✓")
                    postQueuedMessage("تمت الطباعة بنجاح على: $namesStr ✓")
                } else {
                    val errLabel = buildFailedPrintersLabel(errorPrinterNames, printersToPrint.size)
                    setPrintDiagnostic("تعذر الطباعة: $errLabel", autoClearSeconds = 8)
                    postQueuedMessage("تعذر الطباعة على $errLabel — ${errorDetails.joinToString(" | ")}")
                }
            } catch (e: Exception) {
                isPrintingOrder.value = false
                setPrintDiagnostic("خطأ في الطباعة")
                postQueuedMessage("خطأ في الطباعة: ${e.localizedMessage}")
            }
        }
    }

    fun printModifiedOrderAtCashier(orderId: Long) {
        if (deviceRole.value == "SELLER") {
            viewModelScope.launch {
                sendRemotePrintRequestToCashier(orderId, "ALL", "نسخة معدلة", "نسخة معدلة")
            }
            return
        }

        viewModelScope.launch {
            try {
                val orderWithItems = repository.getOrderByIdDirect(orderId)
                if (orderWithItems == null) {
                    userMessage.value = "الأوردر غير موجود في قاعدة البيانات"
                    return@launch
                }

                val allConfigs = repository.getAllPrinterConfigs().filter { it.isEnabled }
                val invoicePrinters = allConfigs.filter { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" || it.isCashierDefault }
                val targetCashierPrinter = if (invoicePrinters.isNotEmpty()) {
                    invoicePrinters.first().id
                } else if (allConfigs.isNotEmpty()) {
                    allConfigs.first().id
                } else {
                    "MAIN"
                }
                val invoiceConfig = allConfigs.firstOrNull { it.id == targetCashierPrinter }
                    ?: repository.getPrinterConfig(targetCashierPrinter)

                printDiagnosticStatus.value = "جارٍ طباعة نسخة معدلة للأوردر #${orderWithItems.order.orderNumber}..."
                isPrintingOrder.value = true

                val succeededNames = mutableListOf<String>()
                val failedPrinterNames = mutableListOf<String>()
                var firstFailReason: String? = null
                var totalAttempted = 1

                val invoiceResult = withTimeoutOrNull(30_000L) {
                    repository.dispatchPrint(
                        orderId = orderId,
                        printerTarget = targetCashierPrinter,
                        reprintReason = "نسخة معدلة",
                        targetDocumentType = "CUSTOMER_INVOICE"
                    )
                } ?: com.example.printer.PrinterResult.Error("انتهت مهلة الاتصال بالطابعة")
                if (invoiceResult is com.example.printer.PrinterResult.Success) {
                    succeededNames.add(invoiceConfig.displayName)
                } else {
                    failedPrinterNames.add(invoiceConfig.displayName)
                    firstFailReason = extractPrinterErrorMessage(invoiceResult)
                }

                val kitchenPrinters = allConfigs.filter { it.role == "KITCHEN_ORDER" }
                for (kp in kitchenPrinters) {
                    totalAttempted++
                    try {
                        val kRes = withTimeoutOrNull(30_000L) {
                            repository.dispatchPrint(
                                orderId = orderId,
                                printerTarget = kp.id,
                                reprintReason = "فارق تعديل الأصناف",
                                targetDocumentType = "KITCHEN_ORDER"
                            )
                        } ?: com.example.printer.PrinterResult.Error("انتهت مهلة الاتصال بالطابعة")
                        if (kRes is com.example.printer.PrinterResult.Success) {
                            succeededNames.add(kp.displayName)
                        } else {
                            failedPrinterNames.add(kp.displayName)
                            if (firstFailReason == null) {
                                firstFailReason = extractPrinterErrorMessage(kRes)
                            }
                        }
                    } catch (ex: Exception) {
                        failedPrinterNames.add(kp.displayName)
                        if (firstFailReason == null) firstFailReason = ex.localizedMessage
                    }
                }

                isPrintingOrder.value = false
                val cUser = cloudUser.value
                val cloudId = orderWithItems.order.cloudOrderId?.takeIf { it.isNotBlank() }
                if (cUser != null && cloudId != null) {
                    val terminal = activeSellerName.value
                    val nextCloudPrintState = if (failedPrinterNames.isEmpty()) "PRINTED" else "ERROR"
                    viewModelScope.launch(Dispatchers.IO) {
                        cloudRepository.updateCloudOrderPrintState(cUser.uid, cloudId, nextCloudPrintState, terminal)
                    }
                }
                if (failedPrinterNames.isEmpty()) {
                    val okNames = succeededNames.joinToString("+")
                    setPrintDiagnostic("طُبعت النسخة المعدلة: #${orderWithItems.order.orderNumber} ($okNames) ✓")
                    postQueuedMessage("تمت طباعة النسخة المعدلة للأوردر #${orderWithItems.order.orderNumber} ($okNames) ✓")
                } else {
                    val errLabel = buildFailedPrintersLabel(failedPrinterNames, totalAttempted)
                    val reasonSuffix = firstFailReason?.let { " — $it" } ?: ""
                    setPrintDiagnostic("تعذر الطباعة: $errLabel", autoClearSeconds = 8)
                    val okPart = if (succeededNames.isNotEmpty()) " (طُبع على ${succeededNames.joinToString("+")} فقط)" else ""
                    postQueuedMessage("تعذر طباعة النسخة المعدلة للأوردر #${orderWithItems.order.orderNumber} على $errLabel$okPart$reasonSuffix")
                }
            } catch (e: Exception) {
                isPrintingOrder.value = false
                setPrintDiagnostic("تعذر طباعة النسخة المعدلة")
                postQueuedMessage("خطأ في طباعة النسخة المعدلة: ${e.localizedMessage}")
            }
        }
    }

    fun dismissStockPrompt() {
        _stockOverridePrompt.value = null
    }

    // Customer operations
    suspend fun getCustomerAddresses(customerId: Long): List<CustomerAddress> {
        return repository.getAddressesForCustomer(customerId)
    }

    fun selectCustomer(customer: Customer) {
        selectedCustomer.value = customer
        orderCustomerName.value = customer.name
        orderCustomerPhone.value = customer.primaryPhone
        orderCustomerSecondaryPhone.value = customer.secondaryPhone ?: ""
        viewModelScope.launch {
            val addrs = repository.getAddressesForCustomer(customer.id)
            customerAddresses.value = addrs
            val defaultAddr = addrs.firstOrNull { it.isDefault } ?: addrs.firstOrNull()
            selectedAddress.value = defaultAddr
            if (defaultAddr != null) {
                deliveryFee.value = defaultAddr.deliveryFee
                orderCustomerArea.value = defaultAddr.area
                orderCustomerStreet.value = defaultAddr.streetDetails
            }
        }
    }

    fun clearSelectedCustomer() {
        selectedCustomer.value = null
        selectedAddress.value = null
        customerAddresses.value = emptyList()
        orderCustomerName.value = ""
        orderCustomerPhone.value = ""
        orderCustomerSecondaryPhone.value = ""
        orderCustomerArea.value = ""
        orderCustomerStreet.value = ""
    }

    fun saveCustomer(
        name: String,
        phone: String,
        secondaryPhone: String?,
        notes: String,
        addressTitle: String?,
        area: String?,
        street: String?,
        addressFee: Double
    ) {
        viewModelScope.launch {
            val cust = Customer(
                name = name,
                primaryPhone = phone,
                secondaryPhone = secondaryPhone,
                notes = notes
            )
            val addr = if (!street.isNullOrBlank()) {
                CustomerAddress(
                    customerId = 0,
                    title = addressTitle ?: "الرئيسي",
                    area = area ?: "",
                    streetDetails = street,
                    deliveryFee = addressFee,
                    isDefault = true
                )
            } else null

            val id = repository.saveCustomerWithAddress(cust, addr)
            val savedCust = cust.copy(id = id)
            val addrs = repository.getAddressesForCustomer(id)
            
            userMessage.value = "تم حفظ بيانات العميل بنجاح"
            selectCustomer(savedCust)
        }
    }

    fun addCustomerAddress(customerId: Long, title: String, area: String, street: String, fee: Double, isDefault: Boolean) {
        viewModelScope.launch {
            repository.addAddress(
                CustomerAddress(
                    customerId = customerId,
                    title = title,
                    area = area,
                    streetDetails = street,
                    deliveryFee = fee,
                    isDefault = isDefault
                )
            )
            val addrs = repository.getAddressesForCustomer(customerId)
            customerAddresses.value = addrs
            val cust = repository.getAllCustomersList().firstOrNull { it.id == customerId }
            if (cust != null) {
                
            }
            userMessage.value = "تم إضافة العنوان بنجاح"
        }
    }

    // Category CRUD
    fun saveCategory(category: MenuCategory) {
        viewModelScope.launch {
            val savedId = repository.saveCategory(category)
            val fullCat = if (category.id == 0L) category.copy(id = savedId) else category
            pushCategoryToCloud(fullCat)
            userMessage.value = "تم حفظ القسم ومزامنته سحابياً بنجاح 🟢"
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            val cat = repository.getAllCategoriesList().firstOrNull { it.id == id }
            repository.deleteCategory(id)
            userMessage.value = "تم حذف القسم بنجاح ومزامنته"
        }
    }

    fun batchUpdateItemsCategory(itemIds: List<Long>, targetCategory: String) {
        viewModelScope.launch {
            repository.batchUpdateItemsCategory(itemIds, targetCategory)
            val updated = repository.getAllMenuItemsIncludingArchivedList().filter { it.id in itemIds }
            for (itm in updated) {
                pushMenuItemToCloud(itm)
            }
            userMessage.value = "تم نقل وتحديث أقسام الأصناف (${itemIds.size} صنف) بنجاح"
        }
    }

    fun batchUpdateItemPrices(priceUpdates: Map<Long, Double>) {
        if (priceUpdates.isEmpty()) return
        viewModelScope.launch {
            repository.updateDailyPrices(priceUpdates)
            val all = repository.getAllMenuItemsIncludingArchivedList()
            val updatedItems = all.filter { priceUpdates.containsKey(it.id) }
            for (itm in updatedItems) {
                pushMenuItemToCloud(itm.copy(basePrice = priceUpdates[itm.id] ?: itm.basePrice))
            }
            userMessage.value = "تم تحديث أسعار ${priceUpdates.size} صنف ومزامنتها سحابياً بنجاح ✓"
        }
    }

    // Menu Item CRUD
    fun saveMenuItem(item: MenuItem, oldName: String? = null) {
        viewModelScope.launch {
            val existing = if (item.id > 0L) repository.getMenuItemById(item.id) else null
            val resolvedCategory = if (item.category.isNotBlank() && item.category.trim() != "أخرى") {
                item.category.trim()
            } else {
                existing?.category?.trim()?.ifBlank { null } ?: item.category.trim().ifBlank { "عام" }
            }
            val resolvedUnit = if (item.unit.isNotBlank()) item.unit else (existing?.unit ?: "كيلو")
            val resolvedPrinter = if (item.targetPrinter.isNotBlank()) item.targetPrinter else (existing?.targetPrinter ?: "MAIN")

            val safeItem = item.copy(
                category = resolvedCategory,
                unit = resolvedUnit,
                targetPrinter = resolvedPrinter,
                updatedAtMillis = System.currentTimeMillis()
            )
            val savedId = repository.saveMenuItem(safeItem)
            val fullSavedItem = repository.getMenuItemById(savedId) ?: (if (safeItem.id > 0L) safeItem else safeItem.copy(id = savedId))
            pushMenuItemToCloud(fullSavedItem)
            userMessage.value = "تم حفظ الصنف ومزامنته سحابياً بنجاح 🟢"
        }
    }

    fun saveMenuItemWithIngredients(item: MenuItem, ingredients: List<MenuItemIngredient>, oldName: String? = null) {
        viewModelScope.launch {
            val existing = if (item.id > 0L) repository.getMenuItemById(item.id) else null
            val resolvedCategory = if (item.category.isNotBlank() && item.category.trim() != "أخرى") {
                item.category.trim()
            } else {
                existing?.category?.trim()?.ifBlank { null } ?: item.category.trim().ifBlank { "عام" }
            }
            val resolvedUnit = if (item.unit.isNotBlank()) item.unit else (existing?.unit ?: "كيلو")
            val resolvedPrinter = if (item.targetPrinter.isNotBlank()) item.targetPrinter else (existing?.targetPrinter ?: "MAIN")

            val safeItem = item.copy(
                category = resolvedCategory,
                unit = resolvedUnit,
                targetPrinter = resolvedPrinter,
                updatedAtMillis = System.currentTimeMillis()
            )
            val savedId = repository.saveMenuItemWithIngredients(safeItem, ingredients)
            val fullSavedItem = repository.getMenuItemById(savedId) ?: safeItem.copy(id = savedId)
            pushMenuItemToCloud(fullSavedItem)
            userMessage.value = if (ingredients.isNotEmpty()) {
                "تم حفظ الصنف ومزامنته سحابياً وربط ${ingredients.size} صنف مسحوب بنجاح 🟢"
            } else {
                "تم حفظ الصنف ومزامنته سحابياً بنجاح 🟢"
            }
        }
    }

    fun archiveMenuItem(id: Long, archived: Boolean) {
        viewModelScope.launch {
            val originalItem = repository.getMenuItemById(id) ?: repository.getAllMenuItemsIncludingArchivedList().firstOrNull { it.id == id }
            if (originalItem != null) {
                val cId = resolveCanonicalCloudId(originalItem)
                recentLocalArchiveOverrides[cId] = archived to System.currentTimeMillis()
            }
            repository.archiveMenuItem(id, archived)
            val refreshed = repository.getMenuItemById(id) ?: originalItem?.copy(isArchived = archived)
            if (refreshed != null) {
                val cId = resolveCanonicalCloudId(refreshed)
                recentLocalArchiveOverrides[cId] = archived to System.currentTimeMillis()
                pushMenuItemToCloud(refreshed.copy(isArchived = archived))
            }
            userMessage.value = if (archived) {
                "📦 تم نقل الصنف '${originalItem?.name ?: ""}' إلى الأرشيف ومزامنته سحابياً 🟢"
            } else {
                "🔄 تمت استعادة الصنف '${originalItem?.name ?: ""}' وتنشيطه سحابياً 🟢"
            }
        }
    }

    private fun resolveExistingCloudIdsForDelete(item: MenuItem): List<String> {
        val ids = mutableSetOf<String>()
        if (item.cloudId.isNotBlank() && item.cloudId != "item_unknown") {
            ids.add(item.cloudId)
        } else {
            val normName = repository.normalizeArabicText(item.name)
            val normCat = repository.normalizeArabicText(item.category)
            lastObservedCloudMenuItems
                .filter {
                    repository.normalizeArabicText(it.name) == normName &&
                        repository.normalizeArabicText(it.category) == normCat &&
                        it.unit.trim().equals(item.unit.trim(), ignoreCase = true) &&
                        Math.abs(it.price - item.basePrice) < 0.01
                }
                .mapTo(ids) { it.id }
        }
        return ids.toList()
    }

    fun deleteMenuItem(id: Long) {
        viewModelScope.launch {
            val itm = repository.getMenuItemById(id)
                ?: repository.getAllMenuItemsIncludingArchivedList().firstOrNull { it.id == id }
            val cloudIds = if (itm != null) resolveExistingCloudIdsForDelete(itm) else emptyList()
            repository.deleteMenuItem(id)
            val user = cloudUser.value
            if (cloudIds.isNotEmpty()) {
                recordMenuItemTombstones(cloudIds, enqueueCloudDelete = true)
                if (user != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val failed = withTimeoutOrNull(12_000L) {
                            cloudRepository.deleteMenuItemsWithTombstone(user.uid, cloudIds)
                        } ?: cloudIds.toSet()
                        val succeeded = cloudIds.filterNot { it in failed }
                        if (succeeded.isNotEmpty()) {
                            clearPendingMenuDeletes(succeeded)
                        }
                    }
                }
            }
            userMessage.value = "🗑️ تم حذف الصنف نهائياً ومزامنة الحذف مع جميع الأجهزة 🟢"
        }
    }

    fun purgeAllArchivedMenuItems() {
        viewModelScope.launch {
            val archived = repository.getAllMenuItemsIncludingArchivedList().filter { it.isArchived }
            val cloudIds = archived.flatMap { resolveExistingCloudIdsForDelete(it) }.distinct()
            for (item in archived) {
                repository.deleteMenuItem(item.id)
            }
            val user = cloudUser.value
            if (cloudIds.isNotEmpty()) {
                recordMenuItemTombstones(cloudIds, enqueueCloudDelete = true)
                if (user != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val failed = withTimeoutOrNull(15_000L) {
                            cloudRepository.deleteMenuItemsWithTombstone(user.uid, cloudIds)
                        } ?: cloudIds.toSet()
                        val succeeded = cloudIds.filterNot { it in failed }
                        if (succeeded.isNotEmpty()) {
                            clearPendingMenuDeletes(succeeded)
                        }
                    }
                }
            }
            userMessage.value = "تم تنظيف وحذف ${archived.size} صنف مؤرشف نهائياً ومزامنة الحذف لجميع الأجهزة 🗑️"
        }
    }

    // Preparation CRUD
    fun savePreparation(prep: PreparationType) {
        viewModelScope.launch {
            val savedId = repository.savePreparation(prep)
            val fullPrep = if (prep.id > 0) prep else prep.copy(id = savedId)
            
            userMessage.value = "تم حفظ طريقة التجهيز بنجاح"
        }
    }

    fun deletePreparation(id: Long) {
        viewModelScope.launch {
            val existing = repository.getAllPreparationsList().firstOrNull { it.id == id }
            repository.deletePreparation(id)
            
            userMessage.value = "تم حذف طريقة التجهيز"
        }
    }

    // Invoice Specific Items Editing
    fun updateOrderItem(orderId: Long, item: OrderItem) {
        viewModelScope.launch {
            repository.updateOrderItemAndRecalculate(orderId, item)
            pushFullOrderUpdateToCloud(orderId)
            userMessage.value = "تم تعديل الصنف داخل الفاتورة ومزامنته سحابياً ✓"
        }
    }

    fun addCustomItemToOrder(orderId: Long, item: OrderItem) {
        viewModelScope.launch {
            repository.addAdHocItemToExistingOrder(orderId, item)
            pushFullOrderUpdateToCloud(orderId)
            userMessage.value = "تمت إضافة الصنف الاستثنائي للفاتورة ومزامنته سحابياً ✓"
        }
    }

    fun addItemToOrder(orderId: Long, item: OrderItem) {
        viewModelScope.launch {
            repository.addOrderItemToExistingOrder(orderId, item)
            pushFullOrderUpdateToCloud(orderId)
            userMessage.value = "تمت إضافة الصنف للفاتورة ومزامنته سحابياً ✓"
        }
    }

    fun getOrderByIdFlow(orderId: Long): Flow<OrderWithItems?> {
        return repository.getOrderByIdFlow(orderId)
    }

    fun deleteOrderItemFromOrder(orderId: Long, itemId: Long) {
        viewModelScope.launch {
            repository.deleteOrderItemFromOrder(orderId, itemId)
            pushFullOrderUpdateToCloud(orderId)
            userMessage.value = "تم حذف الصنف من الفاتورة وتحديث الإجمالي والمزامنة ✓"
        }
    }

    // Printer Management & Real Print Dispatch
    fun loadPrinters() {
        viewModelScope.launch {
            _allPrinters.value = repository.getAllPrinterConfigs()
        }
    }

    fun printReceipt(
        orderId: Long,
        printerTarget: String,
        reprintReason: String? = null,
        targetDocumentType: String? = null
    ) {
        if (deviceRole.value == "SELLER") {
            val remoteLabel = remoteSyncedPrinters.value.firstOrNull { it.id == printerTarget }?.displayName ?: printerTarget
            viewModelScope.launch {
                sendRemotePrintRequestToCashier(orderId, printerTarget, reprintReason, remoteLabel)
            }
            return
        }

        printDiagnosticStatus.value = "تم الضغط على زر طباعة الفاتورة للأوردر #$orderId..."
        isPrintingOrder.value = true

        viewModelScope.launch {
            try {
                val config = repository.getPrinterConfig(printerTarget)
                setPrintDiagnostic("جارٍ الاتصال بـ ${config.displayName}...", autoClearSeconds = 2)

                val result = withTimeoutOrNull(30_000L) {
                    repository.dispatchPrint(orderId, printerTarget, reprintReason, targetDocumentType)
                } ?: PrinterResult.Error("انتهت مهلة الاتصال بالطابعة")
                isPrintingOrder.value = false

                when (result) {
                    is PrinterResult.Success -> {
                        setPrintDiagnostic("طُبعت الفاتورة: ${config.displayName} ✓")
                        postQueuedMessage(result.message)
                    }
                    is PrinterResult.Error -> {
                        val isConn = result.errorMessage.contains("connect", ignoreCase = true) || result.errorMessage.contains("timeout", ignoreCase = true)
                        val shortErr = if (isConn) "تعذر الاتصال بـ ${config.displayName}" else "خطأ طابعة: ${config.displayName}"
                        setPrintDiagnostic(shortErr, autoClearSeconds = 8)
                        postQueuedMessage("تعذر الطباعة على ${config.displayName}: ${result.errorMessage}")
                    }
                    is PrinterResult.PermissionRequired -> {
                        setPrintDiagnostic("مطلوب إذن (${config.displayName}): ${result.permissionName}", autoClearSeconds = 8)
                        postQueuedMessage("تعذر الطباعة على ${config.displayName}: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                isPrintingOrder.value = false
                setPrintDiagnostic("تعذر أمر الطباعة")
                postQueuedMessage("خطأ غير متوقع في الطباعة: ${e.localizedMessage}")
            }
        }
    }

    fun printKitchenAdditions(orderId: Long) {
        if (deviceRole.value == "SELLER") {
            viewModelScope.launch {
                sendRemotePrintRequestToCashier(orderId, "KITCHEN_ALL", "إضافة تشغيل", "أمر تشغيل المطبخ")
            }
            return
        }

        printDiagnosticStatus.value = "جارٍ إرسال أمر التشغيل للكميات المضافة للأوردر #$orderId..."
        isPrintingOrder.value = true

        viewModelScope.launch {
            try {
                val allConfigs = repository.getAllPrinterConfigs().filter { it.isEnabled }
                val kitchenConfigs = allConfigs.filter { it.role == "KITCHEN_ORDER" || it.role == "BOTH" || it.id.contains("KITCHEN") }

                val targets = if (kitchenConfigs.isNotEmpty()) {
                    kitchenConfigs.map { it.id }
                } else {
                    listOf("KITCHEN_1")
                }

                var printedAny = false
                val messages = mutableListOf<String>()
                val failedKitchenNames = mutableListOf<String>()

                for (target in targets) {
                    val kcName = kitchenConfigs.firstOrNull { it.id == target }?.displayName ?: target
                    val result = withTimeoutOrNull(30_000L) {
                        repository.dispatchPrint(orderId, target, "إضافة تشغيل", targetDocumentType = "KITCHEN_ORDER")
                    } ?: PrinterResult.Error("انتهت مهلة الاتصال بطابعة المطبخ")
                    when (result) {
                        is PrinterResult.Success -> {
                            printedAny = true
                            messages.add(result.message)
                        }
                        is PrinterResult.Error -> {
                            failedKitchenNames.add(kcName)
                            messages.add("$kcName: ${result.errorMessage}")
                        }
                        is PrinterResult.PermissionRequired -> {
                            failedKitchenNames.add(kcName)
                            messages.add("$kcName: ${result.message}")
                        }
                    }
                }
                isPrintingOrder.value = false
                if (failedKitchenNames.isEmpty() && printedAny) {
                    setPrintDiagnostic("تم إرسال أمر التشغيل للمطبخ بنجاح ✓")
                    postQueuedMessage(messages.joinToString(" | "))
                } else if (failedKitchenNames.isNotEmpty()) {
                    val failLabel = buildFailedPrintersLabel(failedKitchenNames, targets.size)
                    setPrintDiagnostic("تعذر الطباعة: $failLabel", autoClearSeconds = 8)
                    postQueuedMessage("تعذر الطباعة على $failLabel — ${messages.joinToString(" | ")}")
                } else {
                    postQueuedMessage(messages.joinToString(" | "))
                }
            } catch (e: Exception) {
                isPrintingOrder.value = false
                setPrintDiagnostic("تعذر أمر التشغيل")
                postQueuedMessage("خطأ غير متوقع: ${e.localizedMessage}")
            }
        }
    }

    fun testPrinter(printerId: String, onResult: (PrinterResult) -> Unit) {
        if (deviceRole.value == "SELLER") {
            onResult(PrinterResult.Error("اختبار والاتصال بالطابعات متاح فقط من جهاز الكاشير الرئيسي"))
            return
        }
        viewModelScope.launch {
            val result = repository.testPrinter(printerId)
            onResult(result)
        }
    }

    suspend fun getPrinterConfig(printerId: String): PrinterConfig {
        return repository.getPrinterConfig(printerId)
    }

    fun savePrinterConfig(config: PrinterConfig) {
        viewModelScope.launch {
            repository.savePrinterConfig(config)
            val updated = repository.getAllPrinterConfigs()
            _allPrinters.value = updated
            cloudUser.value?.uid?.let { uid ->
                launch(Dispatchers.IO) { coordinator.registerTerminalWithPrinters(uid) }
            }
            userMessage.value = "تم حفظ إعدادات الطابعة '${config.displayName}' بنجاح"
        }
    }

    fun deletePrinter(printerId: String) {
        viewModelScope.launch {
            try {
                val cleanId = printerId.removePrefix("p_").trim()
                repository.markPrinterDeletedTombstone(cleanId)
                repository.deletePrinterConfig(cleanId)
                val updated = repository.getAllPrinterConfigs()
                _allPrinters.value = updated
                cloudUser.value?.uid?.let { uid ->
                    launch(Dispatchers.IO) { coordinator.registerTerminalWithPrinters(uid) }
                }
                userMessage.value = "تم حذف الطابعة محلياً بنجاح ✓"
            } catch (e: Exception) {
                userMessage.value = "تعذر حذف الطابعة: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun clearAllPrinters() {
        viewModelScope.launch {
            try {
                val all = repository.getAllPrinterConfigs()
                for (p in all) {
                    repository.markPrinterDeletedTombstone(p.id)
                }
                repository.clearAllPrinters()
                _allPrinters.value = emptyList()
                cloudUser.value?.uid?.let { uid ->
                    launch(Dispatchers.IO) { coordinator.registerTerminalWithPrinters(uid) }
                }
                userMessage.value = "تم مسح كافة الطابعات محلياً بنجاح ✓"
            } catch (e: Exception) {
                userMessage.value = "تعذر مسح الطابعات: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun recordEmployeeAbsence(employeeId: Long, notes: String = "") {
        viewModelScope.launch {
            repository.recordEmployeeAbsence(employeeId, notes)
            userMessage.value = "تم تسجيل غياب الموظف بنجاح"
        }
    }

    suspend fun getInvoiceTemplateConfig(): InvoiceTemplateConfig {
        return repository.getInvoiceTemplateConfig()
    }

    fun saveInvoiceTemplateConfig(template: InvoiceTemplateConfig) {
        viewModelScope.launch {
            repository.saveInvoiceTemplateConfig(template)
            userMessage.value = "تم حفظ قالب وتنسيق الفاتورة بنجاح"
        }
    }

    fun updateDeliveryManifestFontSize(size: String) {
        viewModelScope.launch {
            try {
                val current = repository.getInvoiceTemplateConfig()
                val updated = current.copy(deliveryManifestFontSize = size)
                repository.saveInvoiceTemplateConfig(updated)
            } catch (_: Exception) {}
        }
    }

    suspend fun getKitchenOrderTemplateConfig(): KitchenOrderTemplateConfig {
        return repository.getKitchenOrderTemplateConfig()
    }

    fun saveKitchenOrderTemplateConfig(template: KitchenOrderTemplateConfig) {
        viewModelScope.launch {
            repository.saveKitchenOrderTemplateConfig(template)
            userMessage.value = "تم حفظ فورمة وتنسيق أمر التشغيل بنجاح ✓"
        }
    }

    fun saveRestaurantLogo(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = java.io.File(context.filesDir, "restaurant_logo.png")
                    val outputStream = java.io.FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    val current = repository.getInvoiceTemplateConfig()
                    repository.saveInvoiceTemplateConfig(current.copy(logoPath = file.absolutePath))
                    withContext(Dispatchers.Main) {
                        userMessage.value = "تم تعيين وحفظ لوجو المطعم بنجاح ✓"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    userMessage.value = "تعذر حفظ اللوجو: ${e.message}"
                }
            }
        }
    }

    fun removeRestaurantLogo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val file = java.io.File(context.filesDir, "restaurant_logo.png")
                if (file.exists()) {
                    file.delete()
                }
                val current = repository.getInvoiceTemplateConfig()
                repository.saveInvoiceTemplateConfig(current.copy(logoPath = null))
                withContext(Dispatchers.Main) {
                    userMessage.value = "تمت إزالة لوجو المطعم بنجاح"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    userMessage.value = "خطأ أثناء إزالة اللوجو: ${e.message}"
                }
            }
        }
    }

    val nextOrderNumber = MutableStateFlow(1)

    fun loadNextOrderNumber() {
        viewModelScope.launch {
            nextOrderNumber.value = repository.getNextOrderNumber()
        }
    }

    fun resetOrderCounter(startingNum: Int = 1) {
        setNextOrderNumber(startingNum)
    }

    fun setNextOrderNumber(num: Int) {
        viewModelScope.launch {
            val safeNum = if (num <= 0) 1 else num
            val cUser = cloudUser.value
            if (cUser != null && deviceRole.value != "CASHIER") {
                userMessage.value = "⚠️ تغيير أو تصفير ترقيم الفواتير متاح من جهاز الكاشير الرئيسي فقط أثناء الاتصال بالقاعدة المشتركة"
                return@launch
            }
            repository.resetOrderCounter(safeNum)
            nextOrderNumber.value = safeNum
            if (cUser != null) {
                repository.saveSetting("pending_cloud_order_counter_override", safeNum.toString())
                val cloudRes = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(10_000L) {
                        cloudRepository.setCloudOrderNumberOverride(cUser.uid, safeNum)
                    }
                }
                if (cloudRes?.isSuccess == true) {
                    repository.saveSetting("pending_cloud_order_counter_override", "")
                    userMessage.value = "تم تعيين رقم الأوردر التالي (#$safeNum) ومزامنته على السحابة المشتركة لجميع الأجهزة ✓"
                } else {
                    userMessage.value = "تم ضبط الرقم محلياً (#$safeNum) وسيتم تطبيقه على السحابة تلقائياً فور توفر الإنترنت ⏳"
                }
            } else {
                userMessage.value = "تم تعيين رقم الأوردر التالي: #$safeNum ✓"
            }
        }
    }

    fun setDefaultRestaurantLogo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val drawableId = com.example.R.drawable.ic_app_logo
                val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, drawableId)
                if (bitmap != null) {
                    val file = java.io.File(context.filesDir, "restaurant_logo.png")
                    val out = java.io.FileOutputStream(file)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                    out.close()
                    val current = repository.getInvoiceTemplateConfig()
                    repository.saveInvoiceTemplateConfig(current.copy(logoPath = file.absolutePath))
                    withContext(Dispatchers.Main) {
                        userMessage.value = "تم تعيين لوجو وشعار المطعم الافتراضي للفاتورة بنجاح ✓"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    userMessage.value = "تعذر تعيين اللوجو: ${e.message}"
                }
            }
        }
    }

    fun getPairedBluetoothPrinters(): List<Pair<String, String>> {
        return repository.printerManager.getPairedBluetoothDevices()
    }

    fun getConnectedUsbPrinters(): List<com.example.printer.UsbPrinterInfo> {
        return repository.printerManager.getConnectedUsbPrinters()
    }

    fun requestUsbPermission(device: android.hardware.usb.UsbDevice) {
        repository.printerManager.requestUsbPermission(device)
    }

    fun openCashDrawer(printerId: String, onResult: (com.example.printer.PrinterResult) -> Unit) {
        viewModelScope.launch {
            val res = repository.openCashDrawer(printerId)
            onResult(res)
        }
    }

    /**
     * Auto-discovers thermal printers across the local network (Easy Thermal Print simulation).
     */
    fun scanNetworkPrinters() {
        if (deviceRole.value == "SELLER") {
            userMessage.value = "البحث عن الطابعات والاتصال بها متاح فقط من جهاز الكاشير الرئيسي"
            return
        }
        viewModelScope.launch {
            isScanningNetworkPrinters.value = true
            try {
                val list = NetworkPrinterScanner.scanLocalNetworkForPrinters(getApplication())
                discoveredNetworkPrinters.value = list
                if (list.isEmpty()) {
                    userMessage.value = "لم يتم العثور على طابعات حرارية تلقائياً. تأكد من اتصال هاتفك بنفس شبكة الراوتر (واي فاي)."
                } else {
                    userMessage.value = "تم العثور على ${list.size} طابعة شبكة بنجاح ✓"
                }
            } catch (e: Exception) {
                userMessage.value = "خطأ أثناء فحص الشبكة: ${e.localizedMessage}"
            } finally {
                isScanningNetworkPrinters.value = false
            }
        }
    }

    /**
     * Prints order directly via Android Print Framework.
     */
    fun printOrderViaSystemSpooler(orderId: Long, targetId: String = "MAIN") {
        if (deviceRole.value == "SELLER") {
            userMessage.value = "الطباعة متاحة حصرياً من جهاز الكاشير الرئيسي"
            return
        }
        viewModelScope.launch {
            val orderWithItems = repository.getOrderByIdDirect(orderId)
            if (orderWithItems != null) {
                val template = repository.getInvoiceTemplateConfig()
                val ok = repository.printerManager.printDirectViaSystemSpooler(orderWithItems, template, targetId)
                if (ok) {
                    userMessage.value = "تم فتح نافذة الطباعة عبر نظام أندرويد ✓"
                } else {
                    userMessage.value = "تعذر فتح نافذة طباعة أندرويد"
                }
            } else {
                userMessage.value = "الأوردر غير موجود"
            }
        }
    }

    fun loadOwnerWhatsAppPhone() {
        viewModelScope.launch {
            ownerWhatsAppPhone.value = repository.getOwnerWhatsAppPhone()
        }
    }

    fun saveOwnerWhatsAppPhone(phone: String) {
        viewModelScope.launch {
            repository.setOwnerWhatsAppPhone(phone)
            ownerWhatsAppPhone.value = phone.trim()
            userMessage.value = "تم حفظ رقم هاتف الأونر لمراسلة التنبيهات ✓"
        }
    }

    fun sendWhatsAppLateNotification(
        ownerPhone: String,
        employee: Employee,
        actualTime: String,
        delayMinutes: Int,
        dateStr: String,
        context: Context
    ) {
        try {
            val intent = repository.createWhatsAppLateEmployeeIntent(
                ownerPhone = ownerPhone,
                employeeName = employee.name,
                employeeRole = employee.role,
                scheduledTime = employee.shiftStartTime,
                actualTime = actualTime,
                delayMinutes = delayMinutes,
                dateStr = dateStr
            )
            context.startActivity(intent)
            userMessage.value = "جارٍ إرسال تنبيه التأخير للأونر عبر واتساب 🐦..."
        } catch (e: Exception) {
            userMessage.value = "تعذر فتح تطبيق واتساب: ${e.localizedMessage}"
        }
    }

    fun recordEmployeeCheckIn(
        employeeId: Long,
        actualTimeString: String,
        delayMinutes: Int,
        lateAction: String?,
        penaltyAmount: Double,
        notes: String,
        date: Long
    ) {
        viewModelScope.launch {
            repository.recordEmployeeCheckIn(
                employeeId = employeeId,
                actualTimeString = actualTimeString,
                delayMinutes = delayMinutes,
                lateAction = lateAction,
                penaltyAmount = penaltyAmount,
                notes = notes,
                date = date
            )
            userMessage.value = "تم تسجيل الحضور بنجاح ✓"
        }
    }

    fun recordEmployeeCheckOut(
        employeeId: Long,
        departureTimeString: String,
        actualHoursWorked: Double,
        overtimeHours: Double,
        notes: String,
        date: Long
    ) {
        viewModelScope.launch {
            repository.recordEmployeeCheckOut(
                employeeId = employeeId,
                departureTimeString = departureTimeString,
                actualHoursWorked = actualHoursWorked,
                overtimeHours = overtimeHours,
                notes = notes,
                date = date
            )
            userMessage.value = "تم تسجيل الانصراف بنجاح ✓"
        }
    }

    // Statements
    fun loadEmployeeStatement(employeeId: Long, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val st = repository.generateEmployeeStatement(employeeId, startDate, endDate)
            activeEmployeeStatement.value = st
        }
    }

    fun loadDriverStatement(driverId: Long, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val st = repository.generateDriverStatement(driverId, startDate, endDate)
            activeDriverStatement.value = st
        }
    }

    fun closeStatements() {
        activeEmployeeStatement.value = null
        activeDriverStatement.value = null
    }

    // Staff & Driver CRUD
    fun saveEmployee(employee: Employee) {
        viewModelScope.launch {
            val savedId = repository.saveEmployee(employee)
            userMessage.value = "تم حفظ بيانات الموظف بنجاح"
        }
    }

    fun archiveEmployee(id: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveEmployee(id, archived)
            userMessage.value = if (archived) "تم أرشفة الموظف" else "تم إلغاء أرشفة الموظف"
        }
    }

    fun deleteEmployee(id: Long) {
        viewModelScope.launch {
            repository.deleteEmployee(id)
            userMessage.value = "تم حذف الموظف نهائياً بنجاح"
        }
    }

    fun saveDriver(driver: DeliveryDriver) {
        viewModelScope.launch {
            val savedId = repository.saveDriver(driver)
            userMessage.value = "تم حفظ بيانات كابتن التوصيل"
        }
    }

    fun archiveDriver(id: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveDriver(id, archived)
            userMessage.value = if (archived) "تم أرشفة السائق" else "تم إلغاء أرشفة السائق"
        }
    }

    fun deleteDriver(id: Long) {
        viewModelScope.launch {
            repository.deleteDriver(id)
            userMessage.value = "تم حذف السائق نهائياً بنجاح"
        }
    }

    // Inventory operations
    fun receiveShipment(menuItemId: Long, quantity: Double, notes: String) {
        viewModelScope.launch {
            val itm = repository.getMenuItemById(menuItemId) ?: return@launch
            repository.receiveInventoryBatch(menuItemId, quantity, null, notes)
            val updated = repository.getMenuItemById(menuItemId)
            if (updated != null) {
                pushMenuItemToCloud(updated)
            }
            userMessage.value = "تمت إضافة رصيد المخزون ومزامنته سحابياً بنجاح ($quantity كجم) ✓"
        }
    }

    fun adjustStock(menuItemId: Long, newBalance: Double, reason: String) {
        viewModelScope.launch {
            val itm = repository.getMenuItemById(menuItemId) ?: return@launch
            repository.adjustInventory(menuItemId, newBalance, reason)
            val updated = repository.getMenuItemById(menuItemId)
            if (updated != null) {
                pushMenuItemToCloud(updated)
            }
            userMessage.value = "تم تسجيل التسوية وتحديث رصيد المخزون ومزامنته سحابياً بنجاح ✓"
        }
    }

    fun saveDailyPrices(updates: Map<Long, Double>) {
        if (updates.isEmpty()) return
        viewModelScope.launch {
            repository.updateDailyPrices(updates)
            val allItems = repository.getAllMenuItemsIncludingArchivedList()
            val affected = allItems.filter { updates.containsKey(it.id) }
            for (itm in affected) {
                pushMenuItemToCloud(itm.copy(basePrice = updates[itm.id] ?: itm.basePrice))
            }
            userMessage.value = "تم تحديث أسعار الأسماك اليومية ومزامنتها سحابياً للجميع ✓"
            dismissDailyPriceReminder()
        }
    }

    // Order cancellation
    fun cancelOrder(orderId: Long, passwordInput: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (verifyManagerPassword(passwordInput)) {
                val existingOrd = repository.getOrderByIdDirect(orderId)?.order
                val affectedMenuIds = repository.cancelOrderAndGetAffectedMenuIds(orderId, "إلغاء بواسطة الإدارة")
                val cUser = cloudUser.value
                if (cUser != null) {
                    val cloudId = existingOrd?.cloudOrderId?.takeIf { it.isNotBlank() }
                    if (cloudId != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            cloudRepository.updateCloudOrderStatusAndArchive(cUser.uid, cloudId, status = "CANCELLED")
                        }
                    }
                    if (affectedMenuIds.isNotEmpty()) {
                        val updatedItems = repository.getAllMenuItemsIncludingArchivedList().filter { it.id in affectedMenuIds }
                        for (itm in updatedItems) {
                            pushMenuItemToCloud(itm)
                        }
                    }
                }
                userMessage.value = "تم إلغاء الأوردر واسترجاع الكميات للمخزون ومزامنته سحابياً ✓"
                onComplete(true)
            } else {
                userMessage.value = "كلمة مرور الإدارة غير صحيحة"
                onComplete(false)
            }
        }
    }

    // Order Archiving
    fun archiveOrder(orderId: Long) {
        viewModelScope.launch {
            val ord = repository.getOrderByIdDirect(orderId)?.order
            val cloudId = ord?.cloudOrderId?.takeIf { it.isNotBlank() }
            if (cloudId != null) {
                enqueuePendingCloudArchive(mapOf(cloudId to true))
            }
            repository.setOrderArchived(orderId, true)
            val cUser = cloudUser.value
            if (cUser != null && cloudId != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val failed = withTimeoutOrNull(12_000L) {
                        cloudRepository.batchArchiveCloudOrdersDetailed(cUser.uid, listOf(cloudId), true)
                    } ?: setOf(cloudId)
                    if (cloudId !in failed) {
                        clearPendingCloudArchive(listOf(cloudId))
                    }
                }
            }
            userMessage.value = "تم ترحيل الأوردر إلى قائمة 'الأوردرات المؤرشفة' ومزامنته سحابياً 📦"
        }
    }

    fun unarchiveOrder(orderId: Long) {
        viewModelScope.launch {
            val ord = repository.getOrderByIdDirect(orderId)?.order
            val cloudId = ord?.cloudOrderId?.takeIf { it.isNotBlank() }
            if (cloudId != null) {
                enqueuePendingCloudArchive(mapOf(cloudId to false))
            }
            repository.setOrderArchived(orderId, false)
            val cUser = cloudUser.value
            if (cUser != null && cloudId != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val failed = withTimeoutOrNull(12_000L) {
                        cloudRepository.batchArchiveCloudOrdersDetailed(cUser.uid, listOf(cloudId), false)
                    } ?: setOf(cloudId)
                    if (cloudId !in failed) {
                        clearPendingCloudArchive(listOf(cloudId))
                    }
                }
            }
            userMessage.value = "تم إلغاء أرشفة الأوردر وإعادته لقائمة الأوردرات النشطة سحابياً ✓"
        }
    }

    fun archiveAllOrders(orderIds: List<Long>) {
        if (orderIds.isEmpty()) return
        viewModelScope.launch {
            val idSet = orderIds.toSet()
            val allLocal = repository.getAllOrdersDirect()
            val cloudIds = allLocal.filter { it.order.id in idSet }
                .mapNotNull { it.order.cloudOrderId?.takeIf { cid -> cid.isNotBlank() } }
                .distinct()
            if (cloudIds.isNotEmpty()) {
                enqueuePendingCloudArchive(cloudIds.associateWith { true })
            }
            repository.archiveOrders(orderIds)
            val cUser = cloudUser.value
            if (cUser != null && cloudIds.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val failed = withTimeoutOrNull(15_000L) {
                        cloudRepository.batchArchiveCloudOrdersDetailed(cUser.uid, cloudIds, true)
                    } ?: cloudIds.toSet()
                    val succeeded = cloudIds.filterNot { it in failed }
                    if (succeeded.isNotEmpty()) {
                        clearPendingCloudArchive(succeeded)
                    }
                }
            }
            userMessage.value = "تم أرشفة ${orderIds.size} أوردر ونقلهم للأرشيف ومزامنتهم لجميع الأجهزة 📦"
        }
    }

    // Direct Order Status & Dispatch Updates
    fun updateOrderStatus(orderId: Long, newStatus: String) {
        viewModelScope.launch {
            val orderWithItems = orders.value.firstOrNull { it.order.id == orderId } ?: repository.getOrderByIdDirect(orderId)
            repository.updateOrderStatus(orderId, newStatus)
            val cUser = cloudUser.value
            val cloudId = orderWithItems?.order?.cloudOrderId?.takeIf { it.isNotBlank() }
            if (cUser != null && cloudId != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepository.updateCloudOrderStatusAndArchive(cUser.uid, cloudId, status = newStatus)
                }
            }
            val isTakeaway = orderWithItems?.order?.orderType != "DELIVERY"
            val statusArabic = when (newStatus) {
                "NEW" -> if (isTakeaway) "جديد في المحل (غير محسوب بالدرج بعد)" else "جديد"
                "PREPARING" -> if (isTakeaway) "قيد التجهيز بالمحل (غير محسوب بالدرج)" else "قيد التجهيز بالمحل"
                "OUT_FOR_DELIVERY" -> "خارج للتوصيل (خرج من المحل)"
                "COMPLETED" -> if (isTakeaway) "تم تأكيد استلام العميل وخروج الطلب من المحل (أضيف للخزينة بنجاح ✓)" else "تم التسليم بنجاح"
                "CANCELLED" -> "ملغي ومسترجع للمخزن"
                else -> newStatus
            }
            userMessage.value = "تم تحديث الأوردر: $statusArabic"
        }
    }

    fun updateOrderPayment(orderId: Long, paidAmount: Double, changeAmount: Double, isPaid: Boolean) {
        viewModelScope.launch {
            repository.updateOrderPayment(orderId, paidAmount, changeAmount, isPaid)
            val msg = if (isPaid) "تم تسجيل الأوردر كـ خالص بالكامل ✓" else "تم تسجيل سداد مبلغ $paidAmount ج.م بنجاح ✓"
            userMessage.value = msg
        }
    }

    fun dispatchOrderWithDriver(orderId: Long, driver: DeliveryDriver) {
        viewModelScope.launch {
            val ord = repository.getOrderByIdDirect(orderId)?.order
            repository.assignDriverToOrder(orderId, driver.id, driver.name, driver.ratePerOrder, "خروج مع الكابتن")
            repository.updateOrderStatus(orderId, "OUT_FOR_DELIVERY")
            val cUser = cloudUser.value
            val cloudId = ord?.cloudOrderId?.takeIf { it.isNotBlank() }
            if (cUser != null && cloudId != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepository.updateCloudOrderStatusAndArchive(
                        userId = cUser.uid,
                        orderId = cloudId,
                        status = "OUT_FOR_DELIVERY",
                        driverName = driver.name
                    )
                }
            }
            userMessage.value = "تم خروج الأوردر للتوصيل مع الكابتن ${driver.name}"
        }
    }

    // Driver Settlement
    suspend fun getTodayOrdersForDriver(driverId: Long): List<OrderWithItems> {
        return repository.getTodayOrdersForDriver(driverId)
    }

    suspend fun getTodayUnsettledOrdersForDriver(driverId: Long): List<OrderWithItems> {
        return repository.getTodayUnsettledOrdersForDriver(driverId)
    }

    fun settleDriver(
        driverId: Long,
        fuelExpenses: Double,
        actualHandover: Double = 0.0,
        customCommissions: Map<Long, Double> = emptyMap(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            val unsettled = try { repository.getTodayUnsettledOrdersForDriver(driverId) } catch (_: Exception) { emptyList() }
            val st = repository.calculateAndSaveDriverSettlement(
                driverId = driverId,
                fuelExpenses = fuelExpenses,
                actualHandoverAmount = actualHandover,
                customOrderCommissions = customCommissions,
                notes = notes
            )
            if (st != null) {
                val settledIdSet = st.settledOrderIds
                    .split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
                    .toSet()
                val settledOrders = if (settledIdSet.isNotEmpty()) {
                    unsettled.filter { it.order.id in settledIdSet }
                        .ifEmpty { repository.getAllOrdersDirect().filter { it.order.id in settledIdSet } }
                } else emptyList()
                val settledCloudIds = settledOrders
                    .mapNotNull { it.order.cloudOrderId?.takeIf { cid -> cid.isNotBlank() } }
                    .distinct()
                val completedTransitionCloudIds = settledOrders
                    .filter { it.order.status == "OUT_FOR_DELIVERY" }
                    .mapNotNull { it.order.cloudOrderId?.takeIf { cid -> cid.isNotBlank() } }
                    .distinct()
                val archiveOnlyCloudIds = (settledCloudIds - completedTransitionCloudIds.toSet()).distinct()

                if (settledCloudIds.isNotEmpty()) {
                    enqueuePendingCloudArchive(settledCloudIds.associateWith { true })
                    val cUser = cloudUser.value
                    if (cUser != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            val failedCompleted = if (completedTransitionCloudIds.isNotEmpty()) {
                                withTimeoutOrNull(15_000L) {
                                    cloudRepository.batchArchiveCloudOrdersDetailed(
                                        userId = cUser.uid,
                                        orderIds = completedTransitionCloudIds,
                                        isArchived = true,
                                        statusOnArchive = "COMPLETED"
                                    )
                                } ?: completedTransitionCloudIds.toSet()
                            } else emptySet()

                            val failedArchiveOnly = if (archiveOnlyCloudIds.isNotEmpty()) {
                                withTimeoutOrNull(15_000L) {
                                    cloudRepository.batchArchiveCloudOrdersDetailed(
                                        userId = cUser.uid,
                                        orderIds = archiveOnlyCloudIds,
                                        isArchived = true
                                    )
                                } ?: archiveOnlyCloudIds.toSet()
                            } else emptySet()

                            val allFailed = failedCompleted + failedArchiveOnly
                            val succeeded = settledCloudIds.filterNot { it in allFailed }
                            if (succeeded.isNotEmpty()) {
                                clearPendingCloudArchive(succeeded)
                            }
                        }
                    }
                }

                val diffText = when {
                    st.financialDifference > 0 -> " (فائض: +${String.format(Locale.US, "%.1f", st.financialDifference)} ج.م)"
                    st.financialDifference < 0 -> " (عجز: ${String.format(Locale.US, "%.1f", st.financialDifference)} ج.م)"
                    else -> " (مطابق تماماً)"
                }
                userMessage.value = "تمت تسوية حساب الكابتن ${st.driverName} وأرشفة (${st.ordersCount} أوردر) بنجاح ✓. الصافي المسلّم: ${String.format(Locale.US, "%.1f", st.actualHandoverAmount)} ج.م$diffText"
            }
        }
    }

    // Delivery Area CRUD
    fun saveDeliveryArea(area: DeliveryArea) {
        viewModelScope.launch {
            val savedId = repository.saveDeliveryArea(area)
            val fullArea = if (area.id > 0) area else area.copy(id = savedId)
            
            userMessage.value = "تم حفظ منطقة التوصيل بنجاح"
        }
    }

    fun deleteDeliveryArea(id: Long) {
        viewModelScope.launch {
            repository.deleteDeliveryArea(id)
            
            userMessage.value = "تم حذف منطقة التوصيل"
        }
    }

    // Customer & Order detail modifications
    fun updateOrderCustomerDetails(
        orderId: Long,
        orderType: String,
        driverId: Long?,
        driverName: String?,
        name: String,
        phone: String,
        secondaryPhone: String? = null,
        area: String?,
        address: String?,
        fee: Double,
        driverCommission: Double,
        notes: String
    ) {
        viewModelScope.launch {
            repository.updateOrderCustomerDetails(
                orderId = orderId,
                orderType = orderType,
                driverId = driverId,
                driverName = driverName,
                name = name,
                phone = phone,
                secondaryPhone = secondaryPhone,
                area = area,
                address = address,
                fee = fee,
                driverCommission = driverCommission,
                notes = notes
            )
            pushFullOrderUpdateToCloud(orderId)
            userMessage.value = "تم تعديل بيانات التوصيل والعميل للأوردر ومزامنتها سحابياً ✓"
        }
    }


    fun updateCustomerPermanent(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomerPermanent(customer)
            val addrs = repository.getAddressesForCustomer(customer.id)
            
            userMessage.value = "تم تحديث بيانات العميل الدائمة"
        }
    }

    // Staff actions
    fun addAttendance(
        employeeId: Long,
        status: String,
        hours: Double,
        overtimeHours: Double = 0.0,
        notes: String = "",
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.recordAttendance(
                EmployeeAttendance(
                    employeeId = employeeId,
                    date = date,
                    status = status,
                    hoursWorked = hours,
                    overtimeHours = overtimeHours,
                    notes = notes
                )
            )
            userMessage.value = "تم تسجيل الحضور وساعات العمل بنجاح"
            val st = activeEmployeeStatement.value
            if (st != null && st.employee.id == employeeId) {
                loadEmployeeStatement(employeeId, st.startDate, st.endDate)
            }
        }
    }

    fun addEmployeeTransaction(
        employeeId: Long,
        type: String,
        amount: Double,
        reason: String,
        date: Long = System.currentTimeMillis(),
        isDeductedFromDrawer: Boolean = (type == "ADVANCE" || type == "SALARY_PAYOUT")
    ) {
        viewModelScope.launch {
            repository.recordEmployeeTransaction(
                EmployeeTransaction(
                    employeeId = employeeId,
                    type = type,
                    amount = amount,
                    reason = reason,
                    date = date,
                    isDeductedFromDrawer = isDeductedFromDrawer
                )
            )
            userMessage.value = "تم تسجيل العملية المالية بنجاح"
            val st = activeEmployeeStatement.value
            if (st != null && st.employee.id == employeeId) {
                loadEmployeeStatement(employeeId, st.startDate, st.endDate)
            }
        }
    }

    fun settleEmployeeSalary(employeeId: Long, amount: Double, reason: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.settleEmployeeSalary(employeeId, amount, reason, date)
            userMessage.value = "تم صرف الراتب وتسوية الحساب بنجاح"
            val st = activeEmployeeStatement.value
            if (st != null && st.employee.id == employeeId) {
                loadEmployeeStatement(employeeId, st.startDate, st.endDate)
            }
        }
    }

    suspend fun verifyManagerPassword(password: String): Boolean {
        return repository.verifyManagerPassword(password)
    }

    fun setManagerPassword(newPassword: String) {
        viewModelScope.launch {
            repository.saveSetting("managerPassword", newPassword)
            userMessage.value = "تم تحديث كلمة سر الإدارة بنجاح"
        }
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
            userMessage.value = "تم حفظ الإعداد بنجاح"
        }
    }

    fun clearUserMessage() {
        userMessage.value = null
    }

    // Shifts Management
    fun openShift(cashierName: String, openingBalance: Double) {
        viewModelScope.launch {
            val shift = repository.openNewShift(cashierName, openingBalance)
            val cUser = cloudUser.value
            if (cUser != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepository.upsertShift(cUser.uid, shift.toCloudShiftDoc())
                }
            }
            userMessage.value = "تم فتح الوردية رقم #${shift.shiftNumber} ومزامنتها لجميع الأجهزة للكاشير $cashierName"
        }
    }

    fun closeShift(actualCash: Double, expenses: Double, notes: String) {
        viewModelScope.launch {
            // Collect ALL currently active orders BEFORE closing so 100% of archived cloud orders sync cleanly
            val preCloseUnarchivedCloudIds = repository.getAllOrdersDirect()
                .filter { !it.order.isArchived }
                .mapNotNull { it.order.cloudOrderId?.takeIf { cid -> cid.isNotBlank() } }

            val closed = repository.closeActiveShift(actualCash, expenses, notes)
            if (closed != null) {
                recentlyClosedShift.value = closed
                val shiftOrdersCloudIds = repository.getOrdersForShift(closed.id)
                    .mapNotNull { it.order.cloudOrderId?.takeIf { cid -> cid.isNotBlank() } }
                val cloudIds = (preCloseUnarchivedCloudIds + shiftOrdersCloudIds).distinct()
                if (cloudIds.isNotEmpty()) {
                    enqueuePendingCloudArchive(cloudIds.associateWith { true })
                }

                val cUser = cloudUser.value
                if (cUser != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        cloudRepository.upsertShift(cUser.uid, closed.toCloudShiftDoc())
                        if (cloudIds.isNotEmpty()) {
                            val failed = withTimeoutOrNull(15_000L) {
                                cloudRepository.batchArchiveCloudOrdersDetailed(cUser.uid, cloudIds, true)
                            } ?: cloudIds.toSet()
                            val succeeded = cloudIds.filterNot { it in failed }
                            if (succeeded.isNotEmpty()) {
                                clearPendingCloudArchive(succeeded)
                            }
                        }
                    }
                }

                val diffText = when {
                    closed.difference > 0 -> " (فائض: +${String.format(Locale.US, "%.1f", closed.difference)} ج.م)"
                    closed.difference < 0 -> " (عجز: ${String.format(Locale.US, "%.1f", closed.difference)} ج.م)"
                    else -> " (مطابق تماماً)"
                }
                userMessage.value = "تم تقفيل الوردية #${closed.shiftNumber} وأرشفة أوردراتها ومزامنتها للجميع بنجاح. إجمالي الكاش المسلم: ${String.format(Locale.US, "%.1f", closed.actualDrawerCash)} ج.م$diffText"
            }
        }
    }

    // Recipe / Ingredients Management
    fun saveIngredient(ingredient: MenuItemIngredient) {
        viewModelScope.launch {
            repository.addIngredientToItem(ingredient)
            userMessage.value = "تم ربط المكون/الخام بالصنف بنجاح"
        }
    }

    fun deleteIngredient(id: Long) {
        viewModelScope.launch {
            repository.deleteIngredient(id)
            userMessage.value = "تم فك ارتباط المكون"
        }
    }

    suspend fun checkStockForCartItem(menuItemId: Long, quantity: Double): IngredientCheckResult {
        return repository.checkItemAndIngredientsStock(menuItemId, quantity)
    }

    // Customer & Address Management
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            val savedId = repository.saveCustomer(customer)
            val fullCust = if (customer.id > 0) customer else customer.copy(id = savedId)
            val addrs = repository.getAddressesForCustomer(fullCust.id)
            
            userMessage.value = "تم حفظ بيانات العميل بنجاح"
        }
    }

    fun saveCustomerAddress(address: CustomerAddress) {
        viewModelScope.launch {
            repository.saveCustomerAddress(address)
            val cust = repository.getAllCustomersList().firstOrNull { it.id == address.customerId }
            if (cust != null) {
                val addrs = repository.getAddressesForCustomer(cust.id)
                
            }
            userMessage.value = "تم حفظ العنوان بنجاح"
        }
    }

    fun deleteCustomerAddress(id: Long) {
        viewModelScope.launch {
            val allCusts = repository.getAllCustomersList()
            var targetCust: Customer? = null
            for (c in allCusts) {
                val addrs = repository.getAddressesForCustomer(c.id)
                if (addrs.any { it.id == id }) {
                    targetCust = c
                    break
                }
            }
            repository.deleteCustomerAddress(id)
            if (targetCust != null) {
                val remainingAddrs = repository.getAddressesForCustomer(targetCust.id)
                
            }
            userMessage.value = "تم حذف العنوان بنجاح"
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            val cust = repository.getAllCustomersList().firstOrNull { it.id == id }
            repository.deleteCustomer(id)
            
            userMessage.value = "تم حذف العميل بنجاح"
        }
    }

    // Driver Settlement Flexibility
    fun excludeOrderFromDriver(orderId: Long, isExcluded: Boolean, reason: String?) {
        viewModelScope.launch {
            repository.excludeOrderFromDriver(orderId, isExcluded, reason)
            userMessage.value = if (isExcluded) "تم استبعاد الأوردر من تسوية الموصل ($reason)" else "تم استعادة الأوردر لتسوية الموصل"
        }
    }

    fun assignDriverToOrder(orderId: Long, driverId: Long, driverName: String, commission: Double, reason: String? = null) {
        viewModelScope.launch {
            val ord = repository.getOrderByIdDirect(orderId)?.order
            repository.assignDriverToOrder(orderId, driverId, driverName, commission, reason)
            val cUser = cloudUser.value
            val cloudId = ord?.cloudOrderId?.takeIf { it.isNotBlank() }
            if (cUser != null && cloudId != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    cloudRepository.updateCloudOrderStatusAndArchive(
                        userId = cUser.uid,
                        orderId = cloudId,
                        driverName = driverName
                    )
                }
            }
            userMessage.value = "تم ضم الأوردر لحساب الكابتن $driverName"
        }
    }

    suspend fun getUnsettledDeliveryOrders(): List<OrderWithItems> {
        return repository.getAllUnsettledDeliveryOrders()
    }

    // Restock Item
    fun addStockToItem(itemId: Long, addQty: Double, reason: String = "توريد واستلام مخزون") {
        viewModelScope.launch {
            val item = repository.getMenuItemById(itemId)
            if (item != null) {
                val newStock = item.currentStock + addQty
                repository.updateMenuItemStock(itemId, newStock, reason)
                val updated = repository.getMenuItemById(itemId) ?: item.copy(currentStock = newStock)
                pushMenuItemToCloud(updated)
                userMessage.value = "تمت إضافة $addQty ${item.unit} لرصيد ${item.name} ومزامنته سحابياً ✓"
            }
        }
    }

    // Staff attendance and transaction management
    fun updateAttendance(attendance: EmployeeAttendance) {
        viewModelScope.launch {
            repository.updateAttendance(attendance)
            userMessage.value = "تم تحديث سجل الحضور بنجاح"
            val st = activeEmployeeStatement.value
            if (st != null && st.employee.id == attendance.employeeId) {
                loadEmployeeStatement(attendance.employeeId, st.startDate, st.endDate)
            }
        }
    }

    fun deleteAttendance(id: Long, employeeId: Long) {
        viewModelScope.launch {
            repository.deleteAttendance(id)
            userMessage.value = "تم حذف سجل الحضور"
            val st = activeEmployeeStatement.value
            if (st != null && st.employee.id == employeeId) {
                loadEmployeeStatement(employeeId, st.startDate, st.endDate)
            }
        }
    }

    fun updateEmployeeTransaction(tx: EmployeeTransaction) {
        viewModelScope.launch {
            repository.updateEmployeeTransaction(tx)
            userMessage.value = "تم تعديل العملية المالية بنجاح"
            val st = activeEmployeeStatement.value
            if (st != null && st.employee.id == tx.employeeId) {
                loadEmployeeStatement(tx.employeeId, st.startDate, st.endDate)
            }
        }
    }

    fun deleteEmployeeTransaction(id: Long, employeeId: Long) {
        viewModelScope.launch {
            repository.deleteEmployeeTransaction(id)
            userMessage.value = "تم حذف العملية المالية"
            val st = activeEmployeeStatement.value
            if (st != null && st.employee.id == employeeId) {
                loadEmployeeStatement(employeeId, st.startDate, st.endDate)
            }
        }
    }

    // Advanced Reports
    fun loadAdvancedSalesReport(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val report = repository.getAdvancedSalesReport(startDate, endDate)
            advancedSalesReport.value = report
        }
    }

    // WhatsApp Sharing (supports customer phone or custom entered WhatsApp number)
    fun shareInvoiceViaWhatsApp(orderWithItems: OrderWithItems, customPhone: String?, context: Context) {
        viewModelScope.launch {
            val template = repository.getInvoiceTemplateConfig()
            val text = repository.generateWhatsAppInvoiceText(orderWithItems, template)
            val phoneToUse = if (!customPhone.isNullOrBlank()) customPhone.trim() else orderWithItems.order.customerPhone.trim()
            try {
                if (phoneToUse.isNotBlank()) {
                    val intent = repository.createWhatsAppInvoiceIntent(phoneToUse, text)
                    context.startActivity(intent)
                } else {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الفاتورة عبر واتساب"))
                }
            } catch (_: Exception) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "مشاركة الفاتورة"))
            }
        }
    }

    // Backward compatible overload
    fun shareInvoiceViaWhatsApp(orderWithItems: OrderWithItems, context: Context) {
        shareInvoiceViaWhatsApp(orderWithItems, null, context)
    }

    fun setSalesPeriod(period: com.example.data.model.SalesPeriodType) {
        selectedSalesPeriod.value = period
    }

    fun setCustomSalesPeriod(start: Long, end: Long) {
        customSalesStartDate.value = start
        customSalesEndDate.value = end
        selectedSalesPeriod.value = com.example.data.model.SalesPeriodType.CUSTOM
    }

        fun calculateSalesSummaryForPeriod(
        localOrders: List<OrderWithItems>,
        period: com.example.data.model.SalesPeriodType,
        customStart: Long? = null,
        customEnd: Long? = null
    ): TodaySalesSummary {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val (startMillis, endMillis) = when (period) {
            com.example.data.model.SalesPeriodType.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(s, cal.timeInMillis)
            }
            com.example.data.model.SalesPeriodType.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(s, cal.timeInMillis)
            }
            com.example.data.model.SalesPeriodType.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            com.example.data.model.SalesPeriodType.LAST_WEEK -> {
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(s, cal.timeInMillis)
            }
            com.example.data.model.SalesPeriodType.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            com.example.data.model.SalesPeriodType.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(s, cal.timeInMillis)
            }
            com.example.data.model.SalesPeriodType.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
            com.example.data.model.SalesPeriodType.CUSTOM -> {
                val s = customStart ?: (now - 86400000L)
                val e = customEnd ?: now
                Pair(s, e)
            }
        }

        fun getEffectiveOrderTime(order: com.example.data.model.Order): Long = order.createdAt

        val filteredLocal = localOrders.filter {
            val effTime = getEffectiveOrderTime(it.order)
            effTime in startMillis..endMillis && it.order.status != "CANCELLED"
        }

        class ItemAccumulator(
            var menuItemId: Long? = null,
            val itemName: String,
            var category: String = "عام",
            var unit: String = "طبق",
            var quantity: Double = 0.0,
            var revenue: Double = 0.0,
            val orderIds: MutableSet<String> = mutableSetOf()
        )

        class TimeBucketAccumulator(
            val label: String,
            val timestamp: Long,
            var revenue: Double = 0.0,
            var ordersCount: Int = 0,
            var quantity: Double = 0.0
        )

        val statsMap = mutableMapOf<String, ItemAccumulator>()
        val timeBucketsMap = mutableMapOf<String, TimeBucketAccumulator>()
        var totalRevenue = 0.0
        var totalQuantity = 0.0
        val distinctOrderIds = mutableSetOf<String>()
        val catRevenueMap = mutableMapOf<String, Double>()
        val catQuantityMap = mutableMapOf<String, Double>()

        val isSingleDay = period == com.example.data.model.SalesPeriodType.TODAY || period == com.example.data.model.SalesPeriodType.YESTERDAY || (endMillis - startMillis <= 86400000L)
        val hourFormat = SimpleDateFormat("HH:00", Locale.US)
        val dayFormat = SimpleDateFormat("E d/M", Locale("ar"))
        val monthFormat = SimpleDateFormat("MM/yyyy", Locale.US)

        for (o in filteredLocal) {
            distinctOrderIds.add("local_${o.order.id}")
            totalRevenue += o.order.totalAmount

            val effTime = getEffectiveOrderTime(o.order)
            val bCal = Calendar.getInstance().apply { timeInMillis = effTime }
            val bKey = if (isSingleDay) {
                hourFormat.format(bCal.time)
            } else if (period == com.example.data.model.SalesPeriodType.ALL_TIME) {
                monthFormat.format(bCal.time)
            } else {
                dayFormat.format(bCal.time)
            }
            val bucket = timeBucketsMap.getOrPut(bKey) {
                TimeBucketAccumulator(label = bKey, timestamp = effTime)
            }
            bucket.revenue += o.order.totalAmount
            bucket.ordersCount += 1

            for (itm in o.items) {
                val normName = itm.itemName.trim()
                if (normName.isBlank()) continue
                val stat = statsMap.getOrPut(normName) {
                    ItemAccumulator(
                        menuItemId = itm.menuItemId,
                        itemName = normName,
                        category = itm.category.ifBlank { "عام" },
                        unit = itm.unit.ifBlank { "طبق" }
                    )
                }
                stat.quantity += itm.quantity
                stat.revenue += itm.totalPrice
                stat.orderIds.add("local_${o.order.id}")
                totalQuantity += itm.quantity
                bucket.quantity += itm.quantity

                val cat = itm.category.ifBlank { "عام" }
                catRevenueMap[cat] = (catRevenueMap[cat] ?: 0.0) + itm.totalPrice
                catQuantityMap[cat] = (catQuantityMap[cat] ?: 0.0) + itm.quantity
            }
        }

        val topSelling = statsMap.values
            .sortedByDescending { it.revenue }
            .map {
                TodayItemSalesStat(
                    menuItemId = it.menuItemId,
                    itemName = it.itemName,
                    category = it.category,
                    unit = it.unit,
                    totalQuantitySold = it.quantity,
                    totalRevenue = it.revenue,
                    ordersCount = it.orderIds.size,
                    averageUnitPrice = if (it.quantity > 0.0) it.revenue / it.quantity else 0.0,
                    percentageOfTotalQuantity = if (totalQuantity > 0.0) (it.quantity / totalQuantity) * 100.0 else 0.0,
                    percentageOfTotalRevenue = if (totalRevenue > 0.0) (it.revenue / totalRevenue) * 100.0 else 0.0
                )
            }

        val timeDistribution = timeBucketsMap.values
            .sortedBy { it.timestamp }
            .map {
                SalesBucketStat(
                    label = it.label,
                    timestamp = it.timestamp,
                    revenue = it.revenue,
                    ordersCount = it.ordersCount,
                    quantity = it.quantity
                )
            }

        val avgOrderVal = if (distinctOrderIds.isNotEmpty()) totalRevenue / distinctOrderIds.size else 0.0

        return TodaySalesSummary(
            startOfDayMillis = startMillis,
            endOfDayMillis = endMillis,
            periodType = period.name,
            periodTitle = period.title,
            totalSalesRevenue = totalRevenue,
            totalQuantitySold = totalQuantity,
            totalOrdersCount = distinctOrderIds.size,
            averageOrderValue = avgOrderVal,
            topSellingItems = topSelling,
            categoryBreakdown = catRevenueMap,
            categoryQuantityBreakdown = catQuantityMap,
            timeDistribution = timeDistribution,
            isRealTime = true,
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    fun calculateTodaySalesSummary(
        localOrders: List<OrderWithItems>
    ): TodaySalesSummary {
        return calculateSalesSummaryForPeriod(
            localOrders = localOrders,
            period = selectedSalesPeriod.value,
            customStart = customSalesStartDate.value,
            customEnd = customSalesEndDate.value
        )
    }

    fun generateTodaySalesShareText(): String {
        val summary = todaySalesSummary.value
        val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())
        val sb = StringBuilder()
        sb.appendLine("📊 *تقرير ومخطط مبيعات ${summary.periodTitle} - صَنارة وشبكة*")
        sb.appendLine("📅 *تاريخ التقرير:* $dateStr")
        sb.appendLine("💰 *إجمالي المبيعات:* ${String.format(Locale.US, "%.1f", summary.totalSalesRevenue)} ج.م")
        sb.appendLine("📦 *إجمالي كميات الأصناف المباعة:* ${String.format(Locale.US, "%.2f", summary.totalQuantitySold)}")
        sb.appendLine("🧾 *إجمالي الأوردرات:* ${summary.totalOrdersCount} أوردر")
        sb.appendLine("💳 *متوسط قيمة الأوردر:* ${String.format(Locale.US, "%.1f", summary.averageOrderValue)} ج.م")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🐟 *تفاصيل الأصناف المباعة (ترتيب الأكثر طلباً):*")
        if (summary.topSellingItems.isEmpty()) {
            sb.appendLine("لا توجد مبيعات مسجلة في هذه الفترة المختارة.")
        } else {
            summary.topSellingItems.forEachIndexed { idx, item ->
                val medal = when (idx) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> "#${idx + 1}"
                }
                sb.appendLine("$medal *${item.itemName}* (${item.category})")
                sb.appendLine("   • الكمية المباعة: ${String.format(Locale.US, "%.2f", item.totalQuantitySold)} ${item.unit}")
                sb.appendLine("   • الإيراد: ${String.format(Locale.US, "%.1f", item.totalRevenue)} ج.م (في ${item.ordersCount} أوردر)")
                sb.appendLine("   • النسبة من مبيعات الفترة: ${String.format(Locale.US, "%.1f", item.percentageOfTotalQuantity)}%")
            }
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("☁️ مربوط ومحدث لجميع الأجهزة")
        return sb.toString()
    }

    // Clean Start Data
    fun clearAllDummyData() {
        viewModelScope.launch {
            repository.clearAllDummyData()
            userMessage.value = "تم تصفير جميع البيانات السابقة بنجاح. التطبيق الآن نظيف وجاهز للبيانات الحقيقية!"
        }
    }

    // General Accounts & Suppliers Management
    fun saveAccount(account: GeneralAccount) {
        viewModelScope.launch {
            repository.saveAccount(account)
            userMessage.value = "تم حفظ بيانات الحساب بنجاح"
        }
    }

    fun archiveAccount(id: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveAccount(id, archived)
            userMessage.value = if (archived) "تم أرشفة الحساب بنجاح" else "تم إلغاء أرشفة الحساب بنجاح"
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteAccount(id)
            userMessage.value = "تم حذف الحساب نهائياً بنجاح"
        }
    }

    fun recordAccountTransaction(
        accountId: Long,
        type: String,
        amount: Double,
        description: String,
        invoiceNumber: String = "",
        date: Long = System.currentTimeMillis(),
        isPaidFromDrawer: Boolean = (type == "PAYMENT" || type == "RECEIPT")
    ) {
        viewModelScope.launch {
            val tx = AccountTransaction(
                accountId = accountId,
                type = type,
                amount = amount,
                description = description,
                invoiceNumber = invoiceNumber,
                date = date,
                isPaidFromDrawer = isPaidFromDrawer
            )
            repository.recordAccountTransaction(tx)
            userMessage.value = "تم تسجيل الحركة المالية وتحديث الرصيد"
        }
    }

    fun deleteAccountTransaction(txId: Long, accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccountTransaction(txId, accountId)
            userMessage.value = "تم حذف الحركة وإعادة حساب الرصيد"
        }
    }

    suspend fun getTransactionsForAccount(accountId: Long): List<AccountTransaction> {
        return repository.getTransactionsListForAccount(accountId)
    }

    suspend fun getShiftOrders(shiftId: Long): List<OrderWithItems> {
        return repository.getOrdersForShift(shiftId)
    }

    suspend fun getShiftDrawerStatement(shiftId: Long): com.example.data.repository.ShiftDrawerStatement {
        return repository.getShiftDrawerStatement(shiftId)
    }

    suspend fun getActiveShiftLiveStatement(): com.example.data.repository.ShiftDrawerStatement? {
        val active = activeShift.value ?: return null
        return repository.getShiftDrawerStatement(active.id)
    }

    fun printShiftStatement(shiftId: Long) {
        viewModelScope.launch {
            try {
                val statement = repository.getShiftDrawerStatement(shiftId)
                val template = repository.getInvoiceTemplateConfig()
                val bitmap = com.example.printer.ReceiptBitmapRenderer.renderShiftReportBitmap(statement, template, 80)

                // 1. Try printing directly to configured customer invoice / cashier thermal printers
                val allConfigs = repository.getAllPrinterConfigs().filter { it.isEnabled }
                val invoicePrinters = allConfigs.filter { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" || it.isCashierDefault }

                var printedNetworkOrBt = false
                if (invoicePrinters.isNotEmpty()) {
                    val rasterBytes = com.example.printer.ReceiptBitmapRenderer.bitmapToEscPosRasterBytes(bitmap)
                    for (p in invoicePrinters) {
                        val res = repository.printerManager.sendBytesToPrinterDirect(p, rasterBytes)
                        if (res is com.example.printer.PrinterResult.Success) {
                            printedNetworkOrBt = true
                        }
                    }
                }

                if (printedNetworkOrBt) {
                    userMessage.value = "تمت طباعة تقرير تقفيل الوردية #${statement.shift.shiftNumber} على طابعة الكاشير بنجاح ✓"
                } else {
                    // 2. Fallback to Android System Spooler
                    val ok = com.example.printer.ReceiptPrintDocumentAdapter.printBitmapViaSystemSpooler(
                        getApplication(),
                        bitmap,
                        "تقرير تقفيل وردية #${statement.shift.shiftNumber}"
                    )
                    if (ok) {
                        userMessage.value = "تم فتح نافذة طباعة تقرير الوردية بنظام أندرويد ✓"
                    } else {
                        userMessage.value = "تم تجهيز تقرير الوردية #${statement.shift.shiftNumber} للطباعة"
                    }
                }
            } catch (e: Exception) {
                userMessage.value = "خطأ في طباعة تقرير الوردية: ${e.localizedMessage}"
            }
        }
    }

    fun reprintShiftReport(shift: Shift) {
        printShiftStatement(shift.id)
    }

    fun printDeliveryBatch(
        ordersToPrint: List<OrderWithItems>,
        targetPrinter: PrinterConfig? = null
    ) {
        if (ordersToPrint.isEmpty()) {
            userMessage.value = "يرجى تحديد أوردر واحد على الأقل للطباعة"
            return
        }

        viewModelScope.launch {
            try {
                val allConfigs = repository.getAllPrinterConfigs().filter { it.isEnabled }

                // Determine target printer: explicit printer or registered cashier printer(s)
                val defaultCashier = allConfigs.firstOrNull { it.isCashierDefault }
                    ?: allConfigs.firstOrNull { it.role == "CUSTOMER_INVOICE" || it.role == "BOTH" }
                    ?: allConfigs.firstOrNull()

                val effectivePrinter = targetPrinter ?: defaultCashier

                if (effectivePrinter == null) {
                    // Fallback to Android System Spooler
                    setPrintDiagnostic("لا توجد طابعة متعرفة، جارٍ التحويل لنظام أندرويد...", autoClearSeconds = 3)
                    val template = repository.getInvoiceTemplateConfig()
                    val fallbackBitmap = com.example.printer.ReceiptBitmapRenderer.renderDeliveryBatchTicket(ordersToPrint, template, 80)
                    com.example.printer.ReceiptPrintDocumentAdapter.printBitmapViaSystemSpooler(
                        getApplication(),
                        fallbackBitmap,
                        "كشف تسليم دليفري (${ordersToPrint.size} أوردر)"
                    )
                    userMessage.value = "تم فتح نافذة الطباعة بنظام أندرويد ✓"
                    return@launch
                }

                setPrintDiagnostic("جارٍ إرسال كشف تسليم الدليفري (${ordersToPrint.size} أوردر) لطابعة ${effectivePrinter.displayName}...", autoClearSeconds = 3)

                val result = repository.dispatchDeliveryManifest(ordersToPrint, effectivePrinter.id)

                when (result) {
                    is com.example.printer.PrinterResult.Success -> {
                        val msg = "تمت طباعة كشف تسليم الدليفري (${ordersToPrint.size} أوردر) على طابعة ${effectivePrinter.displayName} بنجاح ✓"
                        setPrintDiagnostic(msg)
                        userMessage.value = msg
                    }
                    is com.example.printer.PrinterResult.Error -> {
                        val errMsg = "فشل الإرسال لطابعة ${effectivePrinter.displayName}: ${result.errorMessage}"
                        setPrintDiagnostic("خطأ طابعة: ${effectivePrinter.displayName}")
                        userMessage.value = errMsg
                    }
                    is com.example.printer.PrinterResult.PermissionRequired -> {
                        setPrintDiagnostic("مطلوب إذن: ${result.permissionName}")
                        userMessage.value = result.message
                    }
                }
            } catch (e: Exception) {
                setPrintDiagnostic("تعذر طباعة كشف تسليم الدليفري")
                userMessage.value = "خطأ في طباعة كشف الدليفري: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    // Quick Cash Drawer Transaction (In/Deposit or Out/Expense directly from drawer)
    fun recordQuickDrawerTransaction(
        type: String, // "IN" or "OUT"
        amount: Double,
        reason: String,
        accountId: Long? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val active = activeShift.value
            if (active == null) {
                userMessage.value = "تنبيه: لا توجد وردية مفتوحة حالياً لتسجيل حركة الدرج!"
                return@launch
            }
            if (amount <= 0.0) {
                userMessage.value = "تنبيه: يرجى إدخال مبلغ صحيح أكبر من الصفر"
                return@launch
            }
            try {
                repository.recordQuickDrawerTransaction(type, amount, reason, accountId)
                val typeArabic = if (type == "IN") "إيداع / وارد للدرج" else "صرف / خارج من الدرج"
                val prefix = if (type == "IN") "+" else "-"
                userMessage.value = "تم تسجيل $typeArabic ($prefix${String.format(Locale.US, "%.1f", amount)} ج.م) للسبب: $reason بنجاح ✓"
                onSuccess?.invoke()
            } catch (e: Exception) {
                userMessage.value = "تعذر تسجيل حركة الدرج: ${e.localizedMessage}"
            }
        }
    }

    fun recordDirectDrawerExpense(amount: Double, reason: String) {
        recordQuickDrawerTransaction("OUT", amount, reason)
    }

    fun recordDirectDrawerDeposit(amount: Double, reason: String) {
        recordQuickDrawerTransaction("IN", amount, reason)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            com.example.util.BackupManager.setAutoBackupEnabled(getApplication(), enabled)
            isAutoBackupEnabled.value = enabled
            userMessage.value = if (enabled) "تم تفعيل النسخ الاحتياطي التلقائي اليومي ✓" else "تم إيقاف النسخ الاحتياطي التلقائي"
        }
    }

    fun triggerFullBackup(onSuccess: (java.io.File, String) -> Unit) {
        viewModelScope.launch {
            isBackingUp.value = true
            try {
                val stats = com.example.util.BackupManager.performFullBackup(getApplication())
                lastBackupInfo.value = com.example.util.BackupManager.getLastBackupInfo(getApplication())
                userMessage.value = "تم إتمام النسخ الاحتياطي الكامل بنجاح (${stats.fileSizeFormatted})"
                onSuccess(java.io.File(stats.dbFilePath), stats.jsonFilePath)
            } catch (e: Exception) {
                userMessage.value = "تعذر إتمام النسخ الاحتياطي: ${e.message}"
            } finally {
                isBackingUp.value = false
            }
        }
    }

    fun unarchiveAllOrders(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val archivedCloudIds = repository.getAllOrdersDirect()
                    .filter { it.order.isArchived }
                    .mapNotNull { it.order.cloudOrderId?.takeIf { cid -> cid.isNotBlank() } }
                    .distinct()
                if (archivedCloudIds.isNotEmpty()) {
                    enqueuePendingCloudArchive(archivedCloudIds.associateWith { false })
                }
                val count = repository.unarchiveAllOrders()
                val cUser = cloudUser.value
                if (cUser != null && archivedCloudIds.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val failed = withTimeoutOrNull(15_000L) {
                            cloudRepository.batchArchiveCloudOrdersDetailed(cUser.uid, archivedCloudIds, false)
                        } ?: archivedCloudIds.toSet()
                        val succeeded = archivedCloudIds.filterNot { it in failed }
                        if (succeeded.isNotEmpty()) {
                            clearPendingCloudArchive(succeeded)
                        }
                    }
                }
                userMessage.value = "تم إظهار واستعادة كافة الأوردرات السابقة في القائمة ومزامنتها ($count أوردر) ✓"
                onComplete?.invoke(count)
            } catch (e: Exception) {
                userMessage.value = "حدث خطأ أثناء فك الأرشفة: ${e.message}"
            }
        }
    }

    val isRestoringBackup = MutableStateFlow(false)

    private suspend fun pushRestoredDatabaseToCloud(userId: String): Result<Unit> =
        coordinator.pushRestoredDatabaseToCloud(userId)

    fun restoreFromJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        if (isRestoringBackup.value) {
            onComplete(false, "عملية استعادة أخرى قيد التنفيذ بالفعل")
            return
        }
        viewModelScope.launch {
            isRestoringBackup.value = true
            isCloudSyncPausedForRestore = true
            stopCloudSyncObservers()
            val roleBeforeRestore = deviceRole.value
            try {
                val result = com.example.util.BackupManager.restoreDatabaseFromJson(
                    context = getApplication(),
                    jsonString = jsonString,
                    preserveNewerRecords = false
                )
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    // Preserve terminal role and purge any stale sync queues imported from old app_settings
                    if (roleBeforeRestore == "CASHIER" || roleBeforeRestore == "SELLER") {
                        repository.saveSetting("device_role", roleBeforeRestore)
                        coordinator.deviceRole.value = roleBeforeRestore
                    }
                    clearAllSyncQueuesForSupremeRestore()
                    repository.deduplicateOrders()
                    repository.syncCustomerStats()
                    nextOrderNumber.value = repository.getNextOrderNumber()
                    _allPrinters.value = repository.getAllPrinterConfigs()

                    var cloudSyncSuffix = ""
                    val cUser = cloudUser.value
                    if (roleBeforeRestore != "SELLER") {
                        repository.saveSetting("pending_cloud_supreme_restore", "1")
                        if (cUser != null) {
                            coordinator.isRestoringBackupSyncingCloud.value = true
                            val pushRes = withTimeoutOrNull(120_000L) {
                                pushRestoredDatabaseToCloud(cUser.uid)
                            } ?: Result.failure(Exception("انتهت مهلة الرفع للسحابة (120 ثانية)"))
                            coordinator.isRestoringBackupSyncingCloud.value = false
                            cloudSyncSuffix = if (pushRes.isSuccess) {
                                " وتم فرضها كمرجع وحيد على السحابة المشتركة والأجهزة المتصلة ☁️✓"
                            } else {
                                val errDetail = pushRes.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"
                                " (تمت الاستعادة محلياً ولكن تعذر الرفع للسحابة: $errDetail)"
                            }
                        } else {
                            cloudSyncSuffix = " (سيتم فرضها كمرجع أعلى على السحابة تلقائياً عند الربط)"
                        }
                    }

                    val successMsg = "تمت استعادة $count سجل بنجاح$cloudSyncSuffix"
                    userMessage.value = successMsg
                    onComplete(true, successMsg)
                } else {
                    val err = result.exceptionOrNull()
                    userMessage.value = "فشلت عملية الاستعادة: ${err?.message}"
                    onComplete(false, err?.message ?: "خطأ غير معروف")
                }
            } finally {
                coordinator.isRestoringBackupSyncingCloud.value = false
                isCloudSyncPausedForRestore = false
                isRestoringBackup.value = false
                val activeUser = cloudUser.value ?: Firebase.auth.currentUser
                val hasPendingSupreme = repository.getSetting("pending_cloud_supreme_restore") == "1"
                if (activeUser != null && (!hasPendingSupreme || roleBeforeRestore == "SELLER")) {
                    startCloudSyncObservers(activeUser.uid, forceRestart = true)
                }
            }
        }
    }

    fun restoreFromUri(uri: android.net.Uri, onComplete: (Boolean, String) -> Unit) {
        if (isRestoringBackup.value) {
            onComplete(false, "عملية استعادة أخرى قيد التنفيذ بالفعل")
            return
        }
        viewModelScope.launch {
            isRestoringBackup.value = true
            isCloudSyncPausedForRestore = true
            stopCloudSyncObservers()
            val roleBeforeRestore = deviceRole.value
            try {
                val result = com.example.util.BackupManager.restoreDatabaseFromUri(
                    context = getApplication(),
                    uri = uri,
                    preserveNewerRecords = false
                )
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    // Preserve terminal role and purge any stale sync queues imported from old app_settings
                    if (roleBeforeRestore == "CASHIER" || roleBeforeRestore == "SELLER") {
                        repository.saveSetting("device_role", roleBeforeRestore)
                        coordinator.deviceRole.value = roleBeforeRestore
                    }
                    clearAllSyncQueuesForSupremeRestore()
                    repository.deduplicateOrders()
                    repository.syncCustomerStats()
                    nextOrderNumber.value = repository.getNextOrderNumber()
                    _allPrinters.value = repository.getAllPrinterConfigs()

                    var cloudSyncSuffix = ""
                    val cUser = cloudUser.value
                    if (roleBeforeRestore != "SELLER") {
                        repository.saveSetting("pending_cloud_supreme_restore", "1")
                        if (cUser != null) {
                            coordinator.isRestoringBackupSyncingCloud.value = true
                            val pushRes = withTimeoutOrNull(120_000L) {
                                pushRestoredDatabaseToCloud(cUser.uid)
                            } ?: Result.failure(Exception("انتهت مهلة الرفع للسحابة (120 ثانية)"))
                            coordinator.isRestoringBackupSyncingCloud.value = false
                            cloudSyncSuffix = if (pushRes.isSuccess) {
                                " وتم فرضها كمرجع وحيد على السحابة المشتركة والأجهزة المتصلة ☁️✓"
                            } else {
                                val errDetail = pushRes.exceptionOrNull()?.localizedMessage ?: "خطأ غير معروف"
                                " (تمت الاستعادة محلياً ولكن تعذر الرفع للسحابة: $errDetail)"
                            }
                        } else {
                            cloudSyncSuffix = " (سيتم فرضها كمرجع أعلى على السحابة تلقائياً عند الربط)"
                        }
                    }

                    val successMsg = "تمت استعادة $count سجل بنجاح وحفظ نقطة رجوع كاملة$cloudSyncSuffix"
                    userMessage.value = successMsg
                    onComplete(true, successMsg)
                } else {
                    val err = result.exceptionOrNull()
                    userMessage.value = "فشلت عملية الاستعادة: ${err?.message}"
                    onComplete(false, err?.message ?: "خطأ غير معروف")
                }
            } catch (e: Exception) {
                userMessage.value = "خطأ أثناء معالجة الملف: ${e.message}"
                onComplete(false, e.message ?: "خطأ غير معروف")
            } finally {
                coordinator.isRestoringBackupSyncingCloud.value = false
                isCloudSyncPausedForRestore = false
                isRestoringBackup.value = false
                val activeUser = cloudUser.value ?: Firebase.auth.currentUser
                val hasPendingSupreme = repository.getSetting("pending_cloud_supreme_restore") == "1"
                if (activeUser != null && (!hasPendingSupreme || roleBeforeRestore == "SELLER")) {
                    startCloudSyncObservers(activeUser.uid, forceRestart = true)
                }
            }
        }
    }

    fun clearAllDummyData(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllDummyData()
                userMessage.value = "تم حذف وتصفير جميع البيانات الوهمية والتجريبية بنجاح بنسبة 100% ✓"
                onComplete(true, "تم حذف الأصناف والاوردرات وكباتن الدليفري الوهمية بنجاح")
            } catch (e: Exception) {
                userMessage.value = "تعذر حذف البيانات الوهمية: ${e.message}"
                onComplete(false, e.message ?: "خطأ غير معروف")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

