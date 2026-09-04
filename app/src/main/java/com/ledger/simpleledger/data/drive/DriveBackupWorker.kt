package com.ledger.simpleledger.data.drive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.ledger.simpleledger.SimpleLedgerApp
import com.ledger.simpleledger.data.SettingsPrefs
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "weekly_drive_backup"

/** Runs weekly (only when connected to the internet). If the user isn't signed in to
 * Google, it simply does nothing that run — no error, no notification. */
class DriveBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val account = DriveBackupManager.lastSignedInAccount(applicationContext) ?: return Result.success()
        val app = applicationContext as SimpleLedgerApp
        val jsonBytes = app.backupManager.buildBackupJson()
        val result = DriveBackupManager.backupNow(applicationContext, jsonBytes)
        return when (result) {
            is DriveBackupResult.Success -> {
                SettingsPrefs(applicationContext).lastDriveBackupAt = System.currentTimeMillis()
                Result.success()
            }
            else -> Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
