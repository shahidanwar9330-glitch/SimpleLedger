package com.ledger.simpleledger.ui.persondetail

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledger.simpleledger.data.model.TransactionType
import com.ledger.simpleledger.ui.SimpleViewModelFactory
import com.ledger.simpleledger.ui.components.ConfirmDialog
import com.ledger.simpleledger.ui.components.EmptyState
import com.ledger.simpleledger.ui.components.PersonAvatar
import com.ledger.simpleledger.ui.currentLedgerApp
import com.ledger.simpleledger.ui.theme.LocalLedgerColors
import com.ledger.simpleledger.util.DateUtils
import com.ledger.simpleledger.util.Money
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onEditPerson: (Long) -> Unit,
    onNewTransactionForPerson: (type: String, personId: Long) -> Unit,
    onDeleted: () -> Unit
) {
    val app = currentLedgerApp()
    val context = LocalContext.current
    val viewModel: PersonDetailViewModel = viewModel(
        factory = SimpleViewModelFactory { PersonDetailViewModel(app.repository, personId) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalLedgerColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.person?.name ?: "Person") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { onEditPerson(personId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        bottomBar = {
            val person = state.person
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNewTransactionForPerson("DIYA", personId) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.diya),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("MAINE DIYA", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { onNewTransactionForPerson("LIYA", personId) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.liya),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("MAINE LIYA", fontWeight = FontWeight.Bold) }
            }
        }
    ) { padding ->
        val person = state.person
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (person != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PersonAvatar(person.name, size = 52.dp)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    Money.format(kotlin.math.abs(person.balanceMinor)),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (person.balanceMinor >= 0) colors.liya else colors.diya,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    when {
                                        person.balanceMinor > 0 -> "You'll get"
                                        person.balanceMinor < 0 -> "You'll give"
                                        else -> "Settled up"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Filled.Assessment,
                        label = "Report",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Detailed reports are on the Reports tab") }
                        }
                    )
                    QuickActionButton(
                        icon = Icons.Filled.Share,
                        label = "Share",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val text = buildString {
                                append("${person.name}: ")
                                append(
                                    when {
                                        person.balanceMinor > 0 -> "${Money.format(person.balanceMinor)} to receive"
                                        person.balanceMinor < 0 -> "${Money.format(kotlin.math.abs(person.balanceMinor))} to pay"
                                        else -> "Settled up"
                                    }
                                )
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share balance"))
                        }
                    )
                    QuickActionButton(
                        icon = Icons.Filled.NotificationsNone,
                        label = "Reminder",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Reminders aren't available yet") }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.transactions.isEmpty() && !state.loading) {
                EmptyState(title = "No transactions with this person yet", subtitle = "Use Maine Diya / Maine Liya below to add the first entry.")
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Entries", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("Maine Diya", style = MaterialTheme.typography.labelMedium, color = colors.diya, modifier = Modifier.weight(1f))
                    Text("Maine Liya", style = MaterialTheme.typography.labelMedium, color = colors.liya, modifier = Modifier.weight(1f))
                }
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(state.transactions, key = { it.transaction.id }) { row ->
                        val t = row.transaction
                        val isLiya = t.type == TransactionType.LIYA
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { onOpenTransaction(t.id) })
                                .padding(vertical = 10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(DateUtils.formatShort(t.date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(t.categoryName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "Bal. ${Money.formatSigned(row.runningBalanceMinor)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                if (!isLiya) {
                                    AmountPill(Money.format(t.amountMinor, t.currency), colors.diya)
                                }
                            }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                if (isLiya) {
                                    AmountPill(Money.format(t.amountMinor, t.currency), colors.liya)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete this person?",
            message = "This will also delete all of their transactions. This cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deletePerson(onDeleted)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AmountPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}
