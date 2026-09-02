package com.ledger.simpleledger.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.simpleledger.ui.SimpleViewModelFactory
import com.ledger.simpleledger.ui.components.BalanceCard
import com.ledger.simpleledger.ui.components.EmptyState
import com.ledger.simpleledger.ui.components.SectionHeader
import com.ledger.simpleledger.ui.components.SummaryCard
import com.ledger.simpleledger.ui.components.TransactionRow
import com.ledger.simpleledger.ui.currentLedgerApp
import com.ledger.simpleledger.ui.theme.LocalLedgerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNewTransaction: (String?) -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val app = currentLedgerApp()
    val viewModel: DashboardViewModel = viewModel(
        factory = SimpleViewModelFactory { DashboardViewModel(app.repository, app.settingsPrefs) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalLedgerColors.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Simple Ledger", style = MaterialTheme.typography.titleLarge) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNewTransaction(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Transaction") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BalanceCard(state.balanceMinor, state.currency, Modifier.fillMaxWidth())
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "Maine Liya",
                        amountMinor = state.totalLiyaMinor,
                        currency = state.currency,
                        accentColor = colors.liya,
                        modifier = Modifier.weight(1f),
                        onClick = { onNewTransaction("LIYA") }
                    )
                    SummaryCard(
                        title = "Maine Diya",
                        amountMinor = state.totalDiyaMinor,
                        currency = state.currency,
                        accentColor = colors.diya,
                        modifier = Modifier.weight(1f),
                        onClick = { onNewTransaction("DIYA") }
                    )
                }
            }
            item { SectionHeader("Recent Transactions") }
            if (state.recent.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "No transactions yet",
                        subtitle = "Tap + New Transaction to record your first Maine Liya or Maine Diya entry."
                    )
                }
            } else {
                items(state.recent, key = { it.id }) { item ->
                    TransactionRow(item, onClick = { onOpenTransaction(item.id) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}
