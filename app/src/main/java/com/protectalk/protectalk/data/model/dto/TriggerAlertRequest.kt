package com.protectalk.protectalk.data.model.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TriggerAlertRequest(
    val eventId: String,          // client-generated UUID for idempotency
    val callerNumber: String,     // E.164 format
    val modelScore: Double,       // model score used by client
    val riskLevel: RiskLevel,     // client's classification
    val transcript: String?,      // optional transcript
    val modelAnalysis: String?,   // optional model analysis summary
    val occurredAt: String,       // ISO-8601 timestamp when the call happened
    val durationInSeconds: Int    // call duration
)

enum class RiskLevel {
    GREEN,
    YELLOW,
    RED
}
