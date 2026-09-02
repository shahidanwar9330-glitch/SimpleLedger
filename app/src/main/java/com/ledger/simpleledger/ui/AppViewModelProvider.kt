package com.ledger.simpleledger.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.ledger.simpleledger.SimpleLedgerApp

/** Returns the current SimpleLedgerApp so ViewModel factories can reach the repository/backup manager. */
fun CreationExtras.ledgerApp(): SimpleLedgerApp {
    val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
    return application as SimpleLedgerApp
}

@Composable
fun currentLedgerApp(): SimpleLedgerApp = LocalContext.current.applicationContext as SimpleLedgerApp

/** Small helper to build a ViewModelProvider.Factory from a single create lambda. */
class SimpleViewModelFactory<T : ViewModel>(
    private val create: (CreationExtras) -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>, extras: CreationExtras): VM {
        return create(extras) as VM
    }
}
