package com.example.pockettrack.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.pockettrack.data.AppDatabase
import com.example.pockettrack.data.Transaction

class TransactionRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).transactionDao()

    val allTransactions: LiveData<List<Transaction>> = dao.getAll()

    suspend fun insert(t: Transaction) = dao.insert(t)
    suspend fun delete(t: Transaction) = dao.delete(t)
}
