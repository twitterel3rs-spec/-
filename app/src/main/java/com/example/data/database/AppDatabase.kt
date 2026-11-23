package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CustomerDao
import com.example.data.dao.DriverDao
import com.example.data.dao.InventoryDao
import com.example.data.dao.MenuDao
import com.example.data.dao.OrderDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.StaffDao
import com.example.data.model.AppSetting
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
import com.example.data.model.PreparationType
import com.example.data.dao.AccountDao
import com.example.data.model.AccountTransaction
import com.example.data.model.GeneralAccount
import com.example.data.model.PrintLog
import com.example.data.model.Shift

@Database(
    entities = [
        MenuCategory::class,
        MenuItem::class,
        MenuItemIngredient::class,
        PreparationType::class,
        Customer::class,
        CustomerAddress::class,
        DeliveryArea::class,
        Order::class,
        OrderItem::class,
        InventoryMovement::class,
        DeliveryDriver::class,
        DriverSettlement::class,
        Employee::class,
        EmployeeTransaction::class,
        EmployeeAttendance::class,
        PrintLog::class,
        AppSetting::class,
        Shift::class,
        GeneralAccount::class,
        AccountTransaction::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun menuDao(): MenuDao
    abstract fun orderDao(): OrderDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun driverDao(): DriverDao
    abstract fun staffDao(): StaffDao
    abstract fun settingsDao(): SettingsDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE driver_settlements ADD COLUMN shiftId INTEGER")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE employee_transactions ADD COLUMN isDeductedFromDrawer INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE employee_transactions ADD COLUMN shiftId INTEGER")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE general_account_transactions ADD COLUMN isPaidFromDrawer INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE general_account_transactions ADD COLUMN shiftId INTEGER")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE shifts ADD COLUMN totalStoreCashSales REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE shifts ADD COLUMN totalDeliverySettlementCash REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE shifts ADD COLUMN totalStaffWithdrawals REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE shifts ADD COLUMN totalSupplierPayments REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE shifts ADD COLUMN totalOtherExpenses REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE order_items ADD COLUMN printedKitchenQuantity REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("UPDATE order_items SET printedKitchenQuantity = quantity WHERE printedInKitchen = 1")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE menu_items ADD COLUMN isPlateWeightEnabled INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE menu_items ADD COLUMN plateWeightKg REAL NOT NULL DEFAULT 0.5")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE menu_items ADD COLUMN pricePerKg REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN changeAmount REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN sellerName TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN cloudOrderId TEXT")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN deviceRole TEXT NOT NULL DEFAULT 'CASHIER'")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE menu_items ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE menu_items ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    database.execSQL("UPDATE menu_items SET cloudId = 'item_' || id WHERE cloudId = '' OR cloudId IS NULL")
                } catch (_: Exception) {}
                try {
                    database.execSQL("UPDATE menu_items SET updatedAtMillis = ${System.currentTimeMillis()} WHERE updatedAtMillis = 0")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN pendingPrintAction TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN pendingPrintRequestedAtMs INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sennara_shabaka_pos.db"
                )
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
