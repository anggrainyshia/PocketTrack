package com.example.pockettrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.example.pockettrack.viewmodel.AppViewModel

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNavigation(vm: AppViewModel) {
    vm.exchangeRates
    val nav  = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val current = back?.destination?.route

    val items = listOf(
        NavItem("home",         "Home",     Icons.Default.Home),
        NavItem("transactions", "History",  Icons.Default.List),
        NavItem("budget",       "Budget",   Icons.Default.AccountBalanceWallet),
        NavItem("insights",     "Insights", Icons.Default.BarChart),
        NavItem("settings",     "Settings", Icons.Default.Settings),
    )

    // Pages that show the bottom bar
    val bottomBarRoutes = items.map { it.route }.toSet()
    val showBottomBar   = current in bottomBarRoutes

    Scaffold(
        // Zero out system-inset consumption so inner screens control their own insets
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                FloatingBottomNav(items = items, current = current) { route ->
                    nav.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home")         { HomeScreen(nav, vm) }
            composable("transactions") { TransactionScreen(nav, vm) }
            composable("add")          { AddTransactionScreen(nav, vm) }
            composable("edit/{id}") { back ->
                val id = back.arguments?.getString("id")?.toIntOrNull()
                AddTransactionScreen(nav, vm, editId = id)
            }
            composable("budget")       { BudgetScreen(vm) }
            composable("insights")     { InsightScreen(vm) }
            composable("categories")   { CategoryScreen(nav, vm) }
            composable("settings")     { SettingsScreen(nav, vm) }
        }
    }
}

@Composable
private fun FloatingBottomNav(
    items: List<NavItem>,
    current: String?,
    onNavigate: (String) -> Unit
) {
    // Container that draws behind the system navigation bar
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color     = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation  = 3.dp
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag-handle indicator (decorative, matches reference image)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
            )
            Spacer(Modifier.height(4.dp))

            // Nav items row — padded above the system navigation bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = current == item.route
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null
                            ) { onNavigate(item.route) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Icon with pill highlight for selected state
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .then(
                                    if (isSelected)
                                        Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            )
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = item.icon,
                                contentDescription = item.label,
                                modifier           = Modifier.size(22.dp),
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Label
                        Text(
                            text       = item.label,
                            fontSize   = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines   = 1
                        )
                    }
                }
            }
        }
    }
}
