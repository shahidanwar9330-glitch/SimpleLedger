package com.ledger.simpleledger.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.db.dao.TransactionWithPerson
import com.ledger.simpleledger.data.model.TransactionType
import com.ledger.simpleledger.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class QuickFilter { ALL, LIYA, DIYA }

data class TransactionsFilterState(
    val query: String = "",
    val quickFilter: QuickFilter = QuickFilter.ALL,
    val category: String? = null
)

class TransactionsViewModel(private val repository: LedgerRepository) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionsFilterState())
    val filter: StateFlow<TransactionsFilterState> = _filter

    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionWithPerson>> = _filter
        .flatMapLatest { f ->
            val type = when (f.quickFilter) {
                QuickFilter.ALL -> null
                QuickFilter.LIYA -> TransactionType.LIYA
                QuickFilter.DIYA -> TransactionType.DIYA
            }
            repository.searchTransactions(
                type = type,
                category = f.category,
                personId = null,
                startDate = null,
                endDate = null,
                query = f.query
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _filter.value = _filter.value.copy(query = q) }
    fun setQuickFilter(f: QuickFilter) { _filter.value = _filter.value.copy(quickFilter = f) }
    fun setCategory(c: String?) { _filter.value = _filter.value.copy(category = c) }
}
