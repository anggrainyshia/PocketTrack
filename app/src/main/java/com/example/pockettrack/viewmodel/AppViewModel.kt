package com.example.pockettrack.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.pockettrack.data.*
import com.example.pockettrack.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL
import java.time.LocalDate
import javax.net.ssl.HttpsURLConnection

data class ExchangeRateInfo(
    val base: String,
    val rates: Map<String, Double>,
    val updatedOn: String,
    val sourceLabel: String
)

data class ExchangeRateUiState(
    val isLoading: Boolean = false,
    val info: ExchangeRateInfo? = null,
    val errorMessage: String? = null
)

// Supported currencies with conversion rates relative to USD
object CurrencyManager {
    val currencies = listOf("USD", "SGD", "IDR")
    val symbols = mapOf("USD" to "$", "SGD" to "S$", "IDR" to "Rp")
    private var usdPerCurrency = mapOf("USD" to 1.0, "SGD" to 0.74, "IDR" to 0.000062)

    fun usdPerCurrency(currency: String): Double = usdPerCurrency[currency] ?: 1.0

    fun updateUsdPerCurrency(updated: Map<String, Double>) {
        usdPerCurrency = usdPerCurrency + updated.filterKeys { it in currencies }
    }

    fun toUsd(amount: Double, from: String): Double =
        amount * usdPerCurrency(from)

    fun fromUsd(amount: Double, to: String): Double =
        amount / usdPerCurrency(to)

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

    // ── Theme mode preference: "System" | "Light" | "Dark" ───────────────
    private val _themeMode = MutableLiveData("System")
    val themeMode: LiveData<String> = _themeMode
    fun setThemeMode(mode: String) { _themeMode.value = mode }

    // ── Currency preference (default: IDR) ────────────────────────────────
    private val _currency = MutableLiveData("IDR")
    val currency: LiveData<String> = _currency
    fun setCurrency(c: String) { _currency.value = c }

    private val _exchangeRates = MutableLiveData(
        ExchangeRateUiState(
            info = ExchangeRateInfo(
                base = "USD",
                rates = mapOf(
                    "SGD" to 1 / CurrencyManager.usdPerCurrency("SGD"),
                    "IDR" to 1 / CurrencyManager.usdPerCurrency("IDR")
                ),
                updatedOn = "Using built-in fallback rates",
                sourceLabel = "Local fallback"
            )
        )
    )
    val exchangeRates: LiveData<ExchangeRateUiState> = _exchangeRates

    // ── Transactions ──────────────────────────────────────────────────────
    val allTransactions = repo.allTransactions
    fun txByMonth(m: String)    = repo.getByMonth(m)
    fun search(q: String)       = repo.search(q)
    fun txByCategory(id: Int)   = repo.getByCategory(id)
    fun txByType(t: String)     = repo.getByType(t)

    fun addTransaction(t: Transaction) = viewModelScope.launch { repo.insertTx(t) }
    fun updateTransaction(t: Transaction) = viewModelScope.launch { repo.updateTx(t) }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch { repo.deleteTx(t) }

