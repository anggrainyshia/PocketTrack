package com.example.pockettrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pockettrack.data.Category
import com.example.pockettrack.data.Transaction
import com.example.pockettrack.ui.theme.ExpenseRed
import com.example.pockettrack.ui.theme.IncomeGreen
import com.example.pockettrack.ui.theme.SageDark
import com.example.pockettrack.ui.theme.SagePrimary
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController, vm: AppViewModel) {
    val currency     = vm.currency.observeAsState("USD").value
    val allTx        = vm.allTransactions.observeAsState(emptyList()).value
    val categories   = vm.allCategories.observeAsState(emptyList()).value
    val budgets      = vm.allBudgets.observeAsState(emptyList()).value
    val month        = vm.currentMonth
    val monthTx      = allTx.filter { it.date.startsWith(month) }
    val recent       = allTx.take(5)
    val balance      = vm.balanceInCurrency(allTx, currency)
    val income       = vm.totalInCurrency(monthTx, "Income", currency)
    val expense      = vm.totalInCurrency(monthTx, "Expense", currency)
    val spending     = vm.spendingByCategory(monthTx, currency)
    val topCats      = spending.entries.sortedByDescending { it.value }.take(3)

    // Weekly summary
    val today        = LocalDate.now()
    val weekStart    = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val weekTx       = allTx.filter {
        val d = LocalDate.parse(it.date)
        !d.isBefore(weekStart) && !d.isAfter(today)
    }
    val weekExpense  = vm.totalInCurrency(weekTx, "Expense", currency)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pocket Track") },
                actions = {
                    Text(
                        currency,
                        modifier = Modifier.padding(end = 16.dp),
                        color = SagePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("add") }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        LazyColumn(Modifier
            .padding(padding)
            .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Balance card
            item {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(SagePrimary, SageDark)),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Balance", color = Color.White.copy(.8f), fontSize = 13.sp)
                        Text(CurrencyManager.format(balance, currency), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("This Month Income", color = Color.White.copy(.7f), fontSize = 11.sp)
                                Text(CurrencyManager.format(income, currency), color = Color(0xFF80FFAA), fontWeight = FontWeight.SemiBold)
                            }
                            Divider(Modifier
                                .height(40.dp)
                                .width(1.dp), color = Color.White.copy(.3f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("This Month Expense", color = Color.White.copy(.7f), fontSize = 11.sp)
                                Text(CurrencyManager.format(expense, currency), color = Color(0xFFFF8080), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Weekly summary
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("This Week", fontWeight = FontWeight.SemiBold)
                            Text("Spent so far", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(CurrencyManager.format(weekExpense, currency), color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }

            // Budget progress bars
            if (budgets.isNotEmpty()) {
                item {
                    Text("Budget Overview", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                items(budgets.filter { it.month == month }.take(3)) { budget ->
                    val cat = categories.find { it.id == budget.categoryId }
                    val spent = spending[budget.categoryId] ?: 0.0
                    val limit = CurrencyManager.fromUsd(
                        CurrencyManager.toUsd(budget.limitAmount, budget.currency), currency
                    )
                    val progress = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0) else 0.0
                    val overBudget = spent > limit
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat?.icon ?: "📦", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cat?.name ?: "Unknown", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text(
                                        "${CurrencyManager.format(spent, currency)} / ${CurrencyManager.format(limit, currency)}",
                                        fontSize = 12.sp, color = Color.Gray
                                    )
                                }
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    color = if (overBudget) ExpenseRed else SagePrimary,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = progress.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = if (overBudget) ExpenseRed else SagePrimary,
                                trackColor = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }

            // Top spending categories
            if (topCats.isNotEmpty()) {
                item { Text("Top Spending", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            topCats.forEach { (catId, amt) ->
                                val cat = categories.find { it.id == catId }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat?.icon ?: "📦", fontSize = 22.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(cat?.name ?: "Unknown", Modifier.weight(1f))
                                    Text(CurrencyManager.format(amt, currency), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Recent transactions
            item { Text("Recent Transactions", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            if (recent.isEmpty()) {
                item { Text("No transactions yet. Tap + to add one!", color = Color.Gray) }
            } else {
                items(recent) { tx ->
                    TxCard(tx, categories, currency, onClick = { nav.navigate("edit/${tx.id}") })
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxCard(tx: Transaction, categories: List<Category>, currency: String, onClick: () -> Unit = {}) {
    val cat = categories.find { it.id == tx.categoryId }
    val isIncome = tx.type == "Income"
    val displayAmt = CurrencyManager.fromUsd(tx.amountInUsd, currency)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        Color(
                            android.graphics.Color.parseColor(
                                cat?.color ?: "#9CA3AF"
                            )
                        ).copy(.15f),
                        RoundedCornerShape(12.dp)
                    ), contentAlignment = Alignment.Center
            ) { Text(cat?.icon ?: "📦", fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${cat?.name ?: "?"} • ${tx.date}", fontSize = 12.sp, color = Color.Gray)
                if (tx.note.isNotBlank()) Text(tx.note, fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncome) "+" else "-"}${CurrencyManager.format(displayAmt, currency)}",
                    color = if (isIncome) IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                Text(tx.currency, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}