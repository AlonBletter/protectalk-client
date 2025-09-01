package com.protectalk.protectalk.ui.protection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.domain.SendContactRequestUseCase
import com.protectalk.protectalk.domain.GetUserProfileUseCase
import com.protectalk.protectalk.domain.ApproveContactRequestUseCase
import com.protectalk.protectalk.domain.DenyContactRequestUseCase
import com.protectalk.protectalk.domain.DeleteLinkedContactUseCase
import com.protectalk.protectalk.domain.CancelRequestUseCase
import com.protectalk.protectalk.data.remote.network.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProtectionUiState(
    val outgoing: List<PendingRequest> = emptyList(),   // Requests I sent
    val incomingProtectionRequests: List<PendingRequest> = emptyList(), // People asking for my protection
    val incomingProtectionOffers: List<PendingRequest> = emptyList(),   // People offering to protect me
    val trusted:  List<LinkContact>   = emptyList(),    // my trusted contacts
    val protegees: List<LinkContact>  = emptyList(),    // people I protect
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,  // Add refresh state for pull-to-refresh
    val error: String? = null
)

class ProtectionViewModel : ViewModel() {

    private val sendContactRequestUseCase = SendContactRequestUseCase()
    private val getUserProfileUseCase = GetUserProfileUseCase() // Use the existing use case instead of repository directly
    private val approveContactRequestUseCase = ApproveContactRequestUseCase()
    private val denyContactRequestUseCase = DenyContactRequestUseCase()
    private val deleteLinkedContactUseCase = DeleteLinkedContactUseCase()
    private val cancelRequestUseCase = CancelRequestUseCase()

