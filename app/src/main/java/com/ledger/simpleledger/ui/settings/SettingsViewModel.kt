package com.ledger.simpleledger.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.SettingsPrefs
import com.ledger.simpleledger.data.backup.BackupManager
import com.ledger.simpleledger.data.backup.RestoreResult
import com.ledger.simpleledger.data.db.entities.CategoryEntity
import com.ledger.simpleledger.data.drive.DriveBackupManager
import com.ledger.simpleledger.data.drive.DriveBackupWorker
import com.ledger.simpleledger.data.repository.LedgerRepository
import com.ledger.simpleledger.update.UpdateChecker
import com.ledger.simpleledger.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class UpdateCheckStatus { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, ERROR }
enum class DriveBackupStatus { IDLE, BACKING_UP, SUCCESS, ERROR }

data class SettingsUiState(
    val lastBackupAt: Long = -1L,
    val darkModeOverride: String = "system",
    val defaultCurrency: String = "PKR",
    val isWorking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val installedBuild: Int = 0,
    val updateStatus: UpdateCheckStatus = UpdateCheckStatus.IDLE,
    val latestUpdate: UpdateInfo? = null,
    val downloadProgress: Float = 0f,
    val downloadedApkFile: File? = null,
    val updateError: String? = null,
    val driveAccountEmail: String? = null,
    val lastDriveBackupAt: Long = -1L,
    val driveStatus: DriveBackupStatus = DriveBackupStatus.IDLE,
    val driveConsentIntent: android.content.Intent? = null,
    val driveError: String? = null
)

class SettingsViewModel(
    private val repository: LedgerRepository,
    private val backupManager: BackupManager,
    private val settingsPrefs: SettingsPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            lastBackupAt = settingsPrefs.lastBackupAt,
            darkModeOverride = settingsPrefs.darkModeOverride,
            defaultCurrency = settingsPrefs.defaultCurrency,
            installedBuild = settingsPrefs.installedBuildNumber,
            lastDriveBackupAt = settingsPrefs.lastDriveBackupAt
        )
    )
    val state: StateFlow<SettingsUiState> = _state

    fun refreshDriveAccount(context: Context) {
        val account = DriveBackupManager.lastSignedInAccount(context)
        _state.value = _state.value.copy(driveAccountEmail = account?.email)
    }

    fun onDriveSignInResult(context: Context, account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        _state.value = _state.value.copy(driveAccountEmail = account?.email)
        if (account != null) {
            DriveBackupWorker.schedule(context)
            backupToDriveNow(context)
        }
    }

    fun disconnectDrive(context: Context) {
        DriveBackupManager.signInClient(context).signOut()
        DriveBackupWorker.cancel(context)
        _state.value = _state.value.copy(driveAccountEmail = null, driveStatus = DriveBackupStatus.IDLE)
    }

    fun backupToDriveNow(context: Context) {
        _state.value = _state.value.copy(driveStatus = DriveBackupStatus.BACKING_UP, driveError = null, driveConsentIntent = null)
        viewModelScope.launch {
            val jsonBytes = backupManager.buildBackupJson()
            when (val result = DriveBackupManager.backupNow(context, jsonBytes)) {
                is com.ledger.simpleledger.data.drive.DriveBackupResult.Success -> {
                    val now = System.currentTimeMillis()
                    settingsPrefs.lastDriveBackupAt = now
                    _state.value = _state.value.copy(driveStatus = DriveBackupStatus.SUCCESS, lastDriveBackupAt = now)
                }
                is com.ledger.simpleledger.data.drive.DriveBackupResult.NeedsConsent -> {
                    _state.value = _state.value.copy(
                        driveStatus = DriveBackupStatus.ERROR,
                        driveConsentIntent = result.intent,
                        driveError = "One more permission needed — tap below to allow Drive access."
                    )
                }
                is com.ledger.simpleledger.data.drive.DriveBackupResult.Error -> {
                    _state.value = _state.value.copy(driveStatus = DriveBackupStatus.ERROR, driveError = result.message)
                }
            }
        }
    }

    fun clearDriveConsentIntent() {
        _state.value = _state.value.copy(driveConsentIntent = null)
    }

    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDarkMode(value: String) {
        settingsPrefs.darkModeOverride = value
        _state.value = _state.value.copy(darkModeOverride = value)
    }

    fun setCurrency(value: String) {
        settingsPrefs.defaultCurrency = value
        _state.value = _state.value.copy(defaultCurrency = value)
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addCategory(name) }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun backup(context: Context, uri: Uri) {
        _state.value = _state.value.copy(isWorking = true, message = null, error = null)
        viewModelScope.launch {
            try {
                val result: RestoreResult = backupManager.exportTo(context, uri)
                val now = System.currentTimeMillis()
                settingsPrefs.lastBackupAt = now
                _state.value = _state.value.copy(
                    isWorking = false,
                    lastBackupAt = now,
                    message = "Backup complete: ${result.peopleCount} people, ${result.transactionsCount} transactions saved."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isWorking = false, error = e.message ?: "Backup failed")
            }
        }
    }

    fun restore(context: Context, uri: Uri) {
        _state.value = _state.value.copy(isWorking = true, message = null, error = null)
        viewModelScope.launch {
            try {
                val result: RestoreResult = backupManager.importFrom(context, uri)
                _state.value = _state.value.copy(
                    isWorking = false,
                    message = "Restore complete: ${result.peopleCount} people, ${result.transactionsCount} transactions restored."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isWorking = false, error = e.message ?: "Restore failed")
            }
        }
    }

    fun checkForUpdate() {
        _state.value = _state.value.copy(updateStatus = UpdateCheckStatus.CHECKING, updateError = null)
        viewModelScope.launch {
            val info = UpdateChecker.fetchLatest()
            if (info == null) {
                _state.value = _state.value.copy(
                    updateStatus = UpdateCheckStatus.ERROR,
                    updateError = "Couldn't check for updates. Check your internet connection."
                )
                return@launch
            }
            if (info.buildNumber > settingsPrefs.installedBuildNumber) {
                _state.value = _state.value.copy(updateStatus = UpdateCheckStatus.AVAILABLE, latestUpdate = info)
            } else {
                _state.value = _state.value.copy(updateStatus = UpdateCheckStatus.UP_TO_DATE, latestUpdate = info)
            }
        }
    }

    fun downloadUpdate(context: Context) {
        val info = _state.value.latestUpdate ?: return
        _state.value = _state.value.copy(updateStatus = UpdateCheckStatus.DOWNLOADING, downloadProgress = 0f)
        viewModelScope.launch {
            val file = UpdateChecker.downloadApk(context, info.downloadUrl) { progress ->
                _state.value = _state.value.copy(downloadProgress = progress)
            }
            if (file == null) {
                _state.value = _state.value.copy(
                    updateStatus = UpdateCheckStatus.ERROR,
                    updateError = "Download failed. Please try again."
                )
            } else {
                settingsPrefs.installedBuildNumber = info.buildNumber
                _state.value = _state.value.copy(
                    updateStatus = UpdateCheckStatus.AVAILABLE,
                    downloadedApkFile = file,
                    installedBuild = info.buildNumber
                )
            }
        }
    }
}
