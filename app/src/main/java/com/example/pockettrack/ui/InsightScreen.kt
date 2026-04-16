package com.example.pockettrack.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pockettrack.ui.theme.ExpenseRed
import com.example.pockettrack.ui.theme.IncomeGreen
import com.example.pockettrack.ui.theme.SagePrimary
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightScreen(vm: AppViewModel) {
    val currency    = vm.currency.observeAsState("IDR").value
    val allTx       = vm.allTransactions.observeAsState(emptyList()).value
    val categories  = vm.allCategories.observeAsState(emptyList()).value
    val month       = vm.currentMonth
    val monthTx     = allTx.filter { it.date.startsWith(month) }
    val monthlyData = vm.monthlyTotals(allTx, currency)
    val spending    = vm.spendingByCategory(monthTx, currency)
    val totalExp    = vm.totalInCurrency(monthTx, "Expense", currency)
    val totalInc    = vm.totalInCurrency(monthTx, "Income", currency)

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Income vs Expense summary
        item {
            Text("This Month", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Income", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        Text(
                            CurrencyManager.format(totalInc, currency),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Card(
                    Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expense", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        Text(
                            CurrencyManager.format(totalExp, currency),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Trend line chart (Income vs Expense, last 6 months)
        item {
            Text("6-Month Trend", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(IncomeGreen, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(4.dp)); Text("Income", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(ExpenseRed, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(4.dp)); Text("Expense", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LineChart(monthlyData)
                }
            }
        }

        // Category breakdown (pie-like bar chart)
        if (spending.isNotEmpty()) {
            item { Text("Spending by Category", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            items(spending.entries.sortedByDescending { it.value }.take(6).toList()) { (catId, amt) ->
                val cat = categories.find { it.id == catId }
                val pct = if (totalExp > 0) amt / totalExp else 0.0
                val barColor = try { Color(android.graphics.Color.parseColor(cat?.color ?: "#9CA3AF")) } catch (e: Exception) { Color.Gray }
                Row(verticalAlignment = Alignment.Top) {
                    Text(cat?.icon ?: "📦", fontSize = 20.sp, modifier = Modifier.width(32.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(cat?.name ?: "Unknown", fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("${(pct * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        }
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = pct.toFloat(),
                            modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = barColor, trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                        Text(CurrencyManager.format(amt, currency), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
            }
        }

        // Biggest expense
        val biggestExpense = monthTx.filter { it.type == "Expense" }
            .maxByOrNull { it.amountInUsd }
        if (biggestExpense != null) {
            item {
                val cat = categories.find { it.id == biggestExpense.categoryId }
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Text("🔴", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Biggest Expense", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            Text(biggestExpense.title, fontWeight = FontWeight.SemiBold)
                            Text("${cat?.icon} ${cat?.name} • ${biggestExpense.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        }
                        Text(
                            CurrencyManager.format(CurrencyManager.fromUsd(biggestExpense.amountInUsd, currency), currency),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun LineChart(data: List<Pair<String, Pair<Double, Double>>>) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { maxOf(it.second.first, it.second.second) }.coerceAtLeast(1.0)

    Box(Modifier.fillMaxWidth().height(160.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height - 24.dp.toPx()
            val step = if (data.size > 1) w / (data.size - 1) else w

            fun yPos(v: Double) = h - (v / maxVal * h).toFloat()

            // Draw income line
            val incPath = Path()
            data.forEachIndexed { i, (_, v) ->
                val x = i * step; val y = yPos(v.first)
                if (i == 0) incPath.moveTo(x, y) else incPath.lineTo(x, y)
            }
            drawPath(incPath, color = IncomeGreen, style = Stroke(width = 3.dp.toPx()))

            // Draw expense line
            val expPath = Path()
            data.forEachIndexed { i, (_, v) ->
                val x = i * step; val y = yPos(v.second)
                if (i == 0) expPath.moveTo(x, y) else expPath.lineTo(x, y)
            }
            drawPath(expPath, color = ExpenseRed, style = Stroke(width = 3.dp.toPx()))

            // Dots
            data.forEachIndexed { i, (_, v) ->
                val x = i * step
                drawCircle(IncomeGreen, 5.dp.toPx(), Offset(x, yPos(v.first)))
                drawCircle(ExpenseRed, 5.dp.toPx(), Offset(x, yPos(v.second)))
            }
        }

        // X labels
        Row(Modifier.fillMaxWidth().align(Alignment.BottomStart), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { (label, _) ->
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
