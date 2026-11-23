package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.InventoryMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<InventoryMovement>>

    @Query("SELECT * FROM inventory_movements WHERE menuItemId = :menuItemId ORDER BY timestamp DESC")
    fun getMovementsForItem(menuItemId: Long): Flow<List<InventoryMovement>>

    @Query("SELECT * FROM inventory_movements WHERE orderId = :orderId")
    suspend fun getMovementsForOrder(orderId: Long): List<InventoryMovement>

    @Query("SELECT COUNT(*) FROM inventory_movements WHERE orderId = :orderId")
    suspend fun hasMovementsForOrder(orderId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: InventoryMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<InventoryMovement>)

    @Query("DELETE FROM inventory_movements WHERE orderId = :orderId")
    suspend fun deleteMovementsForOrder(orderId: Long)
}