    // ── Categories ────────────────────────────────────────────────────────
    val allCategories  = repo.allCategories
    val topCategories  = repo.topCategories
    fun subCategories(pid: Int) = repo.subCategories(pid)
    fun addCategory(c: Category)    = viewModelScope.launch { repo.insertCat(c) }
    fun updateCategory(c: Category) = viewModelScope.launch { repo.updateCat(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCat(c) }

    // ── Budgets ───────────────────────────────────────────────────────────
    val allBudgets = repo.allBudgets
    fun budgetsByMonth(m: String) = repo.budgetsByMonth(m)
    fun addBudget(b: Budget)    = viewModelScope.launch { repo.insertBudget(b) }
    fun updateBudget(b: Budget) = viewModelScope.launch { repo.updateBudget(b) }
    fun deleteBudget(b: Budget) = viewModelScope.launch { repo.deleteBudget(b) }

    init {
        refreshExchangeRates()
        processRecurringExpenses()
    }

    fun refreshExchangeRates() {
        val currentInfo = _exchangeRates.value?.info
        _exchangeRates.value = ExchangeRateUiState(isLoading = true, info = currentInfo)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val connection = (URL("https://api.frankfurter.dev/v2/rates?base=USD&quotes=SGD,IDR")
                    .openConnection() as HttpsURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val payload = JSONArray(response)
                val latestRates = mutableMapOf<String, Double>()
                var updatedOn = ""

                for (i in 0 until payload.length()) {
                    val item = payload.getJSONObject(i)
                    val quote = item.getString("quote")
                    val rate = item.getDouble("rate")
                    latestRates[quote] = rate
                    if (updatedOn.isBlank()) updatedOn = item.getString("date")
                }

                val usdPerCurrency = latestRates.mapValues { (_, rate) -> 1.0 / rate } + ("USD" to 1.0)
                CurrencyManager.updateUsdPerCurrency(usdPerCurrency)

                _exchangeRates.postValue(
                    ExchangeRateUiState(
                        info = ExchangeRateInfo(
                            base = "USD",
                            rates = latestRates,
                            updatedOn = updatedOn,
                            sourceLabel = "Frankfurter API"
                        )
                    )
                )
            } catch (e: Exception) {
                _exchangeRates.postValue(
                    ExchangeRateUiState(
                        info = currentInfo,
                        errorMessage = "Couldn't refresh rates right now."
                    )
                )
            }
        }
    }

    // ── Recurring expense engine ──────────────────────────────────────────
    // Called once on app start. For each recurring expense template, if today
    // is the configured day-of-month and the current month/year is on or after
    // the template's start month/year, and no auto-generated entry exists for
    // this month yet, a new transaction is inserted automatically.
    fun processRecurringExpenses() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val templates = repo.getRecurringExpenses()
            val allTx = repo.getAllSync()
            val currentYM = "${today.year}-${today.monthValue.toString().padStart(2, '0')}"

            templates.forEach { template ->
                // Only trigger on the configured day
                if (template.recurringDay != today.dayOfMonth) return@forEach

                // Only trigger from the start month/year onward
                val startYM = "${template.recurringYear}-${template.recurringMonth.toString().padStart(2, '0')}"
                if (currentYM < startYM) return@forEach

                // Check if already auto-generated for this month
                val alreadyDone = allTx.any {
                    it.isAutoGenerated &&
                    it.recurringSourceId == template.id &&
                    it.date.startsWith(currentYM)
                }
                if (alreadyDone) return@forEach

                // Insert the auto-deduction
                repo.insertTx(
                    template.copy(
                        id = 0,
                        date = today.toString(),
                        isRecurring = false,
                        isAutoGenerated = true,
                        recurringSourceId = template.id,
                        note = if (template.note.isBlank())
                            "Auto-deducted recurring expense"
                        else
                            "${template.note} (auto)"
                    )
                )
            }
        }
    }

    // ── Derived stats ─────────────────────────────────────────────────────
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

    fun spendingByCategory(list: List<Transaction>, toCurrency: String): Map<Int, Double> {
        return list.filter { it.type == "Expense" }
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { CurrencyManager.fromUsd(it.amountInUsd, toCurrency) } }
    }

    fun monthlyTotals(list: List<Transaction>, toCurrency: String): List<Pair<String, Pair<Double, Double>>> {
        val months = (5 downTo 0).map {
            LocalDate.now().minusMonths(it.toLong()).toString().substring(0, 7)
        }
        return months.map { m ->
            val txs = list.filter { it.date.startsWith(m) }
            val inc = txs.filter { it.type == "Income" }.sumOf { CurrencyManager.fromUsd(it.amountInUsd, toCurrency) }
            val exp = txs.filter { it.type == "Expense" }.sumOf { CurrencyManager.fromUsd(it.amountInUsd, toCurrency) }
            m.substring(5) to Pair(inc, exp)
        }
    }
}
