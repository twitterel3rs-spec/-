package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CloudBackupDoc
import com.example.data.model.CloudCategory
import com.example.data.model.CloudMenuItem
import com.example.data.model.CloudOrderChange
import com.example.data.model.CloudOrderDoc
import com.example.data.model.CloudShiftDoc
import com.example.data.model.TerminalDoc
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(userId: String): String {
        val currentUid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
        if (currentUid != userId) {
            throw SecurityException("User mismatch: authenticated as $currentUid but accessed $userId")
        }
        return currentUid
    }

    private fun DocumentSnapshot.toCloudMenuItem(): CloudMenuItem {
        val archivedFlag = getBoolean("isArchived") ?: getBoolean("archived") ?: false
        val activeFlag = getBoolean("isActive") ?: getBoolean("active") ?: true
        val plateEnabled = getBoolean("isPlateWeightEnabled") ?: getBoolean("plateWeightEnabled") ?: false
        return CloudMenuItem(
            id = id,
            name = getString("name") ?: "",
            category = getString("category") ?: "",
            price = getDouble("price") ?: 0.0,
            costPrice = getDouble("costPrice") ?: 0.0,
            stockQuantity = getDouble("stockQuantity") ?: 0.0,
            minStockAlert = getDouble("minStockAlert") ?: 3.0,
            unit = getString("unit") ?: "وجبة",
            targetPrinter = getString("targetPrinter") ?: "MAIN",
            kitchenName = getString("kitchenName") ?: "",
            supportsPreparation = getBoolean("supportsPreparation") ?: false,
            isArchived = archivedFlag,
            isActive = activeFlag,
            displayOrder = getLong("displayOrder")?.toInt() ?: 0,
            isPlateWeightEnabled = plateEnabled,
            plateWeightKg = getDouble("plateWeightKg") ?: 0.5,
            pricePerKg = getDouble("pricePerKg") ?: 0.0,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toCloudCategory(): CloudCategory {
        val archivedFlag = getBoolean("isArchived") ?: getBoolean("archived") ?: false
        return CloudCategory(
            id = id,
            name = getString("name") ?: "",
            color = getString("color") ?: "#0288D1",
            sortOrder = getLong("sortOrder")?.toInt() ?: 0,
            isArchived = archivedFlag,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toCloudOrderDoc(): CloudOrderDoc {
        val archivedFlag = getBoolean("isArchived") ?: getBoolean("archived") ?: false
        return CloudOrderDoc(
            id = id,
            orderNumber = getLong("orderNumber")?.toInt() ?: 0,
            customerName = getString("customerName") ?: "",
            customerPhone = getString("customerPhone") ?: "",
            customerSecondaryPhone = getString("customerSecondaryPhone") ?: "",
            customerArea = getString("customerArea") ?: "",
            deliveryAddressTitle = getString("deliveryAddressTitle") ?: "",
            deliveryAddressFull = getString("deliveryAddressFull") ?: "",
            orderType = getString("orderType") ?: "TAKEAWAY",
            status = getString("status") ?: "PENDING",
            printStatus = getString("printStatus") ?: "PENDING_PRINT",
            requestedPrinterTarget = getString("requestedPrinterTarget") ?: "ALL",
            reprintReason = getString("reprintReason") ?: "",
            isArchived = archivedFlag,
            unarchiveTimestamp = getLong("unarchiveTimestamp") ?: 0L,
            shiftNumber = getLong("shiftNumber")?.toInt() ?: 0,
            itemsJson = getString("itemsJson") ?: "[]",
            subtotal = getDouble("subtotal") ?: 0.0,
            deliveryFee = getDouble("deliveryFee") ?: 0.0,
            discount = getDouble("discount") ?: 0.0,
            total = getDouble("total") ?: 0.0,
            paymentMethod = getString("paymentMethod") ?: "CASH",
            notes = getString("notes") ?: "",
            driverName = getString("driverName") ?: "",
            createdByTerminal = getString("createdByTerminal") ?: "",
            printedByTerminal = getString("printedByTerminal") ?: "",
            createdAtMs = getLong("createdAtMs") ?: 0L,
            hasFullDetails = getBoolean("hasFullDetails") ?: false,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toCloudShiftDoc(): CloudShiftDoc {
        val closedFlag = getBoolean("isClosed") ?: getBoolean("closed") ?: false
        return CloudShiftDoc(
            id = id,
            shiftNumber = getLong("shiftNumber")?.toInt() ?: 1,
            cashierName = getString("cashierName") ?: "",
            openedAtMs = getLong("openedAtMs") ?: 0L,
            closedAtMs = getLong("closedAtMs") ?: 0L,
            openingBalance = getDouble("openingBalance") ?: 0.0,
            totalCashSales = getDouble("totalCashSales") ?: 0.0,
            totalWalletSales = getDouble("totalWalletSales") ?: 0.0,
            totalExpenses = getDouble("totalExpenses") ?: 0.0,
            actualDrawerCash = getDouble("actualDrawerCash") ?: 0.0,
            expectedDrawerCash = getDouble("expectedDrawerCash") ?: 0.0,
            difference = getDouble("difference") ?: 0.0,
            ordersCount = getLong("ordersCount")?.toInt() ?: 0,
            isClosed = closedFlag,
            notes = getString("notes") ?: "",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toTerminalDoc(): TerminalDoc {
        val masterFlag = getBoolean("isMaster") ?: getBoolean("master") ?: false
        return TerminalDoc(
            id = id,
            name = getString("name") ?: "",
            isMaster = masterFlag,
            canPrint = getBoolean("canPrint") ?: false,
            printerListJson = getString("printerListJson") ?: "",
            lastActive = getTimestamp("lastActive")
        )
    }

    // --- 1. Menu Items (No duplicate items, no unintended merging) ---

    fun observeMenuItems(userId: String): Flow<List<CloudMenuItem>> {
        val path = "users/$userId/menu_items"
        return db.collection("users").document(userId).collection("menu_items")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toCloudMenuItem() } }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun upsertMenuItem(userId: String, item: CloudMenuItem): Result<Unit> {
        val uid = requireUserId(userId)
        val itemId = if (item.id.isNotBlank()) item.id else "item_${UUID.randomUUID()}"
        val docRef = db.collection("users").document(uid).collection("menu_items").document(itemId)

        return try {
            val cached = try { docRef.get(Source.CACHE).await() } catch (_: Exception) { null }
            val existing = if (cached != null && cached.exists()) cached else docRef.get().await()
            val existingCreatedAt = if (existing.exists()) existing.getTimestamp("createdAt") else null
            val createdAtVal = existingCreatedAt ?: FieldValue.serverTimestamp()

            val payload = mutableMapOf<String, Any>(
                "name" to item.name,
                "category" to item.category,
                "price" to item.price,
                "costPrice" to item.costPrice,
                "stockQuantity" to item.stockQuantity,
                "minStockAlert" to item.minStockAlert,
                "unit" to item.unit,
                "targetPrinter" to item.targetPrinter,
                "kitchenName" to item.kitchenName,
                "supportsPreparation" to item.supportsPreparation,
                "isArchived" to item.isArchived,
                "isActive" to item.isActive,
                "displayOrder" to item.displayOrder,
                "isPlateWeightEnabled" to item.isPlateWeightEnabled,
                "plateWeightKg" to item.plateWeightKg,
                "pricePerKg" to item.pricePerKg,
                "createdAt" to createdAtVal,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun extractTombstoneMap(raw: Any?): MutableMap<String, Long> {
        val result = mutableMapOf<String, Long>()
        val map = raw as? Map<*, *> ?: return result
        for ((k, v) in map) {
            val key = k as? String ?: continue
            val ts = (v as? Number)?.toLong() ?: continue
            if (key.isNotBlank() && ts > 0L) {
                result[key] = ts
            }
        }
        return result
    }

    fun observeDeletedMenuItemIds(userId: String): Flow<Map<String, Long>> {
        val docRef = db.collection("users").document(userId).collection("counters").document("deleted_menu_items")
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) emptyMap()
                else extractTombstoneMap(snapshot.get("deletedMap"))
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun getDeletedMenuItemIdsOnce(userId: String): Map<String, Long> {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("counters").document("deleted_menu_items")
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) emptyMap() else extractTombstoneMap(snap.get("deletedMap"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun clearAllMenuItemTombstones(userId: String): Result<Unit> {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("counters").document("deleted_menu_items")
        return try {
            docRef.set(
                mapOf(
                    "deletedMap" to emptyMap<String, Long>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Deletes menu item documents from Firestore and records tombstones in
     * `users/{uid}/counters/deleted_menu_items` so all connected devices (and offline devices
     * reconnecting later) permanently delete those items and never resurrect them.
     *
     * @return Set of item cloudIds that failed due to transient errors and must be retried.
     */
    suspend fun deleteMenuItemsWithTombstone(userId: String, itemCloudIds: List<String>): Set<String> {
        val cleanIds = itemCloudIds.filter { it.isNotBlank() && it != "item_unknown" }.distinct()
        if (cleanIds.isEmpty()) return emptySet()
        val uid = requireUserId(userId)
        val menuCol = db.collection("users").document(uid).collection("menu_items")
        val tombstoneRef = db.collection("users").document(uid).collection("counters").document("deleted_menu_items")
        val transientFailed = mutableSetOf<String>()

        // 1. Delete item docs from menu_items
        for (chunk in cleanIds.chunked(400)) {
            try {
                val batch = db.batch()
                for (cid in chunk) {
                    batch.delete(menuCol.document(cid))
                }
                batch.commit().await()
            } catch (batchErr: Exception) {
                if (batchErr is CancellationException) throw batchErr
                for (cid in chunk) {
                    try {
                        menuCol.document(cid).delete().await()
                    } catch (docErr: Exception) {
                        if (docErr is CancellationException) throw docErr
                        if (!isPermanentOrderDocUpdateError(docErr)) {
                            transientFailed.add(cid)
                            handleFirestoreError(docErr, OperationType.DELETE, "${menuCol.path}/$cid")
                        }
                    }
                }
            }
        }

        // 2. Record tombstones in counters/deleted_menu_items (bounded to 30 days / 500 entries)
        try {
            val nowMs = System.currentTimeMillis()
            val maxAgeMs = 30L * 24L * 60L * 60L * 1000L
            db.runTransaction { tx ->
                val snap = tx.get(tombstoneRef)
                val merged = if (snap.exists()) extractTombstoneMap(snap.get("deletedMap")) else mutableMapOf()
                for (cid in cleanIds) {
                    merged[cid] = nowMs
                }
                val bounded = merged.entries
                    .filter { (nowMs - it.value) <= maxAgeMs }
                    .sortedByDescending { it.value }
                    .take(500)
                    .associate { it.key to it.value }
                tx.set(
                    tombstoneRef,
                    mapOf(
                        "deletedMap" to bounded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, tombstoneRef.path)
            transientFailed.addAll(cleanIds)
        }

        return transientFailed
    }

    suspend fun deleteMenuItem(userId: String, itemId: String): Result<Unit> {
        val failed = deleteMenuItemsWithTombstone(userId, listOf(itemId))
        return if (failed.isEmpty()) Result.success(Unit)
        else Result.failure(Exception("تعذر مزامنة حذف الصنف مؤقتاً"))
    }

    // --- 2. Categories ---

    fun observeCategories(userId: String): Flow<List<CloudCategory>> {
        val path = "users/$userId/categories"
        return db.collection("users").document(userId).collection("categories")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toCloudCategory() } }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun upsertCategory(userId: String, category: CloudCategory): Result<Unit> {
        val uid = requireUserId(userId)
        val categoryId = if (category.id.isNotBlank()) category.id else "cat_${UUID.randomUUID()}"
        val docRef = db.collection("users").document(uid).collection("categories").document(categoryId)

        return try {
            val cached = try { docRef.get(Source.CACHE).await() } catch (_: Exception) { null }
            val existing = if (cached != null && cached.exists()) cached else docRef.get().await()
            val createdAtVal = if (existing.exists()) (existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()) else FieldValue.serverTimestamp()
            val payload = mapOf(
                "name" to category.name,
                "color" to category.color,
                "sortOrder" to category.sortOrder,
                "isArchived" to category.isArchived,
                "createdAt" to createdAtVal,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 3. Cloud Orders (Seller Terminals -> Master Printer Queue) ---

    fun observeCloudOrders(userId: String): Flow<List<CloudOrderDoc>> {
        val path = "users/$userId/cloud_orders"
        return db.collection("users").document(userId).collection("cloud_orders")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toCloudOrderDoc() } }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    /**
     * Emits ONLY incremental document changes (ADDED / MODIFIED / REMOVED) so large collections
     * (800+ orders) never re-parse and stall the Cashier emulator when 1 new order arrives.
     */
    fun observeCloudOrderChanges(userId: String): Flow<List<CloudOrderChange>> {
        val path = "users/$userId/cloud_orders"
        return db.collection("users").document(userId).collection("cloud_orders")
            .snapshots()
            .map { snapshot ->
                snapshot.documentChanges.map { dc ->
                    val typeStr = when (dc.type) {
                        DocumentChange.Type.ADDED -> "ADDED"
                        DocumentChange.Type.MODIFIED -> "MODIFIED"
                        DocumentChange.Type.REMOVED -> "REMOVED"
                    }
                    CloudOrderChange(
                        order = dc.document.toCloudOrderDoc(),
                        changeType = typeStr
                    )
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createOrder(userId: String, order: CloudOrderDoc): Result<String> {
        val uid = requireUserId(userId)
        val orderId = if (order.id.isNotBlank()) order.id else "order_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("users").document(uid).collection("cloud_orders").document(orderId)

        return try {
            val payload = mapOf(
                "orderNumber" to order.orderNumber,
                "customerName" to order.customerName,
                "customerPhone" to order.customerPhone,
                "customerSecondaryPhone" to order.customerSecondaryPhone,
                "customerArea" to order.customerArea,
                "deliveryAddressTitle" to order.deliveryAddressTitle,
                "deliveryAddressFull" to order.deliveryAddressFull,
                "orderType" to order.orderType,
                "status" to order.status,
                "printStatus" to order.printStatus,
                "requestedPrinterTarget" to order.requestedPrinterTarget,
                "reprintReason" to order.reprintReason,
                "isArchived" to order.isArchived,
                "unarchiveTimestamp" to order.unarchiveTimestamp,
                "shiftNumber" to order.shiftNumber,
                "itemsJson" to order.itemsJson,
                "subtotal" to order.subtotal,
                "deliveryFee" to order.deliveryFee,
                "discount" to order.discount,
                "total" to order.total,
                "paymentMethod" to order.paymentMethod,
                "notes" to order.notes,
                "driverName" to order.driverName,
                "createdByTerminal" to order.createdByTerminal,
                "printedByTerminal" to order.printedByTerminal,
                "createdAtMs" to order.createdAtMs.coerceAtLeast(0L),
                "printRequestedAtMs" to order.printRequestedAtMs.coerceAtLeast(0L),
                "hasFullDetails" to order.hasFullDetails,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Atomically transitions a cloud order's printStatus from PENDING_PRINT or PRINT_REQUESTED
     * to PRINTING inside a Firestore transaction, preventing double-printing across reconnects or restarts.
     */
    suspend fun tryClaimOrderForPrinting(
        userId: String,
        orderId: String,
        expectedStatuses: Set<String> = setOf("PENDING_PRINT", "PRINT_REQUESTED")
    ): Boolean {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("cloud_orders").document(orderId)

        return try {
            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                if (!snap.exists()) return@runTransaction false
                val currentPrintStatus = snap.getString("printStatus") ?: ""
                if (currentPrintStatus !in expectedStatuses) {
                    return@runTransaction false
                }
                transaction.update(
                    docRef,
                    mapOf(
                        "printStatus" to "PRINTING",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                true
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("FirestoreCloudRepository", "tryClaimOrderForPrinting error for $orderId", e)
            false
        }
    }

    suspend fun requestRemotePrint(
        userId: String,
        orderId: String,
        requestedPrinterTarget: String,
        reprintReason: String = ""
    ): Result<Unit> {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("cloud_orders").document(orderId)

        return try {
            val nowReqMs = System.currentTimeMillis()
            docRef.update(
                mapOf(
                    "printStatus" to "PRINT_REQUESTED",
                    "requestedPrinterTarget" to requestedPrinterTarget,
                    "reprintReason" to reprintReason,
                    "printRequestedAtMs" to nowReqMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Pushes a complete edited order snapshot (items, customer, address, delivery fee, totals, driver,
     * notes, and optional remote print state) while preserving the existing document's `createdAt`
     * timestamp required by Firestore security rules. Falls back to `createOrder` if missing remotely.
     */
    suspend fun syncEditedOrderToCloud(userId: String, order: CloudOrderDoc): Result<Unit> {
        val uid = requireUserId(userId)
        val cleanId = order.id.trim()
        if (cleanId.isBlank()) {
            return createOrder(uid, order.copy(hasFullDetails = true)).map { Unit }
        }
        val docRef = db.collection("users").document(uid).collection("cloud_orders").document(cleanId)
        val updates = mutableMapOf<String, Any>(
            "orderNumber" to order.orderNumber,
            "customerName" to order.customerName,
            "customerPhone" to order.customerPhone,
            "customerSecondaryPhone" to order.customerSecondaryPhone,
            "customerArea" to order.customerArea,
            "deliveryAddressTitle" to order.deliveryAddressTitle,
            "deliveryAddressFull" to order.deliveryAddressFull,
            "orderType" to order.orderType,
            "status" to order.status,
            "printStatus" to order.printStatus,
            "requestedPrinterTarget" to order.requestedPrinterTarget,
            "reprintReason" to order.reprintReason,
            "isArchived" to order.isArchived,
            "itemsJson" to order.itemsJson,
            "subtotal" to order.subtotal.coerceAtLeast(0.0),
            "deliveryFee" to order.deliveryFee.coerceAtLeast(0.0),
            "discount" to order.discount.coerceAtLeast(0.0),
            "total" to order.total.coerceAtLeast(0.0),
            "paymentMethod" to order.paymentMethod,
            "notes" to order.notes,
            "driverName" to order.driverName,
            "hasFullDetails" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (order.createdAtMs > 0L) {
            updates["createdAtMs"] = order.createdAtMs
        }
        if (order.printRequestedAtMs > 0L) {
            updates["printRequestedAtMs"] = order.printRequestedAtMs
        }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val code = (e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code
            val isNotFound = code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND ||
                (e.message?.contains("NOT_FOUND", ignoreCase = true) == true)
            if (isNotFound) {
                createOrder(uid, order.copy(id = cleanId, hasFullDetails = true)).map { Unit }
            } else {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                Result.failure(e)
            }
        }
    }

    suspend fun markOrderPrinted(userId: String, orderId: String, masterTerminalName: String): Result<Unit> {
        return updateCloudOrderPrintState(userId, orderId, "PRINTED", masterTerminalName)
    }

    suspend fun updateCloudOrderPrintState(
        userId: String,
        orderId: String,
        printStatus: String,
        masterTerminalName: String
    ): Result<Unit> {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("cloud_orders").document(orderId)

        return try {
            docRef.update(
                mapOf(
                    "printStatus" to printStatus,
                    "printedByTerminal" to masterTerminalName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCloudOrderStatusAndArchive(
        userId: String,
        orderId: String,
        status: String? = null,
        isArchived: Boolean? = null,
        driverName: String? = null
    ): Result<Unit> {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("cloud_orders").document(orderId)

        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        if (status != null) updates["status"] = status
        if (driverName != null) updates["driverName"] = driverName
        if (isArchived != null) {
            updates["isArchived"] = isArchived
            updates["unarchiveTimestamp"] = if (!isArchived) System.currentTimeMillis() else 0L
        }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCloudOrderCustomerDetails(
        userId: String,
        orderId: String,
        orderType: String,
        driverName: String,
        customerName: String,
        customerPhone: String,
        customerSecondaryPhone: String,
        customerArea: String,
        deliveryAddressFull: String,
        deliveryFee: Double,
        total: Double,
        notes: String
    ): Result<Unit> {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("cloud_orders").document(orderId)
        val updates = mapOf(
            "orderType" to orderType,
            "driverName" to driverName,
            "customerName" to customerName,
            "customerPhone" to customerPhone,
            "customerSecondaryPhone" to customerSecondaryPhone,
            "customerArea" to customerArea,
            "deliveryAddressFull" to deliveryAddressFull,
            "deliveryFee" to deliveryFee.coerceAtLeast(0.0),
            "total" to total.coerceAtLeast(0.0),
            "notes" to notes,
            "hasFullDetails" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    private fun isPermanentOrderDocUpdateError(err: Throwable): Boolean {
        val code = (err as? com.google.firebase.firestore.FirebaseFirestoreException)?.code
        if (code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND ||
            code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            code == com.google.firebase.firestore.FirebaseFirestoreException.Code.INVALID_ARGUMENT
        ) {
            return true
        }
        val msg = err.message ?: ""
        return msg.contains("NOT_FOUND", ignoreCase = true) ||
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("INVALID_ARGUMENT", ignoreCase = true)
    }

    /**
     * Archives or unarchives cloud orders in batches of 400. If any chunk batch commit fails
     * (for example due to a single missing or schema-invalid legacy doc), falls back to per-doc
     * updates so one bad doc never aborts the rest of the chunk.
     *
     * @return Set of cloud order IDs that failed due to transient/network errors and should be retried.
     */
    suspend fun batchArchiveCloudOrdersDetailed(
        userId: String,
        orderIds: List<String>,
        isArchived: Boolean,
        statusOnArchive: String? = null
    ): Set<String> {
        val cleanIds = orderIds.filter { it.isNotBlank() }.distinct()
        if (cleanIds.isEmpty()) return emptySet()
        val uid = requireUserId(userId)
        val col = db.collection("users").document(uid).collection("cloud_orders")
        val transientFailedIds = mutableSetOf<String>()
        val unarchiveTs = if (!isArchived) System.currentTimeMillis() else 0L

        for (chunk in cleanIds.chunked(400)) {
            val payload = mutableMapOf<String, Any>(
                "isArchived" to isArchived,
                "unarchiveTimestamp" to unarchiveTs,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (statusOnArchive != null) {
                payload["status"] = statusOnArchive
            }
            try {
                val batch = db.batch()
                for (oid in chunk) {
                    batch.update(col.document(oid), payload)
                }
                batch.commit().await()
            } catch (batchErr: Exception) {
                if (batchErr is CancellationException) throw batchErr
                android.util.Log.w("FirestoreCloudRepo", "Batch archive chunk failed, falling back to per-doc updates: ${batchErr.message}")
                for (oid in chunk) {
                    try {
                        col.document(oid).update(payload).await()
                    } catch (docErr: Exception) {
                        if (docErr is CancellationException) throw docErr
                        if (isPermanentOrderDocUpdateError(docErr)) {
                            android.util.Log.w("FirestoreCloudRepo", "Skipping permanent doc update error for $oid: ${docErr.message}")
                        } else {
                            transientFailedIds.add(oid)
                            handleFirestoreError(docErr, OperationType.UPDATE, "${col.path}/$oid")
                        }
                    }
                }
            }
        }
        return transientFailedIds
    }

    suspend fun batchArchiveCloudOrders(
        userId: String,
        orderIds: List<String>,
        isArchived: Boolean
    ): Result<Unit> {
        val cleanIds = orderIds.filter { it.isNotBlank() }.distinct()
        if (cleanIds.isEmpty()) return Result.success(Unit)
        return try {
            val transientFailed = batchArchiveCloudOrdersDetailed(userId, cleanIds, isArchived)
            if (transientFailed.isEmpty()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("تعذر مزامنة أرشفة ${transientFailed.size} أوردر مؤقتاً وسيتم إعادة المحاولة"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/cloud_orders")
            Result.failure(e)
        }
    }

    // --- 4. Terminals Registration ---

    fun observeTerminals(userId: String): Flow<List<TerminalDoc>> {
        val path = "users/$userId/terminals"
        return db.collection("users").document(userId).collection("terminals")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toTerminalDoc() } }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun registerTerminal(userId: String, terminal: TerminalDoc): Result<Unit> {
        val uid = requireUserId(userId)
        val terminalId = if (terminal.id.isNotBlank()) terminal.id else "term_${UUID.randomUUID()}"
        val docRef = db.collection("users").document(uid).collection("terminals").document(terminalId)

        return try {
            val payload = mapOf(
                "name" to terminal.name,
                "isMaster" to terminal.isMaster,
                "canPrint" to terminal.canPrint,
                "printerListJson" to terminal.printerListJson,
                "lastActive" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 5. Work Shifts Sync ---

    fun observeShifts(userId: String): Flow<List<CloudShiftDoc>> {
        val path = "users/$userId/shifts"
        return db.collection("users").document(userId).collection("shifts")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.toCloudShiftDoc() } }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun upsertShift(userId: String, shift: CloudShiftDoc): Result<Unit> {
        val uid = requireUserId(userId)
        val shiftId = if (shift.id.isNotBlank()) shift.id else "shift_${shift.shiftNumber}"
        val docRef = db.collection("users").document(uid).collection("shifts").document(shiftId)

        return try {
            val cached = try { docRef.get(Source.CACHE).await() } catch (_: Exception) { null }
            val existing = if (cached != null && cached.exists()) cached else docRef.get().await()
            val createdAtVal = if (existing.exists()) {
                existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            } else {
                FieldValue.serverTimestamp()
            }

            val payload = mapOf(
                "shiftNumber" to shift.shiftNumber,
                "cashierName" to shift.cashierName,
                "openedAtMs" to shift.openedAtMs,
                "closedAtMs" to shift.closedAtMs,
                "openingBalance" to shift.openingBalance,
                "totalCashSales" to shift.totalCashSales,
                "totalWalletSales" to shift.totalWalletSales,
                "totalExpenses" to shift.totalExpenses,
                "actualDrawerCash" to shift.actualDrawerCash,
                "expectedDrawerCash" to shift.expectedDrawerCash,
                "difference" to shift.difference,
                "ordersCount" to shift.ordersCount,
                "isClosed" to shift.isClosed,
                "notes" to shift.notes,
                "createdAt" to createdAtVal,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 6. Batch Cloud Upload Helpers (For Fast Auto-Push after Backup Restore) ---

    suspend fun batchUpsertCategories(
        userId: String,
        categories: List<CloudCategory>,
        skipExistingCheck: Boolean = false
    ): Result<Unit> {
        if (categories.isEmpty()) return Result.success(Unit)
        val uid = requireUserId(userId)
        val col = db.collection("users").document(uid).collection("categories")
        return try {
            val existingTimestamps = if (skipExistingCheck) {
                emptyMap()
            } else {
                col.get().await().documents.associate { it.id to it.getTimestamp("createdAt") }
            }
            for (chunk in categories.chunked(200)) {
                val batch = db.batch()
                for (cat in chunk) {
                    val catId = if (cat.id.isNotBlank()) cat.id else "cat_${UUID.randomUUID()}"
                    val createdAtVal: Any = existingTimestamps[catId] ?: FieldValue.serverTimestamp()
                    val payload = mapOf(
                        "name" to cat.name,
                        "color" to cat.color,
                        "sortOrder" to cat.sortOrder,
                        "isArchived" to cat.isArchived,
                        "createdAt" to createdAtVal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(col.document(catId), payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
            Result.failure(e)
        }
    }

    suspend fun batchUpsertMenuItems(
        userId: String,
        items: List<CloudMenuItem>,
        skipExistingCheck: Boolean = false
    ): Result<Unit> {
        if (items.isEmpty()) return Result.success(Unit)
        val uid = requireUserId(userId)
        val col = db.collection("users").document(uid).collection("menu_items")
        return try {
            val existingTimestamps = if (skipExistingCheck) {
                emptyMap()
            } else {
                col.get().await().documents.associate { it.id to it.getTimestamp("createdAt") }
            }
            for (chunk in items.chunked(200)) {
                val batch = db.batch()
                for (item in chunk) {
                    val itemId = if (item.id.isNotBlank()) item.id else "item_${UUID.randomUUID()}"
                    val createdAtVal: Any = existingTimestamps[itemId] ?: FieldValue.serverTimestamp()
                    val payload = mapOf(
                        "name" to item.name,
                        "category" to item.category,
                        "price" to item.price,
                        "costPrice" to item.costPrice,
                        "stockQuantity" to item.stockQuantity,
                        "minStockAlert" to item.minStockAlert,
                        "unit" to item.unit,
                        "targetPrinter" to item.targetPrinter,
                        "kitchenName" to item.kitchenName,
                        "supportsPreparation" to item.supportsPreparation,
                        "isArchived" to item.isArchived,
                        "isActive" to item.isActive,
                        "displayOrder" to item.displayOrder,
                        "isPlateWeightEnabled" to item.isPlateWeightEnabled,
                        "plateWeightKg" to item.plateWeightKg,
                        "pricePerKg" to item.pricePerKg,
                        "createdAt" to createdAtVal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(col.document(itemId), payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
            Result.failure(e)
        }
    }

    suspend fun batchUpsertShifts(
        userId: String,
        shifts: List<CloudShiftDoc>,
        skipExistingCheck: Boolean = false
    ): Result<Unit> {
        if (shifts.isEmpty()) return Result.success(Unit)
        val uid = requireUserId(userId)
        val col = db.collection("users").document(uid).collection("shifts")
        return try {
            val existingTimestamps = if (skipExistingCheck) {
                emptyMap()
            } else {
                col.get().await().documents.associate { it.id to it.getTimestamp("createdAt") }
            }
            for (chunk in shifts.chunked(200)) {
                val batch = db.batch()
                for (shift in chunk) {
                    val shiftId = if (shift.id.isNotBlank()) shift.id else "shift_${shift.shiftNumber}"
                    val createdAtVal: Any = existingTimestamps[shiftId] ?: FieldValue.serverTimestamp()
                    val payload = mapOf(
                        "shiftNumber" to shift.shiftNumber,
                        "cashierName" to shift.cashierName,
                        "openedAtMs" to shift.openedAtMs,
                        "closedAtMs" to shift.closedAtMs,
                        "openingBalance" to shift.openingBalance,
                        "totalCashSales" to shift.totalCashSales,
                        "totalWalletSales" to shift.totalWalletSales,
                        "totalExpenses" to shift.totalExpenses,
                        "actualDrawerCash" to shift.actualDrawerCash,
                        "expectedDrawerCash" to shift.expectedDrawerCash,
                        "difference" to shift.difference,
                        "ordersCount" to shift.ordersCount,
                        "isClosed" to shift.isClosed,
                        "notes" to shift.notes,
                        "createdAt" to createdAtVal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(col.document(shiftId), payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
            Result.failure(e)
        }
    }

    suspend fun batchUpsertOrders(
        userId: String,
        orders: List<CloudOrderDoc>,
        skipExistingCheck: Boolean = false
    ): Result<Unit> {
        if (orders.isEmpty()) return Result.success(Unit)
        val uid = requireUserId(userId)
        val col = db.collection("users").document(uid).collection("cloud_orders")
        return try {
            val existingTimestamps = if (skipExistingCheck) {
                emptyMap()
            } else {
                col.get().await().documents.associate { it.id to it.getTimestamp("createdAt") }
            }
            for (chunk in orders.chunked(150)) {
                val batch = db.batch()
                for (order in chunk) {
                    val orderId = if (order.id.isNotBlank()) order.id else "order_${order.orderNumber}"
                    val createdAtVal: Any = existingTimestamps[orderId] ?: FieldValue.serverTimestamp()
                    val payload = mapOf(
                        "orderNumber" to order.orderNumber,
                        "customerName" to order.customerName,
                        "customerPhone" to order.customerPhone,
                        "customerSecondaryPhone" to order.customerSecondaryPhone,
                        "customerArea" to order.customerArea,
                        "deliveryAddressTitle" to order.deliveryAddressTitle,
                        "deliveryAddressFull" to order.deliveryAddressFull,
                        "orderType" to order.orderType,
                        "status" to order.status,
                        "printStatus" to order.printStatus,
                        "requestedPrinterTarget" to order.requestedPrinterTarget,
                        "reprintReason" to order.reprintReason,
                        "isArchived" to order.isArchived,
                        "unarchiveTimestamp" to order.unarchiveTimestamp,
                        "shiftNumber" to order.shiftNumber,
                        "itemsJson" to order.itemsJson,
                        "subtotal" to order.subtotal,
                        "deliveryFee" to order.deliveryFee,
                        "discount" to order.discount,
                        "total" to order.total,
                        "paymentMethod" to order.paymentMethod,
                        "notes" to order.notes,
                        "driverName" to order.driverName,
                        "createdByTerminal" to order.createdByTerminal,
                        "printedByTerminal" to order.printedByTerminal,
                        "createdAtMs" to order.createdAtMs.coerceAtLeast(0L),
                        "hasFullDetails" to order.hasFullDetails,
                        "createdAt" to createdAtVal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(col.document(orderId), payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
            Result.failure(e)
        }
    }

    // --- 7. Backups & Restores (Safe snapshot repository) ---

    suspend fun createCloudBackup(userId: String, backup: CloudBackupDoc): Result<String> {
        val uid = requireUserId(userId)
        val backupId = if (backup.id.isNotBlank()) backup.id else "backup_${System.currentTimeMillis()}"
        val docRef = db.collection("users").document(uid).collection("backups").document(backupId)

        return try {
            val payload = mapOf(
                "backupName" to backup.backupName,
                "dataJson" to backup.dataJson,
                "version" to backup.version,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(backupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCloudBackups(userId: String): Flow<List<CloudBackupDoc>> {
        val path = "users/$userId/backups"
        return db.collection("users").document(userId).collection("backups")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(CloudBackupDoc::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // --- 8. Shared Atomic Order Counter ---

    fun observeCloudOrderCounter(userId: String): Flow<Long?> {
        val counterRef = db.collection("users").document(userId).collection("counters").document("order_counter")
        return counterRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null else snapshot.getLong("lastOrderNumber")
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, counterRef.path)
                throw error
            }
    }

    suspend fun setCloudOrderNumberOverride(userId: String, nextOrderNum: Int): Result<Unit> {
        val uid = requireUserId(userId)
        val counterRef = db.collection("users").document(uid).collection("counters").document("order_counter")
        val safeNext = nextOrderNum.coerceAtLeast(1)
        val desiredLast = (safeNext - 1).toLong()

        return try {
            counterRef.set(
                mapOf(
                    "lastOrderNumber" to desiredLast,
                    "overrideTimestamp" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, counterRef.path)
            Result.failure(e)
        }
    }

    /**
     * Strict server-verified sequential order counter allocation for Seller terminals.
     * Because Firestore transactions require server connectivity, this fails cleanly when offline
     * instead of assigning an un-synced local order number.
     */
    suspend fun tryAllocateOnlineOrderNumber(userId: String, fallbackLocalMax: Int): Result<Int> {
        val uid = requireUserId(userId)
        val counterRef = db.collection("users").document(uid).collection("counters").document("order_counter")

        return try {
            val assigned = db.runTransaction { transaction ->
                val snapshot = transaction.get(counterRef)
                val nextVal = if (snapshot.exists()) {
                    val currentInCloud = snapshot.getLong("lastOrderNumber") ?: 0L
                    currentInCloud.coerceAtLeast(0L) + 1L
                } else {
                    (fallbackLocalMax - 1).coerceAtLeast(0).toLong() + 1L
                }

                transaction.set(
                    counterRef,
                    mapOf(
                        "lastOrderNumber" to nextVal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                nextVal.toInt()
            }.await()
            Result.success(assigned)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("FirestoreCloudRepository", "tryAllocateOnlineOrderNumber failed (offline/unreachable): ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAndIncrementOrderNumber(userId: String, fallbackLocalMax: Int): Int {
        return tryAllocateOnlineOrderNumber(userId, fallbackLocalMax).getOrElse { fallbackLocalMax }
    }

    suspend fun syncMaxOrderNumber(userId: String, localMax: Int): Result<Unit> {
        val uid = requireUserId(userId)
        val counterRef = db.collection("users").document(uid).collection("counters").document("order_counter")

        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(counterRef)
                if (!snapshot.exists()) {
                    val desiredLast = (localMax - 1).coerceAtLeast(0).toLong()
                    transaction.set(
                        counterRef,
                        mapOf(
                            "lastOrderNumber" to desiredLast,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- 9. Supreme-Authority Backup Restore (Wipe + Epoch Sync) ---

    private suspend fun deleteAllDocsInCollection(collectionPath: String, colRef: com.google.firebase.firestore.CollectionReference) {
        while (true) {
            val snap = colRef.limit(250).get().await()
            if (snap.isEmpty) break
            val batch = db.batch()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            if (snap.size() < 250) break
        }
    }

    /**
     * Clears all shared operational collections on Firestore before pushing a restored backup
     * from the Cashier terminal, ensuring stale/newer shifts, orders, categories, menu items,
     * and tombstones cannot resurrect or auto-archive the restored backup.
     */
    suspend fun wipeCloudWorkspaceForRestore(userId: String): Result<Unit> {
        val uid = requireUserId(userId)
        val userDoc = db.collection("users").document(uid)
        return try {
            deleteAllDocsInCollection("users/$uid/cloud_orders", userDoc.collection("cloud_orders"))
            deleteAllDocsInCollection("users/$uid/shifts", userDoc.collection("shifts"))
            deleteAllDocsInCollection("users/$uid/menu_items", userDoc.collection("menu_items"))
            deleteAllDocsInCollection("users/$uid/categories", userDoc.collection("categories"))
            clearAllMenuItemTombstones(uid).getOrThrow()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun publishRestoreEpoch(userId: String, epochMs: Long, restoredBy: String): Result<Unit> {
        val uid = requireUserId(userId)
        val syncStateRef = db.collection("users").document(uid).collection("counters").document("sync_state")
        return try {
            syncStateRef.set(
                mapOf(
                    "restoreEpoch" to epochMs,
                    "restoredBy" to restoredBy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, syncStateRef.path)
            Result.failure(e)
        }
    }

    fun observeRestoreEpoch(userId: String): Flow<Long?> {
        val syncStateRef = db.collection("users").document(userId).collection("counters").document("sync_state")
        return syncStateRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null else snapshot.getLong("restoreEpoch")
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, syncStateRef.path)
                throw error
            }
    }
}

