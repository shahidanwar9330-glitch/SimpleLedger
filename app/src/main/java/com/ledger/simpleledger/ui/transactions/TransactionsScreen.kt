package com.ledger.simpleledger.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledger.simpleledger.ui.SimpleViewModelFactory
import com.ledger.simpleledger.ui.components.EmptyState
import com.ledger.simpleledger.ui.components.TransactionRow
import com.ledger.simpleledger.ui.currentLedgerApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(onOpenTransaction: (Long) -> Unit) {
    val app = currentLedgerApp()
    val viewModel: TransactionsViewModel = viewModel(
        factory = SimpleViewModelFactory { TransactionsViewModel(app.repository) }
    )
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val items by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transactions") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search person, category, note, reference") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter.quickFilter == QuickFilter.ALL,
                        onClick = { viewModel.setQuickFilter(QuickFilter.ALL) },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter.quickFilter == QuickFilter.LIYA,
                        onClick = { viewModel.setQuickFilter(QuickFilter.LIYA) },
                        label = { Text("Maine Liya") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter.quickFilter == QuickFilter.DIYA,
                        onClick = { viewModel.setQuickFilter(QuickFilter.DIYA) },
                        label = { Text("Maine Diya") }
                    )
                }
                items(categories) { c ->
                    FilterChip(
                        selected = filter.category == c.name,
                        onClick = {
                            viewModel.setCategory(if (filter.category == c.name) null else c.name)
                        },
                        label = { Text(c.name) }
                    )
                }
            }

            if (items.isEmpty()) {
                EmptyState(
                    title = "No transactions found",
                    subtitle = "Try a different search term or clear the filters."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        TransactionRow(item, onClick = { onOpenTransaction(item.id) })
                    }
                }
            }
        }
    }
}
