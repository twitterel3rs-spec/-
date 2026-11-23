package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Employee
import com.example.data.model.EmployeeAttendance
import com.example.data.model.EmployeeTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM employees WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE isArchived = 0 ORDER BY name ASC")
    suspend fun getAllEmployeesList(): List<Employee>

    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployeesIncludingArchived(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE isActive = 1 AND isArchived = 0 ORDER BY name ASC")
    fun getActiveEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Query("UPDATE employees SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateEmployeeArchived(id: Long, isArchived: Boolean)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployee(id: Long)

    // Attendance
    @Query("SELECT * FROM employee_attendance ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<EmployeeAttendance>>

    @Query("SELECT * FROM employee_attendance WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getAttendanceForEmployee(employeeId: Long): Flow<List<EmployeeAttendance>>

    @Query("SELECT * FROM employee_attendance WHERE employeeId = :employeeId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getAttendanceListForEmployeeBetween(employeeId: Long, startDate: Long, endDate: Long): List<EmployeeAttendance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: EmployeeAttendance): Long

    @Update
    suspend fun updateAttendance(attendance: EmployeeAttendance)

    @Query("DELETE FROM employee_attendance WHERE id = :id")
    suspend fun deleteAttendance(id: Long)

    @Query("SELECT * FROM employee_attendance WHERE employeeId = :employeeId AND date >= :startOfDay AND date <= :endOfDay LIMIT 1")
    suspend fun getAttendanceForEmployeeToday(employeeId: Long, startOfDay: Long, endOfDay: Long): EmployeeAttendance?

    @Query("SELECT * FROM employee_attendance WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY date DESC")
    fun getAttendanceForToday(startOfDay: Long, endOfDay: Long): Flow<List<EmployeeAttendance>>

    // Transactions
    @Query("SELECT * FROM employee_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<EmployeeTransaction>>

    @Query("SELECT * FROM employee_transactions WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getTransactionsForEmployee(employeeId: Long): Flow<List<EmployeeTransaction>>

    @Query("SELECT * FROM employee_transactions WHERE employeeId = :employeeId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getTransactionsListForEmployeeBetween(employeeId: Long, startDate: Long, endDate: Long): List<EmployeeTransaction>

    @Query("SELECT * FROM employee_transactions WHERE (shiftId = :shiftId OR (shiftId IS NULL AND date >= :startDate AND date <= :endDate)) AND isDeductedFromDrawer = 1 ORDER BY date DESC")
    suspend fun getDrawerTransactionsForShift(shiftId: Long, startDate: Long, endDate: Long): List<EmployeeTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: EmployeeTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: EmployeeTransaction)

    @Query("DELETE FROM employee_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)
}
