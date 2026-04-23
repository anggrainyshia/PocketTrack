package com.example.pockettrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pockettrack.data.Budget
import com.example.pockettrack.ui.theme.ExpenseRed
import com.example.pockettrack.ui.theme.IncomeGreen
import com.example.pockettrack.ui.theme.SagePrimary
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager

// ── Month helpers ─────────────────────────────────────────────────────────────

private fun budgetPrevMonth(m: String): String {
    val parts = m.split("-"); val y = parts[0].toInt(); val mo = parts[1].toInt()
    return if (mo == 1) "${y - 1}-12" else "$y-${(mo - 1).toString().padStart(2, '0')}"
}

private fun budgetNextMonth(m: String): String {
    val parts = m.split("-"); val y = parts[0].toInt(); val mo = parts[1].toInt()
    return if (mo == 12) "${y + 1}-01" else "$y-${(mo + 1).toString().padStart(2, '0')}"
}

private fun formatBudgetMonthYear(m: String): String {
    val parts = m.split("-"); val y = parts[0].toInt(); val mo = parts[1].toInt()
    val names = listOf("January","February","March","April","May","June",
                       "July","August","September","October","November","December")
    return "${names[mo - 1]} $y"
}

// ── BudgetScreen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(vm: AppViewModel) {
    val currency      = vm.currency.observeAsState("IDR").value
    val today         = vm.currentMonth
    val allTx         = vm.allTransactions.observeAsState(emptyList()).value
    val categories    = vm.allCategories.observeAsState(emptyList()).value

    var selectedMonth by remember { mutableStateOf(today) }
    val canGoNext = selectedMonth < today

    val budgets  = vm.budgetsByMonth(selectedMonth).observeAsState(emptyList()).value
    val monthTx  = allTx.filter { it.date.startsWith(selectedMonth) }
    val spending = vm.spendingByCategory(monthTx, currency)

    var showAdd      by remember { mutableStateOf(false) }
    var editBudget   by remember { mutableStateOf<Budget?>(null) }
    var deleteBudget by remember { mutableStateOf<Budget?>(null) }

    deleteBudget?.let { b ->
        val cat = categories.find { it.id == b.categoryId }
        ConfirmDialog(
            title         = "Remove Budget",
            message       = { Text("Remove budget for \"${cat?.name ?: "this category"}\"?") },
            confirmLabel  = "Remove",
            isDestructive = true,
            onConfirm     = { vm.deleteBudget(b) },
            onDismiss     = { deleteBudget = null }
        )
    }

    if (showAdd || editBudget != null) {
        BudgetDialog(
            existing        = editBudget,
            categories      = categories.filter { it.type == "Expense" && it.parentId == 0 },
            currentCurrency = currency,
            month           = selectedMonth,
            onSave          = { b ->
                if (editBudget != null) vm.updateBudget(b) else vm.addBudget(b)
                showAdd = false; editBudget = null
            },
            onDismiss       = { showAdd = false; editBudget = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Budget") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, "Add Budget")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Month navigator
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { selectedMonth = budgetPrevMonth(selectedMonth) }) {
                        Icon(Icons.Default.ChevronLeft, "Previous")
                    }
                    Text(
                        formatBudgetMonthYear(selectedMonth),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        modifier   = Modifier.weight(1f),
                        textAlign  = TextAlign.Center
                    )
                    IconButton(
                        onClick  = { if (canGoNext) selectedMonth = budgetNextMonth(selectedMonth) },
                        enabled  = canGoNext
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            "Next",
                            tint = if (canGoNext) MaterialTheme.colorScheme.onSurface
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Summary header (only when there are budgets)
            if (budgets.isNotEmpty()) {
                item {
                    val totalBudgeted = budgets.sumOf { b ->
                        CurrencyManager.fromUsd(CurrencyManager.toUsd(b.limitAmount, b.currency), currency)
                    }
                    val totalSpent = budgets.sumOf { b -> spending[b.categoryId] ?: 0.0 }
                    BudgetSummaryHeader(
                        totalBudgeted = totalBudgeted,
                        totalSpent    = totalSpent,
                        currency      = currency
                    )
                }
            }

            // Budget cards
            if (budgets.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No budgets for this month.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("Tap + to add one.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                            // Copy from previous month option
                            Spacer(Modifier.height(16.dp))
                            val prevM = budgetPrevMonth(selectedMonth)
                            val prevBudgets = vm.budgetsByMonth(prevM).observeAsState(emptyList()).value
                            if (prevBudgets.isNotEmpty()) {
                                OutlinedButton(onClick = { vm.copyBudgetsFromMonth(prevM, selectedMonth) }) {
                                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy from ${formatBudgetMonthYear(prevM)}")
                                }
                            }
                        }
                    }
                }
            } else {
                items(budgets) { budget ->
                    val cat    = categories.find { it.id == budget.categoryId }
                    val spent  = spending[budget.categoryId] ?: 0.0
                    val limit  = CurrencyManager.fromUsd(CurrencyManager.toUsd(budget.limitAmount, budget.currency), currency)
                    AdvancedBudgetCard(
                        budget   = budget,
                        cat      = cat,
                        spent    = spent,
                        limit    = limit,
                        currency = currency,
                        onEdit   = { editBudget = budget },
                        onDelete = { deleteBudget = budget }
                    )
                }

                // Copy-from-last-month at the bottom
                item {
                    val prevM      = budgetPrevMonth(selectedMonth)
                    val prevBudgets = vm.budgetsByMonth(prevM).observeAsState(emptyList()).value
                    if (prevBudgets.isNotEmpty()) {
                        TextButton(
                            onClick  = { vm.copyBudgetsFromMonth(prevM, selectedMonth) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy budgets from ${formatBudgetMonthYear(prevM)}")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

// ── Summary Header Card ───────────────────────────────────────────────────────

@Composable
private fun BudgetSummaryHeader(totalBudgeted: Double, totalSpent: Double, currency: String) {
    val remaining       = totalBudgeted - totalSpent
    val overallProgress = if (totalBudgeted > 0) (totalSpent / totalBudgeted).coerceIn(0.0, 1.0) else 0.0
    val progressColor   = when {
        overallProgress > 0.9 -> ExpenseRed
        overallProgress > 0.7 -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Monthly Overview", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                OverviewMetric(
                    label    = "Budgeted",
                    value    = CurrencyManager.format(totalBudgeted, currency),
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                OverviewMetric(
                    label    = "Spent",
                    value    = CurrencyManager.format(totalSpent, currency),
                    color    = if (overallProgress > 0.9) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OverviewMetric(
                    label    = if (remaining >= 0) "Remaining" else "Over Budget",
                    value    = CurrencyManager.format(kotlin.math.abs(remaining), currency),
                    color    = if (remaining < 0) ExpenseRed else IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress   = overallProgress.toFloat(),
                modifier   = Modifier.fillMaxWidth().height(6.dp),
                color      = progressColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(overallProgress * 100).toInt()}% of total budget used",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun OverviewMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
    }
}

// ── Advanced Budget Card ──────────────────────────────────────────────────────

@Composable
private fun AdvancedBudgetCard(
    budget   : Budget,
    cat      : com.example.pockettrack.data.Category?,
    spent    : Double,
    limit    : Double,
    currency : String,
    onEdit   : () -> Unit,
    onDelete : () -> Unit
) {
    val progress = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0) else 0.0
    val over     = spent > limit
    val catColor = try {
        Color(android.graphics.Color.parseColor(cat?.color ?: "#10B981"))
    } catch (e: Exception) { SagePrimary }
    val progressColor = when {
        progress > 0.9 -> ExpenseRed
        progress > 0.7 -> Color(0xFFF59E0B)
        else -> catColor
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (over)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon
                Box(
                    Modifier
                        .size(44.dp)
                        .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(cat?.icon ?: "📦", fontSize = 22.sp) }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(cat?.name ?: "Unknown", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "Limit: ${CurrencyManager.format(limit, currency)}",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Circular arc progress indicator
                CircularArcIndicator(
                    progress  = progress.toFloat(),
                    color     = progressColor,
                    modifier  = Modifier.size(68.dp)
                ) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = progressColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Thin progress bar
            LinearProgressIndicator(
                progress   = progress.toFloat(),
                modifier   = Modifier.fillMaxWidth().height(4.dp),
                color      = progressColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Spent: ${CurrencyManager.format(spent, currency)}",
                        fontSize = 13.sp,
                        color    = if (over) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurface
                    )
                    if (over) {
                        Text(
                            "Over by ${CurrencyManager.format(spent - limit, currency)}",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "Remaining: ${CurrencyManager.format(limit - spent, currency)}",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Circular Arc Indicator ────────────────────────────────────────────────────

@Composable
fun CircularArcIndicator(
    progress    : Float,
    color       : Color,
    modifier    : Modifier = Modifier.size(72.dp),
    strokeWidth : Dp = 6.dp,
    content     : @Composable () -> Unit
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val sw     = strokeWidth.toPx()
            val radius = (size.minDimension / 2f) - sw
            val center = Offset(size.width / 2f, size.height / 2f)
            val tl     = Offset(center.x - radius, center.y - radius)
            val sz     = Size(radius * 2, radius * 2)

            // Background arc (270 degrees starting at 135°)
            drawArc(
                color      = color.copy(alpha = 0.15f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter  = false,
                topLeft    = tl,
                size       = sz,
                style      = Stroke(sw, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweep = (progress * 270f).coerceIn(0f, 270f)
            if (sweep > 0f) {
                drawArc(
                    color      = color,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter  = false,
                    topLeft    = tl,
                    size       = sz,
                    style      = Stroke(sw, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

// ── Budget Dialog ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetDialog(
    existing        : Budget?,
    categories      : List<com.example.pockettrack.data.Category>,
    currentCurrency : String,
    month           : String,
    onSave          : (Budget) -> Unit,
    onDismiss       : () -> Unit
) {
    var selCatId  by remember { mutableIntStateOf(existing?.categoryId ?: 0) }
    var limitStr  by remember { mutableStateOf(existing?.limitAmount?.toString() ?: "") }
    var currency  by remember { mutableStateOf(existing?.currency ?: currentCurrency) }
    var error     by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Budget" else "Set Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Category", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selCatId == cat.id,
                            onClick  = { selCatId = cat.id; error = "" },
                            label    = { Text("${cat.icon} ${cat.name}") }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.Top
                ) {
                    OutlinedTextField(
                        value         = limitStr,
                        onValueChange = { limitStr = it; error = "" },
                        label         = { Text("Limit") },
                        modifier      = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        isError = error.isNotEmpty()
                    )
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded        = expanded,
                        onExpandedChange = { expanded = it },
                        modifier        = Modifier.width(100.dp)
                    ) {
                        OutlinedTextField(
                            value         = currency,
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("CCY") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier      = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            CurrencyManager.currencies.forEach { c ->
                                DropdownMenuItem(
                                    text    = { Text(c) },
                                    onClick = { currency = c; expanded = false }
                                )
                            }
                        }
                    }
                }
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val limit = limitStr.toDoubleOrNull()
                when {
                    selCatId == 0              -> error = "Please select a category."
                    limit == null || limit <= 0 -> error = "Please enter a valid amount."
                    else -> onSave(
                        Budget(
                            id          = existing?.id ?: 0,
                            categoryId  = selCatId,
                            limitAmount = limit,
                            currency    = currency,
                            month       = month
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
