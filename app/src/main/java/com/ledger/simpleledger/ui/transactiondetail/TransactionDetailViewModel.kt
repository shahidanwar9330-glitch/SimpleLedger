package com.ledger.simpleledger.ui.transactiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.db.entities.PersonEntity
import com.ledger.simpleledger.data.db.entities.TransactionEntity
import com.ledger.simpleledger.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TransactionDetailUiState(
    val transaction: TransactionEntity? = null,
    val person: PersonEntity? = null,
    val loading: Boolean = true,
    val deleted: Boolean = false
)

class TransactionDetailViewModel(
    private val repository: LedgerRepository,
    private val transactionId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailUiState())
    val state: StateFlow<TransactionDetailUiState> = _state

    init {
        viewModelScope.launch {
            val t = repository.getTransaction(transactionId)
            val p = t?.let { repository.getPerson(it.personId) }
            _state.value = TransactionDetailUiState(transaction = t, person = p, loading = false)
        }
    }

    fun delete(onDone: () -> Unit) {
        val t = _state.value.transaction ?: return
        viewModelScope.launch {
            repository.deleteTransaction(t)
            _state.value = _state.value.copy(deleted = true)
            onDone()
        }
    }
}
