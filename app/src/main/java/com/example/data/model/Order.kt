package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String, // e.g. "#1001"
    val orderType: String, // "DELIVERY", "TAKEAWAY", "DINE_IN"
    val customerId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerSecondaryPhone: String? = null,
    val customerArea: String? = null,
    val deliveryAddressTitle: String? = null,
    val deliveryAddressFull: String? = null,
    val deliveryFee: Double = 0.0,
    val driverId: Long? = null,
    val driverName: String? = null,
    val driverCommission: Double = 0.0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "CASH", // "CASH", "CARD", "WALLET"
    val status: String = "NEW", // "NEW", "PREPARING", "OUT_FOR_DELIVERY", "COMPLETED", "CANCELLED"
    val overridePasswordUsed: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val printedMain: Boolean = false,
    val printedKitchen1: Boolean = false,
    val printedKitchen2: Boolean = false,
    val isSettledWithDriver: Boolean = false,
    val shiftId: Long? = null, // الوردية المرتبط بها الأوردر
    val isExcludedFromDriverSettlement: Boolean = false, // استبعاد من حساب الموصل (مثل استلام عميل بنفسه)
    val driverSettlementAdjustmentReason: String? = null, // سبب التعديل / الاستبعاد
    val isArchived: Boolean = false, // أرشفة الأوردر عند تقفيل الوردية أو يدوياً
    val paidAmount: Double = 0.0, // المبلغ المدفوع من العميل
    val changeAmount: Double = 0.0, // الباقي المستحق للعميل
    val isPaid: Boolean = false, // هل تم السداد بالكامل / خالص
    val sellerName: String = "", // اسم البائع الذي استلم وأنشأ الأوردر
    val cloudOrderId: String? = null, // معرف إضافي فريد للأوردر
    val deviceRole: String = "CASHIER", // دور الجهاز عند الإنشاء: CASHIER أو SELLER
    val syncState: String = "SYNCED", // "SYNCED", "PENDING_OFFLINE", "WAITING_CASHIER_ACK", "PRINT_CONFIRMED", "PRINT_ERROR"
    val pendingPrintAction: String = "", // "", "NEW_ORDER", "NEW_ORDER_SAVED_ONLY", "EDIT_PRINT", "EDIT_SAVED_ONLY", "REPRINT|<target>|<reason>"
    val pendingPrintRequestedAtMs: Long = 0L // وقت طلب الطباعة الفعلي لحساب مهلة تأكيد الـ 30 ثانية
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long = 0,
    val menuItemId: Long? = null, // null for custom exceptional item
    val itemName: String,
    val kitchenName: String = "", // اسم الصنف لأمر التشغيل / المطبخ
    val category: String = "فريش",
    val unit: String = "كيلو", // "كيلو", "جرام", "قطعة"
    val quantity: Double, // e.g. 1.25 kg
    val unitPrice: Double, // Snapshot base unit price at purchase time
    val preparationId: Long? = null,
    val preparationName: String? = null,
    val preparationPrice: Double = 0.0, // Additional prep fee per unit
    val itemNotes: String = "",
    val totalPrice: Double, // (unitPrice + preparationPrice) * quantity
    val isCustomAdHoc: Boolean = false,
    val targetPrinter: String = "MAIN", // MAIN, KITCHEN_1, KITCHEN_2, or comma-separated
    val printedInKitchen: Boolean = false,
    val printedKitchenQuantity: Double = 0.0 // الكمية التي طُبعت بالفعل في أمر التشغيل للمطبخ (لمنع التكرار وطباعة الزيادة فقط)
)

data class OrderWithItems(
    @Embedded val order: Order,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItem>
)

data class CancelledKitchenItem(
    val itemName: String,
    val kitchenName: String = "",
    val targetPrinter: String = "MAIN",
    val preparationName: String? = null,
    val unit: String = "كيلو",
    val cancelledQty: Double,
    val prevQty: Double,
    val newQty: Double
)

