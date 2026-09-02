package com.ledger.simpleledger.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.db.dao.CategoryTotal
import com.ledger.simpleledger.data.repository.LedgerRepository
import com.ledger.simpleledger.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ReportPeriod { TODAY, THIS_WEEK, THIS_MONTH, CUSTOM }

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.TODAY,
    val customStart: Long? = null,
    val customEnd: Long? = null,
    val totalLiyaMinor: Long = 0,
    val totalDiyaMinor: Long = 0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val loading: Boolean = true
) {
    val netBalanceMinor: Long get() = totalLiyaMinor - totalDiyaMinor
}

class ReportsViewModel(private val repository: LedgerRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state

    init {
        load()
    }

    fun setPeriod(period: ReportPeriod) {
        _state.value = _state.value.copy(period = period)
        if (period != ReportPeriod.CUSTOM) load()
    }

    fun setCustomRange(start: Long, end: Long) {
        _state.value = _state.value.copy(period = ReportPeriod.CUSTOM, customStart = start, customEnd = end)
        load()
    }

    private fun rangeFor(state: ReportsUiState): Pair<Long, Long> = when (state.period) {
        ReportPeriod.TODAY -> DateUtils.startOfToday() to DateUtils.endOfToday()
        ReportPeriod.THIS_WEEK -> DateUtils.startOfWeek() to DateUtils.endOfToday()
        ReportPeriod.THIS_MONTH -> DateUtils.startOfMonth() to DateUtils.endOfToday()
        ReportPeriod.CUSTOM -> {
            val s = state.customStart ?: DateUtils.startOfToday()
            val e = state.customEnd ?: DateUtils.endOfToday()
            DateUtils.startOfDay(s) to DateUtils.endOfDay(e)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val (start, end) = rangeFor(_state.value)
            val totals = repository.getPeriodTotals(start, end)
            val categoryTotals = repository.getCategoryTotals(start, end)
            _state.value = _state.value.copy(
                totalLiyaMinor = totals.totalLiyaMinor,
                totalDiyaMinor = totals.totalDiyaMinor,
                categoryTotals = categoryTotals,
                loading = false
            )
        }
    }
}
