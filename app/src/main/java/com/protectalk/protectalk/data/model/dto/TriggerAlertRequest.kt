package com.protectalk.protectalk.data.model.dto

data class TriggerAlertRequest(
    val message: String,
    val severity: String,        // e.g., "info" | "urgent"
    val callId: String? = null,  // optional linking later
    val transcriptId: String? = null
)