    private val _ui = MutableStateFlow(
        ProtectionUiState(
            // Remove mock data - will be loaded from server
            outgoing = emptyList(),
            incomingProtectionRequests = emptyList(),
            incomingProtectionOffers = emptyList(),
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
                    outgoing = _ui.value.outgoing + PendingRequest(id, name, phone, relation, "TRUSTED_CONTACT"),
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
                // Add to local state as pending - this should be OUTGOING, not incoming
                val id = System.currentTimeMillis().toString()
                _ui.value = _ui.value.copy(
                    outgoing = _ui.value.outgoing + PendingRequest(id, name, phone, relation, "PROTEGEE"),
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
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        Log.d("ProtectionViewModel", "Canceling outgoing request with ID: ${req.id}")

        val result = cancelRequestUseCase(req.id)

        when (result) {
            is ResultModel.Ok -> {
                Log.d("ProtectionViewModel", "Request canceled successfully")
                // Update UI state: remove from outgoing
                _ui.value = _ui.value.copy(
                    outgoing = _ui.value.outgoing.filterNot { it.id == req.id },
                    isLoading = false,
                    error = null
                )
            }

            is ResultModel.Err -> {
                Log.e("ProtectionViewModel", "Failed to cancel request: ${result.message}")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = "Failed to cancel request: ${result.message}"
                )
            }
        }
    }

    fun removeTrusted(c: LinkContact) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        val result = deleteLinkedContactUseCase(c.phone, "TRUSTED_CONTACT")

        when (result) {
            is ResultModel.Ok -> {
                _ui.value = _ui.value.copy(
                    trusted = _ui.value.trusted.filterNot { it.id == c.id },
                    isLoading = false,
                    error = null
                )
            }

            is ResultModel.Err -> {
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = "Failed to remove trusted contact: ${result.message}"
                )
            }
        }
    }

    fun accept(req: PendingRequest) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        Log.d("ProtectionViewModel", "Approving contact request with ID: ${req.id}")

        val result = approveContactRequestUseCase(req.id)

        when (result) {
            is ResultModel.Ok -> {
                Log.d("ProtectionViewModel", "Contact request approved successfully")
                // Update UI state based on request type
                when (req.contactType) {
                    "TRUSTED_CONTACT", "TRUSTED" -> {
                        // Someone asked for my protection - they become my protegee
                        _ui.value = _ui.value.copy(
                            incomingProtectionRequests = _ui.value.incomingProtectionRequests.filterNot { it.id == req.id },
                            protegees = _ui.value.protegees + LinkContact(
                                "prot_${req.id}",
                                req.otherName,
                                req.otherPhone,
                                req.relation
                            ),
                            isLoading = false,
                            error = null
                        )
                    }
                    "PROTEGEE" -> {
                        // Someone offered to protect me - they become my trusted contact
                        _ui.value = _ui.value.copy(
                            incomingProtectionOffers = _ui.value.incomingProtectionOffers.filterNot { it.id == req.id },
                            trusted = _ui.value.trusted + LinkContact(
                                "trusted_${req.id}",
                                req.otherName,
                                req.otherPhone,
                                req.relation
                            ),
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }

            is ResultModel.Err -> {
                Log.e("ProtectionViewModel", "Failed to approve contact request: ${result.message}")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = "Failed to approve request: ${result.message}"
                )
            }
        }
    }

    fun decline(req: PendingRequest) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        Log.d("ProtectionViewModel", "Denying contact request with ID: ${req.id}")

        val result = denyContactRequestUseCase(req.id)

        when (result) {
            is ResultModel.Ok -> {
                Log.d("ProtectionViewModel", "Contact request denied successfully")
                // Update UI state: remove from appropriate incoming list
                when (req.contactType) {
                    "TRUSTED_CONTACT", "TRUSTED" -> {
                        _ui.value = _ui.value.copy(
                            incomingProtectionRequests = _ui.value.incomingProtectionRequests.filterNot { it.id == req.id },
                            isLoading = false,
                            error = null
                        )
                    }
                    "PROTEGEE" -> {
                        _ui.value = _ui.value.copy(
                            incomingProtectionOffers = _ui.value.incomingProtectionOffers.filterNot { it.id == req.id },
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }

            is ResultModel.Err -> {
                Log.e("ProtectionViewModel", "Failed to deny contact request: ${result.message}")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = "Failed to deny request: ${result.message}"
                )
            }
        }
    }

    fun removeProtegee(c: LinkContact) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        Log.d("ProtectionViewModel", "Removing protegee: ${c.phone}")

        val result = deleteLinkedContactUseCase(c.phone, "PROTEGEE")

        when (result) {
            is ResultModel.Ok -> {
                Log.d("ProtectionViewModel", "Protegee removed successfully")
                // Update UI state: remove from protegees
                _ui.value = _ui.value.copy(
                    protegees = _ui.value.protegees.filterNot { it.id == c.id },
                    isLoading = false,
                    error = null
                )
            }

            is ResultModel.Err -> {
                Log.e("ProtectionViewModel", "Failed to remove protegee: ${result.message}")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = "Failed to remove protegee: ${result.message}"
                )
            }
        }
    }

    // Refresh functionality - polls server and updates UI
    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(isRefreshing = true, error = null)
        try {
            Log.d("ProtectionViewModel", "Refreshing data from server using GetUserProfileUseCase...")

            // Use the existing use case instead of calling repository directly
            val result = getUserProfileUseCase()

            when (result) {
                is ResultModel.Ok -> {
                    val profile = result.data
                    Log.d("ProtectionViewModel", "Profile fetched successfully via use case")

                    // Map server data to UI models and separate by contact type
                    val outgoingRequests = profile.pendingSentRequests?.map { it.toUIModel() } ?: emptyList()
                    val allIncomingRequests = profile.pendingReceivedRequests?.map { it.toUIModel() } ?: emptyList()

                    // Separate incoming requests by type
                    val protectionRequests = allIncomingRequests.filter { it.contactType == "TRUSTED_CONTACT" || it.contactType == "TRUSTED" }
                    val protectionOffers = allIncomingRequests.filter { it.contactType == "PROTEGEE" }

                    val trustedContacts = profile.linkedContacts?.filter { it.contactType == "TRUSTED_CONTACT" }?.map { it.toUIModel() } ?: emptyList()
                    val protegeeContacts = profile.linkedContacts?.filter { it.contactType == "PROTEGEE" }?.map { it.toUIModel() } ?: emptyList()

                    // Update UI state with properly separated data
                    _ui.value = _ui.value.copy(
                        outgoing = outgoingRequests,
                        incomingProtectionRequests = protectionRequests, // People asking for my protection
                        incomingProtectionOffers = protectionOffers,     // People offering to protect me
                        trusted = trustedContacts,
                        protegees = protegeeContacts,
                        isRefreshing = false,
                        error = null
                    )

                    Log.d("ProtectionViewModel", "UI updated - Outgoing: ${outgoingRequests.size}, Protection Requests: ${protectionRequests.size}, Protection Offers: ${protectionOffers.size}, Trusted: ${trustedContacts.size}, Protegees: ${protegeeContacts.size}")
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
