package com.protectalk.protectalk.data.repo

import android.os.Build
import androidx.annotation.RequiresApi
import com.protectalk.protectalk.data.model.dto.TriggerAlertRequest
import com.protectalk.protectalk.data.model.dto.RiskLevel
import com.protectalk.protectalk.data.remote.network.ApiService
import com.protectalk.protectalk.data.model.ResultModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class AlertRepository(private val api: ApiService) {

    // Keep the old method for backward compatibility with basic alerts
    suspend fun triggerAlert(message: String, severity: String): ResultModel<Unit> = try {
        // Create a basic alert using the proper format
        val request = TriggerAlertRequest(
            eventId = UUID.randomUUID().toString(),
            callerNumber = "unknown",
            modelScore = if (severity == "urgent") 0.9 else 0.1,
            riskLevel = when (severity) {
                "urgent" -> RiskLevel.RED
                "info" -> RiskLevel.GREEN
                else -> RiskLevel.YELLOW
            },
            transcript = message,
            modelAnalysis = "Basic alert: $message",
            occurredAt = getCurrentISOTimestamp(),
            durationInSeconds = 0
        )
        api.triggerAlert(request)
        ResultModel.Ok(Unit)
    } catch (t: Throwable) {
        ResultModel.Err("Failed to trigger alert", t)
    }

    // New method for proper scam detection alerts
    suspend fun triggerScamAlert(
        callerNumber: String,
        modelScore: Double,
        riskLevel: RiskLevel,
        transcript: String,
        modelAnalysis: String,
        durationInSeconds: Int = 0
    ): ResultModel<Unit> = try {
        val request = TriggerAlertRequest(
            eventId = UUID.randomUUID().toString(),
            callerNumber = callerNumber,
            modelScore = modelScore,
            riskLevel = riskLevel,
            transcript = transcript,
            modelAnalysis = modelAnalysis,
            occurredAt = getCurrentISOTimestamp(),
            durationInSeconds = durationInSeconds
        )
        api.triggerAlert(request)
        ResultModel.Ok(Unit)
    } catch (t: Throwable) {
        ResultModel.Err("Failed to trigger scam alert", t)
    }

    // Backward-compatible ISO timestamp generation (API 24+)
    private fun getCurrentISOTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
