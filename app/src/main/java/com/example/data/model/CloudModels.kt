package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudMenuItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val minStockAlert: Double = 3.0,
    val unit: String = "وجبة",
    val targetPrinter: String = "MAIN",
    val kitchenName: String = "",
    val supportsPreparation: Boolean = false,
    @get:PropertyName("isArchived") @field:PropertyName("isArchived")
    val isArchived: Boolean = false,
    @get:PropertyName("isActive") @field:PropertyName("isActive")
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    @get:PropertyName("isPlateWeightEnabled") @field:PropertyName("isPlateWeightEnabled")
    val isPlateWeightEnabled: Boolean = false,
    val plateWeightKg: Double = 0.5,
    val pricePerKg: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val color: String = "#0288D1",
    val sortOrder: Int = 0,
    @get:PropertyName("isArchived") @field:PropertyName("isArchived")
    val isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudOrderDoc(
    @DocumentId val id: String = "",
    val orderNumber: Int = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerSecondaryPhone: String = "",
    val customerArea: String = "",
    val deliveryAddressTitle: String = "",
    val deliveryAddressFull: String = "",
    val orderType: String = "TAKEAWAY", // TAKEAWAY, DINE_IN, HALL, DELIVERY
    val status: String = "PENDING",     // NEW, PENDING, PREPARING, READY, OUT_FOR_DELIVERY, COMPLETED, CANCELLED
    val printStatus: String = "PENDING_PRINT", // SAVED_ONLY, PENDING_PRINT, PRINTING, PRINT_REQUESTED, PRINTED, ERROR
    val requestedPrinterTarget: String = "ALL", // ALL, KITCHEN_ALL, MAIN, KITCHEN_1, KITCHEN_2, etc.
    val reprintReason: String = "",
    @get:PropertyName("isArchived") @field:PropertyName("isArchived")
    val isArchived: Boolean = false,
    val unarchiveTimestamp: Long = 0L,
    val shiftNumber: Int = 0,
    val itemsJson: String = "[]",
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: String = "CASH",
    val notes: String = "",
    val driverName: String = "",
    val createdByTerminal: String = "",
    val printedByTerminal: String = "",
    val createdAtMs: Long = 0L,
    val printRequestedAtMs: Long = 0L,
    @get:PropertyName("hasFullDetails") @field:PropertyName("hasFullDetails")
    val hasFullDetails: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CloudOrderChange(
    val order: CloudOrderDoc,
    val changeType: String // "ADDED", "MODIFIED", "REMOVED"
)

@IgnoreExtraProperties
data class CloudShiftDoc(
    @DocumentId val id: String = "",
    val shiftNumber: Int = 1,
    val cashierName: String = "",
    val openedAtMs: Long = 0L,
    val closedAtMs: Long = 0L,
    val openingBalance: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val totalWalletSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val actualDrawerCash: Double = 0.0,
    val expectedDrawerCash: Double = 0.0,
    val difference: Double = 0.0,
    val ordersCount: Int = 0,
    @get:PropertyName("isClosed") @field:PropertyName("isClosed")
    val isClosed: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TerminalDoc(
    @DocumentId val id: String = "",
    val name: String = "",
    @get:PropertyName("isMaster") @field:PropertyName("isMaster")
    val isMaster: Boolean = false,
    val canPrint: Boolean = false,
    val printerListJson: String = "",
    @ServerTimestamp val lastActive: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudCounterDoc(
    @DocumentId val id: String = "",
    val lastOrderNumber: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudBackupDoc(
    @DocumentId val id: String = "",
    val backupName: String = "",
    val dataJson: String = "",
    val version: String = "1.0",
    @ServerTimestamp val createdAt: Timestamp? = null
)
