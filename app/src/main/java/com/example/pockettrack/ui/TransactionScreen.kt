package com.example.pockettrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pockettrack.data.Transaction
import com.example.pockettrack.viewmodel.AppViewModel
import com.example.pockettrack.viewmodel.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(nav: NavController, vm: AppViewModel) {
    val currency    = vm.currency.observeAsState("IDR").value
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
                Text("${if (t.type == "Income") "+" else "-"}${CurrencyManager.format(t.amount, t.currency)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            },
            confirmLabel = "Delete", isDestructive = true,
            onConfirm = { vm.deleteTransaction(t) },
            onDismiss = { txToDelete = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
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
                    Text("No transactions found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { tx ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == EndToStart) {
                                    txToDelete = tx
                                }
                                false
                            },
                            positionalThreshold = { distance -> distance * 0.3f }
                        )

                        SwipeToDismissBox(
                            modifier = Modifier.fillMaxWidth(),
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.graphicsLayer {
                                            alpha = if (
                                                dismissState.dismissDirection == EndToStart ||
                                                dismissState.targetValue == EndToStart
                                            ) 1f else 0f
                                        }
                                    )
                                }
                            }
                        ) {
                            TxCard(
                                tx,
                                categories,
                                currency,
                                onClick = { nav.navigate("edit/${tx.id}") }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }
}
