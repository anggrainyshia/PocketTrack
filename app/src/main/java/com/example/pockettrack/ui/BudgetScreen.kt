package com.example.pockettrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pockettrack.data.Budget
import com.example.pockettrack.ui.theme.ExpenseRed
import com.example.pockettrack.ui.theme.SagePrimary
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(vm: AppViewModel) {
    val currency    = vm.currency.observeAsState("USD").value
    val month       = vm.currentMonth
    val budgets     = vm.budgetsByMonth(month).observeAsState(emptyList()).value
    val categories  = vm.allCategories.observeAsState(emptyList()).value
    val allTx       = vm.allTransactions.observeAsState(emptyList()).value
    val monthTx     = allTx.filter { it.date.startsWith(month) }
    val spending    = vm.spendingByCategory(monthTx, currency)
    var showAdd     by remember { mutableStateOf(false) }
    var editBudget  by remember { mutableStateOf<Budget?>(null) }
    var deleteBudget by remember { mutableStateOf<Budget?>(null) }

    deleteBudget?.let { b ->
        val cat = categories.find { it.id == b.categoryId }
        ConfirmDialog(
            title = "Remove Budget",
            message = { Text("Remove budget for \"${cat?.name}\"?") },
            confirmLabel = "Remove", isDestructive = true,
            onConfirm = { vm.deleteBudget(b) },
            onDismiss = { deleteBudget = null }
        )
    }

    if (showAdd || editBudget != null) {
        BudgetDialog(
            existing = editBudget,
            categories = categories.filter { it.type == "Expense" && it.parentId == 0 },
            currentCurrency = currency,
            month = month,
            onSave = { b -> if (editBudget != null) vm.updateBudget(b) else vm.addBudget(b); showAdd = false; editBudget = null },
            onDismiss = { showAdd = false; editBudget = null }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Budget — $month") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add Budget") }
        }
    ) { padding ->
        if (budgets.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No budgets set.", color = Color.Gray)
                    Text("Tap + to add one for this month.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(budgets) { budget ->
                    val cat = categories.find { it.id == budget.categoryId }
                    val spent = spending[budget.categoryId] ?: 0.0
                    val limit = CurrencyManager.fromUsd(CurrencyManager.toUsd(budget.limitAmount, budget.currency), currency)
                    val progress = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0) else 0.0
                    val over = spent > limit

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat?.icon ?: "📦", fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cat?.name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                                    Text("Limit: ${CurrencyManager.format(limit, currency)}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { editBudget = budget }) { Icon(Icons.Default.Edit, null, tint = Color.Gray) }
                                IconButton(onClick = { deleteBudget = budget }) { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = progress.toFloat(),
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = when {
                                    progress > 0.9 -> ExpenseRed
                                    progress > 0.7 -> Color(0xFFF59E0B)
                                    else           -> SagePrimary
                                },
                                trackColor = Color(0xFFE0E0E0)
                            )
                            Spacer(Modifier.height(6.dp))
                            Row {
                                Text(
                                    "Spent: ${CurrencyManager.format(spent, currency)}",
                                    fontSize = 13.sp,
                                    color = if (over) ExpenseRed else Color.Gray,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (over) ExpenseRed else SagePrimary
                                )
                            }
                            if (over) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "⚠️ Over budget by ${CurrencyManager.format(spent - limit, currency)}",
                                    fontSize = 12.sp, color = ExpenseRed
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
fun BudgetDialog(
    existing: Budget?, categories: List<com.example.pockettrack.data.Category>,
    currentCurrency: String, month: String,
    onSave: (Budget) -> Unit, onDismiss: () -> Unit
) {
    var selCatId  by remember { mutableIntStateOf(existing?.categoryId ?: 0) }
    var limitStr  by remember { mutableStateOf(existing?.limitAmount?.toString() ?: "") }
    var currency  by remember { mutableStateOf(existing?.currency ?: currentCurrency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Budget" else "Set Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Category")
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(selected = selCatId == cat.id, onClick = { selCatId = cat.id },
                            label = { Text("${cat.icon} ${cat.name}") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = limitStr, onValueChange = { limitStr = it },
                        label = { Text("Limit") }, modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                    var expanded by remember { mutableStateOf(false) }
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.width(100.dp)) {
                        @OptIn(ExperimentalMaterial3Api::class)
                        OutlinedTextField(value = currency, onValueChange = {}, readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor())
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            CurrencyManager.currencies.forEach { c ->
                                DropdownMenuItem(text = { Text(c) }, onClick = { currency = c; expanded = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val limit = limitStr.toDoubleOrNull()
                if (selCatId != 0 && limit != null && limit > 0) {
                    onSave(Budget(id = existing?.id ?: 0, categoryId = selCatId, limitAmount = limit, currency = currency, month = month))
                }
            }) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}