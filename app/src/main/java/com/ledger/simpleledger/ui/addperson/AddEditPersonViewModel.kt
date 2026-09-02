package com.ledger.simpleledger.ui.addperson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.simpleledger.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AddEditPersonUiState(
    val editId: Long? = null,
    val name: String = "",
    val phone: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class AddEditPersonViewModel(
    private val repository: LedgerRepository,
    private val editId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditPersonUiState(editId = editId))
    val state: StateFlow<AddEditPersonUiState> = _state

    init {
        if (editId != null) {
            viewModelScope.launch {
                repository.getPerson(editId)?.let { p ->
                    _state.value = _state.value.copy(
                        name = p.name,
                        phone = p.phone ?: "",
                        notes = p.notes ?: ""
                    )
                }
            }
        }
    }

    fun setName(v: String) { _state.value = _state.value.copy(name = v, nameError = null) }
    fun setPhone(v: String) { _state.value = _state.value.copy(phone = v) }
    fun setNotes(v: String) { _state.value = _state.value.copy(notes = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.isSaving) return
        if (s.name.isBlank()) {
            _state.value = s.copy(nameError = "Name is required")
            return
        }
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            try {
                if (s.editId == null) {
                    repository.addPerson(s.name, s.phone.ifBlank { null }, s.notes.ifBlank { null })
                } else {
                    val existing = repository.getPerson(s.editId)
                    if (existing != null) {
                        repository.updatePerson(
                            existing.copy(
                                name = s.name.trim(),
                                phone = s.phone.ifBlank { null },
                                notes = s.notes.ifBlank { null }
                            )
                        )
                    }
                }
                _state.value = _state.value.copy(isSaving = false, saved = true)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, nameError = e.message)
            }
        }
    }
}
