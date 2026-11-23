package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSetting
import com.example.data.model.DeliveryArea
import com.example.data.model.PrintLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    // Print logs
    @Query("SELECT * FROM print_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllPrintLogs(): Flow<List<PrintLog>>

    @Query("SELECT * FROM print_logs WHERE orderId = :orderId")
    suspend fun getLogsForOrder(orderId: Long): List<PrintLog>

    @Query("SELECT COUNT(*) FROM print_logs WHERE orderId = :orderId AND printerTarget = :printerTarget")
    suspend fun countPrintLogsForTarget(orderId: Long, printerTarget: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrintLog(log: PrintLog): Long

    // Delivery Areas
    @Query("SELECT * FROM delivery_areas WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveDeliveryAreas(): Flow<List<DeliveryArea>>

    @Query("SELECT * FROM delivery_areas ORDER BY name ASC")
    fun getAllDeliveryAreas(): Flow<List<DeliveryArea>>

    @Query("SELECT * FROM delivery_areas")
    suspend fun getAllDeliveryAreasList(): List<DeliveryArea>

    @Query("SELECT * FROM delivery_areas WHERE id = :id LIMIT 1")
    suspend fun getDeliveryAreaById(id: Long): DeliveryArea?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryArea(area: DeliveryArea): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryAreas(areas: List<DeliveryArea>)

    @Update
    suspend fun updateDeliveryArea(area: DeliveryArea)

    @Query("DELETE FROM delivery_areas WHERE id = :id")
    suspend fun deleteDeliveryArea(id: Long)

    @Query("SELECT COUNT(*) FROM delivery_areas")
    suspend fun getDeliveryAreasCount(): Int

    // Shifts Management
    @Query("SELECT * FROM shifts WHERE isClosed = 0 ORDER BY id DESC LIMIT 1")
    fun getActiveShift(): Flow<com.example.data.model.Shift?>

    @Query("SELECT * FROM shifts WHERE isClosed = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveShiftDirect(): com.example.data.model.Shift?

    @Query("SELECT * FROM shifts ORDER BY id DESC")
    fun getAllShifts(): Flow<List<com.example.data.model.Shift>>

    @Query("SELECT * FROM shifts ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentShiftsDirect(limit: Int = 30): List<com.example.data.model.Shift>

    @Query("SELECT * FROM shifts WHERE shiftNumber = :shiftNumber ORDER BY id DESC LIMIT 1")
    suspend fun getShiftByNumber(shiftNumber: Int): com.example.data.model.Shift?

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): com.example.data.model.Shift?

    @Query("SELECT * FROM shifts ORDER BY id DESC LIMIT 1")
    suspend fun getLastShift(): com.example.data.model.Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: com.example.data.model.Shift): Long

    @Update
    suspend fun updateShift(shift: com.example.data.model.Shift)
}
