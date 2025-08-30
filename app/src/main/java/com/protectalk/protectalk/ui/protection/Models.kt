package com.protectalk.protectalk.ui.protection

import com.protectalk.protectalk.data.model.dto.ContactType

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
    val relation: Relation
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
