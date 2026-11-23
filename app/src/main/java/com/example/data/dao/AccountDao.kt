package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AccountTransaction
import com.example.data.model.GeneralAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM general_accounts WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<GeneralAccount>>

    @Query("SELECT * FROM general_accounts ORDER BY name ASC")
    fun getAllAccountsIncludingArchived(): Flow<List<GeneralAccount>>

    @Query("SELECT * FROM general_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): GeneralAccount?

    @Query("SELECT * FROM general_accounts WHERE name = :name LIMIT 1")
    suspend fun getAccountByName(name: String): GeneralAccount?

    @Query("SELECT * FROM general_accounts WHERE isArchived = 0 ORDER BY name ASC")
    suspend fun getAllAccountsList(): List<GeneralAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: GeneralAccount): Long

    @Update
    suspend fun updateAccount(account: GeneralAccount)

    @Query("UPDATE general_accounts SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateAccountArchived(id: Long, isArchived: Boolean)

    @Query("DELETE FROM general_accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)

    @Query("UPDATE general_accounts SET currentBalance = :newBalance WHERE id = :id")
    suspend fun updateAccountBalance(id: Long, newBalance: Double)

    // Transactions
    @Query("SELECT * FROM general_account_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM general_account_transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM general_account_transactions WHERE accountId = :accountId ORDER BY date ASC")
    suspend fun getTransactionsListForAccount(accountId: Long): List<AccountTransaction>

    @Query("SELECT * FROM general_account_transactions WHERE accountId = :accountId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getTransactionsListForAccountBetween(accountId: Long, startDate: Long, endDate: Long): List<AccountTransaction>

    @Query("SELECT * FROM general_account_transactions WHERE (shiftId = :shiftId OR (shiftId IS NULL AND date >= :startDate AND date <= :endDate)) AND isPaidFromDrawer = 1 ORDER BY date DESC")
    suspend fun getDrawerTransactionsForShift(shiftId: Long, startDate: Long, endDate: Long): List<AccountTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AccountTransaction): Long

    @Query("DELETE FROM general_account_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)
}
