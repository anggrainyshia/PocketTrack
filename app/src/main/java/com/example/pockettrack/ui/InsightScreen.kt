package com.example.pockettrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pockettrack.ui.theme.ExpenseRed
import com.example.pockettrack.ui.theme.IncomeGreen
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager
import java.time.LocalDate

// ── Month helpers ─────────────────────────────────────────────────────────────

private fun prevMonth(m: String): String {
    val parts = m.split("-"); val y = parts[0].toInt(); val mo = parts[1].toInt()
    return if (mo == 1) "${y - 1}-12" else "$y-${(mo - 1).toString().padStart(2, '0')}"
}

private fun nextMonth(m: String): String {
    val parts = m.split("-"); val y = parts[0].toInt(); val mo = parts[1].toInt()
    return if (mo == 12) "${y + 1}-01" else "$y-${(mo + 1).toString().padStart(2, '0')}"
}

private fun formatMonthYear(m: String): String {
    val parts = m.split("-"); val y = parts[0].toInt(); val mo = parts[1].toInt()
    val names = listOf("January","February","March","April","May","June",
                       "July","August","September","October","November","December")
    return "${names[mo - 1]} $y"
}

private fun formatCompact(value: Double, currency: String): String {
    val sym = CurrencyManager.symbols[currency] ?: currency
    return when {
        value >= 1_000_000_000 -> "$sym${(value / 1_000_000_000).toInt()}B"
        value >= 1_000_000     -> "$sym${(value / 1_000_000).toInt()}M"
        value >= 1_000         -> "$sym${(value / 1_000).toInt()}K"
        else                   -> "$sym${value.toInt()}"
    }
}

