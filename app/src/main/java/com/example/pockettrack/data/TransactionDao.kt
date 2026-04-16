package com.example.pockettrack.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllSync(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE isRecurring = 1 AND type = 'Expense'")
    suspend fun getRecurringExpenses(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC")
    fun getByMonth(monthPrefix: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
    fun search(query: String): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :catId ORDER BY date DESC")
    fun getByCategory(catId: Int): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: String): LiveData<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: Transaction)

    @Update
    suspend fun update(t: Transaction)

    @Delete
    suspend fun delete(t: Transaction)
}
