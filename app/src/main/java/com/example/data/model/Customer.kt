package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index(value = ["primaryPhone"], unique = true)]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryPhone: String,
    val secondaryPhone: String? = null,
    val notes: String = "",
    val totalOrdersCount: Int = 0,
    val totalSpent: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_addresses",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class CustomerAddress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val title: String, // e.g. "المنزل", "العمل", "المصيف"
    val area: String, // الحي / المنطقة
    val streetDetails: String, // الشارع ورقم العمارة والدور والشقة
    val deliveryFee: Double = 20.0,
    val isDefault: Boolean = false
)@Entity(tableName = "delivery_areas")
data class DeliveryArea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "حي الأربعين", "حي السويس", "بورفؤاد", "الملاحة"
    val deliveryFee: Double = 20.0,
    val defaultDriverCommission: Double = 15.0,
    val isActive: Boolean = true
)
