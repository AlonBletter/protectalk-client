package com.protectalk.protectalk.ui.protection

import com.protectalk.protectalk.data.model.dto.ContactType
import com.protectalk.protectalk.data.model.dto.LinkedContactDto
import com.protectalk.protectalk.data.model.dto.PendingContactRequestDto

// UI-only models (replace with real entities later)
enum class Relation {
    Family, Friend, Other;

    // Convert UI relation to server relationship string
    fun toServerString(): String = when (this) {
        Family -> "FAMILY"
        Friend -> "FRIEND"
        Other -> "OTHER"
    }
}

data class PendingRequest(
    val id: String,
    val otherName: String,
    val otherPhone: String,
    val relation: Relation,
    val contactType: String // "TRUSTED_CONTACT" or "PROTEGEE"
)

data class LinkContact(
    val id: String,
    val name: String,
    val phone: String,
    val relation: Relation
)

enum class DialogRole {
    ProtegeeAsks, TrustedOffers;

    // Convert dialog role to server ContactType
    fun toContactType(): ContactType = when (this) {
        ProtegeeAsks -> ContactType.TRUSTED  // I'm asking someone to be my trusted contact
        TrustedOffers -> ContactType.PROTEGEE // I'm offering to protect someone (they become my protegee)
    }
}

enum class ProtectionTab { Protegee, Trusted }

// Mapper functions to convert server DTOs to UI models
fun LinkedContactDto.toUIModel(): LinkContact {
    val relation = when (relationship.uppercase()) {
        "FAMILY" -> Relation.Family
        "FRIEND" -> Relation.Friend
        else -> Relation.Other
    }
    return LinkContact(
        id = phoneNumber, // Use phone as ID since server doesn't provide separate ID
        name = name,
        phone = phoneNumber,
        relation = relation
    )
}

// For incoming requests (from pendingReceivedRequests) - show who sent the request
fun PendingContactRequestDto.toUIModelForIncoming(): PendingRequest {
    val relation = when (relationship.uppercase()) {
        "FAMILY" -> Relation.Family
        "FRIEND" -> Relation.Friend
        else -> Relation.Other
    }

    val requestId = id ?: run {
        android.util.Log.w("Models", "Server didn't provide ID for pending request")
        "${targetName}_${targetPhoneNumber}_${System.currentTimeMillis()}"
    }

    return PendingRequest(
        id = requestId,
        otherName = targetName ?: targetPhoneNumber, // Show the target user's name (who sent us the request)
        otherPhone = targetPhoneNumber,
        relation = relation,
        contactType = contactType
    )
}

// For outgoing requests (from pendingSentRequests) - show who we sent the request to
fun PendingContactRequestDto.toUIModelForOutgoing(): PendingRequest {
    val relation = when (relationship.uppercase()) {
        "FAMILY" -> Relation.Family
        "FRIEND" -> Relation.Friend
        else -> Relation.Other
    }

    val requestId = id ?: run {
        android.util.Log.w("Models", "Server didn't provide ID for pending request")
        "${targetName}_${targetPhoneNumber}_${System.currentTimeMillis()}"
    }

    return PendingRequest(
        id = requestId,
        otherName = targetName ?: targetPhoneNumber, // Show who we sent the request to
        otherPhone = targetPhoneNumber,
        relation = relation,
        contactType = contactType
    )
}
