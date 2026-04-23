package com.example.pockettrack.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, vm: AppViewModel) {
    val currency  = vm.currency.observeAsState("IDR").value
    val themeMode = vm.themeMode.observeAsState("System").value
    val exchangeRates = vm.exchangeRates.observeAsState().value
    val rateInfo = exchangeRates?.info
    var currencyExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })

        Column(
            Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Preferences section ──
            Text(
                "Preferences",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(Modifier.fillMaxWidth()) {
                Column {
                    // Theme mode selector
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Theme", fontWeight = FontWeight.Medium)
                                Text(
                                    when (themeMode) {
                                        "Dark"  -> "Futuristic dark theme"
                                        "Light" -> "Light theme"
                                        else    -> "Follows system setting"
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf("System", "Light", "Dark").forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = themeMode == mode,
                                    onClick  = { vm.setThemeMode(mode) },
                                    shape    = SegmentedButtonDefaults.itemShape(index, 3),
                                    icon     = {},
                                    label    = { Text(mode, fontSize = 13.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Currency selector
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Display Currency", fontWeight = FontWeight.Medium)
                            Text(
                                "${CurrencyManager.symbols[currency]}  $currency",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = currencyExpanded,
                            onExpandedChange = { currencyExpanded = it }
                        ) {
                            OutlinedButton(
                                onClick = { currencyExpanded = true },
                                modifier = Modifier
                                    .widthIn(min = 104.dp)
                                    .menuAnchor()
                            ) {
                                Text(
                                    "${CurrencyManager.symbols[currency]} $currency",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(4.dp))
                                ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded)
                            }
                            ExposedDropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false }
                            ) {
                                CurrencyManager.currencies.forEach { c ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    CurrencyManager.symbols[c] ?: c,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(32.dp)
                                                )
                                                Text(c, maxLines = 1)
                                            }
                                        },
                                        onClick = {
                                            vm.setCurrency(c)
                                            currencyExpanded = false
                                        },
                                        trailingIcon = {
                                            if (c == currency) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Manage section ──
            Text(
                "Manage",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(
                        icon     = Icons.Default.Category,
                        title    = "Categories",
                        subtitle = "Add, edit or delete categories",
                        onClick  = { nav.navigate("categories") }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsRow(
                        icon     = Icons.Default.Info,
                        title    = "About",
                        subtitle = "PocketTrack v2.0"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Exchange rate note ──
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Exchange Rate Info", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        if (exchangeRates?.isLoading == true) {
                            Text("Refreshing latest rates...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        rateInfo?.rates?.forEach { (code, rate) ->
                            Text(
                                "1 ${rateInfo.base} = ${CurrencyManager.format(rate, code)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        rateInfo?.let { info ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Updated: ${info.updatedOn} via ${info.sourceLabel}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        exchangeRates?.errorMessage?.let { message ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                message,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Latest fetched rates are used for display conversion. All amounts are still stored in USD internally.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { vm.refreshExchangeRates() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh exchange rates",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
}
