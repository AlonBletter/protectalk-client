package com.protectalk.protectalk.ui.protection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.domain.SendContactRequestUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProtectionUiState(
    val outgoing: List<PendingRequest> = emptyList(),   // protegee -> trusted (pending)
    val incoming: List<PendingRequest> = emptyList(),   // trusted <- protegee (pending)
    val trusted:  List<LinkContact>   = emptyList(),    // my trusted contacts
    val protegees: List<LinkContact>  = emptyList(),    // people I protect
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProtectionViewModel : ViewModel() {

    private val sendContactRequestUseCase = SendContactRequestUseCase()

    private val _ui = MutableStateFlow(
        ProtectionUiState(
            outgoing = listOf(PendingRequest("out1","Alex","+1 555-0100", Relation.Friend)),
            incoming = listOf(PendingRequest("in1","Dana","+44 7700 900123", Relation.Family)),
            trusted  = listOf(LinkContact("t1","Noa","+972 52-555-1111", Relation.Family)),
            protegees= listOf(LinkContact("p1","Omer","+972 54-111-2222", Relation.Friend)),
        )
    )
    val ui: StateFlow<ProtectionUiState> = _ui.asStateFlow()

    // ---- Protegee actions ----
    fun sendRequest(name: String, phone: String, relation: Relation) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        val result = sendContactRequestUseCase(
            name = name,
            phoneNumber = phone,
            relationship = relation.toServerString(),
            contactType = DialogRole.ProtegeeAsks.toContactType()
        )

        when (result) {
            is ResultModel.Ok -> {
                Log.d("ProtectionViewModel", "Contact request sent successfully")
                // Add to local state as pending
                val id = System.currentTimeMillis().toString()
                _ui.value = _ui.value.copy(
                    outgoing = _ui.value.outgoing + PendingRequest(id, name, phone, relation),
                    isLoading = false,
                    error = null
                )
            }
            is ResultModel.Err -> {
                Log.e("ProtectionViewModel", "Failed to send contact request: ${result.message}")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = "Failed to send contact request: ${result.message}"
                )
            }
        }
    }

    fun offerProtection(name: String, phone: String, relation: Relation) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        val result = sendContactRequestUseCase(
            name = name,
            phoneNumber = phone,
            relationship = relation.toServerString(),
            contactType = DialogRole.TrustedOffers.toContactType()
        )

        when (result) {
            is ResultModel.Ok -> {
                Log.d("ProtectionViewModel", "Protection offer sent successfully")
                // Add to local state as pending
                val id = System.currentTimeMillis().toString()
                _ui.value = _ui.value.copy(
                    incoming = _ui.value.incoming + PendingRequest(id, name, phone, relation),
                    isLoading = false,
                    error = null
                )
            }
            is ResultModel.Err -> {
                Log.e("ProtectionViewModel", "Failed to send protection offer: ${result.message}")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = "Failed to send protection offer: ${result.message}"
                )
            }
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    // ...existing local state management methods...
    fun cancelOutgoing(req: PendingRequest) = viewModelScope.launch {
        _ui.value = _ui.value.copy(outgoing = _ui.value.outgoing.filterNot { it.id == req.id })
        // TODO(DB): mark canceled
    }

    fun removeTrusted(c: LinkContact) = viewModelScope.launch {
        _ui.value = _ui.value.copy(trusted = _ui.value.trusted.filterNot { it.id == c.id })
        // TODO(DB): remove relationship both sides
    }

    fun accept(req: PendingRequest) = viewModelScope.launch {
        _ui.value = _ui.value.copy(
            incoming = _ui.value.incoming.filterNot { it.id == req.id },
            protegees = _ui.value.protegees + LinkContact("prot_${req.id}", req.otherName, req.otherPhone, req.relation)
        )
        // TODO(DB): mark accepted & create relationship
        // TODO(FCM): notify requester
    }

    fun decline(req: PendingRequest) = viewModelScope.launch {
        _ui.value = _ui.value.copy(incoming = _ui.value.incoming.filterNot { it.id == req.id })
        // TODO(DB): mark declined
    }

    fun removeProtegee(c: LinkContact) = viewModelScope.launch {
        _ui.value = _ui.value.copy(protegees = _ui.value.protegees.filterNot { it.id == c.id })
        // TODO(DB): remove relationship both sides
    }
}