// ── InsightScreen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightScreen(vm: AppViewModel) {
    val currency    = vm.currency.observeAsState("IDR").value
    val allTx       = vm.allTransactions.observeAsState(emptyList()).value
    val categories  = vm.allCategories.observeAsState(emptyList()).value
    val today       = vm.currentMonth

    var selectedMonth by remember { mutableStateOf(today) }
    val canGoNext = selectedMonth < today

    val monthTx      = allTx.filter { it.date.startsWith(selectedMonth) }
    val monthlyData  = vm.monthlyTotals(allTx, currency)
    val spending     = vm.spendingByCategory(monthTx, currency)
    val totalExp     = vm.totalInCurrency(monthTx, "Expense", currency)
    val totalInc     = vm.totalInCurrency(monthTx, "Income", currency)
    val savings      = totalInc - totalExp

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Insights") }) }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Month navigator
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { selectedMonth = prevMonth(selectedMonth) }) {
                        Icon(Icons.Default.ChevronLeft, "Previous month")
                    }
                    Text(
                        formatMonthYear(selectedMonth),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { if (canGoNext) selectedMonth = nextMonth(selectedMonth) },
                        enabled = canGoNext
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            "Next month",
                            tint = if (canGoNext) MaterialTheme.colorScheme.onSurface
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Income / Expense / Savings cards
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        label    = "Income",
                        value    = CurrencyManager.format(totalInc, currency),
                        color    = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label    = "Expense",
                        value    = CurrencyManager.format(totalExp, currency),
                        color    = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                SavingsCard(savings = savings, currency = currency)
            }

            // 6-month trend chart
            item {
                Text("6-Month Trend", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(IncomeGreen, "Income")
                            LegendDot(ExpenseRed, "Expense")
                        }
                        Spacer(Modifier.height(8.dp))
                        GradientAreaChart(data = monthlyData, currency = currency)
                    }
                }
            }

            // Spending donut chart
            if (spending.isNotEmpty() && totalExp > 0) {
                item {
                    Text("Spending Breakdown", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            val spendingList = spending.entries
                                .sortedByDescending { it.value }
                                .take(6)
                                .toList()
                            val segments = buildSegments(spendingList, categories, totalExp)

                            // Donut + legend side by side
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DonutChart(
                                    segments    = segments,
                                    centerLabel = CurrencyManager.format(totalExp, currency),
                                    modifier    = Modifier.size(160.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    segments.forEach { (color, fraction, label) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                Modifier
                                                    .size(10.dp)
                                                    .background(color, CircleShape)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                label,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1
                                            )
                                            Text(
                                                "${(fraction * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )

                            // Category bars below
                            spendingList.forEach { (catId, amt) ->
                                val cat = categories.find { it.id == catId }
                                val pct = if (totalExp > 0) amt / totalExp else 0.0
                                val barColor = try {
                                    Color(android.graphics.Color.parseColor(cat?.color ?: "#9CA3AF"))
                                } catch (e: Exception) { Color.Gray }

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat?.icon ?: "📦", fontSize = 18.sp, modifier = Modifier.width(28.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(
                                                cat?.name ?: "Unknown",
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                "${(pct * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            )
                                        }
                                        Spacer(Modifier.height(3.dp))
                                        LinearProgressIndicator(
                                            progress        = pct.toFloat(),
                                            modifier        = Modifier.fillMaxWidth().height(5.dp),
                                            color           = barColor,
                                            trackColor      = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                        Text(
                                            CurrencyManager.format(amt, currency),
                                            fontSize = 11.sp,
                                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Biggest expense
            val biggestExpense = monthTx.filter { it.type == "Expense" }.maxByOrNull { it.amountInUsd }
            if (biggestExpense != null) {
                item {
                    val cat = categories.find { it.id == biggestExpense.categoryId }
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔴", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Biggest Expense",
                                    fontSize = 11.sp,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(biggestExpense.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${cat?.icon ?: "📦"} ${cat?.name ?: "Unknown"} • ${biggestExpense.date}",
                                    fontSize = 12.sp,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                CurrencyManager.format(
                                    CurrencyManager.fromUsd(biggestExpense.amountInUsd, currency),
                                    currency
                                ),
                                color        = MaterialTheme.colorScheme.error,
                                fontWeight   = FontWeight.Bold,
                                textAlign    = TextAlign.End
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

internal data class DonutSegment(val color: Color, val fraction: Float, val label: String)

internal fun buildSegments(
    spendingList : List<Map.Entry<Int, Double>>,
    categories   : List<com.example.pockettrack.data.Category>,
    totalExp     : Double
): List<DonutSegment> {
    val segments = spendingList.map { (catId, amt) ->
        val cat = categories.find { it.id == catId }
        val color = try { Color(android.graphics.Color.parseColor(cat?.color ?: "#9CA3AF")) }
                    catch (e: Exception) { Color.Gray }
        val fraction = if (totalExp > 0) (amt / totalExp).toFloat() else 0f
        DonutSegment(color, fraction, cat?.name ?: "Unknown")
    }
    val covered = segments.sumOf { it.fraction.toDouble() }.toFloat()
    return if (covered < 0.98f) segments + DonutSegment(Color(0xFF6B7280), (1f - covered).coerceAtLeast(0f), "Other")
           else segments
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = color)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SavingsCard(savings: Double, currency: String) {
    val positive  = savings >= 0
    val color     = if (positive) IncomeGreen else ExpenseRed
    val label     = if (positive) "Net Savings" else "Deficit"
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (positive) "💚" else "🔴", fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                CurrencyManager.format(kotlin.math.abs(savings), currency),
                color      = color,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

// ── Gradient Area Chart ───────────────────────────────────────────────────────

@Composable
fun GradientAreaChart(
    data     : List<Pair<String, Pair<Double, Double>>>,
    currency : String
) {
    if (data.isEmpty()) return
    val maxVal    = data.maxOf { maxOf(it.second.first, it.second.second) }.coerceAtLeast(1.0)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f).toArgb()
    val density    = androidx.compose.ui.platform.LocalDensity.current.density

    Box(Modifier.fillMaxWidth().height(220.dp)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val yAxisW   = 50.dp.toPx()
            val topPad   = 16.dp.toPx()
            val botPad   = 24.dp.toPx()
            val chartL   = yAxisW
            val chartR   = size.width
            val chartT   = topPad
            val chartB   = size.height - botPad
            val chartH   = chartB - chartT
            val chartW   = chartR - chartL
            val n        = data.size
            val step     = if (n > 1) chartW / (n - 1) else chartW

            fun xOf(i: Int)  = chartL + i * step
            fun yOf(v: Double) = (chartB - (v / maxVal * chartH)).toFloat()

            // Horizontal grid lines (25 / 50 / 75 / 100 %)
            val gridColor = Color.Gray.copy(alpha = 0.12f)
            listOf(0.25, 0.5, 0.75, 1.0).forEach { pct ->
                val y = (chartB - pct * chartH).toFloat()
                drawLine(gridColor, Offset(chartL, y), Offset(chartR, y), strokeWidth = 1.dp.toPx())
            }

            // Smooth bezier path builder
            fun buildPath(points: List<Offset>): Path {
                val path = Path()
                if (points.isEmpty()) return path
                path.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val p = points[i - 1]; val c = points[i]
                    val mx = (p.x + c.x) / 2f
                    path.cubicTo(mx, p.y, mx, c.y, c.x, c.y)
                }
                return path
            }

            val incPts = data.mapIndexed { i, (_, v) -> Offset(xOf(i), yOf(v.first)) }
            val expPts = data.mapIndexed { i, (_, v) -> Offset(xOf(i), yOf(v.second)) }

            fun fillBelow(linePath: Path, pts: List<Offset>, colorTop: Color) {
                val fill = Path().apply {
                    addPath(linePath)
                    lineTo(pts.last().x,  chartB)
                    lineTo(pts.first().x, chartB)
                    close()
                }
                drawPath(fill, brush = Brush.verticalGradient(
                    listOf(colorTop.copy(alpha = 0.38f), Color.Transparent),
                    startY = chartT, endY = chartB
                ))
            }

            val incPath = buildPath(incPts)
            val expPath = buildPath(expPts)

            fillBelow(incPath, incPts, IncomeGreen)
            fillBelow(expPath, expPts, ExpenseRed)

            val stroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(incPath, color = IncomeGreen, style = stroke)
            drawPath(expPath, color = ExpenseRed,  style = stroke)

            // Data point dots
            fun drawDot(pt: Offset, color: Color) {
                drawCircle(Color.White, 4.5.dp.toPx(), pt)
                drawCircle(color, 3.dp.toPx(), pt)
            }
            incPts.forEach { drawDot(it, IncomeGreen) }
            expPts.forEach { drawDot(it, ExpenseRed) }

            // Y-axis labels via native canvas
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color     = labelColor
                    textSize  = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                listOf(1.0, 0.75, 0.5, 0.25, 0.0).forEach { pct ->
                    val y  = (chartB - pct * chartH).toFloat()
                    val lbl = formatCompact(maxVal * pct, currency)
                    canvas.nativeCanvas.drawText(lbl, chartL - 4.dp.toPx(), y + 3.dp.toPx(), paint)
                }
            }
        }

        // X-axis month labels
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(start = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (label, _) ->
                Text(
                    label,
                    fontSize = 10.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ── Donut Chart ───────────────────────────────────────────────────────────────

@Composable
internal fun DonutChart(
    segments    : List<DonutSegment>,
    centerLabel : String,
    modifier    : Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val strokeW = 24.dp.toPx()
            val radius  = (size.minDimension / 2f) - strokeW
            val center  = Offset(size.width / 2f, size.height / 2f)
            val gapDeg  = 3f

            // Background ring
            drawCircle(
                color  = Color.Gray.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style  = Stroke(strokeW)
            )

            var startAngle = -90f
            segments.forEach { (color, fraction, _) ->
                val sweep = (fraction * 360f) - gapDeg
                if (sweep > 0f) {
                    drawArc(
                        color      = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter  = false,
                        topLeft    = Offset(center.x - radius, center.y - radius),
                        size       = Size(radius * 2, radius * 2),
                        style      = Stroke(strokeW, cap = StrokeCap.Round)
                    )
                }
                startAngle += fraction * 360f
            }
        }
        Text(
            text       = centerLabel,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.padding(horizontal = 4.dp)
        )
    }
}
