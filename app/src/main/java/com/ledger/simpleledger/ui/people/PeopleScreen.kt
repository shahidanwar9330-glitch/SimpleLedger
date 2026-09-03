package com.ledger.simpleledger.ui.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledger.simpleledger.ui.SimpleViewModelFactory
import com.ledger.simpleledger.ui.components.EmptyState
import com.ledger.simpleledger.ui.components.PersonAvatar
import com.ledger.simpleledger.ui.currentLedgerApp
import com.ledger.simpleledger.ui.theme.LocalLedgerColors
import com.ledger.simpleledger.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    onOpenPerson: (Long) -> Unit,
    onAddPerson: () -> Unit
) {
    val app = currentLedgerApp()
    val viewModel: PeopleViewModel = viewModel(
        factory = SimpleViewModelFactory { PeopleViewModel(app.repository) }
    )
    val query by viewModel.query.collectAsStateWithLifecycle()
    val all by viewModel.people.collectAsStateWithLifecycle()
    val list = viewModel.filtered(all, query)
    val colors = LocalLedgerColors.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("People") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPerson,
                icon = { Icon(Icons.Filled.PersonAddAlt, contentDescription = null) },
                text = { Text("Add Person") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(if (all.isEmpty()) "Search people" else "${all.size} People") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (list.isEmpty()) {
                EmptyState(title = "No people yet", subtitle = "Tap Add Person to add your first person or account.")
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(list, key = { it.id }) { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPerson(p.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PersonAvatar(p.name)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (p.balanceMinor == 0L) "Settled up"
                                    else if (p.balanceMinor > 0) "You'll get" else "You'll give",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                Money.format(kotlin.math.abs(p.balanceMinor)),
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    p.balanceMinor > 0 -> colors.liya
                                    p.balanceMinor < 0 -> colors.diya
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
