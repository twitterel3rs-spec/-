package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersDirect(): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    fun getOrderByIdFlow(orderId: Long): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders WHERE cloudOrderId = :cloudId LIMIT 1")
    suspend fun getOrderByCloudId(cloudId: String): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber ORDER BY createdAt DESC LIMIT 1")
    suspend fun getOrderByOrderNumber(orderNumber: String): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber ORDER BY createdAt DESC")
    suspend fun getOrdersByOrderNumberAll(orderNumber: String): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber AND createdAt >= :sinceTimestamp ORDER BY createdAt DESC LIMIT 1")
    suspend fun getRecentOrderByOrderNumber(orderNumber: String, sinceTimestamp: Long): OrderWithItems?

    @Query("UPDATE orders SET cloudOrderId = :cloudOrderId WHERE id = :orderId")
    suspend fun updateCloudOrderId(orderId: Long, cloudOrderId: String)

    @Query("UPDATE orders SET createdAt = :createdAt WHERE id = :orderId")
    suspend fun updateOrderCreatedAt(orderId: Long, createdAt: Long)

    @Transaction
    @Query("SELECT * FROM orders WHERE cloudOrderId IS NULL OR TRIM(cloudOrderId) = '' ORDER BY createdAt DESC LIMIT 150")
    suspend fun getUnsyncedOrders(): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE (cloudOrderId IS NULL OR TRIM(cloudOrderId) = '') AND isArchived = 0 ORDER BY createdAt DESC LIMIT 50")
    suspend fun getUnsyncedActiveOrders(): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE syncState = 'PENDING_OFFLINE' ORDER BY createdAt ASC")
    suspend fun getPendingOfflineOrders(): List<OrderWithItems>

    @Query("SELECT COUNT(*) FROM orders WHERE syncState = 'PENDING_OFFLINE' OR syncState = 'WAITING_CASHIER_ACK'")
    fun getPendingSyncOrdersCountFlow(): Flow<Int>

    @Query("UPDATE orders SET orderNumber = :orderNumber, cloudOrderId = :cloudOrderId, syncState = :syncState, pendingPrintAction = :pendingPrintAction, pendingPrintRequestedAtMs = :pendingPrintRequestedAtMs WHERE id = :orderId")
    suspend fun updateOrderSyncMetadata(
        orderId: Long,
        orderNumber: String,
        cloudOrderId: String?,
        syncState: String,
        pendingPrintAction: String,
        pendingPrintRequestedAtMs: Long
    )

    @Query("UPDATE orders SET syncState = :syncState, pendingPrintAction = :pendingPrintAction, pendingPrintRequestedAtMs = :pendingPrintRequestedAtMs WHERE id = :orderId")
    suspend fun updateOrderSyncState(
        orderId: Long,
        syncState: String,
        pendingPrintAction: String = "",
        pendingPrintRequestedAtMs: Long = 0L
    )

    @Transaction
    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC LIMIT 10")
    fun getOrdersByCustomer(customerId: Long): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC LIMIT 10")
    suspend fun getOrdersListByCustomer(customerId: Long): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE customerPhone = :phone ORDER BY createdAt DESC LIMIT 10")
    suspend fun getOrdersListByPhone(phone: String): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE driverId = :driverId AND createdAt >= :sinceTimestamp AND status != 'CANCELLED' ORDER BY createdAt DESC")
    suspend fun getOrdersForDriverSince(driverId: Long, sinceTimestamp: Long): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE driverId = :driverId AND createdAt >= :startDate AND createdAt <= :endDate AND status != 'CANCELLED' ORDER BY createdAt DESC")
    suspend fun getOrdersForDriverBetween(driverId: Long, startDate: Long, endDate: Long): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE driverId = :driverId AND createdAt >= :sinceTimestamp AND isSettledWithDriver = 0 AND status != 'CANCELLED' ORDER BY createdAt DESC")
    suspend fun getUnsettledOrdersForDriverSince(driverId: Long, sinceTimestamp: Long): List<OrderWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItem): Long

    @Update
    suspend fun updateOrderItem(item: OrderItem)

    @Query("DELETE FROM order_items WHERE id = :itemId")
    suspend fun deleteOrderItem(itemId: Long)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: Long)

    @Update
    suspend fun updateOrder(order: Order)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String)

    @Query("UPDATE orders SET printedMain = :printed WHERE id = :orderId")
    suspend fun updateMainPrinted(orderId: Long, printed: Boolean)

    @Query("UPDATE orders SET printedKitchen1 = :printed WHERE id = :orderId")
    suspend fun updateKitchen1Printed(orderId: Long, printed: Boolean)

    @Query("UPDATE orders SET printedKitchen2 = :printed WHERE id = :orderId")
    suspend fun updateKitchen2Printed(orderId: Long, printed: Boolean)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: Long)

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getTotalOrdersCount(): Int

    @Query("SELECT SUM(totalAmount) FROM orders WHERE status != 'CANCELLED'")
    suspend fun getTotalSales(): Double?

    @Query("SELECT SUM(totalAmount) FROM orders WHERE status != 'CANCELLED' AND createdAt >= :startOfDay")
    suspend fun getTodaySales(startOfDay: Long): Double?

    @Query("UPDATE orders SET orderType = :orderType, driverId = :driverId, driverName = :driverName, customerName = :name, customerPhone = :phone, customerSecondaryPhone = :secondaryPhone, customerArea = :area, deliveryAddressFull = :address, deliveryFee = :fee, totalAmount = :total, driverCommission = :driverCommission, notes = :notes WHERE id = :orderId")
    suspend fun updateOrderCustomerAndDetails(
        orderId: Long,
        orderType: String,
        driverId: Long?,
        driverName: String?,
        name: String,
        phone: String,
        secondaryPhone: String?,
        area: String?,
        address: String?,
        fee: Double,
        total: Double,
        driverCommission: Double,
        notes: String
    )

    @Query("UPDATE orders SET isSettledWithDriver = 1, isArchived = 1, isPaid = 1, status = CASE WHEN status = 'OUT_FOR_DELIVERY' THEN 'COMPLETED' ELSE status END WHERE id IN (:orderIds)")
    suspend fun markOrdersSettledWithDriver(orderIds: List<Long>)

    @Transaction
    @Query("SELECT * FROM orders WHERE driverId = :driverId AND createdAt >= :sinceTimestamp AND status != 'CANCELLED' ORDER BY createdAt DESC")
    suspend fun getTodayOrdersForDriver(driverId: Long, sinceTimestamp: Long): List<OrderWithItems>

    @Query("UPDATE orders SET paidAmount = :paidAmount, changeAmount = :changeAmount, isPaid = :isPaid WHERE id = :orderId")
    suspend fun updateOrderPayment(orderId: Long, paidAmount: Double, changeAmount: Double, isPaid: Boolean)

    @Query("UPDATE order_items SET printedInKitchen = :printed WHERE id = :itemId")
    suspend fun updateOrderItemKitchenPrinted(itemId: Long, printed: Boolean)

    @Query("UPDATE order_items SET printedInKitchen = :printed, printedKitchenQuantity = :qty WHERE id = :itemId")
    suspend fun updateOrderItemKitchenPrintedQuantity(itemId: Long, printed: Boolean, qty: Double)

    @Query("UPDATE order_items SET printedInKitchen = :printed WHERE orderId = :orderId")
    suspend fun markAllOrderItemsKitchenPrinted(orderId: Long, printed: Boolean)

    @Query("UPDATE order_items SET printedInKitchen = 1, printedKitchenQuantity = quantity WHERE orderId = :orderId")
    suspend fun markAllOrderItemsKitchenPrintedWithCurrentQuantity(orderId: Long)

    // Shift Queries
    @Transaction
    @Query("SELECT * FROM orders WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    fun getOrdersByShift(shiftId: Long): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    suspend fun getOrdersListByShift(shiftId: Long): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders WHERE createdAt >= :startDate AND createdAt <= :endDate ORDER BY createdAt DESC")
    fun getOrdersBetween(startDate: Long, endDate: Long): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE createdAt >= :startDate AND createdAt <= :endDate ORDER BY createdAt DESC")
    suspend fun getOrdersListBetween(startDate: Long, endDate: Long): List<OrderWithItems>

    // Driver Settlement Adjustments
    @Query("UPDATE orders SET isExcludedFromDriverSettlement = :isExcluded, driverSettlementAdjustmentReason = :reason WHERE id = :orderId")
    suspend fun updateOrderDriverExclusion(orderId: Long, isExcluded: Boolean, reason: String?)

    @Query("UPDATE orders SET driverId = :driverId, driverName = :driverName, driverCommission = :commission, isExcludedFromDriverSettlement = 0, driverSettlementAdjustmentReason = :reason WHERE id = :orderId")
    suspend fun assignDriverToOrder(orderId: Long, driverId: Long, driverName: String, commission: Double, reason: String? = null)

    @Transaction
    @Query("SELECT * FROM orders WHERE orderType = 'DELIVERY' AND isSettledWithDriver = 0 AND isArchived = 0 ORDER BY createdAt DESC")
    suspend fun getAllUnsettledDeliveryOrders(): List<OrderWithItems>

    // Order Archiving
    @Query("UPDATE orders SET isArchived = 1 WHERE isArchived = 0 AND syncState != 'PENDING_OFFLINE' AND (shiftId = :shiftId OR createdAt <= :cutoffTimestamp)")
    suspend fun archiveOrdersForShift(shiftId: Long, cutoffTimestamp: Long)

    @Query("UPDATE orders SET isArchived = :isArchived WHERE id = :orderId")
    suspend fun setOrderArchived(orderId: Long, isArchived: Boolean)

    @Query("UPDATE orders SET isArchived = 1 WHERE id IN (:orderIds)")
    suspend fun archiveOrders(orderIds: List<Long>)

    @Query("UPDATE orders SET isArchived = 1 WHERE createdAt <= :timestamp AND isArchived = 0")
    suspend fun archiveOrdersUpTo(timestamp: Long)

    @Query("UPDATE orders SET isArchived = 0")
    suspend fun unarchiveAllOrders(): Int
}
