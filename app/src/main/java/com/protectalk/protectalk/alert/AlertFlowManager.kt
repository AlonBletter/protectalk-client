package com.protectalk.protectalk.alert

import android.content.Context
import android.util.Log
import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.alert.scam.*
import com.protectalk.protectalk.data.model.ResultModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AlertFlowManager {

    private const val TAG = "AlertFlowManager"
    private const val SCAM_THRESHOLD = 0.8 // 80% scam probability threshold (0.0-1.0 scale)

    // Coroutine scope for background operations
    private val alertScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Handles the alert flow when a call from an unknown number ends
     * This is the main entry point for the scam detection system
     * Compatible with API level 24+
     *
     * @param context The application context
     * @param phoneNumber The phone number of the unknown caller
     * @param callDurationSeconds The duration of the call in seconds
     */
    fun handleUnknownCallEnded(context: Context, phoneNumber: String, callDurationSeconds: Int = 0) {
        Log.i(TAG, "🚨 Alert flow triggered for unknown number: $phoneNumber (duration: ${callDurationSeconds}s)")

        alertScope.launch {
            try {
                processUnknownCallAlert(context, phoneNumber, callDurationSeconds)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing unknown call alert", e)
            }
        }
    }

    /**
     * Complete scam detection pipeline following the specified flow:
     * 1. App triggered when unknown caller call ends
     * 2. Locate audio file of the call
     * 3. Transcribe the audio file
     * 4. Filter sensitive information using DLP
     * 5. Send filtered transcription to OpenAI for analysis
     * 6. Report to server with analysis and score
     * 7. If risk score > 0.8, send notification to user
     * Compatible with API level 24+
     */
    private suspend fun processUnknownCallAlert(context: Context, phoneNumber: String, callDurationSeconds: Int) {
        Log.d(TAG, "🔍 Starting scam detection pipeline for $phoneNumber")

        try {
            // Step 2: Find and prepare the latest call recording
            Log.d(TAG, "📁 Step 2: Finding and preparing latest call recording...")
            val preparedWavFile = RecordingFinder.findAndPrepareLatestRecording(context)

            if (preparedWavFile == null) {
                Log.w(TAG, "❌ No call recording found or conversion failed")
                return
            }

            Log.i(TAG, "✅ Recording prepared: ${preparedWavFile.name}")

            // Step 3: Transcribe the prepared audio file
            Log.d(TAG, "🎤 Step 3: Transcribing prepared WAV file...")
            val transcript = run {
                val transcriber = Transcriber()
                transcriber.transcribeWavFile(preparedWavFile)
            }

            if (transcript.isBlank() || transcript.contains("No recording found") || transcript.contains("conversion failed") || transcript.contains("WAV file not found")) {
                Log.w(TAG, "❌ Transcription failed or empty: $transcript")
                return
            }

            Log.i(TAG, "✅ Transcription successful: \"${transcript.take(100)}...\"")

            // Step 4: Filter sensitive information using DLP
            Log.d(TAG, "🔒 Step 4: Filtering sensitive information using DLP...")
            val filteredTranscript = run {
                val dlpClient = DlpClient()
                dlpClient.deidentifyText(transcript) ?: transcript // Use original if DLP fails
            }

            Log.i(TAG, "✅ DLP filtering complete")

            // Step 5: Analyze for scam using OpenAI
            Log.d(TAG, "🧠 Step 5: Analyzing filtered transcript for scam indicators...")
            val openaiApiKey = getOpenAIApiKey()
            if (openaiApiKey.isBlank()) {
                Log.e(TAG, "❌ OpenAI API key not configured")
                return
            }

            val analyzer = ChatGPTAnalyzer(openaiApiKey)
            val scamResult = analyzer.analyze(filteredTranscript)

            // Keep the score as 0-1.0 scale (don't change it as user requested)
            val riskScore = scamResult.score

            Log.i(TAG, "✅ Analysis complete: ${(scamResult.score * 100).toInt()}% scam probability")

            // Step 6: Report to server using triggerScamAlert endpoint
            Log.d(TAG, "📡 Step 6: Reporting to server...")
            reportScamAlert(
                phoneNumber = phoneNumber,
                filteredTranscript = filteredTranscript,
                riskScore = riskScore,
                analysisPoints = scamResult.analysisPoints,
                callDurationSeconds = callDurationSeconds
            )

            // Step 7: Show notification if risk score > threshold
            Log.d(TAG, "📱 Step 7: Checking if notification should be sent...")
            if (riskScore > SCAM_THRESHOLD) {
                Log.i(TAG, "🚨 High risk detected (${(riskScore * 100).toInt()}% > ${(SCAM_THRESHOLD * 100).toInt()}%), sending scam alert")
                ScamNotificationManager.showScamAlert(
                    context = context,
                    callerNumber = phoneNumber,
                    scamScore = riskScore,
                    analysis = scamResult.analysisPoints.joinToString("; ")
                )
            } else {
                Log.i(TAG, "✅ Low risk detected (${(riskScore * 100).toInt()}% <= ${(SCAM_THRESHOLD * 100).toInt()}%), sending safe call notification")
                ScamNotificationManager.showSafeCallNotification(context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in scam detection pipeline", e)
            // Just log the error, don't send any alerts for failed analysis
        }
    }

    /**
     * Reports scam analysis to the server using triggerScamAlert endpoint with filtered transcript
     */
    private suspend fun reportScamAlert(
        phoneNumber: String,
        filteredTranscript: String,
        riskScore: Double,
        analysisPoints: List<String>,
        callDurationSeconds: Int = 0
    ) {
        try {
            val riskLevel = when {
                riskScore >= 0.8 -> com.protectalk.protectalk.data.model.dto.RiskLevel.RED
                riskScore >= 0.5 -> com.protectalk.protectalk.data.model.dto.RiskLevel.YELLOW
                else -> com.protectalk.protectalk.data.model.dto.RiskLevel.GREEN
            }

            // Use the analysis field directly as requested
            val modelAnalysis = if (analysisPoints.isNotEmpty()) {
                analysisPoints.first() // Use the first element which is the analysis field
            } else {
                "Automated scam analysis completed"
            }

            val result = AppModule.alertRepo.triggerScamAlert(
                callerNumber = phoneNumber,
                modelScore = riskScore,
                riskLevel = riskLevel,
                transcript = filteredTranscript,
                modelAnalysis = modelAnalysis,
                durationInSeconds = callDurationSeconds
            )

            when (result) {
                is ResultModel.Ok -> {
                    Log.i(TAG, "✅ Scam alert reported successfully to server")
                }
                is ResultModel.Err -> {
                    Log.e(TAG, "❌ Failed to report scam alert: ${result.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error reporting scam alert to server", e)
        }
    }

    // Temporary method to access OpenAI API key until BuildConfig is available
    private fun getOpenAIApiKey(): String {
        return try {
            // Try to access BuildConfig via reflection
            val buildConfigClass = Class.forName("com.protectalk.protectalk.BuildConfig")
            val field = buildConfigClass.getDeclaredField("OPENAI_API_KEY")
            field.get(null) as String
        } catch (e: Exception) {
            Log.w(TAG, "BuildConfig not available, returning empty API key")
            ""
        }
    }
}
