package com.ledger.simpleledger.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.SettingsPrefs
import com.ledger.simpleledger.data.db.dao.TransactionWithPerson
import com.ledger.simpleledger.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val totalLiyaMinor: Long = 0,
    val totalDiyaMinor: Long = 0,
    val recent: List<TransactionWithPerson> = emptyList(),
    val currency: String = "PKR",
    val loading: Boolean = true
) {
    val balanceMinor: Long get() = totalLiyaMinor - totalDiyaMinor
}

class DashboardViewModel(
    repository: LedgerRepository,
    private val settingsPrefs: SettingsPrefs
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeGrandTotals(),
        repository.observeRecentTransactions(15)
    ) { totals, recent ->
        DashboardUiState(
            totalLiyaMinor = totals.totalLiyaMinor,
            totalDiyaMinor = totals.totalDiyaMinor,
            recent = recent,
            currency = settingsPrefs.defaultCurrency,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
