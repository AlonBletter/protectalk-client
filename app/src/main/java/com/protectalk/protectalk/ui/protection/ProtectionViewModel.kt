package com.protectalk.protectalk.ui.protection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.domain.SendContactRequestUseCase
import com.protectalk.protectalk.data.repo.ProtectionRepository
import com.protectalk.protectalk.data.remote.network.ApiClient
import kotlinx.coroutines.delay
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
    val isRefreshing: Boolean = false,  // Add refresh state for pull-to-refresh
    val error: String? = null
)

class ProtectionViewModel : ViewModel() {

    private val sendContactRequestUseCase = SendContactRequestUseCase()
    private val protectionRepository = ProtectionRepository(ApiClient.apiService)

    private val _ui = MutableStateFlow(
        ProtectionUiState(
            // Remove mock data - will be loaded from server
            outgoing = emptyList(),
            incoming = emptyList(),
            trusted = emptyList(),
            protegees = emptyList(),
        )
    )
    val ui: StateFlow<ProtectionUiState> = _ui.asStateFlow()

    init {
        // Load data from server on initialization
        refresh()
    }

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
            protegees = _ui.value.protegees + LinkContact(
                "prot_${req.id}",
                req.otherName,
                req.otherPhone,
                req.relation
            )
        )
        // TODO(DB): mark accepted & create relationship
        // TODO(FCM): notify requester
    }

    fun decline(req: PendingRequest) = viewModelScope.launch {
        _ui.value = _ui.value.copy(incoming = _ui.value.incoming.filterNot { it.id == req.id })
        // TODO(DB): mark declined
        // TODO(FCM): notify requester
    }

    fun removeProtegee(c: LinkContact) = viewModelScope.launch {
        _ui.value = _ui.value.copy(protegees = _ui.value.protegees.filterNot { it.id == c.id })
        // TODO(DB): remove relationship both sides
        // TODO(FCM): notify protegee
    }

    // Refresh functionality - polls server and updates UI
    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(isRefreshing = true, error = null)
        try {
            Log.d("ProtectionViewModel", "Refreshing data from server...")

            // Fetch user profile from server
            val result = protectionRepository.getUserProfile()

            when (result) {
                is ResultModel.Ok -> {
                    val profile = result.data
                    Log.d("ProtectionViewModel", "Profile fetched successfully")

                    // Map server data to UI models
                    val outgoingRequests = profile.pendingSentRequests?.map { it.toUIModel() } ?: emptyList()
                    val incomingRequests = profile.pendingReceivedRequests?.map { it.toUIModel() } ?: emptyList()
                    val trustedContacts = profile.linkedContacts?.filter { it.contactType == "TRUSTED_CONTACT" }?.map { it.toUIModel() } ?: emptyList()
                    val protegeeContacts = profile.linkedContacts?.filter { it.contactType == "PROTEGEE" }?.map { it.toUIModel() } ?: emptyList()

                    // Update UI state with real server data
                    _ui.value = _ui.value.copy(
                        outgoing = outgoingRequests,
                        incoming = incomingRequests,
                        trusted = trustedContacts,
                        protegees = protegeeContacts,
                        isRefreshing = false,
                        error = null
                    )

                    Log.d("ProtectionViewModel", "UI updated - Outgoing: ${outgoingRequests.size}, Incoming: ${incomingRequests.size}, Trusted: ${trustedContacts.size}, Protegees: ${protegeeContacts.size}")
                }

                is ResultModel.Err -> {
                    Log.e("ProtectionViewModel", "Failed to refresh data: ${result.message}")
                    _ui.value = _ui.value.copy(
                        isRefreshing = false,
                        error = "Failed to refresh data: ${result.message}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ProtectionViewModel", "Exception during refresh", e)
            _ui.value = _ui.value.copy(
                isRefreshing = false,
                error = "Failed to refresh data: ${e.message}"
            )
        }
    }
}
