package com.example.pockettrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.example.pockettrack.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(nav: NavController, vm: TransactionViewModel) {
    val transactions = vm.transactions.observeAsState(emptyList()).value
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // ── Delete confirmation dialog ──
    transactionToDelete?.let { t ->
        ConfirmDialog(
            title = "Delete Transaction",
            message = {
                Text("Are you sure you want to delete this transaction?")
                Spacer(Modifier.height(4.dp))
                Text("📝  Title     : ${t.title}")
                Text("💰  Amount  : $${"%.2f".format(t.amount)}")
                Text("🔖  Type      : ${t.type}")
                Text("📂  Category: ${t.category}")
            },
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = { vm.deleteTransaction(t) },
            onDismiss = { transactionToDelete = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("All Transactions") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = transactions,
                    key = { it.id }
                ) { t ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == EndToStart) {
                                transactionToDelete = t
                            }
                            false
                        },
                        positionalThreshold = { it * 0.35f }
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
                                        alpha = if (dismissState.targetValue == EndToStart ||
                                            dismissState.dismissDirection == EndToStart
                                        ) 1f else 0f
                                    }
                                )
                            }
                        }
                    ) {
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${t.category} • ${t.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        t.type,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${if (t.type == "Income") "+" else "-"}$${"%.2f".format(t.amount)}",
                                    color = if (t.type == "Income") Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
