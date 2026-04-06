package com.example.pockettrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                items(transactions) { t ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            Text(
                                "${if (t.type == "Income") "+" else "-"}$${"%.2f".format(t.amount)}",
                                color = if (t.type == "Income") Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            IconButton(onClick = { transactionToDelete = t }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}