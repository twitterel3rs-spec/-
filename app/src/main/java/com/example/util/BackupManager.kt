package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupStats(
    val timestamp: Long,
    val formattedDate: String,
    val fileSizeFormatted: String,
    val totalRecords: Int,
    val dbFilePath: String,
    val jsonFilePath: String,
    val tableCounts: Map<String, Int>
)

data class BackupDiffReport(
    val totalBackupRecords: Int,
    val tablesCount: Int,
    val preservedNewerOrdersCount: Int,
    val preservedNewerMovementsCount: Int,
    val safetySnapshotPath: String,
    val tableCounts: Map<String, Int>,
    val detailsMessage: String
)

object BackupManager {
    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "sennara_backup_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_LAST_BACKUP_DATE = "last_backup_date"
    private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
    private const val KEY_LAST_BACKUP_RECORDS = "last_backup_records"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getLastBackupInfo(context: Context): Map<String, String> {
        val prefs = getPrefs(context)
        return mapOf(
            "date" to (prefs.getString(KEY_LAST_BACKUP_DATE, null) ?: "لم يتم بعد"),
            "size" to (prefs.getString(KEY_LAST_BACKUP_SIZE, null) ?: "0 ك.ب"),
            "records" to (prefs.getString(KEY_LAST_BACKUP_RECORDS, null) ?: "0 سجل"),
            "time" to (prefs.getLong(KEY_LAST_BACKUP_TIME, 0L).toString())
        )
    }

    /**
     * Checks if a daily backup is needed (> 24 hours since last backup) and executes it automatically.
     */
    suspend fun checkAndPerformDailyAutoBackup(context: Context): BackupStats? {
        if (!isAutoBackupEnabled(context)) return null
        val prefs = getPrefs(context)
        val lastTime = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        if (now - lastTime >= oneDayMillis || lastTime == 0L) {
            Log.d(TAG, "Triggering automatic daily backup...")
            return try {
                val stats = performFullBackup(context)
                Log.d(TAG, "Automatic daily backup succeeded: ${stats.formattedDate}")
                stats
            } catch (e: Exception) {
                Log.e(TAG, "Automatic daily backup failed", e)
                null
            }
        }
        return null
    }

    val orderedTablesForRestore = listOf(
        "app_settings",
        "menu_categories",
        "preparation_types",
        "menu_items",
        "customers",
        "delivery_areas",
        "delivery_drivers",
        "employees",
        "general_accounts",
        "shifts",
        "orders",
        "menu_item_ingredients",
        "customer_addresses",
        "driver_settlements",
        "employee_attendance",
        "employee_transactions",
        "order_items",
        "general_account_transactions",
        "inventory_movements",
        "print_logs"
    )

    data class TableColumnInfo(
        val name: String,
        val type: String,
        val isNotNull: Boolean,
        val defaultValue: String?
    )

    private val tableAliases = mapOf(
        "orders" to listOf("orders", "order", "orders_table", "tbl_orders", "orderentity", "pos_orders"),
        "order_items" to listOf("order_items", "order_item", "orderitems", "orderitem", "tbl_order_items", "orderitementity"),
        "menu_items" to listOf("menu_items", "menu_item", "menuitems", "menuitem", "items", "item", "tbl_menu_items"),
        "menu_categories" to listOf("menu_categories", "menu_category", "menucategories", "categories", "category", "tbl_categories"),
        "customers" to listOf("customers", "customer", "tbl_customers", "customerentity"),
        "customer_addresses" to listOf("customer_addresses", "customer_address", "customeraddresses", "addresses", "tbl_addresses"),
        "delivery_drivers" to listOf("delivery_drivers", "delivery_driver", "deliverydrivers", "drivers", "driver", "tbl_drivers"),
        "delivery_areas" to listOf("delivery_areas", "delivery_area", "deliveryareas", "areas", "area", "tbl_delivery_areas"),
        "employees" to listOf("employees", "employee", "staff", "staff_members", "tbl_employees"),
        "employee_attendance" to listOf("employee_attendance", "employeeattendance", "attendance", "tbl_attendance"),
        "employee_transactions" to listOf("employee_transactions", "employeetransactions", "staff_transactions", "stafftransactions"),
        "driver_settlements" to listOf("driver_settlements", "driversettlements", "settlements"),
        "shifts" to listOf("shifts", "shift", "tbl_shifts"),
        "general_accounts" to listOf("general_accounts", "general_account", "generalaccounts", "accounts", "account", "tbl_accounts"),
        "general_account_transactions" to listOf("general_account_transactions", "account_transactions", "accounttransactions", "tbl_account_transactions"),
        "preparation_types" to listOf("preparation_types", "preparation_type", "preparations", "preparation"),
        "menu_item_ingredients" to listOf("menu_item_ingredients", "menuitemingredients", "ingredients", "ingredient"),
        "inventory_movements" to listOf("inventory_movements", "inventorymovements", "stock_movements"),
        "print_logs" to listOf("print_logs", "printlogs"),
        "app_settings" to listOf("app_settings", "appsettings", "settings")
    )

    fun mapSourceTableToTarget(sourceTableName: String): String? {
        val clean = sourceTableName.replace("`", "").replace("'", "").trim().lowercase(Locale.ROOT)
        for ((targetTable, aliases) in tableAliases) {
            if (clean == targetTable || clean in aliases) {
                return targetTable
            }
        }
        return if (clean in orderedTablesForRestore) clean else null
    }

