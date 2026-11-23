package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.model.AccountTransaction
import com.example.data.model.GeneralAccount
import com.example.data.model.AppSetting
import com.example.data.model.CancelledKitchenItem
import com.example.data.model.CloudOrderDoc
import com.example.data.model.Customer
import com.example.data.model.CustomerAddress
import com.example.data.model.DeliveryArea
import com.example.data.model.DeliveryDriver
import com.example.data.model.DriverSettlement
import com.example.data.model.Employee
import com.example.data.model.EmployeeAttendance
import com.example.data.model.EmployeeTransaction
import com.example.data.model.InventoryMovement
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.MenuItemIngredient
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.model.PreparationType
import com.example.data.model.PrintLog
import com.example.data.model.Shift
import com.example.printer.InvoiceTemplateConfig
import com.example.printer.KitchenOrderTemplateConfig
import com.example.printer.PrinterConfig
import com.example.printer.PrinterResult
import com.example.printer.RealPrinterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class OrderPlacementResult {
    data class Success(val orderId: Long, val orderNumber: String) : OrderPlacementResult()
    data class StockError(val itemName: String, val requested: Double, val available: Double, val shortage: Double = 0.0) : OrderPlacementResult()
    data class Error(val message: String) : OrderPlacementResult()
}

data class IngredientShortage(
    val ingredientId: Long,
    val ingredientName: String,
    val required: Double,
    val available: Double,
    val unit: String
)

data class IngredientCheckResult(
    val isAvailable: Boolean,
    val maxPortions: Double,
    val shortages: List<IngredientShortage>
)

data class EmployeeStatement(
    val employee: Employee,
    val startDate: Long,
    val endDate: Long,
    val attendanceRecords: List<EmployeeAttendance>,
    val transactions: List<EmployeeTransaction>,
    val presentDaysCount: Int = 0,
    val absentDaysCount: Int = 0,
    val lateDaysCount: Int = 0,
    val totalHoursWorked: Double = 0.0,
    val totalOvertimeHours: Double = 0.0,
    val totalDelayMinutes: Int = 0,
    val baseAgreedSalary: Double = 0.0,
    val totalEarnedSalary: Double,
    val overtimePay: Double = 0.0,
    val delayDeductions: Double = 0.0,
    val absenceDeductions: Double = 0.0,
    val totalAdvances: Double,
    val totalDeductions: Double,
    val totalBonuses: Double,
    val netEntitlement: Double
)

data class ShiftMovementItem(
    val time: Long,
    val title: String,
    val subtitle: String,
    val category: String, // "OPENING", "STORE_CASH_ORDER", "DRIVER_SETTLEMENT", "ACCOUNT_RECEIPT", "STAFF_ADVANCE", "SUPPLIER_PAYMENT", "EXPENSE", "WALLET_ORDER"
    val inAmount: Double = 0.0,
    val outAmount: Double = 0.0,
    val isDrawerMovement: Boolean = true
)

data class ShiftDrawerStatement(
    val shift: Shift,
    val openingBalance: Double,
    val storeCashOrders: List<OrderWithItems>,
    val totalStoreCash: Double,
    val driverSettlements: List<DriverSettlement>,
    val totalDriverSettlementsCash: Double,
    val accountReceipts: List<Pair<AccountTransaction, String>>,
    val totalAccountReceipts: Double,
    val totalIncomingCash: Double, // openingBalance + totalStoreCash + totalDriverSettlementsCash + totalAccountReceipts

    val staffWithdrawals: List<Pair<EmployeeTransaction, String>>,
    val totalStaffWithdrawals: Double,
    val supplierPayments: List<Pair<AccountTransaction, String>>,
    val totalSupplierPayments: Double,
    val otherExpenses: Double,
    val totalOutgoingCash: Double, // totalStaffWithdrawals + totalSupplierPayments + otherExpenses

    val walletOrders: List<OrderWithItems>,
    val totalWalletSales: Double,

    val expectedDrawerCash: Double,
    val actualDrawerCash: Double,
    val difference: Double,
    val movementsTimeline: List<ShiftMovementItem>
)

data class DriverAccountStatement(
    val driver: DeliveryDriver,
    val startDate: Long,
    val endDate: Long,
    val orders: List<OrderWithItems>,
    val settlements: List<DriverSettlement>,
    val totalDeliveredOrders: Int,
    val totalCashCollected: Double,
    val totalDeliveryFees: Double = 0.0,
    val totalCommissionOrEarnings: Double,
    val totalFuelAndExpenses: Double,
    val netHandoverToRestaurant: Double,
    val actualHandoverAmount: Double = 0.0,
    val financialDifference: Double = 0.0
)

data class ItemSalesStat(
    val menuItemId: Long,
    val itemName: String,
    val category: String,
    val unit: String,
    val totalQuantitySold: Double,
    val totalRevenue: Double,
    val unitCost: Double,
    val totalCost: Double,
    val totalProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val currentStock: Double
)

data class AdvancedSalesReport(
    val startDate: Long,
    val endDate: Long,
    val topSellingItems: List<ItemSalesStat>,
    val leastSellingItems: List<ItemSalesStat>,
    val outOfStockItems: List<MenuItem>,
    val stagnantItems: List<MenuItem>,
    val totalSalesRevenue: Double,
    val totalSalesCost: Double,
    val totalGrossProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val totalOrdersCount: Int,
    val totalDeliveryFees: Double,
    val totalDiscounts: Double
)

class RestaurantRepository(private val db: AppDatabase, private val context: Context) {

    val printerManager = RealPrinterManager(context)

