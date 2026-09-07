package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE partyId = :partyId ORDER BY dateMillis DESC")
    fun getExpensesByParty(partyId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE partyId = :partyId ORDER BY dateMillis DESC")
    suspend fun getExpensesByPartySync(partyId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses WHERE payerName = 'Dibyajyoti'")
    suspend fun cleanLegacyExpenses()

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}
