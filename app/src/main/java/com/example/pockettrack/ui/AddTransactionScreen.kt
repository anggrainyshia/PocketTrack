package com.example.pockettrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pockettrack.data.Transaction
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(nav: NavController, vm: AppViewModel, editId: Int? = null) {
    val categories  = vm.allCategories.observeAsState(emptyList()).value
    val allTx       = vm.allTransactions.observeAsState(emptyList()).value
    val existing    = allTx.find { it.id == editId }

    var title       by remember { mutableStateOf(existing?.title ?: "") }
    var amount      by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var type        by remember { mutableStateOf(existing?.type ?: "Expense") }
    var currency    by remember { mutableStateOf(existing?.currency ?: vm.currency.value ?: "USD") }
    var note        by remember { mutableStateOf(existing?.note ?: "") }
    var date        by remember { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }
    var selCatId    by remember { mutableIntStateOf(existing?.categoryId ?: 0) }
    var showDialog  by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }

    val filteredCats = categories.filter { it.type == type && it.parentId == 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editId != null) "Edit Transaction" else "Add Transaction") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth())

            // Type selector
            Text("Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Income", "Expense").forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t; selCatId = 0 }, label = { Text(t) })
                }
            }

            // Amount + Currency row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.width(110.dp)) {
                    OutlinedTextField(
                        value = currency, onValueChange = {},
                        readOnly = true, label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        CurrencyManager.currencies.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { currency = c; expanded = false })
                        }
                    }
                }
            }

            // Date
            OutlinedTextField(value = date, onValueChange = { date = it },
                label = { Text("Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())

            // Category
            Text("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredCats.forEach { cat ->
                    FilterChip(
                        selected = selCatId == cat.id,
                        onClick = { selCatId = cat.id },
                        label = { Text("${cat.icon} ${cat.name}") }
                    )
                }
            }

            // Note
            OutlinedTextField(value = note, onValueChange = { note = it },
                label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())

            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    when {
                        title.isBlank()         -> errorMsg = "Please enter a title."
                        amt == null || amt <= 0 -> errorMsg = "Please enter a valid amount."
                        selCatId == 0           -> errorMsg = "Please select a category."
                        else -> { errorMsg = ""; showDialog = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (editId != null) "Update Transaction" else "Save Transaction") }

            if (showDialog) {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val cat = categories.find { it.id == selCatId }
                ConfirmDialog(
                    title = if (editId != null) "Confirm Update" else "Confirm Transaction",
                    message = {
                        Text("📝  $title")
                        Text("💰  ${CurrencyManager.format(amt, currency)}")
                        Text("🔖  $type  •  ${cat?.icon} ${cat?.name}")
                        Text("📅  $date")
                        if (note.isNotBlank()) Text("🗒️  $note")
                    },
                    confirmLabel = if (editId != null) "Update" else "Save",
                    dismissLabel = "Edit",
                    onConfirm = {
                        val usdAmt = CurrencyManager.toUsd(amt, currency)
                        val tx = Transaction(
                            id = existing?.id ?: 0,
                            title = title, amount = amt, type = type,
                            categoryId = selCatId, currency = currency,
                            amountInUsd = usdAmt, date = date, note = note
                        )
                        if (editId != null) vm.updateTransaction(tx) else vm.addTransaction(tx)
                        nav.popBackStack()
                    },
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}