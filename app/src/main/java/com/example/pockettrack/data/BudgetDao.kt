package com.example.pockettrack.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun getByMonth(month: String): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets")
    fun getAll(): LiveData<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(b: Budget)

    @Update
    suspend fun update(b: Budget)

    @Delete
    suspend fun delete(b: Budget)
}