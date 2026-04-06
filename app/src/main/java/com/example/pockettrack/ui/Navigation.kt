package com.example.pockettrack.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*
import com.example.pockettrack.viewmodel.AppViewModel

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNavigation(vm: AppViewModel) {
    val nav = rememberNavController()
    val items = listOf(
        NavItem("home",         "Home",         Icons.Default.Home),
        NavItem("transactions", "Transactions", Icons.Default.List),
        NavItem("budget",       "Budget",       Icons.Default.AccountBalanceWallet),
        NavItem("insights",     "Insights",     Icons.Default.BarChart),
        NavItem("settings",     "Settings",     Icons.Default.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val back by nav.currentBackStackEntryAsState()
                val current = back?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.route,
                        onClick = {
                            nav.navigate(item.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home")         { HomeScreen(nav, vm) }
            composable("transactions") { TransactionScreen(nav, vm) }
            composable("add")          { AddTransactionScreen(nav, vm) }
            composable("edit/{id}") { back ->
                val id = back.arguments?.getString("id")?.toIntOrNull()
                AddTransactionScreen(nav, vm, editId = id)
            }
            composable("budget")       { BudgetScreen(vm) }
            composable("insights")     { InsightScreen(vm) }
            composable("categories")   { CategoryScreen(vm) }
            composable("settings")     { SettingsScreen(nav, vm) }
        }
    }
}