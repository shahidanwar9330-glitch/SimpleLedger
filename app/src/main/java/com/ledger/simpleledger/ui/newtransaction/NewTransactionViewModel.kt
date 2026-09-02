package com.ledger.simpleledger.ui.newtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.SettingsPrefs
import com.ledger.simpleledger.data.db.entities.CategoryEntity
import com.ledger.simpleledger.data.db.entities.PersonEntity
import com.ledger.simpleledger.data.model.TransactionType
import com.ledger.simpleledger.data.repository.LedgerRepository
import com.ledger.simpleledger.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewTransactionUiState(
    val editId: Long? = null,
    val type: TransactionType = TransactionType.LIYA,
    val people: List<PersonEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedPersonId: Long? = null,
    val selectedPersonName: String = "",
    val amountText: String = "",
    val currency: String = "PKR",
    val dateMillis: Long = System.currentTimeMillis(),
    val category: String = "Other",
    val note: String = "",
    val paymentMethod: String = "",
    val reference: String = "",
    val attachmentUri: String? = null,
    val amountError: String? = null,
    val personError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class NewTransactionViewModel(
    private val repository: LedgerRepository,
    settingsPrefs: SettingsPrefs,
    initialType: String?,
    initialEditId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(
        NewTransactionUiState(
            editId = initialEditId,
            type = initialType?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() } ?: TransactionType.LIYA,
            currency = settingsPrefs.defaultCurrency
        )
    )
    val state: StateFlow<NewTransactionUiState> = _state

    init {
        viewModelScope.launch {
            repository.observePeople().collect { people ->
                _state.value = _state.value.copy(people = people)
            }
        }
        viewModelScope.launch {
            repository.observeCategories().collect { cats ->
                _state.value = _state.value.copy(categories = cats)
            }
        }
        if (initialEditId != null) {
            viewModelScope.launch {
                repository.getTransaction(initialEditId)?.let { t ->
                    val person = repository.getPerson(t.personId)
                    _state.value = _state.value.copy(
                        type = t.type,
                        selectedPersonId = t.personId,
                        selectedPersonName = person?.name ?: "",
                        amountText = Money.minorToMajor(t.amountMinor).stripTrailingZeros().toPlainString(),
                        currency = t.currency,
                        dateMillis = t.date,
                        category = t.categoryName,
                        note = t.note ?: "",
                        paymentMethod = t.paymentMethod ?: "",
                        reference = t.reference ?: "",
                        attachmentUri = t.attachmentUri
                    )
                }
            }
        }
    }

    fun setType(type: TransactionType) { _state.value = _state.value.copy(type = type) }
    fun setAmount(text: String) { _state.value = _state.value.copy(amountText = text, amountError = null) }
    fun setDate(millis: Long) { _state.value = _state.value.copy(dateMillis = millis) }
    fun setCategory(name: String) { _state.value = _state.value.copy(category = name) }
    fun setNote(text: String) { _state.value = _state.value.copy(note = text) }
    fun setPaymentMethod(text: String) { _state.value = _state.value.copy(paymentMethod = text) }
    fun setReference(text: String) { _state.value = _state.value.copy(reference = text) }
    fun setAttachment(uri: String?) { _state.value = _state.value.copy(attachmentUri = uri) }
    fun selectPerson(id: Long, name: String) {
        _state.value = _state.value.copy(selectedPersonId = id, selectedPersonName = name, personError = null)
    }
    fun setPersonNameOnly(name: String) {
        _state.value = _state.value.copy(selectedPersonName = name, selectedPersonId = null, personError = null)
    }

    fun addNewCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(name)
            _state.value = _state.value.copy(category = name.trim())
        }
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.isSaving) return // prevent duplicate submissions from rapid tapping

        val amountMinor = Money.parseToMinorUnits(s.amountText)
        var personError: String? = null
        var amountError: String? = null

        if (amountMinor == null || amountMinor <= 0) {
            amountError = "Enter a valid amount greater than zero"
        }
        if (s.selectedPersonName.isBlank()) {
            personError = "Enter or select a person"
        }
        if (amountError != null || personError != null) {
            _state.value = s.copy(amountError = amountError, personError = personError)
            return
        }

        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            try {
                var personId = s.selectedPersonId
                if (personId == null) {
                    personId = repository.addPerson(s.selectedPersonName, null, null)
                }

                if (s.editId == null) {
                    repository.addTransaction(
                        personId = personId,
                        type = s.type,
                        amountMinor = amountMinor!!,
                        currency = s.currency,
                        categoryName = s.category,
                        date = s.dateMillis,
                        note = s.note,
                        paymentMethod = s.paymentMethod,
                        reference = s.reference
                    )
                } else {
                    val existing = repository.getTransaction(s.editId)
                    if (existing != null) {
                        repository.updateTransaction(
                            existing.copy(
                                personId = personId,
                                type = s.type,
                                amountMinor = amountMinor!!,
                                currency = s.currency,
                                categoryName = s.category,
                                date = s.dateMillis,
                                note = s.note.ifBlank { null },
                                paymentMethod = s.paymentMethod.ifBlank { null },
                                reference = s.reference.ifBlank { null },
                                attachmentUri = s.attachmentUri
                            )
                        )
                    }
                }
                _state.value = _state.value.copy(isSaving = false, saved = true)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, amountError = e.message)
            }
        }
    }
}
