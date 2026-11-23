package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for General Accounts (Suppliers, Importers, External Persons, Wholesalers, Partners, Maintenance, etc.)
 */
@Entity(tableName = "general_accounts")
data class GeneralAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "SUPPLIER", // "SUPPLIER" (مورد أسماك وخامات), "IMPORTER" (مستورد), "WHOLESALER" (تاجر جملة), "INDIVIDUAL" (شخص عادي), "PARTNER" (شريك), "MAINTENANCE" (صيانة وخدمات), "OTHER" (أخرى)
    val phone: String = "",
    val address: String = "",
    val openingBalance: Double = 0.0, // رصيد افتتاحي: موجب = له (دائن)، سالب = عليه (مدين)
    val currentBalance: Double = 0.0, // موجب = له (دائن للمحل)، سالب = عليه (مدين للمحل)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

/**
 * Transactions on a General Account (Supplies/Purchases, Payments, Cash Receipts, Returns, Adjustments)
 */
@Entity(
    tableName = "general_account_transactions",
    foreignKeys = [
        ForeignKey(
            entity = GeneralAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class AccountTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: String, // "SUPPLY" (توريد بضاعة/فاتورة مشتريات), "PAYMENT" (سداد دفعة للمورد نقدية), "RECEIPT" (قبض نقدية مستلمة منه), "RETURN" (مرتجع بضاعة), "DISCOUNT" (خصم مكتسب / تسوية رصيد)
    val amount: Double,
    val description: String,
    val invoiceNumber: String = "", // رقم الفاتورة أو إيصال السداد إن وجد
    val date: Long = System.currentTimeMillis(),
    val isPaidFromDrawer: Boolean = true, // هل تم الصرف/القبض نقداً من درج الكاشير
    val shiftId: Long? = null // الوردية المرتبطة
)
