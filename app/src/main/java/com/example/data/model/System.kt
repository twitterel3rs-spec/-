package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "print_logs")
data class PrintLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val orderNumber: String,
    val printerTarget: String, // "MAIN", "KITCHEN_1", "KITCHEN_2"
    val timestamp: Long = System.currentTimeMillis(),
    val isReprint: Boolean = false,
    val reprintReason: String? = null
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

/**
 * Entity for Cashier / Restaurant Work Shifts.
 * Each order is linked to a shiftId.
 * Closing a shift isolates its financial calculations without deleting or archiving customer orders.
 */
@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftNumber: Int = 1,
    val cashierName: String = "",
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val openingBalance: Double = 0.0, // رصيد الدرج الافتتاحي (عهدة الدرج)
    val totalCashSales: Double = 0.0, // إجمالي المبيعات كاش (شاملة)
    val totalStoreCashSales: Double = 0.0, // مبيعات كاش مباشرة بالمحل (تيك أواي وصالة) المضافة للدرج
    val totalDeliverySettlementCash: Double = 0.0, // توريدات نقدية مستلمة من تسويات الطيارين للدرج
    val totalStaffWithdrawals: Double = 0.0, // سلف ومسحوبات الموظفين من الدرج
    val totalSupplierPayments: Double = 0.0, // مدفوعات الموردين والخامات من الدرج
    val totalOtherExpenses: Double = 0.0, // مصروفات ونثريات أخرى من الدرج
    val totalWalletSales: Double = 0.0, // إجمالي مبيعات المحافظ الإلكترونية (فودافون كاش / إنستاباي - خارج الدرج)
    val totalExpenses: Double = 0.0, // إجمالي المصروفات والمسحوبات المدفوعة من الدرج
    val expectedDrawerCash: Double = 0.0, // رصيد الدرج المحسوب (افتتاحي + كاش محل + تسويات طيارين - منصرفات)
    val actualDrawerCash: Double = 0.0, // المبالغ المحسوبة فعلياً بالدرج عند الجرد
    val difference: Double = 0.0, // عجز (-) أو زيادة (+)
    val ordersCount: Int = 0,
    val isClosed: Boolean = false,
    val notes: String = ""
)
