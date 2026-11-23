package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DeliveryDriver
import com.example.data.model.DriverSettlement
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {
    @Query("SELECT * FROM delivery_drivers WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllDrivers(): Flow<List<DeliveryDriver>>

    @Query("SELECT * FROM delivery_drivers WHERE isArchived = 0 ORDER BY name ASC")
    suspend fun getAllDriversList(): List<DeliveryDriver>

    @Query("SELECT * FROM delivery_drivers ORDER BY name ASC")
    fun getAllDriversIncludingArchived(): Flow<List<DeliveryDriver>>

    @Query("SELECT * FROM delivery_drivers WHERE isActive = 1 AND isArchived = 0 ORDER BY name ASC")
    fun getActiveDrivers(): Flow<List<DeliveryDriver>>

    @Query("SELECT * FROM delivery_drivers WHERE id = :id LIMIT 1")
    suspend fun getDriverById(id: Long): DeliveryDriver?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DeliveryDriver): Long

    @Update
    suspend fun updateDriver(driver: DeliveryDriver)

    @Query("UPDATE delivery_drivers SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateDriverArchived(id: Long, isArchived: Boolean)

    @Query("DELETE FROM delivery_drivers WHERE id = :id")
    suspend fun deleteDriver(id: Long)

    // Settlements
    @Query("SELECT * FROM driver_settlements ORDER BY date DESC")
    fun getAllSettlements(): Flow<List<DriverSettlement>>

    @Query("SELECT * FROM driver_settlements WHERE driverId = :driverId ORDER BY date DESC")
    fun getSettlementsForDriver(driverId: Long): Flow<List<DriverSettlement>>

    @Query("SELECT * FROM driver_settlements WHERE driverId = :driverId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getSettlementsForDriverBetween(driverId: Long, startDate: Long, endDate: Long): List<DriverSettlement>

    @Query("SELECT * FROM driver_settlements WHERE shiftId = :shiftId OR (shiftId IS NULL AND date >= :startDate AND date <= :endDate) ORDER BY date DESC")
    suspend fun getSettlementsForShift(shiftId: Long, startDate: Long, endDate: Long): List<DriverSettlement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: DriverSettlement): Long

    @Update
    suspend fun updateSettlement(settlement: DriverSettlement)
}
