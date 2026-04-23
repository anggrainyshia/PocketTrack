package com.example.pockettrack.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.pockettrack.data.*

class AppRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val txDao  = db.transactionDao()
    private val catDao = db.categoryDao()
    private val budDao = db.budgetDao()

    // Transactions
    val allTransactions: LiveData<List<Transaction>> = txDao.getAll()
    fun getByMonth(month: String)          = txDao.getByMonth(month)
    fun search(q: String)                  = txDao.search(q)
    fun getByCategory(id: Int)             = txDao.getByCategory(id)
    fun getByType(type: String)            = txDao.getByType(type)
    suspend fun getAllSync()               = txDao.getAllSync()
    suspend fun getRecurringExpenses()    = txDao.getRecurringExpenses()
    suspend fun insertTx(t: Transaction)  = txDao.insert(t)
    suspend fun updateTx(t: Transaction)  = txDao.update(t)
    suspend fun deleteTx(t: Transaction)  = txDao.delete(t)

    // Categories
    val allCategories: LiveData<List<Category>>   = catDao.getAll()
    val topCategories: LiveData<List<Category>>   = catDao.getTopLevel()
    fun subCategories(pid: Int)                   = catDao.getSubCategories(pid)
    suspend fun insertCat(c: Category)            = catDao.insert(c)
    suspend fun updateCat(c: Category)            = catDao.update(c)
    suspend fun deleteCat(c: Category)            = catDao.delete(c)

    // Budgets
    val allBudgets: LiveData<List<Budget>>        = budDao.getAll()
    fun budgetsByMonth(m: String)                 = budDao.getByMonth(m)
    suspend fun getBudgetsByMonthSync(m: String)  = budDao.getByMonthSync(m)
    suspend fun insertBudget(b: Budget)           = budDao.insert(b)
    suspend fun updateBudget(b: Budget)           = budDao.update(b)
    suspend fun deleteBudget(b: Budget)           = budDao.delete(b)
}