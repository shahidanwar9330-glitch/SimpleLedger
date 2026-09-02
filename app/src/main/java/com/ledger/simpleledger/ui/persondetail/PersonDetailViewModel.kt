package com.ledger.simpleledger.ui.persondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.db.dao.PersonWithTotals
import com.ledger.simpleledger.data.db.entities.TransactionEntity
import com.ledger.simpleledger.data.repository.LedgerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val person: PersonWithTotals? = null,
    val transactions: List<TransactionEntity> = emptyList(),
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
        PersonDetailUiState(person = person, transactions = txns, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonDetailUiState())

    fun deletePerson(onDone: () -> Unit) {
        viewModelScope.launch {
            val p = repository.getPerson(personId) ?: return@launch
            repository.deletePerson(p)
            onDone()
        }
    }
}