    val allCategories: Flow<List<MenuCategory>> = db.menuDao().getAllActiveCategories()
    val allCategoriesIncludingArchived: Flow<List<MenuCategory>> = db.menuDao().getAllCategories()
    val allMenuItems: Flow<List<MenuItem>> = db.menuDao().getAllMenuItems()
    val allMenuItemsIncludingArchived: Flow<List<MenuItem>> = db.menuDao().getAllMenuItemsIncludingArchived()
    val allPreparations: Flow<List<PreparationType>> = db.menuDao().getAllPreparations()
    val allPreparationsIncludingInactive: Flow<List<PreparationType>> = db.menuDao().getAllPreparationsIncludingInactive()
    val allOrders: Flow<List<OrderWithItems>> = db.orderDao().getAllOrders()
    val pendingSyncOrdersCount: Flow<Int> = db.orderDao().getPendingSyncOrdersCountFlow()
    val allCustomers: Flow<List<Customer>> = db.customerDao().getAllCustomers()
    val allDrivers: Flow<List<DeliveryDriver>> = db.driverDao().getAllDrivers()
    val allDriversIncludingArchived: Flow<List<DeliveryDriver>> = db.driverDao().getAllDriversIncludingArchived()
    val allSettlements: Flow<List<DriverSettlement>> = db.driverDao().getAllSettlements()
    val allEmployees: Flow<List<Employee>> = db.staffDao().getAllEmployees()
    val allEmployeesIncludingArchived: Flow<List<Employee>> = db.staffDao().getAllEmployeesIncludingArchived()
    val allAttendance: Flow<List<EmployeeAttendance>> = db.staffDao().getAllAttendance()
    val allEmployeeTransactions: Flow<List<EmployeeTransaction>> = db.staffDao().getAllTransactions()
    val allInventoryMovements: Flow<List<InventoryMovement>> = db.inventoryDao().getAllMovements()
    val allPrintLogs: Flow<List<PrintLog>> = db.settingsDao().getAllPrintLogs()
    val allDeliveryAreas: Flow<List<DeliveryArea>> = db.settingsDao().getAllDeliveryAreas()
    val allActiveDeliveryAreas: Flow<List<DeliveryArea>> = db.settingsDao().getAllActiveDeliveryAreas()
    val activeShift: Flow<Shift?> = db.settingsDao().getActiveShift()
    val allShifts: Flow<List<Shift>> = db.settingsDao().getAllShifts()
    val allIngredients: Flow<List<MenuItemIngredient>> = db.menuDao().getAllIngredients()
    val allAccounts: Flow<List<GeneralAccount>> = db.accountDao().getAllAccounts()
    val allAccountsIncludingArchived: Flow<List<GeneralAccount>> = db.accountDao().getAllAccountsIncludingArchived()
    val allAccountTransactions: Flow<List<AccountTransaction>> = db.accountDao().getAllTransactions()

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        if (db.settingsDao().getSettingValue("managerPassword") == null) {
            db.settingsDao().setSetting(AppSetting("managerPassword", "1234"))
        }
        if (db.settingsDao().getSettingValue("restaurantName") == null) {
            db.settingsDao().setSetting(AppSetting("restaurantName", "صنارة وشبكة للمأكولات البحرية"))
        }
        if (db.settingsDao().getSettingValue("restaurantPhone") == null) {
            db.settingsDao().setSetting(AppSetting("restaurantPhone", "01000000000"))
        }
        if (db.settingsDao().getSettingValue("restaurantAddress") == null) {
            db.settingsDao().setSetting(AppSetting("restaurantAddress", "فرع المطعم الرئيسي"))
        }
        if (db.settingsDao().getSettingValue("defaultDeliveryFee") == null) {
            db.settingsDao().setSetting(AppSetting("defaultDeliveryFee", "20.0"))
        }
        if (db.settingsDao().getSettingValue("global_low_stock_threshold") == null) {
            db.settingsDao().setSetting(AppSetting("global_low_stock_threshold", "3.0"))
        }
        if (db.settingsDao().getSettingValue("auto_print_on_order") == null) {
            db.settingsDao().setSetting(AppSetting("auto_print_on_order", "true"))
        }
        if (db.settingsDao().getSettingValue("lastPriceUpdateDate") == null) {
            db.settingsDao().setSetting(AppSetting("lastPriceUpdateDate", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())))
        }



        if (db.settingsDao().getSettingValue("inv_title") == null) {
            db.settingsDao().setSetting(AppSetting("inv_title", "صنارة وشبكة للمأكولات البحرية"))
            db.settingsDao().setSetting(AppSetting("inv_subtitle", "طازج من البحر لبيتك - أسماك وسواحل"))
            db.settingsDao().setSetting(AppSetting("inv_phone1", "01000000000"))
            db.settingsDao().setSetting(AppSetting("inv_phone2", ""))
            db.settingsDao().setSetting(AppSetting("inv_address", "فرع المطعم الرئيسي"))
            db.settingsDao().setSetting(AppSetting("inv_footer", "شكراً لزيارتكم ونتمنى لكم وجبة شهية!"))
            db.settingsDao().setSetting(AppSetting("inv_show_preps", "true"))
            db.settingsDao().setSetting(AppSetting("inv_show_notes", "true"))
            db.settingsDao().setSetting(AppSetting("inv_show_delivery", "true"))
            db.settingsDao().setSetting(AppSetting("inv_font_size", "NORMAL"))
        }

        if (db.settingsDao().getSettingValue("next_order_number") == null) {
            db.settingsDao().setSetting(AppSetting("next_order_number", "1"))
        }

        // Deduplicate and cleanup phantom/blank categories and preparations
        try {
            val allDbCats = db.menuDao().getAllCategoriesList()
            val seenCatNorms = mutableSetOf<String>()
            for (c in allDbCats) {
                val norm = normalizeArabicText(c.name)
                if (norm.isBlank() || seenCatNorms.contains(norm)) {
                    db.menuDao().deleteCategory(c.id)
                } else {
                    seenCatNorms.add(norm)
                }
            }

            val allDbPreps = db.menuDao().getAllPreparationsList()
            val seenPrepNorms = mutableSetOf<String>()
            for (p in allDbPreps) {
                val norm = normalizeArabicText(p.name)
                if (norm.isBlank() || seenPrepNorms.contains(norm)) {
                    db.menuDao().deletePreparation(p.id)
                } else {
                    seenPrepNorms.add(norm)
                }
            }
        } catch (_: Exception) {}

        // Startup initialization - never automatically clear data to protect user records and restored backups
        try {
            db.settingsDao().setSetting(AppSetting("has_cleared_all_dummy_data_v17", "true"))
        } catch (_: Exception) {}

        // Ensure an active shift exists so cashier can work immediately if needed
        try {
            if (db.settingsDao().getActiveShiftDirect() == null) {
                db.settingsDao().insertShift(
                    Shift(
                        shiftNumber = 1,
                        cashierName = "كاشير",
                        openedAt = System.currentTimeMillis(),
                        openingBalance = 0.0,
                        isClosed = false
                    )
                )
            }
        } catch (_: Exception) {}

        // Run menu item deduplication automatically
        try {
            deduplicateMenuItems()
        } catch (_: Exception) {}
    }

    fun normalizeArabicText(text: String): String {
        return text.trim()
            .replace(Regex("[\\s]+"), " ")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .lowercase()
    }

    suspend fun clearAllDummyData() = withContext(Dispatchers.IO) {
        try {
            db.openHelper.writableDatabase.execSQL("DELETE FROM orders")
            db.openHelper.writableDatabase.execSQL("DELETE FROM order_items")
            db.openHelper.writableDatabase.execSQL("DELETE FROM delivery_drivers")
            db.openHelper.writableDatabase.execSQL("DELETE FROM driver_settlements")
            db.openHelper.writableDatabase.execSQL("DELETE FROM menu_items")
            db.openHelper.writableDatabase.execSQL("DELETE FROM menu_item_ingredients")
            db.openHelper.writableDatabase.execSQL("DELETE FROM inventory_movements")
            db.openHelper.writableDatabase.execSQL("DELETE FROM customers")
            db.openHelper.writableDatabase.execSQL("DELETE FROM customer_addresses")
            db.openHelper.writableDatabase.execSQL("DELETE FROM employees")
            db.openHelper.writableDatabase.execSQL("DELETE FROM employee_attendance")
            db.openHelper.writableDatabase.execSQL("DELETE FROM employee_transactions")
            db.openHelper.writableDatabase.execSQL("DELETE FROM general_accounts")
            db.openHelper.writableDatabase.execSQL("DELETE FROM general_account_transactions")
            db.openHelper.writableDatabase.execSQL("DELETE FROM print_logs")
            db.settingsDao().setSetting(AppSetting("next_order_number", "1"))
            db.settingsDao().setSetting(AppSetting("has_cleared_all_dummy_data_v17", "true"))
        } catch (_: Exception) {}
    }

    // Customer Lookup
    suspend fun lookupCustomerByPhone(phone: String): Pair<Customer?, List<OrderWithItems>> = withContext(Dispatchers.IO) {
        val clean = phone.trim()
        val cust = db.customerDao().getCustomerByPhone(clean)
        val orders = if (cust != null) db.orderDao().getOrdersListByCustomer(cust.id) else db.orderDao().getOrdersListByPhone(clean)
        Pair(cust, orders)
    }

    suspend fun getAddressesForCustomer(customerId: Long): List<CustomerAddress> = withContext(Dispatchers.IO) {
        db.customerDao().getAddressesListForCustomer(customerId)
    }

    suspend fun saveCustomerWithAddress(customer: Customer, address: CustomerAddress?): Long = withContext(Dispatchers.IO) {
        val existing = if (customer.id != 0L) {
            db.customerDao().getCustomerById(customer.id)
        } else if (customer.primaryPhone.isNotBlank()) {
            db.customerDao().getCustomerByPhone(customer.primaryPhone.trim())
        } else null

        val finalCust = if (existing != null) {
            customer.copy(
                id = existing.id,
                totalOrdersCount = maxOf(existing.totalOrdersCount, customer.totalOrdersCount),
                totalSpent = maxOf(existing.totalSpent, customer.totalSpent),
                secondaryPhone = customer.secondaryPhone ?: existing.secondaryPhone
            )
        } else customer

        val custId = if (finalCust.id == 0L) {
            db.customerDao().insertCustomer(finalCust)
        } else {
            db.customerDao().updateCustomer(finalCust)
            finalCust.id
        }

        if (address != null) {
            val addrToSave = address.copy(customerId = custId)
            if (addrToSave.id == 0L) db.customerDao().insertAddress(addrToSave) else db.customerDao().updateAddress(addrToSave)
        }
        db.customerDao().syncAllCustomerOrderStats()
        custId
    }

    suspend fun addAddress(address: CustomerAddress): Long = withContext(Dispatchers.IO) {
        if (address.isDefault) db.customerDao().resetDefaultAddress(address.customerId)
        if (address.id == 0L) db.customerDao().insertAddress(address) else { db.customerDao().updateAddress(address); address.id }
    }

    suspend fun updateAddress(address: CustomerAddress) = withContext(Dispatchers.IO) {
        if (address.isDefault) db.customerDao().resetDefaultAddress(address.customerId)
        db.customerDao().updateAddress(address)
    }

    suspend fun deleteAddress(addressId: Long) = withContext(Dispatchers.IO) {
        db.customerDao().deleteAddress(addressId)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        db.customerDao().updateCustomer(customer)
    }

    suspend fun updateCustomerPermanent(customer: Customer) = withContext(Dispatchers.IO) {
        db.customerDao().updateCustomer(customer)
    }

    // Shifts
    suspend fun getActiveShiftDirect(): Shift? = withContext(Dispatchers.IO) {
        db.settingsDao().getActiveShiftDirect()
    }

    suspend fun openNewShift(cashierName: String, openingBalance: Double): Shift = withContext(Dispatchers.IO) {
        val currentActive = db.settingsDao().getActiveShiftDirect()
        if (currentActive != null) return@withContext currentActive

        val lastShift = db.settingsDao().getLastShift()
        val nextNumber = (lastShift?.shiftNumber ?: 0) + 1

        val newShift = Shift(
            shiftNumber = nextNumber,
            cashierName = cashierName.ifBlank { "كاشير رئيسي" },
            openedAt = System.currentTimeMillis(),
            openingBalance = openingBalance,
            isClosed = false
        )
        val id = db.settingsDao().insertShift(newShift)
        newShift.copy(id = id)
    }

    suspend fun getRecentShiftsDirect(limit: Int = 30): List<Shift> = withContext(Dispatchers.IO) {
        db.settingsDao().getRecentShiftsDirect(limit)
    }

    suspend fun getShiftByNumberDirect(shiftNumber: Int): Shift? = withContext(Dispatchers.IO) {
        db.settingsDao().getShiftByNumber(shiftNumber)
    }

    suspend fun forceOpenOrSyncShift(
        shiftNumber: Int,
        cashierName: String,
        openedAt: Long,
        openingBalance: Double = 0.0
    ): Shift = withContext(Dispatchers.IO) {
        val existingByNum = db.settingsDao().getShiftByNumber(shiftNumber)
        if (existingByNum != null && !existingByNum.isClosed) {
            return@withContext existingByNum
        }
        val currentActive = db.settingsDao().getActiveShiftDirect()
        if (currentActive != null) {
            if (currentActive.shiftNumber == shiftNumber && !currentActive.isClosed) {
                return@withContext currentActive
            }
            if (currentActive.shiftNumber < shiftNumber) {
                val closeCutoff = if (openedAt > 0L) openedAt else System.currentTimeMillis()
                db.settingsDao().updateShift(
                    currentActive.copy(
                        isClosed = true,
                        closedAt = closeCutoff,
                        notes = (currentActive.notes ?: "") + " [أغلقت لبدء وردية جديدة]"
                    )
                )
                db.orderDao().archiveOrdersForShift(currentActive.id, closeCutoff)
            } else {
                // Keep newer local shift untouched if cloud has older shift
                if (existingByNum != null) return@withContext existingByNum
            }
        }
        if (existingByNum != null) {
            return@withContext existingByNum
        }

        val newShift = Shift(
            shiftNumber = shiftNumber,
            cashierName = cashierName.ifBlank { "كاشير رئيسي" },
            openedAt = if (openedAt > 0L) openedAt else System.currentTimeMillis(),
            openingBalance = openingBalance,
            isClosed = false
        )
        val id = db.settingsDao().insertShift(newShift)
        newShift.copy(id = id)
    }

    suspend fun syncCloseShiftFromCloud(
        shiftNumber: Int,
        closedAtMs: Long,
        actualDrawerCash: Double,
        totalExpenses: Double,
        notes: String,
        cashierName: String = "كاشير رئيسي",
        openedAtMs: Long = 0L,
        openingBalance: Double = 0.0
    ): Boolean = withContext(Dispatchers.IO) {
        val target = db.settingsDao().getShiftByNumber(shiftNumber)
            ?: db.settingsDao().getActiveShiftDirect()?.takeIf { it.shiftNumber == shiftNumber }

        val effectiveClosedAt = if (closedAtMs > 0L) closedAtMs else System.currentTimeMillis()

        if (target == null) {
            val insertedId = db.settingsDao().insertShift(
                Shift(
                    shiftNumber = shiftNumber,
                    cashierName = cashierName.ifBlank { "كاشير رئيسي" },
                    openedAt = if (openedAtMs > 0L) openedAtMs else effectiveClosedAt,
                    closedAt = effectiveClosedAt,
                    openingBalance = openingBalance,
                    actualDrawerCash = actualDrawerCash,
                    totalOtherExpenses = totalExpenses,
                    totalExpenses = totalExpenses,
                    isClosed = true,
                    notes = notes
                )
            )
            db.orderDao().archiveOrdersForShift(insertedId, effectiveClosedAt)
            return@withContext true
        }

        if (target.isClosed) {
            db.orderDao().archiveOrdersForShift(target.id, target.closedAt ?: effectiveClosedAt)
            return@withContext false
        }

        val statement = getShiftDrawerStatement(target.id)
        val totalStoreCash = statement.totalStoreCash
        val totalDeliverySettlementCash = statement.totalDriverSettlementsCash
        val totalCashSales = totalStoreCash + totalDeliverySettlementCash
        val totalIncoming = target.openingBalance + totalStoreCash + totalDeliverySettlementCash + statement.totalAccountReceipts
        val totalOutgoing = statement.totalStaffWithdrawals + statement.totalSupplierPayments + totalExpenses
        val expectedCash = totalIncoming - totalOutgoing
        val finalActual = if (actualDrawerCash > 0.0) actualDrawerCash else expectedCash

        val closed = target.copy(
            closedAt = effectiveClosedAt,
            totalCashSales = totalCashSales,
            totalStoreCashSales = totalStoreCash,
            totalDeliverySettlementCash = totalDeliverySettlementCash,
            totalStaffWithdrawals = statement.totalStaffWithdrawals,
            totalSupplierPayments = statement.totalSupplierPayments,
            totalOtherExpenses = totalExpenses,
            totalWalletSales = statement.totalWalletSales,
            totalExpenses = totalOutgoing,
            expectedDrawerCash = expectedCash,
            actualDrawerCash = finalActual,
            difference = finalActual - expectedCash,
            ordersCount = statement.storeCashOrders.size + statement.walletOrders.size,
            isClosed = true,
            notes = notes
        )
        db.settingsDao().updateShift(closed)

        db.orderDao().archiveOrdersForShift(target.id, closed.closedAt ?: effectiveClosedAt)
        true
    }

    suspend fun getShiftDrawerStatement(shiftId: Long): ShiftDrawerStatement = withContext(Dispatchers.IO) {
        val shift = db.settingsDao().getShiftById(shiftId) ?: Shift()
        val shiftStartTime = shift.openedAt
        val shiftEndTime = shift.closedAt ?: System.currentTimeMillis()

        val shiftOrders = db.orderDao().getOrdersListByShift(shift.id).filter { it.order.status != "CANCELLED" }

        // 1. In-Store cash orders (Takeaway / Dine-in with CASH - added straight to cashier drawer)
        // Only calculated when confirmed received/completed (تم استلامه وخرج من المحل)
        val storeCashOrders = shiftOrders.filter { 
            it.order.paymentMethod == "CASH" && 
            it.order.orderType != "DELIVERY" &&
            it.order.status == "COMPLETED"
        }
        val totalStoreCash = storeCashOrders.sumOf { it.order.totalAmount }

        // 2. Electronic wallet orders (Vodafone Cash, InstaPay, Card - electronic, not in paper cash drawer)
        val walletOrders = shiftOrders.filter { 
            (it.order.paymentMethod == "WALLET" || it.order.paymentMethod == "CARD") &&
            (it.order.orderType == "DELIVERY" || it.order.status == "COMPLETED")
        }
        val totalWallet = walletOrders.sumOf { it.order.totalAmount }

        // 3. Driver settlements in this shift (cash handed over from delivery to drawer)
        val settlements = db.driverDao().getSettlementsForShift(shift.id, shiftStartTime, shiftEndTime)
        val totalDriverSettlementsCash = settlements.sumOf { if (it.actualHandoverAmount > 0.0) it.actualHandoverAmount else it.netAmountToHandover }

        // 4. Staff transactions drawn from drawer (advances, salary payouts)
        val staffTxList = db.staffDao().getDrawerTransactionsForShift(shift.id, shiftStartTime, shiftEndTime)
        val staffWithdrawals = staffTxList.filter { it.type == "ADVANCE" || it.type == "SALARY_PAYOUT" }.map { tx ->
            val empName = db.staffDao().getEmployeeById(tx.employeeId)?.name ?: "موظف #${tx.employeeId}"
            Pair(tx, empName)
        }
        val totalStaffWithdrawals = staffWithdrawals.sumOf { it.first.amount }

        // 5. Account & supplier transactions paid from or received into drawer
        val accountTxList = db.accountDao().getDrawerTransactionsForShift(shift.id, shiftStartTime, shiftEndTime)
        val supplierPayments = accountTxList.filter { it.type == "PAYMENT" }.map { tx ->
            val accName = db.accountDao().getAccountById(tx.accountId)?.name ?: "حساب #${tx.accountId}"
            Pair(tx, accName)
        }
        val totalSupplierPayments = supplierPayments.sumOf { it.first.amount }

        val accountReceipts = accountTxList.filter { it.type == "RECEIPT" }.map { tx ->
            val accName = db.accountDao().getAccountById(tx.accountId)?.name ?: "حساب #${tx.accountId}"
            Pair(tx, accName)
        }
        val totalAccountReceipts = accountReceipts.sumOf { it.first.amount }

        // 6. Other general shift expenses
        val otherExpenses = if (shift.isClosed) shift.totalOtherExpenses else 0.0

        // 7. Totals
        val totalIncoming = shift.openingBalance + totalStoreCash + totalDriverSettlementsCash + totalAccountReceipts
        val totalOutgoing = totalStaffWithdrawals + totalSupplierPayments + otherExpenses
        val expected = totalIncoming - totalOutgoing
        val actual = if (shift.isClosed) shift.actualDrawerCash else expected
        val diff = if (shift.isClosed) shift.difference else (actual - expected)

        // 8. Chronological Timeline of all cash & financial movements
        val timeline = mutableListOf<ShiftMovementItem>()
        timeline.add(
            ShiftMovementItem(
                time = shift.openedAt,
                title = "رصيد بداية الدرج (العهدة الافتتاحية)",
                subtitle = "كاشير: ${shift.cashierName.ifBlank { "كاشير عام" }}",
                category = "OPENING",
                inAmount = shift.openingBalance,
                outAmount = 0.0,
                isDrawerMovement = true
            )
        )
        storeCashOrders.forEach { ord ->
            timeline.add(
                ShiftMovementItem(
                    time = ord.order.createdAt,
                    title = "مبيعات نقدية بالمحل ${ord.order.orderNumber}",
                    subtitle = ord.order.customerName.ifBlank { "عميل صالة / تيك أواي نقدي" },
                    category = "STORE_CASH_ORDER",
                    inAmount = ord.order.totalAmount,
                    outAmount = 0.0,
                    isDrawerMovement = true
                )
            )
        }
        settlements.forEach { set ->
            val amount = if (set.actualHandoverAmount > 0.0) set.actualHandoverAmount else set.netAmountToHandover
            timeline.add(
                ShiftMovementItem(
                    time = set.date,
                    title = "توريد تسوية دليفري - كابتن ${set.driverName}",
                    subtitle = "تسليم نقدية ${set.ordersCount} أوردرات للمحل ودخولها الدرج",
                    category = "DRIVER_SETTLEMENT",
                    inAmount = amount,
                    outAmount = 0.0,
                    isDrawerMovement = true
                )
            )
        }
        accountReceipts.forEach { (tx, accName) ->
            val isQuick = accName.contains("مصروفات") || accName.contains("نثريات") || accName.contains("درج")
            timeline.add(
                ShiftMovementItem(
                    time = tx.date,
                    title = if (isQuick) "وارد للدرج: ${tx.description.ifBlank { "توريد نقدية" }}" else "قبض نقدية من $accName",
                    subtitle = if (isQuick) "معاملة سريعة واردة بالدرج 🟢" else tx.description.ifBlank { "إيصال توريد نقدية للدرج" },
                    category = "ACCOUNT_RECEIPT",
                    inAmount = tx.amount,
                    outAmount = 0.0,
                    isDrawerMovement = true
                )
            )
        }
        staffWithdrawals.forEach { (tx, empName) ->
            val typeTitle = if (tx.type == "SALARY_PAYOUT") "صرف راتب نقدي" else "سلفة ومسحوبات نقدية"
            timeline.add(
                ShiftMovementItem(
                    time = tx.date,
                    title = "$typeTitle - $empName",
                    subtitle = tx.reason.ifBlank { "مسحوبات موظف من الدرج" },
                    category = "STAFF_ADVANCE",
                    inAmount = 0.0,
                    outAmount = tx.amount,
                    isDrawerMovement = true
                )
            )
        }
        supplierPayments.forEach { (tx, accName) ->
            val isQuick = accName.contains("مصروفات") || accName.contains("نثريات") || accName.contains("درج")
            timeline.add(
                ShiftMovementItem(
                    time = tx.date,
                    title = if (isQuick) "منصرف من الدرج: ${tx.description.ifBlank { "مصروف نثري" }}" else "سداد دفعة للمورد $accName",
                    subtitle = if (isQuick) "معاملة سريعة منصرفة من الدرج 🔴" else tx.description.ifBlank { "سداد فاتورة مشتريات/توريد من الدرج" },
                    category = "SUPPLIER_PAYMENT",
                    inAmount = 0.0,
                    outAmount = tx.amount,
                    isDrawerMovement = true
                )
            )
        }
        if (otherExpenses > 0.0) {
            timeline.add(
                ShiftMovementItem(
                    time = shift.closedAt ?: System.currentTimeMillis(),
                    title = "مصروفات ونثريات بالوردية",
                    subtitle = "منصرفات عامة مسددة من الدرج",
                    category = "EXPENSE",
                    inAmount = 0.0,
                    outAmount = otherExpenses,
                    isDrawerMovement = true
                )
            )
        }
        walletOrders.forEach { ord ->
            timeline.add(
                ShiftMovementItem(
                    time = ord.order.createdAt,
                    title = "أوردر محفظة إلكترونية ${ord.order.orderNumber} (فودافون كاش)",
                    subtitle = "تحويل إلكتروني - ليس بالدرج الورقي",
                    category = "WALLET_ORDER",
                    inAmount = ord.order.totalAmount,
                    outAmount = 0.0,
                    isDrawerMovement = false
                )
            )
        }

        timeline.sortBy { it.time }

        ShiftDrawerStatement(
            shift = shift,
            openingBalance = shift.openingBalance,
            storeCashOrders = storeCashOrders,
            totalStoreCash = totalStoreCash,
            driverSettlements = settlements,
            totalDriverSettlementsCash = totalDriverSettlementsCash,
            accountReceipts = accountReceipts,
            totalAccountReceipts = totalAccountReceipts,
            totalIncomingCash = totalIncoming,
            staffWithdrawals = staffWithdrawals,
            totalStaffWithdrawals = totalStaffWithdrawals,
            supplierPayments = supplierPayments,
            totalSupplierPayments = totalSupplierPayments,
            otherExpenses = otherExpenses,
            totalOutgoingCash = totalOutgoing,
            walletOrders = walletOrders,
            totalWalletSales = totalWallet,
            expectedDrawerCash = expected,
            actualDrawerCash = actual,
            difference = diff,
            movementsTimeline = timeline
        )
    }

    suspend fun closeActiveShift(actualDrawerCash: Double, otherExpenses: Double, notes: String): Shift? = withContext(Dispatchers.IO) {
        val active = db.settingsDao().getActiveShiftDirect() ?: return@withContext null
        val now = System.currentTimeMillis()

        val statement = getShiftDrawerStatement(active.id)
        val totalStoreCash = statement.totalStoreCash
        val totalDeliverySettlementCash = statement.totalDriverSettlementsCash
        val totalStaffWithdrawals = statement.totalStaffWithdrawals
        val totalSupplierPayments = statement.totalSupplierPayments
        val totalAccountReceipts = statement.totalAccountReceipts
        val totalWallet = statement.totalWalletSales

        // Total cash sales overall (store cash + delivery cash collected via settlements)
        val totalCashSales = totalStoreCash + totalDeliverySettlementCash

        val totalIncoming = active.openingBalance + totalStoreCash + totalDeliverySettlementCash + totalAccountReceipts
        val totalOutgoing = totalStaffWithdrawals + totalSupplierPayments + otherExpenses
        val expectedCash = totalIncoming - totalOutgoing
        val diff = actualDrawerCash - expectedCash

        val closed = active.copy(
            closedAt = now,
            totalCashSales = totalCashSales,
            totalStoreCashSales = totalStoreCash,
            totalDeliverySettlementCash = totalDeliverySettlementCash,
            totalStaffWithdrawals = totalStaffWithdrawals,
            totalSupplierPayments = totalSupplierPayments,
            totalOtherExpenses = otherExpenses,
            totalWalletSales = totalWallet,
            totalExpenses = totalOutgoing,
            expectedDrawerCash = expectedCash,
            actualDrawerCash = actualDrawerCash,
            difference = diff,
            ordersCount = statement.storeCashOrders.size + statement.walletOrders.size,
            isClosed = true,
            notes = notes
        )
        db.settingsDao().updateShift(closed)

        // Archive 100% of unarchived orders of this closed shift and any prior unarchived orders up to close time
        db.orderDao().archiveOrdersForShift(active.id, now)

        closed
    }

    suspend fun archiveOrdersUpTo(timestamp: Long) = withContext(Dispatchers.IO) {
        db.orderDao().archiveOrdersUpTo(timestamp)
    }

    // Ingredients & Recipe BOM
    suspend fun checkItemAndIngredientsStock(menuItemId: Long, requestedQty: Double): IngredientCheckResult = withContext(Dispatchers.IO) {
        val item = db.menuDao().getItemById(menuItemId)
            ?: return@withContext IngredientCheckResult(isAvailable = false, maxPortions = 0.0, shortages = emptyList())

        val shortages = mutableListOf<IngredientShortage>()
        var maxPortions = item.currentStock

        if (requestedQty > item.currentStock) {
            shortages.add(
                IngredientShortage(
                    ingredientId = item.id,
                    ingredientName = item.name,
                    required = requestedQty,
                    available = item.currentStock,
                    unit = item.unit
                )
            )
        }

        val ingredients = db.menuDao().getIngredientsListForItem(menuItemId)
        for (ing in ingredients) {
            val ingItem = db.menuDao().getItemById(ing.ingredientItemId)
            if (ingItem != null) {
                val totalNeeded = ing.requiredQuantityPerUnit * requestedQty
                val maxPossible = if (ing.requiredQuantityPerUnit > 0) ingItem.currentStock / ing.requiredQuantityPerUnit else Double.MAX_VALUE
                if (maxPossible < maxPortions) {
                    maxPortions = maxPossible.coerceAtLeast(0.0)
                }
                if (totalNeeded > ingItem.currentStock) {
                    shortages.add(
                        IngredientShortage(
                            ingredientId = ingItem.id,
                            ingredientName = ingItem.name,
                            required = totalNeeded,
                            available = ingItem.currentStock,
                            unit = ingItem.unit
                        )
                    )
                }
            }
        }

        IngredientCheckResult(
            isAvailable = shortages.isEmpty(),
            maxPortions = maxPortions.coerceAtLeast(0.0),
            shortages = shortages
        )
    }

    fun getIngredientsForItem(menuItemId: Long): Flow<List<MenuItemIngredient>> = db.menuDao().getIngredientsForItem(menuItemId)

    suspend fun addIngredientToItem(ingredient: MenuItemIngredient): Long = withContext(Dispatchers.IO) {
        db.menuDao().insertIngredient(ingredient)
    }

    suspend fun deleteIngredient(id: Long) = withContext(Dispatchers.IO) {
        db.menuDao().deleteIngredient(id)
    }

    // Order Placement & Inventory
    suspend fun placeOrder(
        order: Order,
        items: List<OrderItem>,
        managerPasswordOverride: String? = null,
        deductStock: Boolean = true
    ): OrderPlacementResult = withContext(Dispatchers.IO) {
        val isStrictStockLock = db.settingsDao().getSettingValue("strict_stock_lock") == "true"
        if (isStrictStockLock) {
            for (item in items) {
                if (item.menuItemId != null && !item.isCustomAdHoc) {
                    val check = checkItemAndIngredientsStock(item.menuItemId, item.quantity)
                    if (!check.isAvailable) {
                        val managerPass = db.settingsDao().getSettingValue("managerPassword") ?: "1234"
                        if (managerPasswordOverride == null || managerPasswordOverride != managerPass) {
                            val firstShortage = check.shortages.first()
                            return@withContext OrderPlacementResult.StockError(
                                itemName = firstShortage.ingredientName,
                                requested = firstShortage.required,
                                available = firstShortage.available,
                                shortage = firstShortage.required - firstShortage.available
                            )
                        }
                    }
                }
            }
        }

        var linkedCustomerId = order.customerId
        if (order.customerPhone.isNotBlank()) {
            val existingCust = db.customerDao().getCustomerByPhone(order.customerPhone.trim())
            if (existingCust != null) {
                linkedCustomerId = existingCust.id
                if (existingCust.secondaryPhone.isNullOrBlank() && !order.customerSecondaryPhone.isNullOrBlank()) {
                    db.customerDao().updateCustomer(existingCust.copy(secondaryPhone = order.customerSecondaryPhone.trim()))
                }
            } else if (order.customerName.isNotBlank()) {
                val newCust = Customer(
                    name = order.customerName.trim(),
                    primaryPhone = order.customerPhone.trim(),
                    secondaryPhone = order.customerSecondaryPhone?.trim()?.ifBlank { null },
                    notes = order.notes
                )
                val newId = db.customerDao().insertCustomer(newCust)
                linkedCustomerId = newId
                if (!order.deliveryAddressFull.isNullOrBlank()) {
                    db.customerDao().insertAddress(
                        CustomerAddress(
                            customerId = newId,
                            title = order.deliveryAddressTitle ?: "الرئيسي",
                            area = order.customerArea ?: "",
                            streetDetails = order.deliveryAddressFull,
                            deliveryFee = order.deliveryFee,
                            isDefault = true
                        )
                    )
                }
            }
        }

        var driverCommission = order.driverCommission
        if (order.driverId != null && driverCommission <= 0.0) {
            val driver = db.driverDao().getDriverById(order.driverId)
            if (driver != null) {
                driverCommission = if (driver.calculationType == "COMMISSION") driver.ratePerOrder else 0.0
            }
        }

        val currentShift = db.settingsDao().getActiveShiftDirect()
        val shiftId = if (order.shiftId != null && order.shiftId > 0L) order.shiftId else currentShift?.id

        val isPendingOffline = order.syncState == "PENDING_OFFLINE"
        val orderNum = if (order.orderNumber.isNotBlank()) {
            val cleanNum = order.orderNumber.replace("#", "").trim().toIntOrNull()
            if (cleanNum != null && !isPendingOffline) {
                val curSetting = db.settingsDao().getSettingValue("next_order_number")?.toIntOrNull() ?: 1
                if (cleanNum >= curSetting) {
                    db.settingsDao().setSetting(AppSetting("next_order_number", (cleanNum + 1).toString()))
                }
            }
            order.orderNumber
        } else {
            val nextNumSetting = db.settingsDao().getSettingValue("next_order_number")?.toIntOrNull()
            val effectiveNum = if (nextNumSetting != null && nextNumSetting > 0) nextNumSetting else 1
            db.settingsDao().setSetting(AppSetting("next_order_number", (effectiveNum + 1).toString()))
            "#$effectiveNum"
        }
        val finalCloudOrderId = when {
            isPendingOffline -> order.cloudOrderId?.takeIf { it.isNotBlank() }
            !order.cloudOrderId.isNullOrBlank() -> order.cloudOrderId
            else -> {
                val cleanNumStr = orderNum.replace("#", "").trim().ifBlank { "0" }
                "order_${if (order.createdAt > 0L) order.createdAt else System.currentTimeMillis()}_${cleanNumStr}_${java.util.UUID.randomUUID().toString().take(6)}"
            }
        }
        val finalOrder = order.copy(
            orderNumber = orderNum,
            customerId = linkedCustomerId,
            driverCommission = driverCommission,
            shiftId = shiftId,
            overridePasswordUsed = managerPasswordOverride != null,
            cloudOrderId = finalCloudOrderId
        )

        val orderId = db.orderDao().insertOrder(finalOrder)
        val preparedItems = items.map { it.copy(orderId = orderId) }
        db.orderDao().insertOrderItems(preparedItems)

        val alreadyDeducted = db.inventoryDao().hasMovementsForOrder(orderId) > 0
        if (!alreadyDeducted && deductStock) {
            val movements = mutableListOf<InventoryMovement>()
            for (item in preparedItems) {
                if (item.menuItemId != null && !item.isCustomAdHoc) {
                    val menuItem = db.menuDao().getItemById(item.menuItemId)
                    if (menuItem != null) {
                        val newStock = menuItem.currentStock - item.quantity
                        db.menuDao().updateItemStock(menuItem.id, newStock)
                        movements.add(
                            InventoryMovement(
                                menuItemId = menuItem.id,
                                itemName = menuItem.name,
                                movementType = "SALE",
                                quantityChange = -item.quantity,
                                balanceAfter = newStock,
                                orderId = orderId,
                                notes = "بيع أوردر $orderNum"
                            )
                        )
                    }

                    val ingredients = db.menuDao().getIngredientsListForItem(item.menuItemId)
                    for (ing in ingredients) {
                        val ingItem = db.menuDao().getItemById(ing.ingredientItemId)
                        if (ingItem != null) {
                            val qtyDeduct = ing.requiredQuantityPerUnit * item.quantity
                            val ingNewStock = ingItem.currentStock - qtyDeduct
                            db.menuDao().updateItemStock(ingItem.id, ingNewStock)
                            movements.add(
                                InventoryMovement(
                                    menuItemId = ingItem.id,
                                    itemName = "${ingItem.name} (مكون لـ ${item.itemName})",
                                    movementType = "SALE_INGREDIENT",
                                    quantityChange = -qtyDeduct,
                                    balanceAfter = ingNewStock,
                                    orderId = orderId,
                                    notes = "استهلاك خامات في أوردر $orderNum"
                                )
                            )
                        }
                    }
                }
            }
            if (movements.isNotEmpty()) {
                db.inventoryDao().insertMovements(movements)
            }
        }

        if (linkedCustomerId != null && linkedCustomerId > 0) {
            val existingC = db.customerDao().getCustomerById(linkedCustomerId)
            if (existingC != null) {
                db.customerDao().updateCustomer(
                    existingC.copy(
                        totalOrdersCount = existingC.totalOrdersCount + 1,
                        totalSpent = existingC.totalSpent + finalOrder.totalAmount
                    )
                )
            }
        }
        db.customerDao().syncAllCustomerOrderStats()

        OrderPlacementResult.Success(orderId, orderNum)
    }

    suspend fun upsertCloudOrderToLocal(
        cloudOrder: CloudOrderDoc,
        items: List<OrderItem>,
        allowCloudUnarchive: Boolean = cloudOrder.unarchiveTimestamp > 0L
    ): Long = withContext(Dispatchers.IO) {
        val byCloudId = db.orderDao().getOrderByCloudId(cloudOrder.id)
        val byNumber = if (byCloudId == null && cloudOrder.orderNumber > 0) {
            val nowMs = System.currentTimeMillis()
            db.orderDao().getOrderByOrderNumber("#${cloudOrder.orderNumber}")
                ?.takeIf {
                    (it.order.cloudOrderId.isNullOrBlank() || it.order.cloudOrderId == cloudOrder.id) &&
                        !it.order.isArchived &&
                        (nowMs - it.order.createdAt) < (12L * 60L * 60L * 1000L)
                }
        } else null
        val existing = byCloudId ?: byNumber

        val normalizedStatus = if (cloudOrder.status.isBlank() || cloudOrder.status == "PENDING") "NEW" else cloudOrder.status
        val isCloudPrinted = cloudOrder.printStatus == "PRINTED"

        val resolvedCloudTime = if (cloudOrder.createdAtMs > 0L) {
            cloudOrder.createdAtMs
        } else {
            cloudOrder.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        }

        if (existing != null) {
            val ord = existing.order
            if (ord.syncState == "PENDING_OFFLINE") {
                return@withContext ord.id
            }
            val needsCloudIdUpdate = ord.cloudOrderId.isNullOrBlank() && cloudOrder.id.isNotBlank()
            val needsOrderNumberFix = !ord.orderNumber.startsWith("#") && cloudOrder.orderNumber > 0
            val needsStatusUpdate = normalizedStatus.isNotBlank() && ord.status != normalizedStatus
            val targetArchived = when {
                !ord.isArchived && cloudOrder.isArchived -> true
                ord.isArchived && !cloudOrder.isArchived -> !(cloudOrder.unarchiveTimestamp > 0L || allowCloudUnarchive)
                else -> ord.isArchived
            }
            val needsArchiveUpdate = ord.isArchived != targetArchived
            val needsSellerUpdate = ord.sellerName.isBlank() && cloudOrder.createdByTerminal.isNotBlank()
            val needsPrintFlagUpdate = isCloudPrinted && (!ord.printedMain || !ord.printedKitchen1)

            val needsCreatedAtFix = cloudOrder.createdAtMs > 0L && kotlin.math.abs(ord.createdAt - cloudOrder.createdAtMs) > 1_000L
            val cleanSecPhone = cloudOrder.customerSecondaryPhone.trim()
            val cleanArea = cloudOrder.customerArea.trim()
            val cleanAddrTitle = cloudOrder.deliveryAddressTitle.trim()
            val cleanAddressFull = cloudOrder.deliveryAddressFull.trim()
            val cleanDriverName = cloudOrder.driverName.trim()
            val fullDetails = cloudOrder.hasFullDetails

            val needsCustName = if (fullDetails) {
                cloudOrder.customerName.isNotBlank() && ord.customerName != cloudOrder.customerName
            } else {
                cloudOrder.customerName.isNotBlank() && ord.customerName != cloudOrder.customerName
            }
            val needsCustPhone = if (fullDetails) {
                ord.customerPhone != cloudOrder.customerPhone
            } else {
                cloudOrder.customerPhone.isNotBlank() && ord.customerPhone != cloudOrder.customerPhone
            }
            val needsSecPhone = if (fullDetails) {
                (ord.customerSecondaryPhone ?: "") != cleanSecPhone
            } else {
                cleanSecPhone.isNotBlank() && (ord.customerSecondaryPhone ?: "") != cleanSecPhone
            }
            val needsArea = if (fullDetails) {
                (ord.customerArea ?: "") != cleanArea
            } else {
                cleanArea.isNotBlank() && (ord.customerArea ?: "") != cleanArea
            }
            val needsAddrTitle = if (fullDetails) {
                (ord.deliveryAddressTitle ?: "") != cleanAddrTitle
            } else {
                cleanAddrTitle.isNotBlank() && (ord.deliveryAddressTitle ?: "") != cleanAddrTitle
            }
            val needsAddrFull = if (fullDetails) {
                (ord.deliveryAddressFull ?: "") != cleanAddressFull
            } else {
                cleanAddressFull.isNotBlank() && (ord.deliveryAddressFull ?: "") != cleanAddressFull
            }
            val needsDriverName = if (fullDetails) {
                (ord.driverName ?: "") != cleanDriverName
            } else {
                cleanDriverName.isNotBlank() && (ord.driverName ?: "") != cleanDriverName
            }
            val needsOrderType = cloudOrder.orderType.isNotBlank() && ord.orderType != cloudOrder.orderType
            val needsSubtotal = if (fullDetails || (items.isNotEmpty() && cloudOrder.subtotal > 0.0)) {
                cloudOrder.subtotal >= 0.0 && kotlin.math.abs(cloudOrder.subtotal - ord.subtotal) > 0.001
            } else false
            val needsDiscount = if (fullDetails) {
                cloudOrder.discount >= 0.0 && kotlin.math.abs(cloudOrder.discount - ord.discount) > 0.001
            } else false
            val needsDeliveryFee = cloudOrder.deliveryFee >= 0.0 && kotlin.math.abs(cloudOrder.deliveryFee - ord.deliveryFee) > 0.001
            val needsTotal = cloudOrder.total >= 0.0 && kotlin.math.abs(cloudOrder.total - ord.totalAmount) > 0.001
            val needsNotes = if (fullDetails) {
                ord.notes != cloudOrder.notes
            } else {
                cloudOrder.notes.isNotBlank() && ord.notes != cloudOrder.notes
            }

            if (needsCloudIdUpdate || needsOrderNumberFix || needsStatusUpdate || needsArchiveUpdate || needsSellerUpdate || needsPrintFlagUpdate ||
                needsCreatedAtFix || needsCustName || needsCustPhone || needsSecPhone || needsArea ||
                needsAddrTitle || needsAddrFull || needsDriverName || needsOrderType || needsSubtotal ||
                needsDiscount || needsDeliveryFee || needsTotal || needsNotes
            ) {
                val updatedDriverNameVal = if (needsDriverName) cleanDriverName.ifBlank { null } else ord.driverName
                val updatedDriverIdVal = when {
                    !needsDriverName -> ord.driverId
                    updatedDriverNameVal.isNullOrBlank() -> null
                    else -> ord.driverId
                }
                db.orderDao().updateOrder(
                    ord.copy(
                        orderNumber = if (needsOrderNumberFix) "#${cloudOrder.orderNumber}" else ord.orderNumber,
                        cloudOrderId = if (needsCloudIdUpdate) cloudOrder.id else ord.cloudOrderId,
                        status = if (needsStatusUpdate) normalizedStatus else ord.status,
                        isArchived = if (needsArchiveUpdate) targetArchived else ord.isArchived,
                        sellerName = if (needsSellerUpdate) cloudOrder.createdByTerminal else ord.sellerName,
                        printedMain = ord.printedMain || isCloudPrinted,
                        printedKitchen1 = ord.printedKitchen1 || isCloudPrinted,
                        printedKitchen2 = ord.printedKitchen2 || isCloudPrinted,
                        createdAt = if (needsCreatedAtFix) cloudOrder.createdAtMs else ord.createdAt,
                        customerName = if (needsCustName) cloudOrder.customerName else ord.customerName,
                        customerPhone = if (needsCustPhone) cloudOrder.customerPhone else ord.customerPhone,
                        customerSecondaryPhone = if (needsSecPhone) cleanSecPhone.ifBlank { null } else ord.customerSecondaryPhone,
                        customerArea = if (needsArea) cleanArea.ifBlank { null } else ord.customerArea,
                        deliveryAddressTitle = if (needsAddrTitle) cleanAddrTitle.ifBlank { null } else ord.deliveryAddressTitle,
                        deliveryAddressFull = if (needsAddrFull) cleanAddressFull.ifBlank { null } else ord.deliveryAddressFull,
                        driverId = updatedDriverIdVal,
                        driverName = updatedDriverNameVal,
                        orderType = if (needsOrderType) cloudOrder.orderType else ord.orderType,
                        subtotal = if (needsSubtotal) cloudOrder.subtotal else ord.subtotal,
                        discount = if (needsDiscount) cloudOrder.discount else ord.discount,
                        deliveryFee = if (needsDeliveryFee) cloudOrder.deliveryFee else ord.deliveryFee,
                        totalAmount = if (needsTotal) cloudOrder.total else ord.totalAmount,
                        notes = if (needsNotes) cloudOrder.notes else ord.notes
                    )
                )
            }

            // Smart Merge incoming cloud items with local Cashier items:
            // Preserves local printedKitchenQuantity for chef delta printing & records cancelled/reduced alerts.
            if (items.isNotEmpty()) {
                mergeCloudOrderItemsIntoLocal(ord.id, existing.items, items)
            }
            return@withContext ord.id
        }

        val orderTime = resolvedCloudTime
        val cleanOrderNumber = if (cloudOrder.orderNumber > 0) "#${cloudOrder.orderNumber}" else "#1"
        val matchedShift = if (cloudOrder.shiftNumber > 0) {
            db.settingsDao().getShiftByNumber(cloudOrder.shiftNumber) ?: db.settingsDao().getActiveShiftDirect()
        } else {
            db.settingsDao().getActiveShiftDirect()
        }
        val resolvedShiftId = matchedShift?.id
        val isFromClosedShift = matchedShift != null && matchedShift.isClosed &&
            (matchedShift.closedAt == null || orderTime <= matchedShift.closedAt)
        val initialIsArchived = cloudOrder.isArchived || (isFromClosedShift && cloudOrder.unarchiveTimestamp == 0L && !allowCloudUnarchive)

        val newOrder = Order(
            orderNumber = cleanOrderNumber,
            orderType = cloudOrder.orderType,
            customerName = cloudOrder.customerName.ifBlank { "زبون صالة" },
            customerPhone = cloudOrder.customerPhone,
            customerSecondaryPhone = cloudOrder.customerSecondaryPhone.trim().takeIf { it.isNotBlank() },
            customerArea = cloudOrder.customerArea.trim().takeIf { it.isNotBlank() },
            deliveryAddressTitle = cloudOrder.deliveryAddressTitle.trim().takeIf { it.isNotBlank() },
            deliveryAddressFull = cloudOrder.deliveryAddressFull.trim().takeIf { it.isNotBlank() },
            driverName = cloudOrder.driverName.trim().takeIf { it.isNotBlank() },
            deliveryFee = cloudOrder.deliveryFee,
            subtotal = cloudOrder.subtotal,
            discount = cloudOrder.discount,
            totalAmount = cloudOrder.total,
            paymentMethod = cloudOrder.paymentMethod.ifBlank { "CASH" },
            notes = cloudOrder.notes,
            status = normalizedStatus,
            isArchived = initialIsArchived,
            shiftId = resolvedShiftId,
            sellerName = cloudOrder.createdByTerminal,
            deviceRole = "SELLER",
            cloudOrderId = cloudOrder.id,
            createdAt = orderTime,
            printedMain = isCloudPrinted,
            printedKitchen1 = isCloudPrinted,
            printedKitchen2 = isCloudPrinted
        )

        val insertedId = db.orderDao().insertOrder(newOrder)
        val preparedItems = items.map { it.copy(id = 0, orderId = insertedId) }
        db.orderDao().insertOrderItems(preparedItems)

        val curNext = db.settingsDao().getSettingValue("next_order_number")?.toIntOrNull() ?: 1
        if (cloudOrder.orderNumber >= curNext) {
            db.settingsDao().setSetting(AppSetting("next_order_number", (cloudOrder.orderNumber + 1).toString()))
        }

        insertedId
    }

    suspend fun cancelOrderAndGetAffectedMenuIds(orderId: Long, reason: String = ""): Set<Long> = withContext(Dispatchers.IO) {
        val existing = db.orderDao().getOrderById(orderId) ?: return@withContext emptySet()
        if (existing.order.status == "CANCELLED") return@withContext emptySet()

        db.orderDao().updateOrderStatus(orderId, "CANCELLED")
        val affectedIds = mutableSetOf<Long>()
        val movements = db.inventoryDao().getMovementsForOrder(orderId)
        if (movements.isNotEmpty()) {
            for (m in movements) {
                val itm = db.menuDao().getItemById(m.menuItemId)
                if (itm != null) {
                    val restoredStock = itm.currentStock - m.quantityChange
                    db.menuDao().updateItemStock(itm.id, restoredStock)
                    affectedIds.add(itm.id)
                }
            }
            db.inventoryDao().deleteMovementsForOrder(orderId)
        } else {
            // Remote order without local InventoryMovements: restore quantities cleanly once
            for (item in existing.items) {
                if (item.menuItemId != null && !item.isCustomAdHoc) {
                    val menuItem = db.menuDao().getItemById(item.menuItemId)
                    if (menuItem != null) {
                        db.menuDao().updateItemStock(menuItem.id, menuItem.currentStock + item.quantity)
                        affectedIds.add(menuItem.id)
                    }
                    val ingredients = db.menuDao().getIngredientsListForItem(item.menuItemId)
                    for (ing in ingredients) {
                        val ingItem = db.menuDao().getItemById(ing.ingredientItemId)
                        if (ingItem != null) {
                            val qtyRestore = ing.requiredQuantityPerUnit * item.quantity
                            db.menuDao().updateItemStock(ingItem.id, ingItem.currentStock + qtyRestore)
                            affectedIds.add(ingItem.id)
                        }
                    }
                }
            }
        }
        db.customerDao().syncAllCustomerOrderStats()
        affectedIds
    }

    suspend fun cancelOrder(orderId: Long, reason: String = "") {
        cancelOrderAndGetAffectedMenuIds(orderId, reason)
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: String) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderStatus(orderId, newStatus)
        db.customerDao().syncAllCustomerOrderStats()
    }

    suspend fun syncCustomerStats() = withContext(Dispatchers.IO) {
        db.customerDao().syncAllCustomerOrderStats()
    }

    suspend fun updateOrderDriver(orderId: Long, driverId: Long?, driverName: String?) = withContext(Dispatchers.IO) {
        val existing = db.orderDao().getOrderById(orderId)
        if (existing != null) {
            db.orderDao().updateOrder(existing.order.copy(driverId = driverId, driverName = driverName))
        }
    }

    suspend fun updateOrderItemAndRecalculate(orderId: Long, item: OrderItem) = withContext(Dispatchers.IO) {
        val existingItem = db.orderDao().getOrderById(orderId)?.items?.firstOrNull { it.id == item.id }
        val itemToSave = if (existingItem != null && existingItem.printedKitchenQuantity > 0.0001) {
            if (item.quantity < existingItem.printedKitchenQuantity - 0.0001) {
                appendPendingKitchenCancellations(
                    orderId,
                    listOf(
                        CancelledKitchenItem(
                            itemName = existingItem.itemName,
                            kitchenName = existingItem.kitchenName,
                            targetPrinter = existingItem.targetPrinter,
                            preparationName = existingItem.preparationName,
                            unit = existingItem.unit,
                            cancelledQty = existingItem.printedKitchenQuantity - item.quantity,
                            prevQty = existingItem.printedKitchenQuantity,
                            newQty = item.quantity
                        )
                    )
                )
                item.copy(printedKitchenQuantity = item.quantity, printedInKitchen = true)
            } else {
                val printed = item.quantity <= existingItem.printedKitchenQuantity + 0.0001
                item.copy(printedKitchenQuantity = existingItem.printedKitchenQuantity, printedInKitchen = printed)
            }
        } else {
            item
        }
        db.orderDao().updateOrderItem(itemToSave)
        recalculateOrderTotals(orderId)
    }

    suspend fun addAdHocItemToExistingOrder(orderId: Long, item: OrderItem) = withContext(Dispatchers.IO) {
        db.orderDao().insertOrderItem(item.copy(orderId = orderId))
        recalculateOrderTotals(orderId)
    }

    suspend fun addOrderItemToExistingOrder(orderId: Long, item: OrderItem) = withContext(Dispatchers.IO) {
        db.orderDao().insertOrderItem(item.copy(orderId = orderId))
        recalculateOrderTotals(orderId)
    }

    fun getOrderByIdFlow(orderId: Long): Flow<OrderWithItems?> {
        return db.orderDao().getOrderByIdFlow(orderId)
    }

    suspend fun getNextOrderNumber(): Int = withContext(Dispatchers.IO) {
        db.settingsDao().getSettingValue("next_order_number")?.toIntOrNull() ?: 1
    }

    suspend fun resetOrderCounter(startingNumber: Int = 1) = withContext(Dispatchers.IO) {
        val safeNum = if (startingNumber <= 0) 1 else startingNumber
        db.settingsDao().setSetting(AppSetting("next_order_number", safeNum.toString()))
    }

    suspend fun deleteOrderItemFromOrder(orderId: Long, itemId: Long) = withContext(Dispatchers.IO) {
        val existingItem = db.orderDao().getOrderById(orderId)?.items?.firstOrNull { it.id == itemId }
        if (existingItem != null && existingItem.printedKitchenQuantity > 0.0001) {
            appendPendingKitchenCancellations(
                orderId,
                listOf(
                    CancelledKitchenItem(
                        itemName = existingItem.itemName,
                        kitchenName = existingItem.kitchenName,
                        targetPrinter = existingItem.targetPrinter,
                        preparationName = existingItem.preparationName,
                        unit = existingItem.unit,
                        cancelledQty = existingItem.printedKitchenQuantity,
                        prevQty = existingItem.printedKitchenQuantity,
                        newQty = 0.0
                    )
                )
            )
        }
        db.orderDao().deleteOrderItem(itemId)
        recalculateOrderTotals(orderId)
    }

    private suspend fun recalculateOrderTotals(orderId: Long) {
        val oWithItems = db.orderDao().getOrderById(orderId) ?: return
        val newSubtotal = oWithItems.items.sumOf { it.totalPrice }
        val newTotal = (newSubtotal + oWithItems.order.deliveryFee - oWithItems.order.discount).coerceAtLeast(0.0)
        db.orderDao().updateOrder(oWithItems.order.copy(subtotal = newSubtotal, totalAmount = newTotal))
    }

    suspend fun updateOrderFromPos(
        orderId: Long,
        updatedOrder: Order,
        newItems: List<OrderItem>
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = db.orderDao().getOrderById(orderId) ?: return@withContext false
        val oldItems = existing.items
        val pendingCancellations = mutableListOf<CancelledKitchenItem>()

        // 1. Calculate stock adjustments & kitchen cancellations for removed items
        val newItemIds = newItems.mapNotNull { if (it.id > 0) it.id else null }.toSet()
        val deletedItems = oldItems.filter { it.id !in newItemIds }
        for (del in deletedItems) {
            if (del.menuItemId != null && del.menuItemId > 0) {
                val menuItem = db.menuDao().getItemById(del.menuItemId)
                if (menuItem != null) {
                    val restoredStock = menuItem.currentStock + del.quantity
                    db.menuDao().updateItemStock(menuItem.id, restoredStock)
                }
            }
            if (del.printedKitchenQuantity > 0.0001) {
                pendingCancellations.add(
                    CancelledKitchenItem(
                        itemName = del.itemName,
                        kitchenName = del.kitchenName,
                        targetPrinter = del.targetPrinter,
                        preparationName = del.preparationName,
                        unit = del.unit,
                        cancelledQty = del.printedKitchenQuantity,
                        prevQty = del.printedKitchenQuantity,
                        newQty = 0.0
                    )
                )
            }
            db.orderDao().deleteOrderItem(del.id)
        }

        // 2. Updated or inserted items
        for (item in newItems) {
            if (item.id > 0) {
                // Existing item: check quantity change for stock adjustment & kitchen cancellation/clamping
                val oldItem = oldItems.firstOrNull { it.id == item.id }
                if (oldItem != null) {
                    val qtyDiff = item.quantity - oldItem.quantity
                    if (qtyDiff != 0.0 && item.menuItemId != null && item.menuItemId > 0) {
                        val menuItem = db.menuDao().getItemById(item.menuItemId)
                        if (menuItem != null) {
                            val newStock = menuItem.currentStock - qtyDiff
                            db.menuDao().updateItemStock(menuItem.id, newStock)
                        }
                    }
                    val effectivePrintedQty = if (oldItem.printedKitchenQuantity > 0.0001 && item.quantity < oldItem.printedKitchenQuantity - 0.0001) {
                        pendingCancellations.add(
                            CancelledKitchenItem(
                                itemName = oldItem.itemName,
                                kitchenName = oldItem.kitchenName,
                                targetPrinter = oldItem.targetPrinter,
                                preparationName = oldItem.preparationName,
                                unit = oldItem.unit,
                                cancelledQty = oldItem.printedKitchenQuantity - item.quantity,
                                prevQty = oldItem.printedKitchenQuantity,
                                newQty = item.quantity
                            )
                        )
                        item.quantity
                    } else {
                        oldItem.printedKitchenQuantity
                    }
                    val isFullyPrinted = effectivePrintedQty > 0.0001 && item.quantity <= effectivePrintedQty + 0.0001
                    db.orderDao().updateOrderItem(
                        item.copy(
                            orderId = orderId,
                            printedKitchenQuantity = effectivePrintedQty,
                            printedInKitchen = isFullyPrinted
                        )
                    )
                } else {
                    db.orderDao().updateOrderItem(item.copy(orderId = orderId))
                }
            } else {
                // Brand new item added to the order during edit
                if (item.menuItemId != null && item.menuItemId > 0) {
                    val menuItem = db.menuDao().getItemById(item.menuItemId)
                    if (menuItem != null) {
                        val newStock = menuItem.currentStock - item.quantity
                        db.menuDao().updateItemStock(menuItem.id, newStock)
                    }
                }
                db.orderDao().insertOrderItem(
                    item.copy(
                        orderId = orderId,
                        printedInKitchen = false,
                        printedKitchenQuantity = 0.0
                    )
                )
            }
        }
        if (pendingCancellations.isNotEmpty()) {
            appendPendingKitchenCancellations(orderId, pendingCancellations)
        }

        // 3. Update Order header totals
        val subtotal = newItems.sumOf { it.totalPrice }
        val totalAmount = (subtotal + updatedOrder.deliveryFee - updatedOrder.discount).coerceAtLeast(0.0)

        // Persist payment details (paidAmount, changeAmount, isPaid)
        val effectivePaid = updatedOrder.paidAmount
        val effectiveChange = if (effectivePaid > totalAmount) (effectivePaid - totalAmount) else updatedOrder.changeAmount
        val effectiveIsPaid = updatedOrder.isPaid || (effectivePaid >= totalAmount && totalAmount > 0)

        db.orderDao().updateOrder(
            existing.order.copy(
                orderType = updatedOrder.orderType,
                customerId = updatedOrder.customerId,
                customerName = updatedOrder.customerName,
                customerPhone = updatedOrder.customerPhone,
                customerSecondaryPhone = updatedOrder.customerSecondaryPhone,
                customerArea = updatedOrder.customerArea,
                deliveryAddressTitle = updatedOrder.deliveryAddressTitle,
                deliveryAddressFull = updatedOrder.deliveryAddressFull,
                deliveryFee = updatedOrder.deliveryFee,
                driverId = updatedOrder.driverId,
                driverName = updatedOrder.driverName,
                driverCommission = updatedOrder.driverCommission,
                subtotal = subtotal,
                discount = updatedOrder.discount,
                totalAmount = totalAmount,
                paymentMethod = updatedOrder.paymentMethod,
                notes = updatedOrder.notes,
                paidAmount = effectivePaid,
                changeAmount = effectiveChange,
                isPaid = effectiveIsPaid
            )
        )

        db.customerDao().syncAllCustomerOrderStats()
        true
    }

    suspend fun updateOrderCustomerDetails(
        orderId: Long,
        orderType: String,
        driverId: Long?,
        driverName: String?,
        name: String,
        phone: String,
        secondaryPhone: String? = null,
        area: String?,
        address: String?,
        fee: Double,
        driverCommission: Double,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val order = db.orderDao().getOrderById(orderId)?.order ?: return@withContext
        val effectiveFee = if (orderType == "DELIVERY") fee else 0.0
        val effectiveCommission = if (orderType == "DELIVERY") driverCommission else 0.0
        val effectiveDriverId = if (orderType == "DELIVERY") driverId else null
        val effectiveDriverName = if (orderType == "DELIVERY") driverName else null
        val newTotal = (order.subtotal + effectiveFee - order.discount).coerceAtLeast(0.0)
        db.orderDao().updateOrderCustomerAndDetails(
            orderId = orderId,
            orderType = orderType,
            driverId = effectiveDriverId,
            driverName = effectiveDriverName,
            name = name,
            phone = phone,
            secondaryPhone = secondaryPhone,
            area = area,
            address = address,
            fee = effectiveFee,
            total = newTotal,
            driverCommission = effectiveCommission,
            notes = notes
        )
    }

    suspend fun updateOrderPayment(orderId: Long, paidAmount: Double, changeAmount: Double, isPaid: Boolean) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderPayment(orderId, paidAmount, changeAmount, isPaid)
    }

    // Driver Settlements
    suspend fun excludeOrderFromDriver(orderId: Long, isExcluded: Boolean, reason: String?) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderDriverExclusion(orderId, isExcluded, reason)
    }

    suspend fun assignDriverToOrder(orderId: Long, driverId: Long, driverName: String, commission: Double, reason: String? = null) = withContext(Dispatchers.IO) {
        db.orderDao().assignDriverToOrder(orderId, driverId, driverName, commission, reason)
    }

    suspend fun getAllUnsettledDeliveryOrders(): List<OrderWithItems> = withContext(Dispatchers.IO) {
        db.orderDao().getAllUnsettledDeliveryOrders()
    }

    suspend fun getTodayOrdersForDriver(driverId: Long): List<OrderWithItems> = withContext(Dispatchers.IO) {
        val startOfDay = getStartOfDayTimestamp()
        db.orderDao().getTodayOrdersForDriver(driverId, startOfDay)
    }

    suspend fun getTodayUnsettledOrdersForDriver(driverId: Long): List<OrderWithItems> = withContext(Dispatchers.IO) {
        val startOfDay = getStartOfDayTimestamp()
        db.orderDao().getUnsettledOrdersForDriverSince(driverId, startOfDay)
    }

    suspend fun calculateAndSaveDriverSettlement(
        driverId: Long,
        fuelExpenses: Double,
        actualHandoverAmount: Double = 0.0,
        customOrderCommissions: Map<Long, Double> = emptyMap(),
        notes: String = ""
    ): DriverSettlement? = withContext(Dispatchers.IO) {
        val driver = db.driverDao().getDriverById(driverId) ?: return@withContext null
        val startOfDay = getStartOfDayTimestamp()
        val allOrders = db.orderDao().getOrdersForDriverSince(driverId, startOfDay)
            .filter { it.order.status != "CANCELLED" && !it.order.isSettledWithDriver }

        val activeOrders = allOrders.filter { !it.order.isExcludedFromDriverSettlement }
        val ordersCount = activeOrders.size
        var totalCollected = 0.0
        for (o in activeOrders) {
            if (o.order.paymentMethod == "CASH") totalCollected += o.order.totalAmount
        }

        val driverEarnings = if (driver.calculationType == "COMMISSION") {
            activeOrders.sumOf { o ->
                customOrderCommissions[o.order.id] ?: if (o.order.driverCommission > 0) o.order.driverCommission else if (o.order.deliveryFee > 0) o.order.deliveryFee else driver.ratePerOrder
            }
        } else {
            driver.fixedDailySalary
        }

        val netHandover = (totalCollected - driverEarnings - fuelExpenses).coerceAtLeast(0.0)
        val diff = actualHandoverAmount - netHandover
        val settledIds = activeOrders.joinToString(",") { it.order.id.toString() }

        val activeShift = db.settingsDao().getActiveShiftDirect()
        val effectiveHandover = if (actualHandoverAmount > 0.0) actualHandoverAmount else netHandover

        val settlement = DriverSettlement(
            driverId = driverId,
            driverName = driver.name,
            date = System.currentTimeMillis(),
            ordersCount = ordersCount,
            totalCashCollected = totalCollected,
            driverEarnings = driverEarnings,
            fuelAndExpenses = fuelExpenses,
            netAmountToHandover = netHandover,
            actualHandoverAmount = effectiveHandover,
            financialDifference = if (actualHandoverAmount > 0.0) diff else 0.0,
            settledOrderIds = settledIds,
            isSettled = true,
            notes = notes,
            shiftId = activeShift?.id
        )

        db.driverDao().insertSettlement(settlement)
        db.orderDao().markOrdersSettledWithDriver(activeOrders.map { it.order.id })
        settlement
    }

    suspend fun generateDriverStatement(driverId: Long, startDate: Long, endDate: Long): DriverAccountStatement? = withContext(Dispatchers.IO) {
        val driver = db.driverDao().getDriverById(driverId) ?: return@withContext null
        val orders = db.orderDao().getOrdersForDriverBetween(driverId, startDate, endDate)
            .filter { it.order.status != "CANCELLED" && !it.order.isExcludedFromDriverSettlement }
        val settlements = db.driverDao().getSettlementsForDriverBetween(driverId, startDate, endDate)

        var totalCollected = 0.0
        var totalFees = 0.0
        for (o in orders) {
            totalFees += o.order.deliveryFee
            if (o.order.paymentMethod == "CASH") totalCollected += o.order.totalAmount
        }

        val earnings = if (driver.calculationType == "COMMISSION") {
            orders.sumOf { if (it.order.driverCommission > 0) it.order.driverCommission else if (it.order.deliveryFee > 0) it.order.deliveryFee else driver.ratePerOrder }
        } else {
            val days = ((endDate - startDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
            days * driver.fixedDailySalary
        }

        val fuel = settlements.sumOf { it.fuelAndExpenses }
        val net = (totalCollected - earnings - fuel).coerceAtLeast(0.0)
        val actualHandover = settlements.sumOf { it.actualHandoverAmount }
        val diff = actualHandover - net

        DriverAccountStatement(
            driver = driver,
            startDate = startDate,
            endDate = endDate,
            orders = orders,
            settlements = settlements,
            totalDeliveredOrders = orders.size,
            totalCashCollected = totalCollected,
            totalDeliveryFees = totalFees,
            totalCommissionOrEarnings = earnings,
            totalFuelAndExpenses = fuel,
            netHandoverToRestaurant = net,
            actualHandoverAmount = actualHandover,
            financialDifference = diff
        )
    }

    // Staff & Payroll
    suspend fun calculateEmployeeStatement(employeeId: Long, startDate: Long, endDate: Long): EmployeeStatement? = withContext(Dispatchers.IO) {
        val employee = db.staffDao().getEmployeeById(employeeId) ?: return@withContext null
        val attendance = db.staffDao().getAttendanceListForEmployeeBetween(employeeId, startDate, endDate)
        val transactions = db.staffDao().getTransactionsListForEmployeeBetween(employeeId, startDate, endDate)

        val presentDays = attendance.count { it.status == "PRESENT" }
        val lateDays = attendance.count { it.status == "LATE" }
        val absentDays = attendance.count { it.status == "ABSENT" }
        val workedDays = presentDays + lateDays
        val totalHours = attendance.filter { it.status == "PRESENT" || it.status == "LATE" }.sumOf { it.hoursWorked }
        val totalOvertime = attendance.sumOf { it.overtimeHours }
        val totalDelayMinutes = attendance.sumOf { it.delayMinutes }

        val dailyHours = if (employee.dailyWorkHours > 0) employee.dailyWorkHours else 12.0
        val dailyRate = if (employee.salaryType == "DAILY") employee.basicSalary else (employee.basicSalary / 30.0)
        val hourlyRate = dailyRate / dailyHours

        val overtimeRate = if (employee.overtimeHourlyRate > 0) employee.overtimeHourlyRate else hourlyRate * 1.5
        val overtimePay = totalOvertime * overtimeRate

        val delayRate = if (employee.delayHourlyDeductionRate > 0) employee.delayHourlyDeductionRate else hourlyRate
        // Delay deduction only applies if delay was not pardoned and was not already recorded as a penalty transaction
        val unpenalizedDelayMinutes = attendance
            .filter { it.lateActionTaken != "PARDON" && it.lateActionTaken != "PENALTY" }
            .sumOf { it.delayMinutes }
        val delayDeductions = (unpenalizedDelayMinutes / 60.0) * delayRate
        val absenceDeductions = absentDays * dailyRate

        // Prorated strictly by actual attendance recorded in the period:
        // If worker attended days, earned salary is for those days (hours worked / official daily hours * dailyRate).
        // If no attendance records exist yet in the selected period, fallback to basic salary for monthly employees so balances are not artificially negative
        val totalEarnedSalary = if (attendance.isNotEmpty()) {
            attendance.filter { it.status == "PRESENT" || it.status == "LATE" }.sumOf { att ->
                val h = if (att.hoursWorked > 0) att.hoursWorked else dailyHours
                (h / dailyHours) * dailyRate
            }
        } else if (employee.salaryType == "MONTHLY") {
            employee.basicSalary
        } else {
            0.0
        }
        val totalAdvances = transactions.filter { it.type == "ADVANCE" }.sumOf { it.amount }
        val totalDeductions = transactions.filter { it.type == "DEDUCTION" }.sumOf { it.amount }
        val totalBonuses = transactions.filter { it.type == "BONUS" }.sumOf { it.amount }
        val totalPayouts = transactions.filter { it.type == "SALARY_PAYOUT" }.sumOf { it.amount }

        val net = (totalEarnedSalary + totalBonuses + overtimePay) -
                  (totalAdvances + totalDeductions + delayDeductions + totalPayouts)

        EmployeeStatement(
            employee = employee,
            startDate = startDate,
            endDate = endDate,
            attendanceRecords = attendance,
            transactions = transactions,
            presentDaysCount = presentDays,
            absentDaysCount = absentDays,
            lateDaysCount = lateDays,
            totalHoursWorked = totalHours,
            totalOvertimeHours = totalOvertime,
            totalDelayMinutes = totalDelayMinutes,
            baseAgreedSalary = employee.basicSalary,
            totalEarnedSalary = totalEarnedSalary,
            overtimePay = overtimePay,
            delayDeductions = delayDeductions,
            absenceDeductions = absenceDeductions,
            totalAdvances = totalAdvances,
            totalDeductions = totalDeductions,
            totalBonuses = totalBonuses,
            netEntitlement = net
        )
    }

    suspend fun generateEmployeeStatement(employeeId: Long, startDate: Long, endDate: Long): EmployeeStatement? =
        calculateEmployeeStatement(employeeId, startDate, endDate)

    suspend fun settleEmployeeSalary(employeeId: Long, amount: Double, reason: String, date: Long = System.currentTimeMillis(), isDeductedFromDrawer: Boolean = true): Long = withContext(Dispatchers.IO) {
        val currentShift = db.settingsDao().getActiveShiftDirect()
        db.staffDao().insertTransaction(
            EmployeeTransaction(
                employeeId = employeeId,
                type = "SALARY_PAYOUT",
                amount = amount,
                reason = reason,
                date = date,
                isDeductedFromDrawer = isDeductedFromDrawer,
                shiftId = currentShift?.id
            )
        )
    }

    // Advanced Sales Reports
    suspend fun getAdvancedSalesReport(startDate: Long, endDate: Long): AdvancedSalesReport = withContext(Dispatchers.IO) {
        val orders = db.orderDao().getOrdersListBetween(startDate, endDate).filter { it.order.status != "CANCELLED" }
        val itemQtyMap = mutableMapOf<Long, Double>()
        val itemRevenueMap = mutableMapOf<Long, Double>()

        var totalRevenue = 0.0
        var totalDeliveryFees = 0.0
        var totalDiscounts = 0.0

        for (o in orders) {
            totalRevenue += o.order.totalAmount
            totalDeliveryFees += o.order.deliveryFee
            totalDiscounts += o.order.discount

            for (itm in o.items) {
                val mId = itm.menuItemId ?: 0L
                val currentQty = itemQtyMap.getOrDefault(mId, 0.0)
                itemQtyMap[mId] = currentQty + itm.quantity

                val currentRev = itemRevenueMap.getOrDefault(mId, 0.0)
                itemRevenueMap[mId] = currentRev + itm.totalPrice
            }
        }

        val itemsInDb = db.menuDao().getAllMenuItemsList()

        var totalCost = 0.0
        val statsList = mutableListOf<ItemSalesStat>()

        for (m in itemsInDb) {
            val soldQty = itemQtyMap.getOrDefault(m.id, 0.0)
            val rev = itemRevenueMap.getOrDefault(m.id, 0.0)
            val cost = soldQty * m.costPrice
            totalCost += cost

            statsList.add(
                ItemSalesStat(
                    menuItemId = m.id,
                    itemName = m.name,
                    category = m.category,
                    unit = m.unit,
                    totalQuantitySold = soldQty,
                    totalRevenue = rev,
                    unitCost = m.costPrice,
                    totalCost = cost,
                    totalProfit = 0.0,
                    profitMarginPercent = 0.0,
                    currentStock = m.currentStock
                )
            )
        }

        val topSelling = statsList.filter { it.totalQuantitySold > 0 }.sortedByDescending { it.totalQuantitySold }.take(15)
        val leastSelling = statsList.sortedBy { it.totalQuantitySold }.take(15)
        val outOfStock = itemsInDb.filter { it.currentStock <= 0.0 && !it.isArchived }
        val stagnant = itemsInDb.filter { itemQtyMap.getOrDefault(it.id, 0.0) == 0.0 && !it.isArchived }

        AdvancedSalesReport(
            startDate = startDate,
            endDate = endDate,
            topSellingItems = topSelling,
            leastSellingItems = leastSelling,
            outOfStockItems = outOfStock,
            stagnantItems = stagnant,
            totalSalesRevenue = totalRevenue,
            totalSalesCost = totalCost,
            totalGrossProfit = 0.0,
            profitMarginPercent = 0.0,
            totalOrdersCount = orders.size,
            totalDeliveryFees = totalDeliveryFees,
            totalDiscounts = totalDiscounts
        )
    }

    // WhatsApp Invoice Sharing
    fun generateWhatsAppInvoiceText(orderWithItems: OrderWithItems, template: InvoiceTemplateConfig): String {
        val o = orderWithItems.order
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(o.createdAt))

        val sb = StringBuilder()
        sb.appendLine("🧾 *فاتورة مبيعات - ${template.headerTitle}*")
        sb.appendLine("📍 ${template.address}")
        sb.appendLine("📞 ${template.phone1} ${if (template.phone2.isNotBlank()) "- ${template.phone2}" else ""}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🔢 *رقم الفاتورة:* ${o.orderNumber}")
        sb.appendLine("⏰ *التاريخ:* $dateFormat")
        if (o.customerName.isNotBlank() && o.customerName != "زبون مباشر") {
            sb.appendLine("👤 *العميل:* ${o.customerName}")
        }
        if (o.customerPhone.isNotBlank()) {
            sb.appendLine("📱 *الهاتف:* ${o.customerPhone}")
        }
        if (!o.customerSecondaryPhone.isNullOrBlank()) {
            sb.appendLine("📞 *هاتف إضافي:* ${o.customerSecondaryPhone}")
        }
        if (o.orderType == "DELIVERY") {
            sb.appendLine("🛵 *النوع:* توصيل منازل")
            if (!o.deliveryAddressFull.isNullOrBlank()) sb.appendLine("🏠 *العنوان:* ${o.deliveryAddressFull}")
        } else {
            sb.appendLine("🛍️ *النوع:* تيك أواي / استلام")
            if (!o.deliveryAddressFull.isNullOrBlank()) sb.appendLine("🏠 *العنوان:* ${o.deliveryAddressFull}")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("*تفاصيل الطلب:*")
        orderWithItems.items.forEachIndexed { idx, itm ->
            sb.appendLine("${idx + 1}. *${itm.itemName}*")
            sb.appendLine("   - الكمية: ${com.example.printer.ReceiptBitmapRenderer.formatQuantity(itm.quantity)} ${itm.unit} × ${itm.unitPrice} ج.م")
            if (itm.preparationPrice > 0 && !itm.preparationName.isNullOrBlank()) {
                sb.appendLine("   - تجهيز: ${itm.preparationName} (+${itm.preparationPrice} ج.م)")
            }
            sb.appendLine("   - الإجمالي: *${itm.totalPrice} ج.م*")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("▫️ الحساب الفرعي: ${o.subtotal} ج.م")
        if (o.deliveryFee > 0) sb.appendLine("▫️ خدمة التوصيل: +${String.format(Locale.US, "%.0f", o.deliveryFee)} ج.م")
        if (o.discount > 0) sb.appendLine("▫️ الخصم: -${o.discount} ج.م")
        sb.appendLine("💰 *الإجمالي النهائي: ${o.totalAmount} ج.م*")
        sb.appendLine("💳 طريقة الدفع: ${if (o.paymentMethod == "CASH") "كاش" else "محفظة إلكترونية"}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━")
        sb.appendLine(template.footerNote)
        return sb.toString()
    }

    fun createWhatsAppInvoiceIntent(phoneNumber: String, invoiceText: String): Intent {
        var cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.startsWith("01")) {
            cleanPhone = "2$cleanPhone"
        } else if (cleanPhone.startsWith("+")) {
            cleanPhone = cleanPhone.substring(1)
        }

        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(invoiceText)}"
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    suspend fun getOwnerWhatsAppPhone(): String = withContext(Dispatchers.IO) {
        db.settingsDao().getSettingValue("owner_whatsapp_phone") ?: ""
    }

    suspend fun setOwnerWhatsAppPhone(phone: String) = withContext(Dispatchers.IO) {
        db.settingsDao().setSetting(AppSetting("owner_whatsapp_phone", phone.trim()))
    }

    fun createWhatsAppLateEmployeeIntent(
        ownerPhone: String,
        employeeName: String,
        employeeRole: String,
        scheduledTime: String,
        actualTime: String,
        delayMinutes: Int,
        dateStr: String
    ): Intent {
        var cleanPhone = ownerPhone.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.startsWith("01")) {
            cleanPhone = "2$cleanPhone"
        } else if (cleanPhone.startsWith("+")) {
            cleanPhone = cleanPhone.substring(1)
        }
        val msg = buildString {
            appendLine("🐦 *تنبيه تأخير موظف* 🐦")
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("👤 *الموظف:* $employeeName")
            appendLine("💼 *الوظيفة:* $employeeRole")
            appendLine("⏰ *الموعد المقرر:* $scheduledTime")
            appendLine("🕒 *وقت الحضور الفعلي:* $actualTime")
            appendLine("⏳ *مقدار التأخير:* $delayMinutes دقيقة")
            appendLine("📅 *التاريخ:* $dateStr")
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("نظام إدارة مطعم صنارة وشبكة 🐟")
        }
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // Multi-Printer Management & Dynamic Customization
    suspend fun getAllPrinterConfigs(): List<PrinterConfig> = withContext(Dispatchers.IO) {
        val json = db.settingsDao().getSettingValue("printers_list_json")
        if (!json.isNullOrBlank()) {
            try {
                val array = org.json.JSONArray(json)
                val list = mutableListOf<PrinterConfig>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val rawId = obj.optString("id", "PRINTER_${i + 1}")
                    val cleanId = rawId.removePrefix("p_").trim().ifBlank { "PRINTER_${i + 1}" }
                    list.add(
                        PrinterConfig(
                            id = cleanId,
                            displayName = obj.optString("displayName", "طابعة ${i + 1}"),
                            connectionType = obj.optString("connectionType", "NETWORK_IP"),
                            ipAddress = obj.optString("ipAddress", "192.168.1.50"),
                            port = obj.optInt("port", 9100),
                            bluetoothMacOrName = obj.optString("bluetoothMacOrName", ""),
                            usbDeviceName = obj.optString("usbDeviceName", ""),
                            paperWidthMm = obj.optInt("paperWidthMm", 80),
                            codepageMode = obj.optString("codepageMode", "CP864_PAGE_22"),
                            printMode = obj.optString("printMode", "IMAGE"),
                            role = obj.optString("role", "CUSTOMER_INVOICE"),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            isCashierDefault = obj.optBoolean("isCashierDefault", false),
                            customHeader = obj.optString("customHeader", ""),
                            formatTemplate = obj.optString("formatTemplate", "STANDARD")
                        )
                    )
                }
                return@withContext list.distinctBy { it.id }
            } catch (_: Exception) {}
        }
        emptyList()
    }

    suspend fun saveAllPrinterConfigs(configs: List<PrinterConfig>) = withContext(Dispatchers.IO) {
        val distinctList = configs.distinctBy { it.id.removePrefix("p_").trim() }
        val array = org.json.JSONArray()
        for (c in distinctList) {
            val cleanId = c.id.removePrefix("p_").trim().ifBlank { "PRINTER_1" }
            val obj = org.json.JSONObject().apply {
                put("id", cleanId)
                put("displayName", c.displayName)
                put("connectionType", c.connectionType)
                put("ipAddress", c.ipAddress)
                put("port", c.port)
                put("bluetoothMacOrName", c.bluetoothMacOrName)
                put("usbDeviceName", c.usbDeviceName)
                put("paperWidthMm", c.paperWidthMm)
                put("codepageMode", c.codepageMode)
                put("printMode", c.printMode)
                put("role", c.role)
                put("isEnabled", c.isEnabled)
                put("isCashierDefault", c.isCashierDefault)
                put("customHeader", c.customHeader)
                put("formatTemplate", c.formatTemplate)
            }
            array.put(obj)
        }
        db.settingsDao().setSetting(AppSetting("printers_list_json", array.toString()))
    }

    suspend fun clearAllPrinters() = withContext(Dispatchers.IO) {
        db.settingsDao().setSetting(AppSetting("printers_list_json", "[]"))
    }

    suspend fun getPrinterConfig(printerId: String): PrinterConfig = withContext(Dispatchers.IO) {
        val cleanReqId = printerId.removePrefix("p_").trim()
        val all = getAllPrinterConfigs()
        all.firstOrNull { it.id.removePrefix("p_").trim().equals(cleanReqId, ignoreCase = true) }
            ?: all.firstOrNull { it.isCashierDefault }
            ?: all.firstOrNull()
            ?: PrinterConfig(
                id = cleanReqId.ifBlank { "MAIN" },
                displayName = "طابعة عامة",
                connectionType = "SYSTEM_SPOOLER",
                role = "BOTH",
                isEnabled = true,
                isCashierDefault = true
            )
    }

    suspend fun markPrinterDeletedTombstone(printerId: String) = withContext(Dispatchers.IO) {
        val cleanReqId = printerId.removePrefix("p_").trim().lowercase()
        if (cleanReqId.isBlank()) return@withContext
        val currentJson = db.settingsDao().getSettingValue("tombstoned_printers_json") ?: "[]"
        val set = try {
            val arr = org.json.JSONArray(currentJson)
            val list = mutableSetOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i).lowercase())
            list
        } catch (_: Exception) {
            mutableSetOf<String>()
        }
        set.add(cleanReqId)
        val newArr = org.json.JSONArray(set.toList())
        db.settingsDao().setSetting(AppSetting("tombstoned_printers_json", newArr.toString()))
    }

    suspend fun isPrinterTombstoned(printerId: String): Boolean = withContext(Dispatchers.IO) {
        val cleanReqId = printerId.removePrefix("p_").trim().lowercase()
        if (cleanReqId.isBlank()) return@withContext false
        val currentJson = db.settingsDao().getSettingValue("tombstoned_printers_json") ?: "[]"
        try {
            val arr = org.json.JSONArray(currentJson)
            for (i in 0 until arr.length()) {
                if (arr.getString(i).equals(cleanReqId, ignoreCase = true)) return@withContext true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearPrinterTombstone(printerId: String) = withContext(Dispatchers.IO) {
        val cleanReqId = printerId.removePrefix("p_").trim().lowercase()
        if (cleanReqId.isBlank()) return@withContext
        val currentJson = db.settingsDao().getSettingValue("tombstoned_printers_json") ?: "[]"
        try {
            val arr = org.json.JSONArray(currentJson)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                val str = arr.getString(i)
                if (!str.equals(cleanReqId, ignoreCase = true)) set.add(str.lowercase())
            }
            db.settingsDao().setSetting(AppSetting("tombstoned_printers_json", org.json.JSONArray(set.toList()).toString()))
        } catch (_: Exception) {}
    }

    suspend fun savePrinterConfig(config: PrinterConfig) = withContext(Dispatchers.IO) {
        val cleanConfig = config.copy(id = config.id.removePrefix("p_").trim().ifBlank { "PRINTER_1" })
        clearPrinterTombstone(cleanConfig.id)
        val all = getAllPrinterConfigs().toMutableList()
        val index = all.indexOfFirst { it.id.removePrefix("p_").trim().equals(cleanConfig.id, ignoreCase = true) }
        if (index >= 0) {
            all[index] = cleanConfig
        } else {
            all.add(cleanConfig)
        }
        saveAllPrinterConfigs(all.distinctBy { it.id })
    }

    suspend fun deletePrinterConfig(printerId: String) = withContext(Dispatchers.IO) {
        val cleanReqId = printerId.removePrefix("p_").trim()
        markPrinterDeletedTombstone(cleanReqId)
        val all = getAllPrinterConfigs().filter { !it.id.removePrefix("p_").trim().equals(cleanReqId, ignoreCase = true) }
        saveAllPrinterConfigs(all)
    }

    suspend fun getInvoiceTemplateConfig(): InvoiceTemplateConfig = withContext(Dispatchers.IO) {
        InvoiceTemplateConfig(
            headerTitle = db.settingsDao().getSettingValue("inv_title") ?: "صنارة وشبكة للمأكولات البحرية",
            subtitle = db.settingsDao().getSettingValue("inv_subtitle") ?: "طازج من البحر لبيتك - أسماك وسواحل",
            phone1 = db.settingsDao().getSettingValue("inv_phone1") ?: "01001234567",
            phone2 = db.settingsDao().getSettingValue("inv_phone2") ?: "01123456789",
            address = db.settingsDao().getSettingValue("inv_address") ?: "فرع المطعم الرئيسي",
            footerNote = db.settingsDao().getSettingValue("inv_footer") ?: "شكراً لزيارتكم ونتمنى لكم وجبة شهية!",
            showPreparations = db.settingsDao().getSettingValue("inv_show_preps")?.toBooleanStrictOrNull() ?: true,
            showOrderNotes = db.settingsDao().getSettingValue("inv_show_notes")?.toBooleanStrictOrNull() ?: true,
            showDeliveryDetails = db.settingsDao().getSettingValue("inv_show_delivery")?.toBooleanStrictOrNull() ?: true,
            fontSize = db.settingsDao().getSettingValue("inv_font_size") ?: "NORMAL",
            logoPath = db.settingsDao().getSettingValue("inv_logo_path"),
            topPadding = db.settingsDao().getSettingValue("inv_top_padding")?.toIntOrNull() ?: 20,
            headerLineSpacing = db.settingsDao().getSettingValue("inv_line_spacing")?.toIntOrNull() ?: 12,
            headerTitleFontSize = db.settingsDao().getSettingValue("inv_title_font_size")?.toIntOrNull() ?: 30,
            headerDetailsFontSize = db.settingsDao().getSettingValue("inv_details_font_size")?.toIntOrNull() ?: 18,
            deliveryManifestFontSize = db.settingsDao().getSettingValue("inv_manifest_font_size") ?: "LARGE",
            compactHeaderMode = db.settingsDao().getSettingValue("inv_compact_header")?.toBooleanStrictOrNull() ?: true,
            showLogo = db.settingsDao().getSettingValue("inv_show_logo")?.toBooleanStrictOrNull() ?: true,
            showHeaderTitle = db.settingsDao().getSettingValue("inv_show_header_title")?.toBooleanStrictOrNull() ?: true,
            showSubtitle = db.settingsDao().getSettingValue("inv_show_subtitle")?.toBooleanStrictOrNull() ?: true,
            showAddress = db.settingsDao().getSettingValue("inv_show_address")?.toBooleanStrictOrNull() ?: true,
            showPhones = db.settingsDao().getSettingValue("inv_show_phones")?.toBooleanStrictOrNull() ?: true,
            showOrderBadge = db.settingsDao().getSettingValue("inv_show_order_badge")?.toBooleanStrictOrNull() ?: true,
            showDateTime = db.settingsDao().getSettingValue("inv_show_datetime")?.toBooleanStrictOrNull() ?: true,
            showSellerName = db.settingsDao().getSettingValue("inv_show_seller")?.toBooleanStrictOrNull() ?: false,
            showCustomerName = db.settingsDao().getSettingValue("inv_show_cust_name")?.toBooleanStrictOrNull() ?: true,
            showCustomerPhone = db.settingsDao().getSettingValue("inv_show_cust_phone")?.toBooleanStrictOrNull() ?: true,
            showDeliveryAddress = db.settingsDao().getSettingValue("inv_show_delivery_addr")?.toBooleanStrictOrNull() ?: true,
            showDriverName = db.settingsDao().getSettingValue("inv_show_driver")?.toBooleanStrictOrNull() ?: true,
            showSubtotal = db.settingsDao().getSettingValue("inv_show_subtotal")?.toBooleanStrictOrNull() ?: true,
            showDeliveryFee = db.settingsDao().getSettingValue("inv_show_delivery_fee")?.toBooleanStrictOrNull() ?: true,
            showPaymentStatusBox = db.settingsDao().getSettingValue("inv_show_payment_box")?.toBooleanStrictOrNull() ?: true,
            showPaymentBreakdown = db.settingsDao().getSettingValue("inv_show_payment_breakdown")?.toBooleanStrictOrNull() ?: true,
            showFooterNote = db.settingsDao().getSettingValue("inv_show_footer_note")?.toBooleanStrictOrNull() ?: true,
            metadataFontSizePx = db.settingsDao().getSettingValue("inv_meta_font_size")?.toIntOrNull() ?: 16,
            tableHeaderFontSizePx = db.settingsDao().getSettingValue("inv_table_header_font_size")?.toIntOrNull() ?: 18,
            itemFontSizePx = db.settingsDao().getSettingValue("inv_item_font_size")?.toIntOrNull() ?: 22,
            itemPrepFontSizePx = db.settingsDao().getSettingValue("inv_prep_font_size")?.toIntOrNull() ?: 18,
            itemNotesFontSizePx = db.settingsDao().getSettingValue("inv_notes_font_size")?.toIntOrNull() ?: 15,
            totalBoxFontSizePx = db.settingsDao().getSettingValue("inv_total_box_font_size")?.toIntOrNull() ?: 24
        )
    }

    suspend fun saveInvoiceTemplateConfig(template: InvoiceTemplateConfig) = withContext(Dispatchers.IO) {
        db.settingsDao().setSetting(AppSetting("inv_title", template.headerTitle))
        db.settingsDao().setSetting(AppSetting("inv_subtitle", template.subtitle))
        db.settingsDao().setSetting(AppSetting("inv_phone1", template.phone1))
        db.settingsDao().setSetting(AppSetting("inv_phone2", template.phone2))
        db.settingsDao().setSetting(AppSetting("inv_address", template.address))
        db.settingsDao().setSetting(AppSetting("inv_footer", template.footerNote))
        db.settingsDao().setSetting(AppSetting("inv_show_preps", template.showPreparations.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_notes", template.showOrderNotes.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_delivery", template.showDeliveryDetails.toString()))
        db.settingsDao().setSetting(AppSetting("inv_font_size", template.fontSize))
        db.settingsDao().setSetting(AppSetting("inv_manifest_font_size", template.deliveryManifestFontSize))
        db.settingsDao().setSetting(AppSetting("inv_top_padding", template.topPadding.toString()))
        db.settingsDao().setSetting(AppSetting("inv_line_spacing", template.headerLineSpacing.toString()))
        db.settingsDao().setSetting(AppSetting("inv_title_font_size", template.headerTitleFontSize.toString()))
        db.settingsDao().setSetting(AppSetting("inv_details_font_size", template.headerDetailsFontSize.toString()))
        db.settingsDao().setSetting(AppSetting("inv_compact_header", template.compactHeaderMode.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_logo", template.showLogo.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_header_title", template.showHeaderTitle.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_subtitle", template.showSubtitle.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_address", template.showAddress.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_phones", template.showPhones.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_order_badge", template.showOrderBadge.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_datetime", template.showDateTime.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_seller", template.showSellerName.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_cust_name", template.showCustomerName.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_cust_phone", template.showCustomerPhone.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_delivery_addr", template.showDeliveryAddress.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_driver", template.showDriverName.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_subtotal", template.showSubtotal.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_delivery_fee", template.showDeliveryFee.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_payment_box", template.showPaymentStatusBox.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_payment_breakdown", template.showPaymentBreakdown.toString()))
        db.settingsDao().setSetting(AppSetting("inv_show_footer_note", template.showFooterNote.toString()))
        db.settingsDao().setSetting(AppSetting("inv_meta_font_size", template.metadataFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("inv_table_header_font_size", template.tableHeaderFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("inv_item_font_size", template.itemFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("inv_prep_font_size", template.itemPrepFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("inv_notes_font_size", template.itemNotesFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("inv_total_box_font_size", template.totalBoxFontSizePx.toString()))
        if (!template.logoPath.isNullOrBlank()) {
            db.settingsDao().setSetting(AppSetting("inv_logo_path", template.logoPath))
        } else {
            db.settingsDao().deleteSetting("inv_logo_path")
        }
    }

    suspend fun getKitchenOrderTemplateConfig(): KitchenOrderTemplateConfig = withContext(Dispatchers.IO) {
        KitchenOrderTemplateConfig(
            headerTitle = db.settingsDao().getSettingValue("kitchen_title") ?: "أمر تشغيل المطبخ والاستاف",
            kitchenDepartment = db.settingsDao().getSettingValue("kitchen_dept") ?: "",
            showHeaderTitle = db.settingsDao().getSettingValue("kitchen_show_header_title")?.toBooleanStrictOrNull() ?: true,
            showDepartment = db.settingsDao().getSettingValue("kitchen_show_dept")?.toBooleanStrictOrNull() ?: true,
            showCustomerName = db.settingsDao().getSettingValue("kitchen_show_cust_name")?.toBooleanStrictOrNull() ?: true,
            showCustomerPhone = db.settingsDao().getSettingValue("kitchen_show_cust_phone")?.toBooleanStrictOrNull() ?: true,
            showDeliveryAddress = db.settingsDao().getSettingValue("kitchen_show_delivery_addr")?.toBooleanStrictOrNull() ?: true,
            showSellerName = db.settingsDao().getSettingValue("kitchen_show_seller_name")?.toBooleanStrictOrNull() ?: false,
            showOrderTime = db.settingsDao().getSettingValue("kitchen_show_order_time")?.toBooleanStrictOrNull() ?: true,
            showOrderTypeAndTable = db.settingsDao().getSettingValue("kitchen_show_table_type")?.toBooleanStrictOrNull() ?: true,
            compactHeaderMode = db.settingsDao().getSettingValue("kitchen_compact_header")?.toBooleanStrictOrNull() ?: true,
            showTableSectionTitle = db.settingsDao().getSettingValue("kitchen_show_table_sec_title")?.toBooleanStrictOrNull() ?: false,
            showPreparations = db.settingsDao().getSettingValue("kitchen_show_preps")?.toBooleanStrictOrNull() ?: true,
            showOrderNotes = db.settingsDao().getSettingValue("kitchen_show_notes")?.toBooleanStrictOrNull() ?: true,
            showItemPrices = db.settingsDao().getSettingValue("kitchen_show_prices")?.toBooleanStrictOrNull() ?: false,
            showCashierAndTime = db.settingsDao().getSettingValue("kitchen_show_cashier_time")?.toBooleanStrictOrNull() ?: true,
            showItemCountSummary = db.settingsDao().getSettingValue("kitchen_show_count_summary")?.toBooleanStrictOrNull() ?: true,
            highlightItemNames = db.settingsDao().getSettingValue("kitchen_highlight_items")?.toBooleanStrictOrNull() ?: true,
            showFooterNote = db.settingsDao().getSettingValue("kitchen_show_footer_note")?.toBooleanStrictOrNull() ?: false,
            fontSize = db.settingsDao().getSettingValue("kitchen_font_size") ?: "LARGE",
            footerNote = db.settingsDao().getSettingValue("kitchen_footer") ?: "برجاء سرعة التجهيز والالتزام بملاحظات التحضير",
            topPadding = db.settingsDao().getSettingValue("kitchen_top_padding")?.toIntOrNull() ?: 10,
            headerLineSpacing = db.settingsDao().getSettingValue("kitchen_line_spacing")?.toIntOrNull() ?: 6,
            tableHeaderItemColumnTitle = db.settingsDao().getSettingValue("kitchen_table_item_title") ?: "الصنف والتجهيز",
            qtyColumnWidthPercent = db.settingsDao().getSettingValue("kitchen_qty_width_pct")?.toIntOrNull() ?: 28,
            headerFontSizePx = db.settingsDao().getSettingValue("kitchen_header_font_sz")?.toIntOrNull() ?: 24,
            metadataFontSizePx = db.settingsDao().getSettingValue("kitchen_meta_font_sz")?.toIntOrNull() ?: 16,
            tableHeaderFontSizePx = db.settingsDao().getSettingValue("kitchen_tbl_hdr_font_sz")?.toIntOrNull() ?: 18,
            itemFontSizePx = db.settingsDao().getSettingValue("kitchen_item_font_sz")?.toIntOrNull() ?: 26,
            itemPrepFontSizePx = db.settingsDao().getSettingValue("kitchen_item_prep_font_sz")?.toIntOrNull() ?: 20,
            itemNotesFontSizePx = db.settingsDao().getSettingValue("kitchen_item_notes_font_sz")?.toIntOrNull() ?: 17,
            qtyFontSizePx = db.settingsDao().getSettingValue("kitchen_qty_font_sz")?.toIntOrNull() ?: 28
        )
    }

    suspend fun saveKitchenOrderTemplateConfig(template: KitchenOrderTemplateConfig) = withContext(Dispatchers.IO) {
        db.settingsDao().setSetting(AppSetting("kitchen_title", template.headerTitle))
        db.settingsDao().setSetting(AppSetting("kitchen_dept", template.kitchenDepartment))
        db.settingsDao().setSetting(AppSetting("kitchen_show_header_title", template.showHeaderTitle.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_dept", template.showDepartment.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_cust_name", template.showCustomerName.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_cust_phone", template.showCustomerPhone.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_delivery_addr", template.showDeliveryAddress.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_seller_name", template.showSellerName.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_order_time", template.showOrderTime.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_table_type", template.showOrderTypeAndTable.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_compact_header", template.compactHeaderMode.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_table_sec_title", template.showTableSectionTitle.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_preps", template.showPreparations.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_notes", template.showOrderNotes.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_prices", template.showItemPrices.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_cashier_time", template.showCashierAndTime.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_count_summary", template.showItemCountSummary.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_highlight_items", template.highlightItemNames.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_show_footer_note", template.showFooterNote.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_font_size", template.fontSize))
        db.settingsDao().setSetting(AppSetting("kitchen_footer", template.footerNote))
        db.settingsDao().setSetting(AppSetting("kitchen_top_padding", template.topPadding.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_line_spacing", template.headerLineSpacing.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_table_item_title", template.tableHeaderItemColumnTitle))
        db.settingsDao().setSetting(AppSetting("kitchen_qty_width_pct", template.qtyColumnWidthPercent.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_header_font_sz", template.headerFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_meta_font_sz", template.metadataFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_tbl_hdr_font_sz", template.tableHeaderFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_item_font_sz", template.itemFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_item_prep_font_sz", template.itemPrepFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_item_notes_font_sz", template.itemNotesFontSizePx.toString()))
        db.settingsDao().setSetting(AppSetting("kitchen_qty_font_sz", template.qtyFontSizePx.toString()))
    }

    suspend fun setOrderArchived(orderId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        db.orderDao().setOrderArchived(orderId, isArchived)
    }

    suspend fun archiveOrders(orderIds: List<Long>) = withContext(Dispatchers.IO) {
        db.orderDao().archiveOrders(orderIds)
    }

    suspend fun unarchiveAllOrders(): Int = withContext(Dispatchers.IO) {
        db.orderDao().unarchiveAllOrders()
    }

    suspend fun testPrinter(printerId: String): PrinterResult = withContext(Dispatchers.IO) {
        val config = getPrinterConfig(printerId)
        printerManager.testPrinterConnection(config)
    }

    suspend fun openCashDrawer(printerId: String): PrinterResult = withContext(Dispatchers.IO) {
        val config = getPrinterConfig(printerId)
        printerManager.openCashDrawer(config)
    }

    suspend fun getPairedBluetoothPrinters(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        printerManager.getPairedBluetoothDevices()
    }

    suspend fun getOrderByIdDirect(orderId: Long): OrderWithItems? = withContext(Dispatchers.IO) {
        db.orderDao().getOrderById(orderId)
    }

    fun matchesKitchenPrinterTarget(itemTargetPrinter: String, printerTarget: String): Boolean {
        val target = itemTargetPrinter.uppercase()
        return if (printerTarget == "KITCHEN_1") {
            target.contains("KITCHEN_1") || target.contains("ALL") || target == "MAIN" || target.isBlank()
        } else if (printerTarget == "KITCHEN_2") {
            target.contains("KITCHEN_2") || target.contains("ALL")
        } else {
            target.contains(printerTarget.uppercase()) || target.contains("ALL") || target == "MAIN" || target.isBlank()
        }
    }

    private fun pendingKitchenCancellationsKey(orderId: Long): String = "pending_kitchen_cancellations_$orderId"

    suspend fun getPendingKitchenCancellations(orderId: Long): List<CancelledKitchenItem> = withContext(Dispatchers.IO) {
        val raw = db.settingsDao().getSettingValue(pendingKitchenCancellationsKey(orderId)).orEmpty()
        if (raw.isBlank()) return@withContext emptyList()
        val list = mutableListOf<CancelledKitchenItem>()
        try {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CancelledKitchenItem(
                        itemName = obj.optString("itemName", ""),
                        kitchenName = obj.optString("kitchenName", ""),
                        targetPrinter = obj.optString("targetPrinter", "MAIN"),
                        preparationName = obj.optString("preparationName", "").ifBlank { null },
                        unit = obj.optString("unit", "كيلو"),
                        cancelledQty = obj.optDouble("cancelledQty", 0.0),
                        prevQty = obj.optDouble("prevQty", 0.0),
                        newQty = obj.optDouble("newQty", 0.0)
                    )
                )
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getPendingKitchenCancellationsForTarget(orderId: Long, printerTarget: String): List<CancelledKitchenItem> =
        getPendingKitchenCancellations(orderId).filter { matchesKitchenPrinterTarget(it.targetPrinter, printerTarget) }

    private suspend fun savePendingKitchenCancellations(orderId: Long, list: List<CancelledKitchenItem>) {
        val key = pendingKitchenCancellationsKey(orderId)
        if (list.isEmpty()) {
            db.settingsDao().setSetting(AppSetting(key, ""))
            return
        }
        val arr = org.json.JSONArray()
        for (c in list) {
            arr.put(
                org.json.JSONObject().apply {
                    put("itemName", c.itemName)
                    put("kitchenName", c.kitchenName)
                    put("targetPrinter", c.targetPrinter)
                    put("preparationName", c.preparationName ?: "")
                    put("unit", c.unit)
                    put("cancelledQty", c.cancelledQty)
                    put("prevQty", c.prevQty)
                    put("newQty", c.newQty)
                }
            )
        }
        db.settingsDao().setSetting(AppSetting(key, arr.toString()))
    }

    private suspend fun appendPendingKitchenCancellations(orderId: Long, newEntries: List<CancelledKitchenItem>) {
        if (newEntries.isEmpty()) return
        val current = getPendingKitchenCancellations(orderId).toMutableList()
        for (entry in newEntries) {
            val keyName = normalizeArabicText(entry.itemName)
            val keyPrep = normalizeArabicText(entry.preparationName.orEmpty())
            val keyUnit = entry.unit.trim().lowercase()
            val idx = current.indexOfFirst {
                normalizeArabicText(it.itemName) == keyName &&
                    normalizeArabicText(it.preparationName.orEmpty()) == keyPrep &&
                    it.unit.trim().lowercase() == keyUnit &&
                    it.targetPrinter.equals(entry.targetPrinter, ignoreCase = true)
            }
            if (idx >= 0) {
                val prev = current[idx]
                val totalCancelled = (prev.prevQty - entry.newQty).coerceAtLeast(0.0)
                if (totalCancelled > 0.0001) {
                    current[idx] = entry.copy(prevQty = prev.prevQty, cancelledQty = totalCancelled)
                } else {
                    current.removeAt(idx)
                }
            } else {
                current.add(entry)
            }
        }
        savePendingKitchenCancellations(orderId, current)
    }

    suspend fun clearPendingKitchenCancellationsForTarget(orderId: Long, printerTarget: String) = withContext(Dispatchers.IO) {
        val current = getPendingKitchenCancellations(orderId)
        if (current.isEmpty()) return@withContext
        val remaining = current.filterNot { matchesKitchenPrinterTarget(it.targetPrinter, printerTarget) }
        savePendingKitchenCancellations(orderId, remaining)
    }

    private suspend fun mergeCloudOrderItemsIntoLocal(
        orderId: Long,
        existingItems: List<OrderItem>,
        incomingItems: List<OrderItem>
    ) {
        if (incomingItems.isEmpty()) return

        val remainingOld = existingItems.toMutableList()
        val mergedItems = mutableListOf<OrderItem>()
        val pendingCancels = mutableListOf<CancelledKitchenItem>()

        for (inc in incomingItems) {
            val incNameKey = normalizeArabicText(inc.itemName)
            val incUnitKey = inc.unit.trim().lowercase()
            val incPrepKey = normalizeArabicText(inc.preparationName.orEmpty())

            val matchedOldIdx = remainingOld.indexOfFirst { old ->
                normalizeArabicText(old.itemName) == incNameKey &&
                    old.unit.trim().lowercase() == incUnitKey &&
                    normalizeArabicText(old.preparationName.orEmpty()) == incPrepKey
            }.let { exactIdx ->
                if (exactIdx >= 0) exactIdx
                else remainingOld.indexOfFirst { old ->
                    normalizeArabicText(old.itemName) == incNameKey &&
                        old.unit.trim().lowercase() == incUnitKey
                }
            }

            val matchedOld = if (matchedOldIdx >= 0) remainingOld.removeAt(matchedOldIdx) else null
            val mergedItem = if (matchedOld != null) {
                val oldPrintedQty = matchedOld.printedKitchenQuantity
                val effectivePrintedQty = if (oldPrintedQty > 0.0001 && inc.quantity < oldPrintedQty - 0.0001) {
                    pendingCancels.add(
                        CancelledKitchenItem(
                            itemName = matchedOld.itemName,
                            kitchenName = matchedOld.kitchenName.ifBlank { inc.kitchenName },
                            targetPrinter = matchedOld.targetPrinter.ifBlank { inc.targetPrinter },
                            preparationName = matchedOld.preparationName,
                            unit = matchedOld.unit,
                            cancelledQty = oldPrintedQty - inc.quantity,
                            prevQty = oldPrintedQty,
                            newQty = inc.quantity
                        )
                    )
                    inc.quantity
                } else {
                    oldPrintedQty
                }
                val isFullyPrinted = effectivePrintedQty > 0.0001 && inc.quantity <= effectivePrintedQty + 0.0001
                inc.copy(
                    id = 0L,
                    orderId = orderId,
                    menuItemId = inc.menuItemId ?: matchedOld.menuItemId,
                    printedKitchenQuantity = effectivePrintedQty,
                    printedInKitchen = isFullyPrinted
                )
            } else {
                inc.copy(
                    id = 0L,
                    orderId = orderId,
                    printedKitchenQuantity = 0.0,
                    printedInKitchen = false
                )
            }
            mergedItems.add(mergedItem)
        }

        // Remaining unmatched old items were removed remotely -> queue kitchen cancellation alert if previously printed!
        for (removedOld in remainingOld) {
            if (removedOld.printedKitchenQuantity > 0.0001) {
                pendingCancels.add(
                    CancelledKitchenItem(
                        itemName = removedOld.itemName,
                        kitchenName = removedOld.kitchenName,
                        targetPrinter = removedOld.targetPrinter,
                        preparationName = removedOld.preparationName,
                        unit = removedOld.unit,
                        cancelledQty = removedOld.printedKitchenQuantity,
                        prevQty = removedOld.printedKitchenQuantity,
                        newQty = 0.0
                    )
                )
            }
        }

        val itemsChanged = existingItems.size != mergedItems.size ||
            existingItems.zip(mergedItems).any { (old, m) ->
                old.itemName != m.itemName ||
                    old.kitchenName != m.kitchenName ||
                    old.unit != m.unit ||
                    kotlin.math.abs(old.quantity - m.quantity) > 0.0001 ||
                    kotlin.math.abs(old.unitPrice - m.unitPrice) > 0.001 ||
                    (old.preparationName ?: "") != (m.preparationName ?: "") ||
                    kotlin.math.abs(old.preparationPrice - m.preparationPrice) > 0.001 ||
                    old.itemNotes != m.itemNotes ||
                    kotlin.math.abs(old.totalPrice - m.totalPrice) > 0.001 ||
                    old.targetPrinter != m.targetPrinter ||
                    kotlin.math.abs(old.printedKitchenQuantity - m.printedKitchenQuantity) > 0.0001 ||
                    old.printedInKitchen != m.printedInKitchen
            }

        if (itemsChanged) {
            db.orderDao().deleteItemsForOrder(orderId)
            db.orderDao().insertOrderItems(mergedItems)
        }
        if (pendingCancels.isNotEmpty()) {
            appendPendingKitchenCancellations(orderId, pendingCancels)
        }
    }

    suspend fun dispatchPrint(
        orderId: Long,
        printerTarget: String,
        reprintReason: String? = null,
        targetDocumentType: String? = null
    ): PrinterResult = withContext(Dispatchers.IO) {
        try {
            val orderWithItems = db.orderDao().getOrderById(orderId)
                ?: return@withContext PrinterResult.Error("الأوردر غير موجود في قاعدة البيانات (معرف: $orderId)")
            val order = orderWithItems.order

            val config = getPrinterConfig(printerTarget)
            val docType = targetDocumentType ?: config.role
            val isKitchenTicket = docType == "KITCHEN_ORDER" || docType == "BOTH" || printerTarget.contains("KITCHEN")

            val logsCount = db.settingsDao().countPrintLogsForTarget(orderId, printerTarget)
            val isReprint = logsCount > 0

            val effectiveReason = if (isReprint) {
                if (!reprintReason.isNullOrBlank()) reprintReason else "إعادة طباعة"
            } else null

            val template = getInvoiceTemplateConfig()
            val kitchenTemplate = getKitchenOrderTemplateConfig()
            val pendingCancels = if (isKitchenTicket) {
                getPendingKitchenCancellationsForTarget(orderId, printerTarget)
            } else {
                emptyList()
            }
            val printResult = printerManager.printOrder(
                config = config,
                orderWithItems = orderWithItems,
                templateConfig = template,
                isReprint = isReprint,
                reprintReason = effectiveReason,
                targetDocumentType = targetDocumentType,
                kitchenTemplate = kitchenTemplate,
                cancelledKitchenItems = pendingCancels
            )

            if (printResult is PrinterResult.Success) {
                try {
                    db.settingsDao().insertPrintLog(
                        PrintLog(
                            orderId = orderId,
                            orderNumber = order.orderNumber,
                            printerTarget = printerTarget,
                            isReprint = isReprint,
                            reprintReason = effectiveReason
                        )
                    )

                    if (isKitchenTicket) {
                        clearPendingKitchenCancellationsForTarget(orderId, printerTarget)
                        if (printerTarget.equals("KITCHEN_2", ignoreCase = true)) {
                            db.orderDao().updateKitchen2Printed(orderId, true)
                        } else {
                            db.orderDao().updateKitchen1Printed(orderId, true)
                        }
                        for (item in orderWithItems.items) {
                            val matches = matchesKitchenPrinterTarget(item.targetPrinter, printerTarget)
                            if (matches && item.quantity > item.printedKitchenQuantity) {
                                db.orderDao().updateOrderItemKitchenPrintedQuantity(item.id, true, item.quantity)
                            }
                        }
                    }
                    if (docType == "CUSTOMER_INVOICE" || docType == "BOTH" || printerTarget == "MAIN" || config.isCashierDefault) {
                        db.orderDao().updateMainPrinted(orderId, true)
                    }
                } catch (_: Exception) {}
            }

            printResult
        } catch (e: Exception) {
            PrinterResult.Error("خطأ غير متوقع في نظام الطباعة: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun dispatchDeliveryManifest(
        ordersWithItems: List<OrderWithItems>,
        printerTarget: String
    ): PrinterResult = withContext(Dispatchers.IO) {
        try {
            val config = getPrinterConfig(printerTarget)
            val template = getInvoiceTemplateConfig()
            printerManager.printDeliveryManifest(config, ordersWithItems, template)
        } catch (e: Exception) {
            PrinterResult.Error("خطأ في طباعة كشف التسليم: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun newUniqueCloudId(): String =
        "item_${java.util.UUID.randomUUID().toString().replace("-", "").take(16)}"

    // General Menu, Staff, Driver, Settings methods
    suspend fun saveMenuItem(item: MenuItem): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val finalCloudId = if (item.cloudId.isNotBlank() && item.cloudId != "item_unknown") item.cloudId else newUniqueCloudId()
        val safeItem = item.copy(cloudId = finalCloudId, updatedAtMillis = now)
        if (safeItem.id == 0L) {
            db.menuDao().insertMenuItem(safeItem)
        } else {
            db.menuDao().updateMenuItem(safeItem)
            safeItem.id
        }
    }

    suspend fun updateMenuItems(items: List<MenuItem>) = withContext(Dispatchers.IO) {
        if (items.isNotEmpty()) {
            db.menuDao().updateMenuItems(items)
        }
    }

    suspend fun insertMenuItems(items: List<MenuItem>) = withContext(Dispatchers.IO) {
        if (items.isNotEmpty()) {
            val prepared = items.map { itm ->
                if (itm.cloudId.isBlank() || itm.cloudId == "item_unknown") {
                    itm.copy(cloudId = newUniqueCloudId())
                } else itm
            }
            db.menuDao().insertMenuItems(prepared)
        }
    }

    suspend fun saveMenuItemWithIngredients(
        item: MenuItem,
        ingredients: List<MenuItemIngredient>
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val finalCloudId = if (item.cloudId.isNotBlank() && item.cloudId != "item_unknown") item.cloudId else newUniqueCloudId()
        val safeItem = item.copy(cloudId = finalCloudId, updatedAtMillis = now)
        val itemId = if (safeItem.id == 0L) {
            db.menuDao().insertMenuItem(safeItem)
        } else {
            db.menuDao().updateMenuItem(safeItem)
            safeItem.id
        }
        db.menuDao().deleteIngredientsForItem(itemId)
        for (ing in ingredients) {
            db.menuDao().insertIngredient(
                ing.copy(id = 0, menuItemId = itemId)
            )
        }
        itemId
    }

    suspend fun getMenuItemById(id: Long): MenuItem? = withContext(Dispatchers.IO) {
        db.menuDao().getItemById(id)
    }

    suspend fun getMenuItemByCloudId(cloudId: String): MenuItem? = withContext(Dispatchers.IO) {
        if (cloudId.isBlank()) null else db.menuDao().getItemByCloudId(cloudId)
    }

    suspend fun updateMenuItemCloudId(id: Long, cloudId: String) = withContext(Dispatchers.IO) {
        db.menuDao().updateItemCloudId(id, cloudId)
    }

    suspend fun ensureUniqueNumericIdsAndCanonicalCloudIds() = withContext(Dispatchers.IO) {
        val allItems = db.menuDao().getAllMenuItemsIncludingArchivedList()
        for (item in allItems) {
            if ((item.cloudId.isBlank() || item.cloudId == "item_unknown") && item.id > 0L) {
                db.menuDao().updateItemCloudId(item.id, newUniqueCloudId())
            }
        }
    }

    suspend fun archiveMenuItem(id: Long, archived: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.menuDao().updateItemArchived(id, archived, now)
    }

    suspend fun updateMenuItemStock(id: Long, newStock: Double, reason: String = "تعديل رصيد") = withContext(Dispatchers.IO) {
        val item = db.menuDao().getItemById(id)
        if (item != null) {
            val diff = newStock - item.currentStock
            db.menuDao().updateItemStock(id, newStock)
            db.inventoryDao().insertMovement(
                InventoryMovement(
                    menuItemId = id,
                    itemName = item.name,
                    movementType = if (diff >= 0) "RECEIVE" else "ADJUSTMENT",
                    quantityChange = diff,
                    balanceAfter = newStock,
                    notes = reason
                )
            )
        }
    }

    suspend fun receiveInventoryBatch(itemId: Long, quantity: Double, costPerUnit: Double?, notes: String = "") = withContext(Dispatchers.IO) {
        val item = db.menuDao().getItemById(itemId) ?: return@withContext
        val newStock = item.currentStock + quantity
        db.menuDao().updateItemStock(itemId, newStock)
        if (costPerUnit != null && costPerUnit > 0) {
            db.menuDao().updateMenuItem(item.copy(currentStock = newStock, costPrice = costPerUnit))
        }
        db.inventoryDao().insertMovement(
            InventoryMovement(
                menuItemId = itemId,
                itemName = item.name,
                movementType = "RECEIVE",
                quantityChange = quantity,
                balanceAfter = newStock,
                notes = notes.ifBlank { "توريد بضاعة" }
            )
        )
    }

    suspend fun adjustInventory(itemId: Long, actualPhysicalStock: Double, notes: String = "") = withContext(Dispatchers.IO) {
        val item = db.menuDao().getItemById(itemId) ?: return@withContext
        val diff = actualPhysicalStock - item.currentStock
        db.menuDao().updateItemStock(itemId, actualPhysicalStock)
        db.inventoryDao().insertMovement(
            InventoryMovement(
                menuItemId = itemId,
                itemName = item.name,
                movementType = if (diff < 0) "WASTE" else "ADJUSTMENT",
                quantityChange = diff,
                balanceAfter = actualPhysicalStock,
                notes = notes.ifBlank { "تسوية جردية" }
            )
        )
    }

    suspend fun updateDailyPrices(priceUpdates: Map<Long, Double>) = withContext(Dispatchers.IO) {
        for ((id, price) in priceUpdates) {
            db.menuDao().updateItemPrice(id, price)
        }
        db.settingsDao().setSetting(AppSetting("lastPriceUpdateDate", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())))
    }

    suspend fun deleteMenuItem(id: Long) = withContext(Dispatchers.IO) { db.menuDao().deleteMenuItem(id) }

    suspend fun deduplicateMenuItems(): Int = withContext(Dispatchers.IO) {
        try {
            val allItems = db.menuDao().getAllMenuItemsIncludingArchivedList()
            if (allItems.isEmpty()) return@withContext 0

            var deletedCount = 0
            // Group 1: Items that share the EXACT same non-blank cloudId (true duplicates)
            val byCloudId = allItems.filter { it.cloudId.isNotBlank() }.groupBy { it.cloudId }
            for ((cid, group) in byCloudId) {
                if (group.size <= 1) continue
                val anyArchived = group.any { it.isArchived }
                val sorted = group.sortedByDescending { it.id }
                val keeperCandidate = sorted.first()
                val bestStock = group.map { it.currentStock }.maxOrNull() ?: keeperCandidate.currentStock
                val keeper = keeperCandidate.copy(
                    isArchived = if (anyArchived) true else keeperCandidate.isArchived,
                    currentStock = if (keeperCandidate.currentStock > 0.0) keeperCandidate.currentStock else bestStock
                )
                if (keeper != keeperCandidate) {
                    db.menuDao().updateMenuItem(keeper)
                }
                for (dup in sorted.drop(1)) {
                    try {
                        db.openHelper.writableDatabase.execSQL(
                            "UPDATE order_items SET menuItemId = ? WHERE menuItemId = ?",
                            arrayOf(keeper.id, dup.id)
                        )
                        db.openHelper.writableDatabase.execSQL(
                            "UPDATE menu_item_ingredients SET menuItemId = ? WHERE menuItemId = ?",
                            arrayOf(keeper.id, dup.id)
                        )
                    } catch (_: Exception) {}
                    // Soft archival only - NEVER automatically delete any item from database
                    db.menuDao().updateItemArchived(dup.id, true, System.currentTimeMillis())
                    db.menuDao().updateItemAvailability(dup.id, false)
                    deletedCount++
                }
            }

            // Group 2: Items with blank cloudId matching an existing item with exact same name, category, unit, price
            val currentItemsAfterPass1 = db.menuDao().getAllMenuItemsIncludingArchivedList()
            val itemsWithBlankCloudId = currentItemsAfterPass1.filter { it.cloudId.isBlank() }
            for (blankItem in itemsWithBlankCloudId) {
                val match = currentItemsAfterPass1.firstOrNull {
                    it.id != blankItem.id &&
                    it.cloudId.isNotBlank() &&
                    it.name.trim() == blankItem.name.trim() &&
                    it.category.trim() == blankItem.category.trim() &&
                    it.unit.trim().equals(blankItem.unit.trim(), ignoreCase = true) &&
                    Math.abs(it.basePrice - blankItem.basePrice) < 0.01
                }
                if (match != null) {
                    try {
                        db.openHelper.writableDatabase.execSQL(
                            "UPDATE order_items SET menuItemId = ? WHERE menuItemId = ?",
                            arrayOf(match.id, blankItem.id)
                        )
                        db.openHelper.writableDatabase.execSQL(
                            "UPDATE menu_item_ingredients SET menuItemId = ? WHERE menuItemId = ?",
                            arrayOf(match.id, blankItem.id)
                        )
                    } catch (_: Exception) {}
                    // Soft archival only - NEVER automatically delete any item from database
                    db.menuDao().updateItemArchived(blankItem.id, true, System.currentTimeMillis())
                    db.menuDao().updateItemAvailability(blankItem.id, false)
                    deletedCount++
                }
            }

            deletedCount
        } catch (e: Exception) {
            android.util.Log.e("RestaurantRepository", "deduplicateMenuItems error: ${e.message}")
            0
        }
    }

    suspend fun relinkMenuItemReferences(oldId: Long, newId: Long) = withContext(Dispatchers.IO) {
        if (oldId <= 0 || newId <= 0 || oldId == newId) return@withContext
        try {
            db.openHelper.writableDatabase.execSQL(
                "UPDATE order_items SET menuItemId = ? WHERE menuItemId = ?",
                arrayOf(newId, oldId)
            )
            db.openHelper.writableDatabase.execSQL(
                "UPDATE menu_item_ingredients SET menuItemId = ? WHERE menuItemId = ?",
                arrayOf(newId, oldId)
            )
            db.openHelper.writableDatabase.execSQL(
                "UPDATE inventory_movements SET menuItemId = ? WHERE menuItemId = ?",
                arrayOf(newId, oldId)
            )
            // Soft archival only - NEVER automatically delete any item from database
            db.menuDao().updateItemArchived(oldId, true, System.currentTimeMillis())
            db.menuDao().updateItemAvailability(oldId, false)
        } catch (e: Exception) {
            android.util.Log.e("RestaurantRepository", "relinkMenuItemReferences error: ${e.message}")
        }
    }

    suspend fun saveCategory(cat: MenuCategory): Long = withContext(Dispatchers.IO) {
        val cleanName = cat.name.trim()
        if (cleanName.isBlank()) return@withContext 0L
        val allCats = db.menuDao().getAllCategoriesList()
        val existing = allCats.find { it.id == cat.id || it.name.trim().equals(cleanName, ignoreCase = true) }
        if (existing != null) {
            db.menuDao().updateCategory(existing.copy(name = cleanName, displayOrder = cat.displayOrder, isArchived = cat.isArchived))
            existing.id
        } else {
            if (cat.id == 0L) db.menuDao().insertCategory(cat.copy(name = cleanName))
            else { db.menuDao().updateCategory(cat.copy(name = cleanName)); cat.id }
        }
    }

    suspend fun batchUpdateItemsCategory(itemIds: List<Long>, newCategory: String) = withContext(Dispatchers.IO) {
        if (itemIds.isNotEmpty() && newCategory.isNotBlank()) {
            db.menuDao().updateItemsCategory(itemIds, newCategory.trim())
        }
    }

    suspend fun deleteCategory(id: Long) = withContext(Dispatchers.IO) {
        val allCats = db.menuDao().getAllCategoriesList()
        val targetCat = allCats.find { it.id == id }
        val rawCatName = targetCat?.name?.trim() ?: ""
        val normTarget = normalizeArabicText(rawCatName)

        // 1. Delete by exact ID
        db.menuDao().deleteCategory(id)

        // 2. Delete all categories in database matching the normalized name or blank
        for (c in allCats) {
            val cNorm = normalizeArabicText(c.name)
            if (c.id == id || (normTarget.isNotBlank() && cNorm == normTarget) || c.name.isBlank()) {
                db.menuDao().deleteCategory(c.id)
            }
        }
        if (rawCatName.isNotBlank()) {
            try {
                db.openHelper.writableDatabase.execSQL("DELETE FROM menu_categories WHERE TRIM(name) = ?", arrayOf(rawCatName))
            } catch (_: Exception) {}
        }

        // 3. Remaining active categories after deletion
        val remainingActiveCats = db.menuDao().getAllCategoriesList().filter {
            !it.isArchived && normalizeArabicText(it.name) != normTarget && it.name.isNotBlank()
        }
        val fallbackCatName = remainingActiveCats.firstOrNull()?.name?.trim() ?: ""

        // 4. Update all menu items that used this deleted category to fallback category or blank
        val allItems = db.menuDao().getAllMenuItemsIncludingArchivedList()
        allItems.filter { normalizeArabicText(it.category) == normTarget || it.category.isBlank() }.forEach { itm ->
            db.menuDao().updateMenuItem(itm.copy(category = fallbackCatName))
        }
    }

    suspend fun savePreparation(prep: PreparationType): Long = withContext(Dispatchers.IO) {
        val cleanName = prep.name.trim()
        if (cleanName.isBlank()) return@withContext prep.id
        val allPreps = db.menuDao().getAllPreparationsList()
        val existing = allPreps.find { it.id == prep.id || normalizeArabicText(it.name) == normalizeArabicText(cleanName) }
        if (existing != null) {
            db.menuDao().updatePreparation(existing.copy(
                name = cleanName,
                additionalPricePerUnit = prep.additionalPricePerUnit,
                applicableCategory = prep.applicableCategory,
                isActive = prep.isActive
            ))
            existing.id
        } else {
            if (prep.id == 0L) db.menuDao().insertPreparation(prep.copy(name = cleanName))
            else {
                db.menuDao().updatePreparation(prep.copy(name = cleanName))
                prep.id
            }
        }
    }

    suspend fun deletePreparation(id: Long) = withContext(Dispatchers.IO) {
        val allPreps = db.menuDao().getAllPreparationsList()
        val targetPrep = allPreps.find { it.id == id }
        val rawPrepName = targetPrep?.name?.trim() ?: ""
        val normTarget = normalizeArabicText(rawPrepName)

        db.menuDao().deletePreparation(id)
        for (p in allPreps) {
            val pNorm = normalizeArabicText(p.name)
            if (p.id == id || (normTarget.isNotBlank() && pNorm == normTarget) || p.name.isBlank()) {
                db.menuDao().deletePreparation(p.id)
            }
        }
        if (rawPrepName.isNotBlank()) {
            try {
                db.openHelper.writableDatabase.execSQL("DELETE FROM preparation_types WHERE TRIM(name) = ?", arrayOf(rawPrepName))
            } catch (_: Exception) {}
        }
    }

    suspend fun saveDeliveryArea(area: DeliveryArea): Long = withContext(Dispatchers.IO) {
        if (area.id == 0L) {
            db.settingsDao().insertDeliveryArea(area)
        } else {
            db.settingsDao().updateDeliveryArea(area)
            area.id
        }
    }

    suspend fun deleteDeliveryArea(id: Long) = withContext(Dispatchers.IO) { db.settingsDao().deleteDeliveryArea(id) }

    suspend fun saveEmployee(emp: Employee): Long = withContext(Dispatchers.IO) {
        if (emp.id == 0L) {
            db.staffDao().insertEmployee(emp)
        } else {
            db.staffDao().updateEmployee(emp)
            emp.id
        }
    }

    suspend fun archiveEmployee(id: Long, archived: Boolean) = withContext(Dispatchers.IO) {
        db.staffDao().updateEmployeeArchived(id, archived)
    }

    suspend fun deleteEmployee(id: Long) = withContext(Dispatchers.IO) { db.staffDao().deleteEmployee(id) }

    suspend fun recordAttendance(attendance: EmployeeAttendance) = withContext(Dispatchers.IO) {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = attendance.date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        val existing = db.staffDao().getAttendanceForEmployeeToday(attendance.employeeId, startOfDay, endOfDay)
        if (existing != null) {
            db.staffDao().updateAttendance(attendance.copy(id = existing.id))
        } else {
            db.staffDao().insertAttendance(attendance)
        }
    }

    suspend fun recordEmployeeCheckIn(
        employeeId: Long,
        actualTimeString: String,
        delayMinutes: Int,
        lateAction: String?, // "PARDON", "PENALTY", "OWNER_NOTIFIED"
        penaltyAmount: Double,
        notes: String,
        date: Long
    ) = withContext(Dispatchers.IO) {
        val emp = db.staffDao().getEmployeeById(employeeId) ?: return@withContext
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        val status = if (delayMinutes > 0) "LATE" else "PRESENT"
        val existing = db.staffDao().getAttendanceForEmployeeToday(employeeId, startOfDay, endOfDay)
        val att = if (existing != null) {
            existing.copy(
                date = date,
                status = status,
                delayMinutes = delayMinutes,
                checkInTime = date,
                checkInTimeString = actualTimeString,
                lateActionTaken = lateAction,
                penaltyAmount = penaltyAmount,
                notes = if (notes.isNotBlank()) notes else existing.notes
            )
        } else {
            EmployeeAttendance(
                employeeId = employeeId,
                date = date,
                status = status,
                hoursWorked = emp.dailyWorkHours,
                delayMinutes = delayMinutes,
                checkInTime = date,
                checkInTimeString = actualTimeString,
                lateActionTaken = lateAction,
                penaltyAmount = penaltyAmount,
                notes = notes
            )
        }

        if (existing != null) {
            db.staffDao().updateAttendance(att)
        } else {
            db.staffDao().insertAttendance(att)
        }

        if (lateAction == "PENALTY" && penaltyAmount > 0) {
            val currentShift = db.settingsDao().getActiveShiftDirect()
            val tx = EmployeeTransaction(
                employeeId = employeeId,
                type = "DEDUCTION",
                amount = penaltyAmount,
                reason = "جزاء تأخير $delayMinutes دقيقة (وقت الحضور: $actualTimeString)",
                date = date,
                shiftId = currentShift?.id,
                isDeductedFromDrawer = false
            )
            db.staffDao().insertTransaction(tx)
        }
    }

    suspend fun recordEmployeeCheckOut(
        employeeId: Long,
        departureTimeString: String,
        actualHoursWorked: Double,
        overtimeHours: Double,
        notes: String,
        date: Long
    ) = withContext(Dispatchers.IO) {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        val existing = db.staffDao().getAttendanceForEmployeeToday(employeeId, startOfDay, endOfDay)
        if (existing != null) {
            val combinedNotes = if (notes.isNotBlank()) {
                if (existing.notes.isNotBlank()) "${existing.notes} | $notes" else notes
            } else existing.notes
            db.staffDao().updateAttendance(
                existing.copy(
                    checkOutTime = date,
                    checkOutTimeString = departureTimeString,
                    hoursWorked = actualHoursWorked,
                    overtimeHours = overtimeHours,
                    notes = combinedNotes
                )
            )
        } else {
            db.staffDao().insertAttendance(
                EmployeeAttendance(
                    employeeId = employeeId,
                    date = date,
                    status = "PRESENT",
                    hoursWorked = actualHoursWorked,
                    overtimeHours = overtimeHours,
                    checkOutTime = date,
                    checkOutTimeString = departureTimeString,
                    notes = notes
                )
            )
        }
    }

    suspend fun recordEmployeeAbsence(
        employeeId: Long,
        notes: String = "",
        date: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        val existing = db.staffDao().getAttendanceForEmployeeToday(employeeId, startOfDay, endOfDay)
        val att = if (existing != null) {
            existing.copy(
                status = "ABSENT",
                delayMinutes = 0,
                hoursWorked = 0.0,
                notes = if (notes.isNotBlank()) notes else "غياب مسجل لليوم"
            )
        } else {
            EmployeeAttendance(
                employeeId = employeeId,
                date = date,
                status = "ABSENT",
                hoursWorked = 0.0,
                delayMinutes = 0,
                notes = if (notes.isNotBlank()) notes else "غياب مسجل لليوم"
            )
        }
        if (existing != null) {
            db.staffDao().updateAttendance(att)
        } else {
            db.staffDao().insertAttendance(att)
        }
    }

    fun getTodayAttendance(): kotlinx.coroutines.flow.Flow<List<EmployeeAttendance>> {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis
        return db.staffDao().getAttendanceForToday(startOfDay, endOfDay)
    }

    suspend fun updateAttendance(attendance: EmployeeAttendance) = withContext(Dispatchers.IO) {
        db.staffDao().updateAttendance(attendance)
    }

    suspend fun deleteAttendance(id: Long) = withContext(Dispatchers.IO) {
        db.staffDao().deleteAttendance(id)
    }

    suspend fun recordEmployeeTransaction(tx: EmployeeTransaction) = withContext(Dispatchers.IO) {
        val currentShift = db.settingsDao().getActiveShiftDirect()
        val effectiveTx = if (tx.shiftId == null) tx.copy(shiftId = currentShift?.id) else tx
        db.staffDao().insertTransaction(effectiveTx)
    }

    suspend fun updateEmployeeTransaction(tx: EmployeeTransaction) = withContext(Dispatchers.IO) {
        db.staffDao().updateTransaction(tx)
    }

    suspend fun deleteEmployeeTransaction(id: Long) = withContext(Dispatchers.IO) {
        db.staffDao().deleteTransaction(id)
    }

    suspend fun saveDriver(driver: DeliveryDriver): Long = withContext(Dispatchers.IO) {
        if (driver.id == 0L) db.driverDao().insertDriver(driver)
        else { db.driverDao().updateDriver(driver); driver.id }
    }

    suspend fun archiveDriver(id: Long, archived: Boolean) = withContext(Dispatchers.IO) {
        db.driverDao().updateDriverArchived(id, archived)
    }

    suspend fun deleteDriver(id: Long) = withContext(Dispatchers.IO) { db.driverDao().deleteDriver(id) }

    suspend fun saveCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) db.customerDao().insertCustomer(customer)
        else { db.customerDao().updateCustomer(customer); customer.id }
    }

    suspend fun saveCustomerAddress(address: CustomerAddress): Long = withContext(Dispatchers.IO) {
        if (address.id == 0L) db.customerDao().insertAddress(address)
        else { db.customerDao().updateAddress(address); address.id }
    }

    suspend fun deleteCustomerAddress(id: Long) = withContext(Dispatchers.IO) {
        db.customerDao().deleteAddress(id)
    }

    suspend fun deleteCustomer(id: Long) = withContext(Dispatchers.IO) {
        db.customerDao().deleteCustomer(id)
    }

    suspend fun getCustomerByPhone(phone: String): Customer? = withContext(Dispatchers.IO) {
        db.customerDao().getCustomerByPhone(phone.trim())
    }

    suspend fun getOrderById(orderId: Long): OrderWithItems? = withContext(Dispatchers.IO) {
        db.orderDao().getOrderById(orderId)
    }

    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) { db.settingsDao().getSettingValue(key) }

    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        db.settingsDao().setSetting(AppSetting(key, value))
    }

    suspend fun verifyManagerPassword(input: String): Boolean = withContext(Dispatchers.IO) {
        val pass = db.settingsDao().getSettingValue("managerPassword") ?: "1234"
        input.trim() == pass.trim()
    }

    // General Accounts & Suppliers
    suspend fun saveAccount(account: GeneralAccount): Long = withContext(Dispatchers.IO) {
        if (account.id == 0L) {
            val initialBalance = account.openingBalance
            val newAcc = account.copy(currentBalance = initialBalance)
            db.accountDao().insertAccount(newAcc)
        } else {
            db.accountDao().updateAccount(account)
            recalculateAccountBalance(account.id)
            account.id
        }
    }

    suspend fun archiveAccount(id: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        db.accountDao().updateAccountArchived(id, isArchived)
    }

    suspend fun deleteAccount(id: Long) = withContext(Dispatchers.IO) {
        db.accountDao().deleteAccount(id)
    }

    suspend fun getAccountById(id: Long): GeneralAccount? = withContext(Dispatchers.IO) {
        db.accountDao().getAccountById(id)
    }

    suspend fun getTransactionsForAccount(accountId: Long): Flow<List<AccountTransaction>> = withContext(Dispatchers.IO) {
        db.accountDao().getTransactionsForAccount(accountId)
    }

    suspend fun getTransactionsListForAccount(accountId: Long): List<AccountTransaction> = withContext(Dispatchers.IO) {
        db.accountDao().getTransactionsListForAccount(accountId)
    }

    suspend fun recordAccountTransaction(tx: AccountTransaction): Long = withContext(Dispatchers.IO) {
        val currentShift = db.settingsDao().getActiveShiftDirect()
        val effectiveTx = if (tx.shiftId == null) tx.copy(shiftId = currentShift?.id) else tx
        val txId = db.accountDao().insertTransaction(effectiveTx)
        recalculateAccountBalance(effectiveTx.accountId)
        txId
    }

    suspend fun deleteAccountTransaction(txId: Long, accountId: Long) = withContext(Dispatchers.IO) {
        db.accountDao().deleteTransaction(txId)
        recalculateAccountBalance(accountId)
    }

    suspend fun recalculateAccountBalance(accountId: Long) = withContext(Dispatchers.IO) {
        val account = db.accountDao().getAccountById(accountId) ?: return@withContext
        val txs = db.accountDao().getTransactionsListForAccount(accountId)
        var running = account.openingBalance
        for (tx in txs) {
            when (tx.type) {
                "SUPPLY" -> running += tx.amount // توريد بضاعة: زيادة المستحق للمورد
                "PAYMENT" -> running -= tx.amount // سداد نقدية للمورد: تخفيض المستحق
                "RECEIPT" -> running -= tx.amount // استلام نقدية منه
                "RETURN" -> running -= tx.amount // مرتجع بضاعة
                "DISCOUNT" -> running -= tx.amount // تسوية أو خصم
                else -> running += tx.amount
            }
        }
        db.accountDao().updateAccountBalance(accountId, running)
    }

    suspend fun getAllAccountsList(): List<GeneralAccount> = withContext(Dispatchers.IO) {
        db.accountDao().getAllAccountsList()
    }

    suspend fun getAllMenuItemsList(): List<MenuItem> = withContext(Dispatchers.IO) {
        db.menuDao().getAllMenuItemsList()
    }

    suspend fun getAllMenuItemsIncludingArchivedList(): List<MenuItem> = withContext(Dispatchers.IO) {
        db.menuDao().getAllMenuItemsIncludingArchivedList()
    }

    suspend fun getAllCategoriesList(): List<MenuCategory> = withContext(Dispatchers.IO) {
        db.menuDao().getAllCategoriesList()
    }

    suspend fun getAllDeliveryAreasList(): List<DeliveryArea> = withContext(Dispatchers.IO) {
        db.settingsDao().getAllDeliveryAreasList()
    }

    suspend fun getAllPreparationsList(): List<PreparationType> = withContext(Dispatchers.IO) {
        db.menuDao().getAllPreparationsList()
    }

    suspend fun getAllDriversList(): List<DeliveryDriver> = withContext(Dispatchers.IO) {
        db.driverDao().getAllDriversList()
    }

    suspend fun getAllEmployeesList(): List<Employee> = withContext(Dispatchers.IO) {
        db.staffDao().getAllEmployeesList()
    }

    suspend fun getAllCustomersList(): List<Customer> = withContext(Dispatchers.IO) {
        db.customerDao().getAllCustomersList()
    }

    suspend fun getOrCreateDrawerExpensesAccount(): GeneralAccount = withContext(Dispatchers.IO) {
        val existing = db.accountDao().getAccountByName("مصروفات ونثريات المحل")
        if (existing != null) return@withContext existing
        val newAcc = GeneralAccount(
            name = "مصروفات ونثريات المحل",
            type = "MAINTENANCE",
            notes = "حساب افتراضي لتسجيل حركات الدرج النقدية السريعة والمصروفات اليومية"
        )
        val id = db.accountDao().insertAccount(newAcc)
        newAcc.copy(id = id)
    }

    suspend fun recordQuickDrawerTransaction(
        type: String, // "IN" (داخل للدرج) or "OUT" (خارج من الدرج)
        amount: Double,
        reason: String,
        accountId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val targetAccountId = accountId ?: getOrCreateDrawerExpensesAccount().id
        val currentShift = db.settingsDao().getActiveShiftDirect()
        val txType = if (type == "IN") "RECEIPT" else "PAYMENT"
        val effectiveReason = if (reason.isBlank()) {
            if (type == "IN") "توريد نقدية للدرج" else "مصروفات ونثريات عامة"
        } else reason.trim()

        val tx = AccountTransaction(
            accountId = targetAccountId,
            type = txType,
            amount = amount,
            description = effectiveReason,
            invoiceNumber = "حركة درج سريعة",
            date = System.currentTimeMillis(),
            isPaidFromDrawer = true,
            shiftId = currentShift?.id
        )
        val txId = db.accountDao().insertTransaction(tx)
        recalculateAccountBalance(targetAccountId)
        txId
    }

    suspend fun getOrdersForShift(shiftId: Long): List<OrderWithItems> = withContext(Dispatchers.IO) {
        db.orderDao().getOrdersListByShift(shiftId)
    }

    suspend fun getOrderByCloudId(cloudId: String): OrderWithItems? = withContext(Dispatchers.IO) {
        db.orderDao().getOrderByCloudId(cloudId)
    }

    suspend fun getOrderByCloudIdDirect(cloudId: String): OrderWithItems? = withContext(Dispatchers.IO) {
        if (cloudId.isBlank()) null else db.orderDao().getOrderByCloudId(cloudId)
    }

    suspend fun getPendingOfflineOrders(): List<OrderWithItems> = withContext(Dispatchers.IO) {
        db.orderDao().getPendingOfflineOrders()
    }

    suspend fun updateOrderSyncMetadata(
        orderId: Long,
        orderNumber: String,
        cloudOrderId: String?,
        syncState: String,
        pendingPrintAction: String,
        pendingPrintRequestedAtMs: Long
    ) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderSyncMetadata(
            orderId = orderId,
            orderNumber = orderNumber,
            cloudOrderId = cloudOrderId,
            syncState = syncState,
            pendingPrintAction = pendingPrintAction,
            pendingPrintRequestedAtMs = pendingPrintRequestedAtMs
        )
    }

    suspend fun updateOrderSyncState(
        orderId: Long,
        syncState: String,
        pendingPrintAction: String = "",
        pendingPrintRequestedAtMs: Long = 0L
    ) = withContext(Dispatchers.IO) {
        db.orderDao().updateOrderSyncState(
            orderId = orderId,
            syncState = syncState,
            pendingPrintAction = pendingPrintAction,
            pendingPrintRequestedAtMs = pendingPrintRequestedAtMs
        )
    }

    suspend fun getOrderByOrderNumber(orderNumber: String): OrderWithItems? = withContext(Dispatchers.IO) {
        db.orderDao().getOrderByOrderNumber(orderNumber)
    }

    suspend fun updateCloudOrderId(orderId: Long, cloudOrderId: String) = withContext(Dispatchers.IO) {
        db.orderDao().updateCloudOrderId(orderId, cloudOrderId)
    }

    suspend fun getAllOrdersDirect(): List<OrderWithItems> = withContext(Dispatchers.IO) {
        db.orderDao().getAllOrdersDirect()
    }



    /**
     * Safely merges ghost duplicate local orders created seconds apart with identical orderNumber and amount.
     */
    suspend fun deduplicateOrders() = withContext(Dispatchers.IO) {
        try {
            val refreshed = db.orderDao().getAllOrdersDirect()
            val byOrderNumber = refreshed
                .filter { it.order.syncState != "PENDING_OFFLINE" && it.order.orderNumber.trim().startsWith("#") }
                .groupBy { it.order.orderNumber.trim() }
            for ((num, group) in byOrderNumber) {
                if (num.isNotBlank() && group.size > 1) {
                    val sorted = group.sortedBy { it.order.id }
                    val keeper = sorted.first()
                    for (victim in sorted.drop(1)) {
                        val timeDiff = Math.abs(victim.order.createdAt - keeper.order.createdAt)
                        val isExactDuplicate = Math.abs(victim.order.totalAmount - keeper.order.totalAmount) < 0.01 &&
                                victim.order.customerName == keeper.order.customerName &&
                                victim.order.orderType == keeper.order.orderType &&
                                timeDiff < 2 * 3600 * 1000L
                        if (isExactDuplicate) {
                            if (victim.order.status == "COMPLETED" && keeper.order.status != "COMPLETED") {
                                db.orderDao().updateOrderStatus(keeper.order.id, "COMPLETED")
                            }
                            if (victim.order.isPaid && !keeper.order.isPaid) {
                                db.orderDao().updateOrderPayment(keeper.order.id, victim.order.paidAmount, victim.order.changeAmount, true)
                            }
                            db.orderDao().deleteOrder(victim.order.id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException || e is java.util.concurrent.CancellationException) {
                throw e
            }
            android.util.Log.e("RestaurantRepository", "deduplicateOrders error: ${e.message}")
        }
    }

    suspend fun clearSyncedCloudDataForRemoteRestore() = withContext(Dispatchers.IO) {
        val writableDb = db.openHelper.writableDatabase
        writableDb.execSQL("PRAGMA foreign_keys = OFF")
        writableDb.beginTransaction()
        try {
            val tablesToClear = listOf(
                "order_items",
                "orders",
                "shifts",
                "menu_item_ingredients",
                "menu_items",
                "menu_categories"
            )
            for (table in tablesToClear) {
                try {
                    writableDb.execSQL("DELETE FROM `$table`")
                } catch (_: Exception) {}
            }
            writableDb.setTransactionSuccessful()
        } finally {
            writableDb.endTransaction()
            writableDb.execSQL("PRAGMA foreign_keys = ON")
        }
        try {
            writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        } catch (_: Exception) {}
        try {
            db.invalidationTracker.refreshVersionsSync()
        } catch (_: Exception) {}
    }

    private fun getStartOfDayTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
