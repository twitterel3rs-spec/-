package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.CustomerAddress
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY totalOrdersCount DESC, createdAt DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY totalOrdersCount DESC, createdAt DESC")
    suspend fun getAllCustomersList(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE primaryPhone = :phone OR secondaryPhone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR primaryPhone LIKE '%' || :query || '%' OR (secondaryPhone IS NOT NULL AND secondaryPhone LIKE '%' || :query || '%')")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)

    @Query("""
        UPDATE customers SET 
        totalOrdersCount = (
            SELECT COUNT(*) FROM orders 
            WHERE (orders.customerId IS NOT NULL AND orders.customerId = customers.id)
               OR (TRIM(customers.primaryPhone) != '' AND (TRIM(orders.customerPhone) = TRIM(customers.primaryPhone) OR (orders.customerSecondaryPhone IS NOT NULL AND TRIM(orders.customerSecondaryPhone) = TRIM(customers.primaryPhone))))
               OR (customers.secondaryPhone IS NOT NULL AND TRIM(customers.secondaryPhone) != '' AND (TRIM(orders.customerPhone) = TRIM(customers.secondaryPhone) OR (orders.customerSecondaryPhone IS NOT NULL AND TRIM(orders.customerSecondaryPhone) = TRIM(customers.secondaryPhone))))
        ),
        totalSpent = COALESCE((
            SELECT SUM(totalAmount) FROM orders 
            WHERE ((orders.customerId IS NOT NULL AND orders.customerId = customers.id)
               OR (TRIM(customers.primaryPhone) != '' AND (TRIM(orders.customerPhone) = TRIM(customers.primaryPhone) OR (orders.customerSecondaryPhone IS NOT NULL AND TRIM(orders.customerSecondaryPhone) = TRIM(customers.primaryPhone))))
               OR (customers.secondaryPhone IS NOT NULL AND TRIM(customers.secondaryPhone) != '' AND (TRIM(orders.customerPhone) = TRIM(customers.secondaryPhone) OR (orders.customerSecondaryPhone IS NOT NULL AND TRIM(orders.customerSecondaryPhone) = TRIM(customers.secondaryPhone))))
            ) AND orders.status != 'CANCELLED'
        ), 0.0)
    """)
    suspend fun syncAllCustomerOrderStats()

    @Query("""
        UPDATE customers SET 
        totalOrdersCount = (
            SELECT COUNT(*) FROM orders 
            WHERE (orders.customerId = :customerId)
               OR (TRIM(customers.primaryPhone) != '' AND (TRIM(orders.customerPhone) = TRIM(customers.primaryPhone) OR (orders.customerSecondaryPhone IS NOT NULL AND TRIM(orders.customerSecondaryPhone) = TRIM(customers.primaryPhone))))
               OR (customers.secondaryPhone IS NOT NULL AND TRIM(customers.secondaryPhone) != '' AND (TRIM(orders.customerPhone) = TRIM(customers.secondaryPhone) OR (orders.customerSecondaryPhone IS NOT NULL AND TRIM(orders.customerSecondaryPhone) = TRIM(customers.secondaryPhone))))
        ),
        totalSpent = COALESCE((
            SELECT SUM(totalAmount) FROM orders 
            WHERE ((orders.customerId = :customerId)
               OR (TRIM(customers.primaryPhone) != '' AND (TRIM(orders.customerPhone) = TRIM(customers.primaryPhone) OR (orders.customerSecondaryPhone IS NOT NULL AND TRIM(orders.customerSecondaryPhone) = TRIM(customers.primaryPhone))))
               OR (customers.secondaryPhone IS NOT NULL AND TRIM(customers.secondaryPhone) != '' AND (TRIM(orders.customerPhone) = TRIM(customers.secondaryPhone) OR (orders.customerSecondaryPhone IS NOT NULL AND TRIM(orders.customerSecondaryPhone) = TRIM(customers.secondaryPhone))))
            ) AND orders.status != 'CANCELLED'
        ), 0.0)
        WHERE id = :customerId
    """)
    suspend fun syncCustomerStats(customerId: Long)

    // Customer Addresses
    @Query("SELECT * FROM customer_addresses WHERE customerId = :customerId ORDER BY isDefault DESC, id ASC")
    fun getAddressesForCustomer(customerId: Long): Flow<List<CustomerAddress>>

    @Query("SELECT * FROM customer_addresses WHERE customerId = :customerId")
    suspend fun getAddressesListForCustomer(customerId: Long): List<CustomerAddress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: CustomerAddress): Long

    @Update
    suspend fun updateAddress(address: CustomerAddress)

    @Query("DELETE FROM customer_addresses WHERE id = :id")
    suspend fun deleteAddress(id: Long)

    @Query("UPDATE customer_addresses SET isDefault = 0 WHERE customerId = :customerId")
    suspend fun resetDefaultAddress(customerId: Long)
}
