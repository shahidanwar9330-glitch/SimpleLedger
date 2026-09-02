package com.ledger.simpleledger.ui.reports

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledger.simpleledger.ui.SimpleViewModelFactory
import com.ledger.simpleledger.ui.components.EmptyState
import com.ledger.simpleledger.ui.currentLedgerApp
import com.ledger.simpleledger.ui.theme.LocalLedgerColors
import com.ledger.simpleledger.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val app = currentLedgerApp()
    val viewModel: ReportsViewModel = viewModel(
        factory = SimpleViewModelFactory { ReportsViewModel(app.repository) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalLedgerColors.current
    var showRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.period == ReportPeriod.TODAY,
                        onClick = { viewModel.setPeriod(ReportPeriod.TODAY) },
                        label = { Text("Today") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.period == ReportPeriod.THIS_WEEK,
                        onClick = { viewModel.setPeriod(ReportPeriod.THIS_WEEK) },
                        label = { Text("This Week") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.period == ReportPeriod.THIS_MONTH,
                        onClick = { viewModel.setPeriod(ReportPeriod.THIS_MONTH) },
                        label = { Text("This Month") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.period == ReportPeriod.CUSTOM,
                        onClick = { showRangePicker = true },
                        label = { Text("Custom Range") }
                    )
                }
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "Net Balance",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                Money.formatSigned(state.netBalanceMinor),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(Modifier.weight(1f)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Maine Liya", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Money.format(state.totalLiyaMinor), color = colors.liya, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(Modifier.weight(1f)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Maine Diya", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Money.format(state.totalDiyaMinor), color = colors.diya, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }

                if (state.categoryTotals.isEmpty() && !state.loading) {
                    item { EmptyState(title = "No transactions in this period", subtitle = "Try a different date range.") }
                } else {
                    items(state.categoryTotals) { c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(c.categoryName, style = MaterialTheme.typography.bodyLarge)
                            Text(Money.format(c.totalMinor), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showRangePicker) {
        val rangeState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val s = rangeState.selectedStartDateMillis
                    val e = rangeState.selectedEndDateMillis
                    if (s != null && e != null) viewModel.setCustomRange(s, e)
                    showRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(state = rangeState, modifier = Modifier.height(500.dp))
        }
    }
}
