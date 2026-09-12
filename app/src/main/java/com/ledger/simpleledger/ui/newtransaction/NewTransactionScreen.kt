package com.ledger.simpleledger.ui.newtransaction

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ledger.simpleledger.data.model.TransactionType
import com.ledger.simpleledger.ui.SimpleViewModelFactory
import com.ledger.simpleledger.ui.currentLedgerApp
import com.ledger.simpleledger.ui.theme.LocalLedgerColors
import com.ledger.simpleledger.util.AttachmentStorage
import com.ledger.simpleledger.util.DateUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen(
    initialType: String?,
    editId: Long?,
    personId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val app = currentLedgerApp()
    val viewModel: NewTransactionViewModel = viewModel(
        factory = SimpleViewModelFactory {
            NewTransactionViewModel(app.repository, app.settingsPrefs, initialType, editId, personId)
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalLedgerColors.current
    val context = LocalContext.current

    var personMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var pendingRecordingFile by remember { mutableStateOf<File?>(null) }

    fun startRecording() {
        val file = AttachmentStorage.newVoiceNoteFile(context)
        pendingRecordingFile = file
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // Very short recordings can throw on stop(); the partial file is discarded below.
        }
        recorder?.release()
        recorder = null
        isRecording = false
        val file = pendingRecordingFile
        if (file != null && file.exists() && file.length() > 0) {
            viewModel.setVoiceNote(file.absolutePath)
        }
        pendingRecordingFile = null
    }

    fun togglePlayback() {
        val path = state.voiceNotePath ?: return
        if (isPlaying) {
            player?.pause()
            isPlaying = false
        } else {
            if (player == null) {
                player = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    setOnCompletionListener { isPlaying = false }
                }
            }
            player?.start()
            isPlaying = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
            player?.release()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startRecording() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            AttachmentStorage.copyImage(context, uri)?.let { path -> viewModel.setAttachment(path) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editId == null) "New Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Big LIYA / DIYA toggle
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeToggleCard(
                    label = "MAINE LIYA",
                    selected = state.type == TransactionType.LIYA,
                    accent = colors.liya,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setType(TransactionType.LIYA) }
                )
                TypeToggleCard(
                    label = "MAINE DIYA",
                    selected = state.type == TransactionType.DIYA,
                    accent = colors.diya,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setType(TransactionType.DIYA) }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Person
            ExposedDropdownMenuBox(
                expanded = personMenuExpanded && state.people.isNotEmpty(),
                onExpandedChange = { personMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedPersonName,
                    onValueChange = {
                        viewModel.setPersonNameOnly(it)
                        personMenuExpanded = true
                    },
                    label = { Text("Person / Account") },
                    isError = state.personError != null,
                    supportingText = { state.personError?.let { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                val filtered = state.people.filter {
                    state.selectedPersonName.isBlank() || it.name.contains(state.selectedPersonName, ignoreCase = true)
                }
                DropdownMenu(
                    expanded = personMenuExpanded && filtered.isNotEmpty(),
                    onDismissRequest = { personMenuExpanded = false }
                ) {
                    filtered.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                viewModel.selectPerson(p.id, p.name)
                                personMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Amount + currency
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = viewModel::setAmount,
                    label = { Text("Amount") },
                    isError = state.amountError != null,
                    supportingText = { state.amountError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = state.currency,
                    onValueChange = { /* handled via ViewModel if editable in future */ },
                    label = { Text("Currency") },
                    modifier = Modifier.weight(1f),
                    readOnly = false
                )
            }

            Spacer(Modifier.height(12.dp))

            // Date
            OutlinedTextField(
                value = DateUtils.formatFull(state.dateMillis),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            Spacer(Modifier.height(12.dp))

            // Category
            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = { viewModel.setCategory(it) },
                    label = { Text("Category") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    state.categories.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name) },
                            onClick = {
                                viewModel.setCategory(c.name)
                                categoryMenuExpanded = false
                            }
                        )
                    }
                    if (state.category.isNotBlank() && state.categories.none { it.name.equals(state.category, ignoreCase = true) }) {
                        DropdownMenuItem(
                            text = { Text("+ Add \"${state.category}\" as new category") },
                            onClick = {
                                viewModel.addNewCategory(state.category)
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Description / Note") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Attach (optional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            if (state.attachmentUri != null) {
                Box(Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = state.attachmentUri,
                        contentDescription = "Attached image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    IconButton(
                        onClick = { viewModel.setAttachment(null) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove image", tint = Color.White)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Image")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.voiceNotePath != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { togglePlayback() }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause voice note" else "Play voice note"
                        )
                    }
                    Text("Voice note", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        player?.release()
                        player = null
                        isPlaying = false
                        AttachmentStorage.deleteIfExists(state.voiceNotePath)
                        viewModel.setVoiceNote(null)
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove voice note")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) startRecording()
                            else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRecording) "Stop Recording" else "Record Voice Note")
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("SAVE TRANSACTION", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TypeToggleCard(
    label: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontWeight = FontWeight.Bold,
                color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
