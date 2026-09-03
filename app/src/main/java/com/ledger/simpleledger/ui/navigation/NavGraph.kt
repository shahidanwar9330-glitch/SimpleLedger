package com.ledger.simpleledger.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.ledger.simpleledger.ui.addperson.AddEditPersonScreen
import com.ledger.simpleledger.ui.dashboard.DashboardScreen
import com.ledger.simpleledger.ui.newtransaction.NewTransactionScreen
import com.ledger.simpleledger.ui.people.PeopleScreen
import com.ledger.simpleledger.ui.persondetail.PersonDetailScreen
import com.ledger.simpleledger.ui.reports.ReportsScreen
import com.ledger.simpleledger.ui.settings.SettingsScreen
import com.ledger.simpleledger.ui.transactiondetail.TransactionDetailScreen
import com.ledger.simpleledger.ui.transactions.TransactionsScreen

private fun iconFor(route: String) = when (route) {
    Screen.Dashboard.route -> Icons.Filled.Home
    Screen.Transactions.route -> Icons.Filled.Receipt
    Screen.People.route -> Icons.Filled.People
    Screen.Reports.route -> Icons.Filled.PieChart
    Screen.Settings.route -> Icons.Filled.Settings
    else -> Icons.Filled.Home
}

private fun labelFor(route: String) = when (route) {
    Screen.Dashboard.route -> "Home"
    Screen.Transactions.route -> "Transactions"
    Screen.People.route -> "People"
    Screen.Reports.route -> "Reports"
    Screen.Settings.route -> "Settings"
    else -> route
}

@Composable
fun SimpleLedgerNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            val onBottomLevel = bottomNavScreens.any { s ->
                currentDestination?.hierarchy?.any { it.route == s.route } == true
            }
            if (onBottomLevel) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(iconFor(screen.route), contentDescription = labelFor(screen.route)) },
                            label = { Text(labelFor(screen.route)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNewTransaction = { type -> navController.navigate(Screen.NewTransaction.build(type = type)) },
                    onOpenTransaction = { id -> navController.navigate(Screen.TransactionDetail.build(id)) }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onOpenTransaction = { id -> navController.navigate(Screen.TransactionDetail.build(id)) }
                )
            }

            composable(Screen.People.route) {
                PeopleScreen(
                    onOpenPerson = { id -> navController.navigate(Screen.PersonDetail.build(id)) },
                    onAddPerson = { navController.navigate(Screen.AddEditPerson.build()) }
                )
            }

            composable(Screen.Reports.route) { ReportsScreen() }

            composable(Screen.Settings.route) { SettingsScreen() }

            composable(
                route = Screen.NewTransaction.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("editId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("personId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type")?.ifBlank { null }
                val editId = backStackEntry.arguments?.getString("editId")?.ifBlank { null }?.toLongOrNull()
                val personId = backStackEntry.arguments?.getString("personId")?.ifBlank { null }?.toLongOrNull()
                NewTransactionScreen(
                    initialType = type,
                    editId = editId,
                    personId = personId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: return@composable
                TransactionDetailScreen(
                    transactionId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { editId -> navController.navigate(Screen.NewTransaction.build(editId = editId)) },
                    onDeleted = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PersonDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: return@composable
                PersonDetailScreen(
                    personId = id,
                    onBack = { navController.popBackStack() },
                    onOpenTransaction = { txId -> navController.navigate(Screen.TransactionDetail.build(txId)) },
                    onEditPerson = { pid -> navController.navigate(Screen.AddEditPerson.build(pid)) },
                    onNewTransactionForPerson = { type, pid ->
                        navController.navigate(Screen.NewTransaction.build(type = type, personId = pid))
                    },
                    onDeleted = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddEditPerson.route,
                arguments = listOf(navArgument("editId") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { backStackEntry ->
                val editId = backStackEntry.arguments?.getString("editId")?.ifBlank { null }?.toLongOrNull()
                AddEditPersonScreen(
                    editId = editId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}
