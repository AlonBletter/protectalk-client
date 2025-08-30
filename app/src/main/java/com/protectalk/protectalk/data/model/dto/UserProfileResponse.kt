package com.protectalk.protectalk.data.model.dto

data class UserProfileResponse(
    val firebaseUid: String,
    val name: String,
    val phoneNumber: String,
    val userType: String? = null,
    val createdAt: String? = null, // ISO timestamp string
    val linkedContacts: List<LinkedContactDto>? = null,
    val pendingReceivedRequests: List<PendingContactRequestDto>? = null,
    val pendingSentRequests: List<PendingContactRequestDto>? = null
)

data class LinkedContactDto(
    val phoneNumber: String,
    val name: String,
    val relationship: String,
    val contactType: String // "TRUSTED_CONTACT" or "PROTEGEE"
)

data class PendingContactRequestDto(
    val requesterName: String,
    val targetPhoneNumber: String,
    val relationship: String,
    val contactType: String,
    val status: String,
    val createdAt: String? = null // ISO timestamp string
)
