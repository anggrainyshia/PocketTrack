package com.example.pockettrack.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.pockettrack.data.*
import com.example.pockettrack.repository.AppRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

// Supported currencies with conversion rates relative to USD
object CurrencyManager {
    val currencies = listOf("USD", "SGD", "IDR")
    val symbols = mapOf("USD" to "$", "SGD" to "S$", "IDR" to "Rp")
    // Approximate rates to USD
    val toUsd = mapOf("USD" to 1.0, "SGD" to 0.74, "IDR" to 0.000062)

    fun toUsd(amount: Double, from: String): Double =
        amount * (toUsd[from] ?: 1.0)

    fun fromUsd(amount: Double, to: String): Double =
        amount / (toUsd[to] ?: 1.0)

    fun format(amount: Double, currency: String): String {
        val sym = symbols[currency] ?: currency
        return if (currency == "IDR")
            "$sym${"%,.0f".format(amount)}"
        else
            "$sym${"%.2f".format(amount)}"
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)

    // ── Currency preference ──
    private val _currency = MutableLiveData("USD")
    val currency: LiveData<String> = _currency
    fun setCurrency(c: String) { _currency.value = c }

    // ── Transactions ──
    val allTransactions = repo.allTransactions
    fun txByMonth(m: String)    = repo.getByMonth(m)
    fun search(q: String)       = repo.search(q)
    fun txByCategory(id: Int)   = repo.getByCategory(id)
    fun txByType(t: String)     = repo.getByType(t)

    fun addTransaction(t: Transaction) = viewModelScope.launch { repo.insertTx(t) }
    fun updateTransaction(t: Transaction) = viewModelScope.launch { repo.updateTx(t) }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch { repo.deleteTx(t) }

    // ── Categories ──
    val allCategories  = repo.allCategories
    val topCategories  = repo.topCategories
    fun subCategories(pid: Int) = repo.subCategories(pid)
    fun addCategory(c: Category)    = viewModelScope.launch { repo.insertCat(c) }
    fun updateCategory(c: Category) = viewModelScope.launch { repo.updateCat(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCat(c) }

    // ── Budgets ──
    val allBudgets = repo.allBudgets
    fun budgetsByMonth(m: String) = repo.budgetsByMonth(m)
    fun addBudget(b: Budget)    = viewModelScope.launch { repo.insertBudget(b) }
    fun updateBudget(b: Budget) = viewModelScope.launch { repo.updateBudget(b) }
    fun deleteBudget(b: Budget) = viewModelScope.launch { repo.deleteBudget(b) }

    // ── Derived stats ──
    val currentMonth: String get() = LocalDate.now().toString().substring(0, 7)

    fun totalInCurrency(list: List<Transaction>, type: String, toCurrency: String): Double {
        return list.filter { it.type == type }
            .sumOf { CurrencyManager.fromUsd(it.amountInUsd, toCurrency) }
    }

    fun balanceInCurrency(list: List<Transaction>, toCurrency: String): Double {
        val inc = totalInCurrency(list, "Income", toCurrency)
        val exp = totalInCurrency(list, "Expense", toCurrency)
        return inc - exp
    }

    // Spending by category (returns categoryId -> amount in target currency)
    fun spendingByCategory(list: List<Transaction>, toCurrency: String): Map<Int, Double> {
        return list.filter { it.type == "Expense" }
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { CurrencyManager.fromUsd(it.amountInUsd, toCurrency) } }
    }

    // Monthly totals for trend chart (last 6 months)
    fun monthlyTotals(list: List<Transaction>, toCurrency: String): List<Pair<String, Pair<Double, Double>>> {
        val months = (5 downTo 0).map {
            LocalDate.now().minusMonths(it.toLong()).toString().substring(0, 7)
        }
        return months.map { m ->
            val txs = list.filter { it.date.startsWith(m) }
            val inc = txs.filter { it.type == "Income" }.sumOf { CurrencyManager.fromUsd(it.amountInUsd, toCurrency) }
            val exp = txs.filter { it.type == "Expense" }.sumOf { CurrencyManager.fromUsd(it.amountInUsd, toCurrency) }
            m.substring(5) to Pair(inc, exp) // "MM" to (income, expense)
        }
    }
}
