package com.protectalk.protectalk.ui.protection

// UI-only models (replace with real entities later)
enum class Relation { Family, Friend, Other }

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

enum class DialogRole { ProtegeeAsks, TrustedOffers }
enum class ProtectionTab { Protegee, Trusted }
