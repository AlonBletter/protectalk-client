package com.protectalk.protectalk.data.model.dto

data class ContactRequestDto(
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val contactType: ContactType
)

enum class ContactType {
    PROTEGEE,    // I want this person to protect me
    TRUSTED      // I want to protect this person
}

class ProtectionInviteRequest {
}