package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@androidx.compose.runtime.Immutable
@Entity(tableName = "menu_categories")
data class MenuCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val displayOrder: Int = 0,
    val isArchived: Boolean = false
)

@androidx.compose.runtime.Immutable
@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "فريش", "ني", "جمبريات", "كابوريا", "ساندوتشات", "وجبات", "إضافات"
    val unit: String, // "كيلو", "جرام", "قطعة"
    val basePrice: Double, // سعر البيع الحالي
    val costPrice: Double = 0.0,
    val supportsPreparation: Boolean = false, // True for fresh/raw fish, false for sandwiches/meals/sides
    val currentStock: Double = 0.0, // الرصيد الحالي بالكيلو أو بالقطعة
    val minStockAlert: Double = 3.0, // حد إعادة الطلب والتنبيه
    val targetPrinter: String = "MAIN", // MAIN, KITCHEN_1, KITCHEN_2, or comma-separated
    val kitchenName: String = "", // اسم الصنف في أمر التشغيل / المطبخ (إذا كان فارغاً يُستخدم اسم الفاتورة)
    val isAvailable: Boolean = true,
    val isArchived: Boolean = false,
    val displayOrder: Int = 0,
    val isPlateWeightEnabled: Boolean = false,
    val plateWeightKg: Double = 0.5,
    val pricePerKg: Double = 0.0,
    val cloudId: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "preparation_types")
data class PreparationType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // "تنظيف وتجهيز", "شوي سنجاري", "شوي ردة", "شوي زيت وليمون", "قلي مقرمش", "طاجن صلصة", "فحم خلطة"
    val additionalPricePerUnit: Double = 15.0, // تكلفة التجهيز المضافة للكيلو
    val isActive: Boolean = true,
    val applicableCategory: String = "ALL" // "ALL", "فريش", "جمبريات", "ني"
)

/**
 * Entity for linking menu items to ingredients or common stock items.
 * Example: Seafood Meal or Rizo requires 0.25 kg of Cooked Rice, 1 piece Bread, etc.
 * Running out of Rice automatically prevents selling meals that depend on it.
 */
@Entity(
    tableName = "menu_item_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = MenuItem::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["menuItemId"]), Index(value = ["ingredientItemId"])]
)
data class MenuItemIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val menuItemId: Long, // معرف الصنف التابع (الوجبة أو الطبق)
    val ingredientItemId: Long, // معرف الصنف الخام / المكون المشترك
    val ingredientName: String = "", // اسم المكون للعرض السريع
    val requiredQuantityPerUnit: Double = 1.0, // الكمية المطلوبة من المكون لكل وحدة من الصنف
    val unit: String = "كيلو",
    val notes: String = ""
)