    private fun getTableColumnDetails(db: androidx.sqlite.db.SupportSQLiteDatabase, tableName: String): Map<String, TableColumnInfo> {
        val details = mutableMapOf<String, TableColumnInfo>()
        try {
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                val typeIdx = cursor.getColumnIndex("type")
                val notNullIdx = cursor.getColumnIndex("notnull")
                val dfltIdx = cursor.getColumnIndex("dflt_value")
                while (cursor.moveToNext()) {
                    if (nameIdx >= 0) {
                        val name = cursor.getString(nameIdx)
                        val type = if (typeIdx >= 0) cursor.getString(typeIdx).uppercase(Locale.ROOT) else "TEXT"
                        val isNotNull = if (notNullIdx >= 0) cursor.getInt(notNullIdx) == 1 else false
                        val dflt = if (dfltIdx >= 0) cursor.getString(dfltIdx) else null
                        details[name] = TableColumnInfo(name, type, isNotNull, dflt)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching columns for $tableName: ${e.message}")
        }
        return details
    }

    private fun applyDefensiveDefaultsForTargetTable(
        tableName: String,
        cv: android.content.ContentValues,
        targetColumns: Map<String, TableColumnInfo>
    ) {
        for ((colName, colInfo) in targetColumns) {
            val isMissing = !cv.containsKey(colName) || cv.get(colName) == null
            if (colInfo.isNotNull && isMissing) {
                when (tableName) {
                    "orders" -> when (colName) {
                        "sellerName" -> cv.put(colName, "")
                        "deviceRole" -> cv.put(colName, "CASHIER")
                        "orderNumber" -> cv.put(colName, "#${cv.getAsLong("id") ?: (System.currentTimeMillis() % 10000)}")
                        "orderType" -> cv.put(colName, "DELIVERY")
                        "paymentMethod" -> cv.put(colName, "CASH")
                        "status" -> cv.put(colName, "NEW")
                        "notes" -> cv.put(colName, "")
                        "customerName" -> cv.put(colName, "")
                        "customerPhone" -> cv.put(colName, "")
                        "isArchived" -> cv.put(colName, 0)
                        "paidAmount" -> cv.put(colName, cv.getAsDouble("totalAmount") ?: 0.0)
                        "changeAmount" -> cv.put(colName, 0.0)
                        "isPaid" -> cv.put(colName, 0)
                        "isSettledWithDriver" -> cv.put(colName, 0)
                        "isExcludedFromDriverSettlement" -> cv.put(colName, 0)
                        "overridePasswordUsed" -> cv.put(colName, 0)
                        "printedMain" -> cv.put(colName, 0)
                        "printedKitchen1" -> cv.put(colName, 0)
                        "printedKitchen2" -> cv.put(colName, 0)
                        "subtotal" -> cv.put(colName, 0.0)
                        "discount" -> cv.put(colName, 0.0)
                        "totalAmount" -> cv.put(colName, 0.0)
                        "deliveryFee" -> cv.put(colName, 0.0)
                        "driverCommission" -> cv.put(colName, 0.0)
                        "createdAt" -> cv.put(colName, System.currentTimeMillis())
                        else -> putGenericDefault(cv, colName, colInfo)
                    }
                    "order_items" -> when (colName) {
                        "itemName" -> cv.put(colName, "صنف")
                        "kitchenName" -> cv.put(colName, cv.getAsString("itemName") ?: "")
                        "category" -> cv.put(colName, "فريش")
                        "unit" -> cv.put(colName, "كيلو")
                        "quantity" -> cv.put(colName, 1.0)
                        "unitPrice" -> cv.put(colName, 0.0)
                        "totalPrice" -> cv.put(colName, 0.0)
                        "preparationPrice" -> cv.put(colName, 0.0)
                        "itemNotes" -> cv.put(colName, "")
                        "targetPrinter" -> cv.put(colName, "MAIN")
                        "printedInKitchen" -> cv.put(colName, 0)
                        "printedKitchenQuantity" -> cv.put(colName, 0.0)
                        "isCustomAdHoc" -> cv.put(colName, 0)
                        else -> putGenericDefault(cv, colName, colInfo)
                    }
                    "menu_items" -> when (colName) {
                        "name" -> cv.put(colName, "صنف")
                        "category" -> cv.put(colName, "فريش")
                        "unit" -> cv.put(colName, "كيلو")
                        "basePrice" -> cv.put(colName, 0.0)
                        "currentStock" -> cv.put(colName, 0.0)
                        "minStockAlert" -> cv.put(colName, 3.0)
                        "costPrice" -> cv.put(colName, 0.0)
                        "isAvailable" -> cv.put(colName, 1)
                        "isArchived" -> cv.put(colName, 0)
                        "isPlateWeightEnabled" -> cv.put(colName, 0)
                        "plateWeightKg" -> cv.put(colName, 0.5)
                        "pricePerKg" -> cv.put(colName, 0.0)
                        "displayOrder" -> cv.put(colName, 0)
                        "kitchenName" -> cv.put(colName, "")
                        "targetPrinter" -> cv.put(colName, "MAIN")
                        "cloudId" -> cv.put(colName, "item_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16))
                        "updatedAtMillis" -> cv.put(colName, System.currentTimeMillis())
                        else -> putGenericDefault(cv, colName, colInfo)
                    }
                    "customers" -> when (colName) {
                        "name" -> cv.put(colName, "عميل")
                        "primaryPhone" -> cv.put(colName, "")
                        "notes" -> cv.put(colName, "")
                        "totalOrdersCount" -> cv.put(colName, 0)
                        "totalSpent" -> cv.put(colName, 0.0)
                        "createdAt" -> cv.put(colName, System.currentTimeMillis())
                        else -> putGenericDefault(cv, colName, colInfo)
                    }
                    "menu_categories" -> when (colName) {
                        "name" -> cv.put(colName, "تصنيف")
                        "isArchived" -> cv.put(colName, 0)
                        "displayOrder" -> cv.put(colName, 0)
                        else -> putGenericDefault(cv, colName, colInfo)
                    }
                    else -> putGenericDefault(cv, colName, colInfo)
                }
            }
        }
    }

    private fun putGenericDefault(cv: android.content.ContentValues, colName: String, colInfo: TableColumnInfo) {
        val t = colInfo.type
        when {
            t.contains("INT") -> cv.put(colName, 0)
            t.contains("REAL") || t.contains("FLOAT") || t.contains("DOUB") -> cv.put(colName, 0.0)
            t.contains("BLOB") -> cv.put(colName, ByteArray(0))
            else -> cv.put(colName, "")
        }
    }

    /**
     * Performs a complete, 100% authentic database backup:
     * 1. Checkpoints and truncates the SQLite WAL log to ensure raw DB file has all data.
     * 2. Copies the raw .db file.
     * 3. Dumps all tables into a structured JSON archive.
     */
    suspend fun performFullBackup(context: Context): BackupStats = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val writableDb = db.openHelper.writableDatabase

        // Flush WAL completely so that sennara_shabaka_pos.db file on disk contains all current records
        try {
            writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                if (cursor.moveToFirst()) {
                    Log.d(TAG, "wal_checkpoint TRUNCATE completed successfully")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "wal_checkpoint warning: ${e.message}")
        }

        val readableDb = db.openHelper.readableDatabase
        val backupsDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val dailyDir = File(backupsDir, "daily").apply { if (!exists()) mkdirs() }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
        val timestampStr = dateFormat.format(Date())
        val displayFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
        val displayDate = displayFormat.format(Date())

        val dbFile = context.getDatabasePath("sennara_shabaka_pos.db")
        val targetDbFile = File(dailyDir, "sennara_shabaka_pos_backup_$timestampStr.db")
        val targetJsonFile = File(dailyDir, "sennara_shabaka_pos_backup_$timestampStr.json")

        // 1. Copy raw SQLite database file
        if (dbFile.exists()) {
            FileInputStream(dbFile).use { input ->
                FileOutputStream(targetDbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        // 2. Dump all tables into structured JSON
        val rootJson = JSONObject()
        rootJson.put("app_name", "صنارة وشبكة موبايل")
        rootJson.put("backup_type", "FULL_SYSTEM_SNAPSHOT")
        rootJson.put("created_at", System.currentTimeMillis())
        rootJson.put("created_date", displayDate)
        rootJson.put("version", "2.0")

        val tablesObject = JSONObject()
        val tableCounts = mutableMapOf<String, Int>()
        var totalRecords = 0

        val tablesCursor = readableDb.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'"
        )

        val tableNames = mutableListOf<String>()
        while (tablesCursor.moveToNext()) {
            tableNames.add(tablesCursor.getString(0))
        }
        tablesCursor.close()

        for (table in tableNames) {
            val rowsArray = JSONArray()
            val rowCursor = readableDb.query("SELECT * FROM `$table`")
            val colCount = rowCursor.columnCount
            val colNames = rowCursor.columnNames

            var count = 0
            while (rowCursor.moveToNext()) {
                val rowObj = JSONObject()
                for (i in 0 until colCount) {
                    val colName = colNames[i]
                    when (rowCursor.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> rowObj.put(colName, JSONObject.NULL)
                        android.database.Cursor.FIELD_TYPE_INTEGER -> rowObj.put(colName, rowCursor.getLong(i))
                        android.database.Cursor.FIELD_TYPE_FLOAT -> rowObj.put(colName, rowCursor.getDouble(i))
                        android.database.Cursor.FIELD_TYPE_STRING -> rowObj.put(colName, rowCursor.getString(i))
                        android.database.Cursor.FIELD_TYPE_BLOB -> {
                            val blob = rowCursor.getBlob(i)
                            rowObj.put(colName, android.util.Base64.encodeToString(blob, android.util.Base64.NO_WRAP))
                        }
                    }
                }
                rowsArray.put(rowObj)
                count++
            }
            rowCursor.close()

            tablesObject.put(table, rowsArray)
            tableCounts[table] = count
            totalRecords += count
        }

        rootJson.put("tables", tablesObject)
        rootJson.put("total_records", totalRecords)

        targetJsonFile.writeText(rootJson.toString(2))

        val totalBytes = targetDbFile.length() + targetJsonFile.length()
        val sizeFormatted = when {
            totalBytes >= 1024 * 1024 -> String.format(Locale.US, "%.2f ميجابايت", totalBytes.toDouble() / (1024 * 1024))
            else -> String.format(Locale.US, "%.1f كيلوبايت", totalBytes.toDouble() / 1024)
        }

        // Save last backup info in SharedPreferences
        getPrefs(context).edit()
            .putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_BACKUP_DATE, displayDate)
            .putString(KEY_LAST_BACKUP_SIZE, sizeFormatted)
            .putString(KEY_LAST_BACKUP_RECORDS, "$totalRecords سجل")
            .apply()

        // Clean up old daily backups keeping the latest 14
        cleanOldBackups(dailyDir, keepCount = 14)

        BackupStats(
            timestamp = System.currentTimeMillis(),
            formattedDate = displayDate,
            fileSizeFormatted = sizeFormatted,
            totalRecords = totalRecords,
            dbFilePath = targetDbFile.absolutePath,
            jsonFilePath = targetJsonFile.absolutePath,
            tableCounts = tableCounts
        )
    }

    private fun cleanOldBackups(directory: File, keepCount: Int) {
        val files = directory.listFiles() ?: return
        if (files.size > keepCount) {
            files.sortBy { it.lastModified() }
            val deleteCount = files.size - keepCount
            for (i in 0 until deleteCount) {
                try {
                    files[i].delete()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Builds an Intent to share / save the backup file.
     * Uses system chooser to allow saving to Google Drive, Local Storage, or any app.
     */
    fun createBackupShareIntent(context: Context, backupFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (backupFile.name.endsWith(".json")) "application/json" else "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية لقاعدة بيانات مطعم صنارة وشبكة - ${backupFile.name}")
            putExtra(
                Intent.EXTRA_TEXT,
                "النسخة الاحتياطية الرسمية والشاملة لقاعدة بيانات تطبيق صنارة وشبكة موبايل.\nالملف: ${backupFile.name}\nالحجم: ${backupFile.length() / 1024} ك.ب"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(intent, "حفظ أو مشاركة النسخة الاحتياطية")
    }

    /**
     * Builds an Intent specifically for uploading the backup file directly to Google Drive.
     * If the official Google Drive application is installed, it targets it directly; otherwise falls back to chooser.
     */
    fun createGoogleDriveUploadIntent(context: Context, backupFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val driveIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (backupFile.name.endsWith(".json")) "application/json" else "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة قاعدة بيانات مطعم صنارة وشبكة - ${backupFile.name}")
            putExtra(
                Intent.EXTRA_TEXT,
                "نسخة احتياطية لقاعدة بيانات مطعم صنارة وشبكة محفوظة في Google Drive.\nالملف: ${backupFile.name}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.apps.docs")
        }

        val pm = context.packageManager
        val canHandleDrive = try {
            driveIntent.resolveActivity(pm) != null
        } catch (_: Exception) {
            false
        }

        return if (canHandleDrive) {
            driveIntent
        } else {
            createBackupShareIntent(context, backupFile)
        }
    }


    private val restoreMutex = kotlinx.coroutines.sync.Mutex()

    fun performSafetySnapshotBeforeRestore(context: Context): String {
        return try {
            val db = AppDatabase.getInstance(context)
            val writableDb = db.openHelper.writableDatabase
            try {
                writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            } catch (_: Exception) {}

            val dbFile = context.getDatabasePath("sennara_shabaka_pos.db")
            if (dbFile.exists()) {
                val snapshotsDir = File(context.filesDir, "backups/pre_restore_snapshots").apply { if (!exists()) mkdirs() }
                val target = File(snapshotsDir, "pre_restore_snapshot_${System.currentTimeMillis()}.db")
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Safety snapshot before restore created at ${target.absolutePath}")
                target.absolutePath
            } else ""
        } catch (e: Exception) {
            Log.w(TAG, "Failed creating pre-restore safety snapshot: ${e.message}")
            ""
        }
    }

    fun analyzeBackupDiff(context: Context, jsonString: String): BackupDiffReport {
        if (jsonString.isBlank()) {
            return BackupDiffReport(0, 0, 0, 0, "", emptyMap(), "الملف فارغ")
        }
        return try {
            val root = JSONObject(jsonString)
            val tablesObj = root.optJSONObject("tables")
                ?: root.optJSONObject("data")
                ?: root.optJSONObject("database")
                ?: root

            var totalValidRowsInFile = 0
            val tableCounts = mutableMapOf<String, Int>()
            val keysIter = tablesObj.keys()
            var maxBackupOrderTime = 0L

            while (keysIter.hasNext()) {
                val key = keysIter.next()
                val target = mapSourceTableToTarget(key)
                if (target != null) {
                    val arr = tablesObj.optJSONArray(key)
                    if (arr != null && arr.length() > 0) {
                        tableCounts[target] = arr.length()
                        totalValidRowsInFile += arr.length()
                        if (target == "orders") {
                            for (i in 0 until arr.length()) {
                                val o = arr.optJSONObject(i) ?: continue
                                val t = o.optLong("createdAt", 0L)
                                if (t > maxBackupOrderTime) maxBackupOrderTime = t
                            }
                        }
                    }
                }
            }

            var preservedOrders = 0
            var preservedMovements = 0
            if (maxBackupOrderTime > 0L) {
                try {
                    val db = AppDatabase.getInstance(context).openHelper.readableDatabase
                    db.query("SELECT COUNT(*) FROM orders WHERE createdAt > $maxBackupOrderTime").use { c ->
                        if (c.moveToFirst()) preservedOrders = c.getInt(0)
                    }
                    db.query("SELECT COUNT(*) FROM inventory_movements WHERE timestamp > $maxBackupOrderTime").use { c ->
                        if (c.moveToFirst()) preservedMovements = c.getInt(0)
                    }
                } catch (_: Exception) {}
            }

            val summary = "تقرير فحص النسخة قبل الاستعادة:\n" +
                "• إجمالي السجلات بالنسخة: $totalValidRowsInFile سجل موزعة على ${tableCounts.size} جدول.\n" +
                (if (preservedOrders > 0) "• حماية وعدم محو: $preservedOrders أوردرات أحدث من تاريخ النسخة.\n" else "") +
                (if (preservedMovements > 0) "• حماية وعدم محو: $preservedMovements حركة مخزون أحدث من تاريخ النسخة.\n" else "") +
                "• سيتم حفظ نقطة رجوع كاملة (Snapshot) بقاعدة البيانات الحالية قبل الاستبدال."

            BackupDiffReport(
                totalBackupRecords = totalValidRowsInFile,
                tablesCount = tableCounts.size,
                preservedNewerOrdersCount = preservedOrders,
                preservedNewerMovementsCount = preservedMovements,
                safetySnapshotPath = "",
                tableCounts = tableCounts,
                detailsMessage = summary
            )
        } catch (e: Exception) {
            BackupDiffReport(0, 0, 0, 0, "", emptyMap(), "خطأ في قراءة ملف JSON: ${e.message}")
        }
    }

    /**
     * Restores all tables from a structured JSON backup string.
     * Uses strict dependency ordering (children deleted before parents; parents restored before children),
     * table column filtering to prevent schema mismatch errors, defensive default values,
     * concurrency protection, and pre-validation so current data is never wiped on bad input.
     */
    suspend fun restoreDatabaseFromJson(
        context: Context,
        jsonString: String,
        preserveNewerRecords: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!restoreMutex.tryLock()) {
            return@withContext Result.failure(Exception("عملية استعادة أخرى قيد التنفيذ بالفعل، يرجى الانتظار."))
        }
        try {
            if (jsonString.isBlank()) {
                return@withContext Result.failure(Exception("ملف النسخة الاحتياطية فارغ، تم إلغاء الاستعادة لحماية البيانات الحالية."))
            }

            val root = try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("صيغة ملف JSON غير صالحة: ${e.message}"))
            }

            val tablesObj = root.optJSONObject("tables")
                ?: root.optJSONObject("data")
                ?: root.optJSONObject("database")
                ?: root

            // Discover all available table keys in the JSON before touching current database
            val jsonKeyToTargetMap = mutableMapOf<String, String>()
            val keysIter = tablesObj.keys()
            var totalValidRowsInFile = 0
            while (keysIter.hasNext()) {
                val key = keysIter.next()
                val target = mapSourceTableToTarget(key)
                if (target != null) {
                    val arr = tablesObj.optJSONArray(key)
                    if (arr != null && arr.length() > 0) {
                        jsonKeyToTargetMap[key] = target
                        totalValidRowsInFile += arr.length()
                    }
                }
            }

            if (totalValidRowsInFile == 0) {
                return@withContext Result.failure(Exception("ملف النسخة الاحتياطية لا يحتوي على جداول أو سجلات صالحة للاستعادة، تم إلغاء العملية لحماية البيانات."))
            }

            // Detect and log intra-file duplicates for orders table if present
            val ordersSourceKey = jsonKeyToTargetMap.entries.firstOrNull { it.value == "orders" }?.key
            val deduplicatedOrdersArray = if (ordersSourceKey != null) {
                val rawOrders = tablesObj.optJSONArray(ordersSourceKey)
                if (rawOrders != null && rawOrders.length() > 0) {
                    val seenOrderIds = mutableSetOf<Long>()
                    val seenOrderNumbers = mutableSetOf<String>()
                    val cleanOrders = JSONArray()
                    var duplicateCount = 0

                    for (i in 0 until rawOrders.length()) {
                        val row = rawOrders.optJSONObject(i) ?: continue
                        val idVal = row.optLong("id", 0L)
                        val orderNum = row.optString("orderNumber", "")

                        val isDuplicate = (idVal > 0 && seenOrderIds.contains(idVal)) ||
                            (orderNum.isNotBlank() && seenOrderNumbers.contains(orderNum))

                        if (isDuplicate) {
                            duplicateCount++
                            Log.w(TAG, "Intra-file duplicate order detected in backup JSON: id=$idVal, num=$orderNum. Skipping duplicate entry.")
                        } else {
                            if (idVal > 0) seenOrderIds.add(idVal)
                            if (orderNum.isNotBlank()) seenOrderNumbers.add(orderNum)
                            cleanOrders.put(row)
                        }
                    }
                    if (duplicateCount > 0) {
                        Log.i(TAG, "Filtered out $duplicateCount intra-file duplicate orders from backup JSON before restore.")
                    }
                    cleanOrders
                } else null
            } else null

            // Discover max timestamp in backup file to protect newer local data from being erased
            var maxBackupOrderTime = 0L
            if (deduplicatedOrdersArray != null) {
                for (i in 0 until deduplicatedOrdersArray.length()) {
                    val o = deduplicatedOrdersArray.optJSONObject(i) ?: continue
                    val t = o.optLong("createdAt", 0L)
                    if (t > maxBackupOrderTime) maxBackupOrderTime = t
                }
            }

            // Take safety snapshot of current database before any wipe or modification
            performSafetySnapshotBeforeRestore(context)

            val db = AppDatabase.getInstance(context)
            val writableDb = db.openHelper.writableDatabase

            // Backup Preservation: Collect newer local orders and inventory movements that postdate the backup file
            val preservedNewerOrders = mutableListOf<android.content.ContentValues>()
            val preservedNewerOrderItems = mutableListOf<android.content.ContentValues>()
            val preservedNewerMovements = mutableListOf<android.content.ContentValues>()

            if (preserveNewerRecords && maxBackupOrderTime > 0L) {
                try {
                    writableDb.query("SELECT * FROM orders WHERE createdAt > $maxBackupOrderTime").use { c ->
                        val colNames = c.columnNames
                        while (c.moveToNext()) {
                            val cv = android.content.ContentValues()
                            for (i in 0 until c.columnCount) {
                                val col = colNames[i]
                                when (c.getType(i)) {
                                    android.database.Cursor.FIELD_TYPE_INTEGER -> cv.put(col, c.getLong(i))
                                    android.database.Cursor.FIELD_TYPE_FLOAT -> cv.put(col, c.getDouble(i))
                                    android.database.Cursor.FIELD_TYPE_STRING -> cv.put(col, c.getString(i))
                                    else -> {}
                                }
                            }
                            preservedNewerOrders.add(cv)
                        }
                    }
                    if (preservedNewerOrders.isNotEmpty()) {
                        val idsStr = preservedNewerOrders.mapNotNull { it.getAsLong("id") }.joinToString(",")
                        if (idsStr.isNotBlank()) {
                            writableDb.query("SELECT * FROM order_items WHERE orderId IN ($idsStr)").use { c ->
                                val colNames = c.columnNames
                                while (c.moveToNext()) {
                                    val cv = android.content.ContentValues()
                                    for (i in 0 until c.columnCount) {
                                        val col = colNames[i]
                                        when (c.getType(i)) {
                                            android.database.Cursor.FIELD_TYPE_INTEGER -> cv.put(col, c.getLong(i))
                                            android.database.Cursor.FIELD_TYPE_FLOAT -> cv.put(col, c.getDouble(i))
                                            android.database.Cursor.FIELD_TYPE_STRING -> cv.put(col, c.getString(i))
                                            else -> {}
                                        }
                                    }
                                    preservedNewerOrderItems.add(cv)
                                }
                            }
                        }
                    }
                    writableDb.query("SELECT * FROM inventory_movements WHERE timestamp > $maxBackupOrderTime").use { c ->
                        val colNames = c.columnNames
                        while (c.moveToNext()) {
                            val cv = android.content.ContentValues()
                            for (i in 0 until c.columnCount) {
                                val col = colNames[i]
                                when (c.getType(i)) {
                                    android.database.Cursor.FIELD_TYPE_INTEGER -> cv.put(col, c.getLong(i))
                                    android.database.Cursor.FIELD_TYPE_FLOAT -> cv.put(col, c.getDouble(i))
                                    android.database.Cursor.FIELD_TYPE_STRING -> cv.put(col, c.getString(i))
                                    else -> {}
                                }
                            }
                            preservedNewerMovements.add(cv)
                        }
                    }
                    if (preservedNewerOrders.isNotEmpty() || preservedNewerMovements.isNotEmpty()) {
                        Log.i(TAG, "Authority Protection: Preserving ${preservedNewerOrders.size} newer orders and ${preservedNewerMovements.size} newer inventory movements from being overwritten by older backup.")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed collecting newer records for preservation: ${e.message}")
                }
            }

            // Read current next_order_number so we never regress the sequence
            val currentMaxNext = try {
                writableDb.query("SELECT CAST(value AS INTEGER) FROM app_settings WHERE `key` = 'next_order_number'").use { c ->
                    if (c.moveToFirst()) c.getInt(0) else 1
                }
            } catch (_: Exception) { 1 }

            var restoredCount = 0

            writableDb.execSQL("PRAGMA foreign_keys = OFF")
            writableDb.beginTransaction()
            try {
                // 1. Delete in reverse dependency order: children first, parents last
                for (tableName in orderedTablesForRestore.reversed()) {
                    try {
                        writableDb.execSQL("DELETE FROM `$tableName`")
                    } catch (_: Exception) {}
                }

                // 2. Restore tables in forward dependency order: parents first, children last
                for (targetTable in orderedTablesForRestore) {
                    val sourceKey = jsonKeyToTargetMap.entries.firstOrNull { it.value == targetTable }?.key ?: continue
                    val rowsArray = if (targetTable == "orders" && deduplicatedOrdersArray != null) {
                        deduplicatedOrdersArray
                    } else {
                        tablesObj.optJSONArray(sourceKey) ?: continue
                    }
                    val targetColumns = getTableColumnDetails(writableDb, targetTable)
                    if (targetColumns.isEmpty()) continue

                    val normalizedColMap = targetColumns.values.associateBy { it.name.replace("_", "").lowercase(Locale.ROOT) }

                    for (i in 0 until rowsArray.length()) {
                        val row = rowsArray.optJSONObject(i) ?: continue
                        val cv = android.content.ContentValues()
                        val colKeys = row.keys()

                        while (colKeys.hasNext()) {
                            val srcCol = colKeys.next()
                            val normKey = srcCol.replace("_", "").lowercase(Locale.ROOT)
                            val targetCol = normalizedColMap[normKey] ?: continue

                            if (row.isNull(srcCol)) {
                                if (targetCol.isNotNull) {
                                    // Skip putNull so defensive default will supply valid value
                                } else {
                                    cv.putNull(targetCol.name)
                                }
                            } else {
                                when (val v = row.get(srcCol)) {
                                    is Int -> cv.put(targetCol.name, v)
                                    is Long -> cv.put(targetCol.name, v)
                                    is Double -> cv.put(targetCol.name, v)
                                    is Boolean -> cv.put(targetCol.name, if (v) 1 else 0)
                                    is String -> {
                                        val t = targetCol.type
                                        when {
                                            t.contains("INT") -> cv.put(targetCol.name, v.toLongOrNull() ?: if (v.equals("true", true)) 1L else 0L)
                                            t.contains("REAL") || t.contains("FLOAT") || t.contains("DOUB") -> cv.put(targetCol.name, v.toDoubleOrNull() ?: 0.0)
                                            else -> cv.put(targetCol.name, v)
                                        }
                                    }
                                    else -> cv.put(targetCol.name, v.toString())
                                }
                            }
                        }

                        // Apply defensive defaults for all required target columns
                        applyDefensiveDefaultsForTargetTable(targetTable, cv, targetColumns)

                        val rowId = writableDb.insert(targetTable, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                        if (rowId != -1L) {
                            restoredCount++
                        } else {
                            Log.w(TAG, "Failed inserting row in $targetTable during JSON restore")
                        }
                    }
                }

                // 3. Defensive post-restore updates for clean state without altering user order statuses
                try {
                    writableDb.execSQL("UPDATE menu_items SET isArchived = 0 WHERE isArchived IS NULL")
                    writableDb.execSQL("UPDATE menu_items SET isAvailable = 1 WHERE isAvailable IS NULL")
                    writableDb.execSQL("UPDATE menu_items SET updatedAtMillis = ${System.currentTimeMillis()}")
                    writableDb.execSQL("UPDATE menu_items SET cloudId = 'item_' || lower(hex(randomblob(8))) WHERE cloudId IS NULL OR TRIM(cloudId) = '' OR cloudId = 'item_unknown'")
                    writableDb.execSQL("UPDATE menu_categories SET isArchived = 0 WHERE isArchived IS NULL")
                    val restoredNextFromSettings = try {
                        writableDb.query("SELECT CAST(value AS INTEGER) FROM app_settings WHERE `key` = 'next_order_number'").use { c ->
                            if (c.moveToFirst()) c.getInt(0) else 0
                        }
                    } catch (_: Exception) { 0 }
                    writableDb.query("SELECT MAX(CAST(REPLACE(orderNumber, '#', '') AS INTEGER)) FROM orders").use { c ->
                        if (c.moveToFirst()) {
                            val maxNum = c.getInt(0)
                            val updatedNext = if (preserveNewerRecords) {
                                maxOf(maxNum + 1, currentMaxNext)
                            } else {
                                maxOf(restoredNextFromSettings, maxNum + 1).coerceAtLeast(1)
                            }
                            if (updatedNext >= 1) {
                                writableDb.execSQL(
                                    "INSERT OR REPLACE INTO app_settings (`key`, `value`) VALUES ('next_order_number', '$updatedNext')"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Defensive post-restore update warning: ${e.message}")
                }

                // 4. Re-insert preserved newer local records (orders, items, movements) that postdate the backup file
                if (preservedNewerOrders.isNotEmpty()) {
                    for (cv in preservedNewerOrders) {
                        writableDb.insert("orders", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                        restoredCount++
                    }
                }
                if (preservedNewerOrderItems.isNotEmpty()) {
                    for (cv in preservedNewerOrderItems) {
                        writableDb.insert("order_items", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                    }
                }
                if (preservedNewerMovements.isNotEmpty()) {
                    for (cv in preservedNewerMovements) {
                        writableDb.insert("inventory_movements", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                    }
                }

                writableDb.setTransactionSuccessful()
            } finally {
                writableDb.endTransaction()
                writableDb.execSQL("PRAGMA foreign_keys = ON")
            }

            // Flush WAL and force Room InvalidationTracker to notify all active Flow observers immediately
            try {
                writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            } catch (_: Exception) {}

            try {
                db.invalidationTracker.refreshVersionsSync()
            } catch (e: Exception) {
                Log.w(TAG, "invalidationTracker sync warning: ${e.message}")
            }

            // Sync stats
            try {
                db.customerDao().syncAllCustomerOrderStats()
            } catch (_: Exception) {}

            Result.success(restoredCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from JSON backup", e)
            Result.failure(e)
        } finally {
            restoreMutex.unlock()
        }
    }

    /**
     * Restores backup directly from any Uri (Google Drive or Local File).
     * Autodetects SQLite binary database file (.db) or JSON backup text (.json).
     * Guarantees parent-first restoration, schema mismatch safety, and Room observer invalidation.
     */
    suspend fun restoreDatabaseFromUri(
        context: Context,
        uri: Uri,
        preserveNewerRecords: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
            ?: return@withContext Result.failure(Exception("تعذر فتح ملف النسخة الاحتياطية المختار"))

        val headerBytes = ByteArray(64)
        val bytesRead = inputStream.read(headerBytes)
        val headerString = if (bytesRead > 0) String(headerBytes, 0, bytesRead) else ""

        if (headerString.startsWith("SQLite format 3")) {
            if (!restoreMutex.tryLock()) {
                inputStream.close()
                return@withContext Result.failure(Exception("عملية استعادة أخرى قيد التنفيذ بالفعل، يرجى الانتظار."))
            }
            val tempFile = File(context.cacheDir, "imported_restore_temp.db")
            try {
                FileOutputStream(tempFile).use { out ->
                    if (bytesRead > 0) {
                        out.write(headerBytes, 0, bytesRead)
                    }
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } > 0) {
                        out.write(buffer, 0, len)
                    }
                }
                inputStream.close()

                val sourceDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                    tempFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )

                // Pre-validate source database content before touching active data
                val sourceTablesInDb = mutableListOf<String>()
                sourceDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'", null).use { sc ->
                    while (sc.moveToNext()) {
                        sourceTablesInDb.add(sc.getString(0))
                    }
                }

                val sourceToTargetMap = mutableMapOf<String, String>()
                var totalAvailableRecords = 0
                for (srcName in sourceTablesInDb) {
                    val tgt = mapSourceTableToTarget(srcName)
                    if (tgt != null) {
                        sourceToTargetMap[srcName] = tgt
                        try {
                            sourceDb.rawQuery("SELECT COUNT(*) FROM `$srcName`", null).use { cntCursor ->
                                if (cntCursor.moveToFirst()) {
                                    totalAvailableRecords += cntCursor.getInt(0)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                if (totalAvailableRecords == 0) {
                    sourceDb.close()
                    tempFile.delete()
                    return@withContext Result.failure(Exception("ملف قاعدة البيانات المختار فارغ أو لا يحتوي على سجلات صالحة، تم إلغاء الاستعادة لحماية البيانات."))
                }

                // Take safety snapshot of current database before wipe
                performSafetySnapshotBeforeRestore(context)

                var restoredTotal = 0
                val targetDb = AppDatabase.getInstance(context).openHelper.writableDatabase

                val currentMaxNext = try {
                    targetDb.query("SELECT CAST(value AS INTEGER) FROM app_settings WHERE `key` = 'next_order_number'").use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 1
                    }
                } catch (_: Exception) { 1 }

                targetDb.execSQL("PRAGMA foreign_keys = OFF")
                targetDb.beginTransaction()
                try {
                    // 1. Delete in reverse dependency order: children first, parents last
                    for (table in orderedTablesForRestore.reversed()) {
                        try {
                            targetDb.execSQL("DELETE FROM `$table`")
                        } catch (_: Exception) {}
                    }

                    // 2. Restore in forward dependency order: parents first, children last
                    for (targetTable in orderedTablesForRestore) {
                        val sourceTable = sourceToTargetMap.entries.firstOrNull { it.value == targetTable }?.key ?: continue
                        try {
                            val targetColumns = getTableColumnDetails(targetDb, targetTable)
                            if (targetColumns.isEmpty()) continue

                            val normalizedColMap = targetColumns.values.associateBy { it.name.replace("_", "").lowercase(Locale.ROOT) }

                            sourceDb.rawQuery("SELECT * FROM `$sourceTable`", null).use { c ->
                                val colCount = c.columnCount
                                val colNames = c.columnNames
                                while (c.moveToNext()) {
                                    val cv = android.content.ContentValues()
                                    for (i in 0 until colCount) {
                                        val srcCol = colNames[i]
                                        val normKey = srcCol.replace("_", "").lowercase(Locale.ROOT)
                                        val targetCol = normalizedColMap[normKey] ?: continue

                                        when (c.getType(i)) {
                                            android.database.Cursor.FIELD_TYPE_NULL -> {
                                                if (!targetCol.isNotNull) {
                                                    cv.putNull(targetCol.name)
                                                }
                                            }
                                            android.database.Cursor.FIELD_TYPE_INTEGER -> {
                                                val v = c.getLong(i)
                                                val t = targetCol.type
                                                if (t.contains("TEXT")) {
                                                    cv.put(targetCol.name, v.toString())
                                                } else {
                                                    cv.put(targetCol.name, v)
                                                }
                                            }
                                            android.database.Cursor.FIELD_TYPE_FLOAT -> {
                                                val v = c.getDouble(i)
                                                val t = targetCol.type
                                                if (t.contains("TEXT")) {
                                                    cv.put(targetCol.name, v.toString())
                                                } else {
                                                    cv.put(targetCol.name, v)
                                                }
                                            }
                                            android.database.Cursor.FIELD_TYPE_STRING -> {
                                                val v = c.getString(i)
                                                val t = targetCol.type
                                                when {
                                                    t.contains("INT") -> cv.put(targetCol.name, v.toLongOrNull() ?: if (v.equals("true", true)) 1L else 0L)
                                                    t.contains("REAL") || t.contains("FLOAT") || t.contains("DOUB") -> cv.put(targetCol.name, v.toDoubleOrNull() ?: 0.0)
                                                    else -> cv.put(targetCol.name, v)
                                                }
                                            }
                                            android.database.Cursor.FIELD_TYPE_BLOB -> {
                                                cv.put(targetCol.name, c.getBlob(i))
                                            }
                                        }
                                    }

                                    // Apply defensive defaults for all required target columns
                                    applyDefensiveDefaultsForTargetTable(targetTable, cv, targetColumns)

                                    val rowId = targetDb.insert(targetTable, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                                    if (rowId != -1L) {
                                        restoredTotal++
                                    } else {
                                        Log.w(TAG, "Failed inserting row in $targetTable during SQLite restore")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error restoring table $targetTable from $sourceTable: ${e.message}")
                        }
                    }

                    // Defensive updates: ensure valid non-null states without changing orders archive status
                    try {
                        targetDb.execSQL("UPDATE menu_items SET isArchived = 0 WHERE isArchived IS NULL")
                        targetDb.execSQL("UPDATE menu_items SET isAvailable = 1 WHERE isAvailable IS NULL")
                        targetDb.execSQL("UPDATE menu_items SET updatedAtMillis = ${System.currentTimeMillis()}")
                        targetDb.execSQL("UPDATE menu_items SET cloudId = 'item_' || lower(hex(randomblob(8))) WHERE cloudId IS NULL OR TRIM(cloudId) = '' OR cloudId = 'item_unknown'")
                        targetDb.execSQL("UPDATE menu_categories SET isArchived = 0 WHERE isArchived IS NULL")
                        val restoredNextFromSettings = try {
                            targetDb.query("SELECT CAST(value AS INTEGER) FROM app_settings WHERE `key` = 'next_order_number'").use { c ->
                                if (c.moveToFirst()) c.getInt(0) else 0
                            }
                        } catch (_: Exception) { 0 }
                        targetDb.query("SELECT MAX(CAST(REPLACE(orderNumber, '#', '') AS INTEGER)) FROM orders").use { c ->
                            if (c.moveToFirst()) {
                                val maxNum = c.getInt(0)
                                val updatedNext = if (preserveNewerRecords) {
                                    maxOf(maxNum + 1, currentMaxNext)
                                } else {
                                    maxOf(restoredNextFromSettings, maxNum + 1).coerceAtLeast(1)
                                }
                                if (updatedNext >= 1) {
                                    targetDb.execSQL(
                                        "INSERT OR REPLACE INTO app_settings (`key`, `value`) VALUES ('next_order_number', '$updatedNext')"
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    targetDb.setTransactionSuccessful()
                } finally {
                    targetDb.endTransaction()
                    targetDb.execSQL("PRAGMA foreign_keys = ON")
                    sourceDb.close()
                    tempFile.delete()
                }

                // Checkpoint WAL and invalidate Room cached Flow states
                try {
                    targetDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
                } catch (_: Exception) {}

                try {
                    AppDatabase.getInstance(context).invalidationTracker.refreshVersionsSync()
                } catch (e: Exception) {
                    Log.w(TAG, "invalidationTracker sync error: ${e.message}")
                }

                try {
                    AppDatabase.getInstance(context).customerDao().syncAllCustomerOrderStats()
                } catch (_: Exception) {}

                Result.success(restoredTotal)
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring from SQLite file", e)
                Result.failure(Exception("فشل استرجاع قاعدة البيانات: ${e.message}"))
            } finally {
                restoreMutex.unlock()
            }
        } else {
            val baos = java.io.ByteArrayOutputStream()
            if (bytesRead > 0) {
                baos.write(headerBytes, 0, bytesRead)
            }
            val buffer = ByteArray(8192)
            var len: Int
            while (inputStream.read(buffer).also { len = it } > 0) {
                baos.write(buffer, 0, len)
            }
            inputStream.close()
            val jsonString = baos.toString("UTF-8")
            restoreDatabaseFromJson(context, jsonString, preserveNewerRecords = preserveNewerRecords)
        }
    }

    /**
     * Inspects the database and returns live counts of all supported entities to verify 100% backup coverage.
     */
    suspend fun getBackupVerificationSummary(context: Context): Map<String, Int> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Int>()
        try {
            val db = AppDatabase.getInstance(context)
            val readableDb = db.openHelper.readableDatabase

            fun queryCount(table: String): Int {
                return try {
                    val c = readableDb.query("SELECT COUNT(*) FROM `$table`")
                    c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
                } catch (_: Exception) { 0 }
            }

            // Printers count from app_settings
            var printersCount = 0
            try {
                val c = readableDb.query("SELECT value FROM app_settings WHERE `key` = 'printers_list_json'")
                c.use {
                    if (it.moveToFirst()) {
                        val jsonStr = it.getString(0)
                        if (!jsonStr.isNullOrBlank()) {
                            printersCount = JSONArray(jsonStr).length()
                        }
                    }
                }
            } catch (_: Exception) {}

            result["printers"] = printersCount
            result["general_accounts"] = queryCount("general_accounts")
            result["account_transactions"] = queryCount("general_account_transactions")
            result["employees"] = queryCount("employees")
            result["employee_attendance"] = queryCount("employee_attendance")
            result["employee_transactions"] = queryCount("employee_transactions")
            result["delivery_drivers"] = queryCount("delivery_drivers")
            result["driver_settlements"] = queryCount("driver_settlements")
            result["customers"] = queryCount("customers")
            result["orders"] = queryCount("orders")
            result["order_items"] = queryCount("order_items")
            result["menu_items"] = queryCount("menu_items")
            result["menu_categories"] = queryCount("menu_categories")
            result["preparation_types"] = queryCount("preparation_types")
            result["shifts"] = queryCount("shifts")
            result["inventory_movements"] = queryCount("inventory_movements")
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating backup verification summary", e)
        }
        result
    }
}

