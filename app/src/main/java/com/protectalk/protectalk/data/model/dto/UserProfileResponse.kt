package com.protectalk.protectalk.data.model.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfileResponse(
    val firebaseUid: String,
    val name: String,
    val phoneNumber: String,
    val userType: String? = null,
    val createdAt: String? = null,
    val linkedContacts: List<LinkedContactDto>? = null,
    val pendingReceivedRequests: List<PendingContactRequestDto>? = null,
    val pendingSentRequests: List<PendingContactRequestDto>? = null
)

@JsonClass(generateAdapter = true)
data class LinkedContactDto(
    val phoneNumber: String,
    val name: String,
    val relationship: String,
    val contactType: String // "TRUSTED_CONTACT" or "PROTEGEE"
)

@JsonClass(generateAdapter = true)
data class PendingContactRequestDto(
    val id: String? = null,
    val requesterPhoneNumber: String,
    val requesterName: String,
    val targetPhoneNumber: String,
    val targetName: String? = null, // Add target user's name
    val relationship: String,
    val contactType: String,
    val status: String,
    val createdAt: String? = null // Changed back to String for JSON compatibility
)
