package com.ledger.simpleledger.ui.persondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.db.dao.PersonWithTotals
import com.ledger.simpleledger.data.db.entities.TransactionEntity
import com.ledger.simpleledger.data.model.TransactionType
import com.ledger.simpleledger.data.repository.LedgerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A transaction paired with the running balance immediately after it (in chronological
 * order), so the ledger list can show "Balance: Rs X" the way a real khata book does. */
data class TransactionWithRunningBalance(
    val transaction: TransactionEntity,
    val runningBalanceMinor: Long
)

data class PersonDetailUiState(
    val person: PersonWithTotals? = null,
    val transactions: List<TransactionWithRunningBalance> = emptyList(),
    val loading: Boolean = true
)

class PersonDetailViewModel(
    private val repository: LedgerRepository,
    private val personId: Long
) : ViewModel() {

    val state: StateFlow<PersonDetailUiState> = combine(
        repository.observePersonWithTotals(personId),
        repository.observeTransactionsForPerson(personId)
    ) { person, txns ->
        // txns arrive newest-first; walk oldest-first to accumulate a running balance,
        // then present newest-first again (each row shows the balance right after it).
        val chronological = txns.sortedBy { it.date }
        var running = 0L
        val withBalance = chronological.map { t ->
            running += if (t.type == TransactionType.LIYA) t.amountMinor else -t.amountMinor
            TransactionWithRunningBalance(t, running)
        }
        PersonDetailUiState(person = person, transactions = withBalance.reversed(), loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonDetailUiState())

    fun deletePerson(onDone: () -> Unit) {
        viewModelScope.launch {
            val p = repository.getPerson(personId) ?: return@launch
            repository.deletePerson(p)
            onDone()
        }
    }
}
