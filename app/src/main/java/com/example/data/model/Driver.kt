package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_drivers")
data class DeliveryDriver(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val calculationType: String = "COMMISSION", // "COMMISSION" (عمولة لكل أوردر), "FIXED_DAILY" (يومية ثابتة)
    val ratePerOrder: Double = 15.0, // عمولة الأوردر الواحد
    val fixedDailySalary: Double = 150.0, // اليومية الثابتة
    val isActive: Boolean = true,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "driver_settlements",
    indices = [Index(value = ["driverId"])]
)
data class DriverSettlement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val driverId: Long,
    val driverName: String,
    val date: Long = System.currentTimeMillis(),
    val ordersCount: Int = 0,
    val totalCashCollected: Double = 0.0, // إجمالي المبالغ المحصلة من الزبائن
    val driverEarnings: Double = 0.0, // العمولة المستحقة أو اليومية
    val fuelAndExpenses: Double = 0.0, // مصروفات البنزين والإكراميات والصيانة
    val netAmountToHandover: Double = 0.0, // المبلغ الصافي الواجب تسليمه للمطعم (المحصل - المستحق - البنزين)
    val actualHandoverAmount: Double = 0.0, // المبلغ المسلَّم فعلياً للمطعم
    val financialDifference: Double = 0.0, // الفارق المالي (عجز أو زيادة)
    val settledOrderIds: String = "",
    val isSettled: Boolean = false,
    val notes: String = "",
    val shiftId: Long? = null
)
