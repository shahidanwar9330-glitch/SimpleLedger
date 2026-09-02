package com.ledger.simpleledger.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.db.dao.PersonWithTotals
import com.ledger.simpleledger.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PeopleViewModel(repository: LedgerRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val people: StateFlow<List<PersonWithTotals>> = repository.observePeopleWithTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }

    fun filtered(all: List<PersonWithTotals>, q: String): List<PersonWithTotals> =
        if (q.isBlank()) all else all.filter { it.name.contains(q, ignoreCase = true) }
}
