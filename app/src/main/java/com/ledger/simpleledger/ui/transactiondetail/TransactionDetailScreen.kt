package com.ledger.simpleledger.ui.transactiondetail

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ledger.simpleledger.data.model.TransactionType
import com.ledger.simpleledger.ui.SimpleViewModelFactory
import com.ledger.simpleledger.ui.components.ConfirmDialog
import com.ledger.simpleledger.ui.currentLedgerApp
import com.ledger.simpleledger.ui.theme.LocalLedgerColors
import com.ledger.simpleledger.util.DateUtils
import com.ledger.simpleledger.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val app = currentLedgerApp()
    val viewModel: TransactionDetailViewModel = viewModel(
        factory = SimpleViewModelFactory { TransactionDetailViewModel(app.repository, transactionId) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalLedgerColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val t = state.transaction
        if (t == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("This transaction no longer exists.")
            }
            return@Scaffold
        }
        val isLiya = t.type == TransactionType.LIYA
        val accent = if (isLiya) colors.liya else colors.diya

        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        if (isLiya) "MAINE LIYA" else "MAINE DIYA",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Money.format(t.amountMinor, t.currency),
                        style = MaterialTheme.typography.displaySmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            DetailRow("Person", state.person?.name ?: "Unknown")
            DetailRow("Category", t.categoryName)
            DetailRow("Date", DateUtils.formatFull(t.date))
            if (!t.note.isNullOrBlank()) DetailRow("Note", t.note)
            if (!t.paymentMethod.isNullOrBlank()) DetailRow("Payment Method", t.paymentMethod)
            if (!t.reference.isNullOrBlank()) DetailRow("Reference", t.reference)

            if (!t.attachmentUri.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = t.attachmentUri,
                    contentDescription = "Attached image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            if (!t.voiceNotePath.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                var isPlaying by remember { mutableStateOf(false) }
                var player by remember { mutableStateOf<MediaPlayer?>(null) }
                DisposableEffect(t.voiceNotePath) {
                    onDispose { player?.release() }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (isPlaying) {
                            player?.pause()
                            isPlaying = false
                        } else {
                            if (player == null) {
                                player = MediaPlayer().apply {
                                    setDataSource(t.voiceNotePath)
                                    prepare()
                                    setOnCompletionListener { isPlaying = false }
                                }
                            }
                            player?.start()
                            isPlaying = true
                        }
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause voice note" else "Play voice note"
                        )
                    }
                    Text("Voice note")
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onEdit(t.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(" Edit")
                }
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text(" Delete")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete transaction?",
            message = "This cannot be undone. Are you sure you want to delete this transaction?",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete(onDeleted)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
