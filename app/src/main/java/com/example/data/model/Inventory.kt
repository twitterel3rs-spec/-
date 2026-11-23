package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_movements",
    indices = [Index(value = ["menuItemId"]), Index(value = ["orderId"])]
)
data class InventoryMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val menuItemId: Long,
    val itemName: String,
    val movementType: String, // "RECEIVE" (استلام توريد), "SALE" (بيع أوردر), "ADJUSTMENT" (تسوية جردية), "WASTE" (هالك/إتلاف)
    val quantityChange: Double, // positive for in, negative for out
    val balanceAfter: Double,
    val orderId: Long? = null, // Linked order to prevent duplicate deductions
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
