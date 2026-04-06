package com.example.pockettrack.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, vm: AppViewModel) {
    val currency = vm.currency.observeAsState("USD").value
    var currencyExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })

        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Preferences section ──
            Text(
                "Preferences",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                            color = Color.Gray
                        )
                    }
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it }
                    ) {
                        OutlinedButton(
                            onClick = { currencyExpanded = true },
                            modifier = Modifier.menuAnchor()
                        ) {
                            Text(currency)
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                CurrencyManager.symbols[c] ?: c,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(32.dp)
                                            )
                                            Text(c)
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

            Spacer(Modifier.height(8.dp))

            // ── Manage section ──
            Text(
                "Manage",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.Gray,
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
                    HorizontalDivider(color = Color.Gray.copy(0.1f))
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
                    Column {
                        Text("Exchange Rate Info", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("1 USD  =  1.35 SGD", fontSize = 12.sp, color = Color.Gray)
                        Text("1 USD  =  16,100 IDR", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Rates are approximate. All amounts stored in USD internally for accurate comparison.",
                            fontSize = 11.sp, color = Color.Gray
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
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
}