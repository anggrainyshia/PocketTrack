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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pockettrack.data.Transaction
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(nav: NavController, vm: AppViewModel) {
    val currency    = vm.currency.observeAsState("USD").value
    val categories  = vm.allCategories.observeAsState(emptyList()).value
    var query       by remember { mutableStateOf("") }
    var filterType  by remember { mutableStateOf("All") } // All, Income, Expense
    var txToDelete  by remember { mutableStateOf<Transaction?>(null) }

    val baseTx = if (query.isBlank()) vm.allTransactions.observeAsState(emptyList()).value
    else vm.search(query).observeAsState(emptyList()).value

    val filtered = when (filterType) {
        "Income"  -> baseTx.filter { it.type == "Income" }
        "Expense" -> baseTx.filter { it.type == "Expense" }
        else      -> baseTx
    }

    txToDelete?.let { t ->
        ConfirmDialog(
            title = "Delete Transaction",
            message = {
                Text("Delete \"${t.title}\"?")
                Text("${if (t.type == "Income") "+" else "-"}${CurrencyManager.format(t.amount, t.currency)}", color = Color.Gray)
            },
            confirmLabel = "Delete", isDestructive = true,
            onConfirm = { vm.deleteTransaction(t) },
            onDismiss = { txToDelete = null }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transactions") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("add") }) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            // Search bar
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) } },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Income", "Expense").forEach { f ->
                    FilterChip(selected = filterType == f, onClick = { filterType = f }, label = { Text(f) })
                }
            }
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { tx ->
                        Box {
                            TxCard(tx, categories, currency, onClick = { nav.navigate("edit/${tx.id}") })
                            IconButton(
                                onClick = { txToDelete = tx },
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                            ) { Icon(Icons.Default.Delete, null, tint = Color.Gray.copy(.6f)) }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}
