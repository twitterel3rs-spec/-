package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.PreparationType
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    // Categories
    @Query("SELECT * FROM menu_categories WHERE (isArchived = 0 OR isArchived IS NULL) ORDER BY displayOrder ASC, name ASC")
    fun getAllActiveCategories(): Flow<List<MenuCategory>>

    @Query("SELECT * FROM menu_categories ORDER BY displayOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<MenuCategory>>

    @Query("SELECT * FROM menu_categories ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllCategoriesList(): List<MenuCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: MenuCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<MenuCategory>)

    @Update
    suspend fun updateCategory(category: MenuCategory)

    @Query("DELETE FROM menu_categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("DELETE FROM menu_categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun deleteCategoriesByName(name: String)

    @Query("SELECT COUNT(*) FROM menu_categories")
    suspend fun getCategoriesCount(): Int

    // Menu Items
    @Query("SELECT * FROM menu_items WHERE (isArchived = 0 OR isArchived IS NULL) ORDER BY displayOrder ASC, name ASC")
    fun getAllMenuItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE (isArchived = 0 OR isArchived IS NULL) ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllMenuItemsList(): List<MenuItem>

    @Query("SELECT * FROM menu_items ORDER BY displayOrder ASC, name ASC")
    fun getAllMenuItemsIncludingArchived(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllMenuItemsIncludingArchivedList(): List<MenuItem>

    @Query("SELECT * FROM menu_items WHERE (isAvailable = 1 OR isAvailable IS NULL) AND (isArchived = 0 OR isArchived IS NULL) ORDER BY name ASC")
    fun getAvailableMenuItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE category = :category AND (isArchived = 0 OR isArchived IS NULL) ORDER BY name ASC")
    fun getItemsByCategory(category: String): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): MenuItem?

    @Query("SELECT * FROM menu_items WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getItemByCloudId(cloudId: String): MenuItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItem>)

    @Update
    suspend fun updateMenuItem(item: MenuItem)

    @Update
    suspend fun updateMenuItems(items: List<MenuItem>)

    @Query("UPDATE menu_items SET cloudId = :cloudId WHERE id = :id")
    suspend fun updateItemCloudId(id: Long, cloudId: String)

    @Query("UPDATE menu_items SET basePrice = :newPrice, updatedAtMillis = :now WHERE id = :id")
    suspend fun updateItemPrice(id: Long, newPrice: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE menu_items SET currentStock = :newStock, updatedAtMillis = :now WHERE id = :id")
    suspend fun updateItemStock(id: Long, newStock: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE menu_items SET isArchived = :isArchived, updatedAtMillis = :now WHERE id = :id")
    suspend fun updateItemArchived(id: Long, isArchived: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE menu_items SET isAvailable = :isAvailable, updatedAtMillis = :now WHERE id = :id")
    suspend fun updateItemAvailability(id: Long, isAvailable: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE menu_items SET category = :category WHERE id IN (:itemIds)")
    suspend fun updateItemsCategory(itemIds: List<Long>, category: String)

    @Query("DELETE FROM menu_items WHERE id = :id")
    suspend fun deleteMenuItem(id: Long)

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun getCount(): Int

    // Preparation Types
    @Query("SELECT * FROM preparation_types WHERE isActive = 1 ORDER BY id ASC")
    fun getAllPreparations(): Flow<List<PreparationType>>

    @Query("SELECT * FROM preparation_types ORDER BY id ASC")
    fun getAllPreparationsIncludingInactive(): Flow<List<PreparationType>>

    @Query("SELECT * FROM preparation_types ORDER BY id ASC")
    suspend fun getAllPreparationsList(): List<PreparationType>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreparation(prep: PreparationType): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreparations(preps: List<PreparationType>)

    @Update
    suspend fun updatePreparation(prep: PreparationType)

    @Query("DELETE FROM preparation_types WHERE id = :id")
    suspend fun deletePreparation(id: Long)

    // Recipe / Item Ingredients (BOM)
    @Query("SELECT * FROM menu_item_ingredients WHERE menuItemId = :menuItemId ORDER BY id ASC")
    fun getIngredientsForItem(menuItemId: Long): Flow<List<com.example.data.model.MenuItemIngredient>>

    @Query("SELECT * FROM menu_item_ingredients WHERE menuItemId = :menuItemId ORDER BY id ASC")
    suspend fun getIngredientsListForItem(menuItemId: Long): List<com.example.data.model.MenuItemIngredient>

    @Query("SELECT * FROM menu_item_ingredients ORDER BY id ASC")
    fun getAllIngredients(): Flow<List<com.example.data.model.MenuItemIngredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: com.example.data.model.MenuItemIngredient): Long

    @Query("DELETE FROM menu_item_ingredients WHERE id = :id")
    suspend fun deleteIngredient(id: Long)

    @Query("DELETE FROM menu_item_ingredients WHERE menuItemId = :menuItemId")
    suspend fun deleteIngredientsForItem(menuItemId: Long)
}
