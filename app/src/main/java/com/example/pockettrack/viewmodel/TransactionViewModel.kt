package com.example.pockettrack.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.pockettrack.data.Transaction
import com.example.pockettrack.repository.TransactionRepository
import kotlinx.coroutines.launch

class TransactionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepository(app)
    val transactions: LiveData<List<Transaction>> = repo.allTransactions

    fun addTransaction(t: Transaction) = viewModelScope.launch { repo.insert(t) }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch { repo.delete(t) }

    val totalBalance: LiveData<Double> = transactions.map { list ->
        list.sumOf { if (it.type == "Income") it.amount else -it.amount }
    }
    val totalIncome: LiveData<Double> = transactions.map { list ->
        list.filter { it.type == "Income" }.sumOf { it.amount }
    }
    val totalExpense: LiveData<Double> = transactions.map { list ->
        list.filter { it.type == "Expense" }.sumOf { it.amount }
    }
}