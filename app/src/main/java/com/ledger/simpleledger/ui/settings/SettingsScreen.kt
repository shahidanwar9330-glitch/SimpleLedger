package com.ledger.simpleledger.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.ledger.simpleledger.data.drive.DriveBackupManager
import com.ledger.simpleledger.ui.SimpleViewModelFactory
import com.ledger.simpleledger.ui.components.ConfirmDialog
import com.ledger.simpleledger.ui.currentLedgerApp
import com.ledger.simpleledger.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = currentLedgerApp()
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SimpleViewModelFactory { SettingsViewModel(app.repository, app.backupManager, app.settingsPrefs) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.backup(context, uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirm = true
        }
    }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.onDriveSignInResult(context, account)
        } catch (e: ApiException) {
            viewModel.onDriveSignInResult(context, null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDriveAccount(context)
    }

    val driveConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.clearDriveConsentIntent()
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.backupToDriveNow(context)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            item {
                Text("App Update", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        when (state.updateStatus) {
                            UpdateCheckStatus.IDLE -> {
                                Text(
                                    "Installed build: ${if (state.installedBuild == 0) "first install" else "#${state.installedBuild}"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { viewModel.checkForUpdate() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Check for Update")
                                }
                            }
                            UpdateCheckStatus.CHECKING -> {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Checking for updates…")
                                }
                            }
                            UpdateCheckStatus.UP_TO_DATE -> {
                                Text("You're on the latest version.", color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { viewModel.checkForUpdate() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Check Again")
                                }
                            }
                            UpdateCheckStatus.AVAILABLE -> {
                                if (state.downloadedApkFile != null) {
                                    Text("Downloaded — tap to install", color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { installDownloadedApk(context, state.downloadedApkFile!!) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Install Update") }
                                } else {
                                    Text(
                                        "Update available: ${state.latestUpdate?.releaseName ?: ""}",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.downloadUpdate(context) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Download & Install") }
                                }
                            }
                            UpdateCheckStatus.DOWNLOADING -> {
                                Text("Downloading update… ${(state.downloadProgress * 100).toInt()}%")
                                Spacer(Modifier.height(8.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = state.downloadProgress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            UpdateCheckStatus.ERROR -> {
                                Text(state.updateError ?: "Something went wrong.", color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { viewModel.checkForUpdate() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Last Backup: " + if (state.lastBackupAt <= 0) "Never" else DateUtils.formatFullWithTime(state.lastBackupAt),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val fileName = "simple_ledger_backup_${System.currentTimeMillis()}.json"
                                    backupLauncher.launch(fileName)
                                },
                                enabled = !state.isWorking,
                                modifier = Modifier.weight(1f)
                            ) { Text("Backup Data") }
                            OutlinedButton(
                                onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                                enabled = !state.isWorking,
                                modifier = Modifier.weight(1f)
                            ) { Text("Restore Data") }
                        }
                        if (state.isWorking) {
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        }
                        state.message?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.primary)
                        }
                        state.error?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text("Cloud Backup (Google Drive)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        if (state.driveAccountEmail == null) {
                            Text(
                                "Connect your Google account to automatically back up every week.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { driveSignInLauncher.launch(DriveBackupManager.signInClient(context).signInIntent) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Connect Google Drive") }
                        } else {
                            Text("Connected: ${state.driveAccountEmail}", color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (state.lastDriveBackupAt > 0)
                                    "Last Drive backup: ${DateUtils.formatFullWithTime(state.lastDriveBackupAt)}"
                                else "Last Drive backup: Never",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            when (state.driveStatus) {
                                DriveBackupStatus.BACKING_UP -> {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Text("Backing up to Drive…")
                                    }
                                }
                                DriveBackupStatus.ERROR -> {
                                    Text(
                                        state.driveError ?: "Backup failed.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    if (state.driveConsentIntent != null) {
                                        Button(
                                            onClick = { driveConsentLauncher.launch(state.driveConsentIntent!!) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Allow Drive Access") }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                                else -> {}
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { viewModel.backupToDriveNow(context) }) {
                                    Text("Backup Now")
                                }
                                OutlinedButton(onClick = { viewModel.disconnectDrive(context) }) {
                                    Text("Disconnect")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Backs up automatically once every week, as long as you have internet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.darkModeOverride == "system",
                        onClick = { viewModel.setDarkMode("system") },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = state.darkModeOverride == "light",
                        onClick = { viewModel.setDarkMode("light") },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = state.darkModeOverride == "dark",
                        onClick = { viewModel.setDarkMode("dark") },
                        label = { Text("Dark") }
                    )
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text("Default Currency", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PKR", "USD", "AED", "SAR").forEach { cur ->
                        FilterChip(
                            selected = state.defaultCurrency == cur,
                            onClick = { viewModel.setCurrency(cur) },
                            label = { Text(cur) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Categories", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showCategoryDialog = true }) { Text("Manage") }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    categories.joinToString(", ") { it.name },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Future: WhatsApp/WeChat AI-detected transactions will appear here for your approval once enabled. No transaction is ever posted automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showRestoreConfirm && pendingRestoreUri != null) {
        ConfirmDialog(
            title = "Restore data?",
            message = "This will replace all current data on this device with the contents of the selected backup file. This cannot be undone.",
            confirmLabel = "Restore",
            destructive = true,
            onConfirm = {
                showRestoreConfirm = false
                pendingRestoreUri?.let { viewModel.restore(context, it) }
                pendingRestoreUri = null
            },
            onDismiss = {
                showRestoreConfirm = false
                pendingRestoreUri = null
            }
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Manage Categories") },
            text = {
                Column {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategoryText,
                            onValueChange = { newCategoryText = it },
                            label = { Text("New category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            viewModel.addCategory(newCategoryText)
                            newCategoryText = ""
                        }) { Text("Add") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column {
                        categories.forEach { c ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(c.name)
                                IconButton(onClick = { viewModel.deleteCategory(c) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("Done") }
            }
        )
    }
}

/** Launches the system installer for a downloaded APK using the proper APK installation intent.
 * This uses ACTION_INSTALL_PACKAGE which is more reliable than ACTION_VIEW.
 * First time this is used, Android will ask for "install unknown apps" permission. */
private fun installDownloadedApk(context: Context, file: java.io.File) {
    val uri: Uri = FileProvider.getUriForFile(context, "com.ledger.simpleledger.fileprovider", file)
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        data = uri
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}
