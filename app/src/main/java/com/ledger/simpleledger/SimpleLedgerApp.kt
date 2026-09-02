package com.ledger.simpleledger

import android.app.Application
import com.ledger.simpleledger.data.SettingsPrefs
import com.ledger.simpleledger.data.backup.BackupManager
import com.ledger.simpleledger.data.db.AppDatabase
import com.ledger.simpleledger.data.repository.LedgerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SimpleLedgerApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: LedgerRepository by lazy {
        LedgerRepository(database.personDao(), database.categoryDao(), database.transactionDao())
    }

    val backupManager: BackupManager by lazy { BackupManager(repository) }

    val settingsPrefs: SettingsPrefs by lazy { SettingsPrefs(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.ensureDefaultCategoriesSeeded()
        }
    }
}
