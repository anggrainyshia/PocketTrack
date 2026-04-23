package com.example.pockettrack.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    var currency    by remember { mutableStateOf(existing?.currency ?: vm.currency.value ?: "IDR") }
    var note        by remember { mutableStateOf(existing?.note ?: "") }
    var date        by remember { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }
    var selCatId    by remember { mutableIntStateOf(existing?.categoryId ?: 0) }
    var showDialog      by remember { mutableStateOf(false) }
    var showDatePicker  by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf("") }

    // ── Recurring fields ──────────────────────────────────────────────────
    var isRecurring     by remember { mutableStateOf(existing?.isRecurring ?: false) }
    val today = LocalDate.now()
    var recurringDay    by remember { mutableIntStateOf(existing?.recurringDay   ?: today.dayOfMonth) }
    var recurringMonth  by remember { mutableIntStateOf(existing?.recurringMonth ?: today.monthValue) }
    var recurringYear   by remember { mutableIntStateOf(existing?.recurringYear  ?: today.year) }

    var dayExpanded   by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded  by remember { mutableStateOf(false) }

    val monthNames = listOf(
        "Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec"
    )
    val yearRange = (today.year..today.year + 5).toList()

    val filteredCats = categories.filter { it.type == type && it.parentId == 0 }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (editId != null) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth()
            )

            // Type selector
            Text("Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Income", "Expense").forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = {
                            type = t
                            selCatId = 0
                            // Reset recurring when switching away from Expense
                            if (t != "Expense") isRecurring = false
                        },
                        label = { Text(t) }
                    )
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
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.width(110.dp)
                ) {
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

            // Date picker
            OutlinedTextField(
                value         = date,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Date") },
                trailingIcon  = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, "Pick date")
                    }
                },
                modifier      = Modifier.fillMaxWidth().clickable { showDatePicker = true }
            )

            if (showDatePicker) {
                val initialMillis = runCatching {
                    java.time.LocalDate.parse(date).toEpochDay() * 86_400_000L
                }.getOrDefault(System.currentTimeMillis())
                val dpState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            dpState.selectedDateMillis?.let { millis ->
                                date = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString()
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) { DatePicker(state = dpState) }
            }

            // ── Recurring toggle (Expense only) ───────────────────────────
            if (type == "Expense") {
                HorizontalDivider()

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Recurring Expense", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            if (isRecurring) "Auto-deducts monthly on the selected date"
                            else "Tap to enable auto monthly deduction",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                }

                // Date pickers shown only when recurring is enabled
                if (isRecurring) {
                    Text(
                        "Auto-deduct starting from",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day picker
                        ExposedDropdownMenuBox(
                            expanded = dayExpanded,
                            onExpandedChange = { dayExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = recurringDay.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Day") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = dayExpanded,
                                onDismissRequest = { dayExpanded = false }
                            ) {
                                (1..31).forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.toString()) },
                                        onClick = { recurringDay = d; dayExpanded = false }
                                    )
                                }
                            }
                        }

                        // Month picker
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = monthNames[recurringMonth - 1],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Month") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false }
                            ) {
                                monthNames.forEachIndexed { i, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = { recurringMonth = i + 1; monthExpanded = false }
                                    )
                                }
                            }
                        }

                        // Year picker
                        ExposedDropdownMenuBox(
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = recurringYear.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Year") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false }
                            ) {
                                yearRange.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y.toString()) },
                                        onClick = { recurringYear = y; yearExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.35f)
                        )
                    ) {
                        Text(
                            "This expense will auto-deduct from your income on day $recurringDay " +
                            "of each month, starting ${monthNames[recurringMonth - 1]} $recurringYear.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider()
            }

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
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth()
            )

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
            ) {
                Text(if (editId != null) "Update Transaction" else "Save Transaction")
            }

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
                        if (isRecurring) {
                            Text(
                                "🔁  Recurring monthly on day $recurringDay from " +
                                "${monthNames[recurringMonth - 1]} $recurringYear"
                            )
                        }
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
                            amountInUsd = usdAmt, date = date, note = note,
                            isRecurring = isRecurring,
                            recurringDay = if (isRecurring) recurringDay else 0,
                            recurringMonth = if (isRecurring) recurringMonth else 0,
                            recurringYear = if (isRecurring) recurringYear else 0
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
