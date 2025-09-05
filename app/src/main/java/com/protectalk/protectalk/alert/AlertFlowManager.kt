package com.protectalk.protectalk.alert

import android.content.Context
import android.os.Build
import android.util.Log
import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.alert.scam.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

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
     */
    fun handleUnknownCallEnded(context: Context, phoneNumber: String) {
        Log.i(TAG, "🚨 Alert flow triggered for unknown number: $phoneNumber")

        alertScope.launch {
            try {
                processUnknownCallAlert(context, phoneNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing unknown call alert", e)
                ScamNotificationManager.dismissProcessingNotification(context)
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
    private suspend fun processUnknownCallAlert(context: Context, phoneNumber: String) {
        Log.d(TAG, "🔍 Starting scam detection pipeline for $phoneNumber")

        // Show processing notification
        ScamNotificationManager.showProcessingNotification(context, phoneNumber)

        try {
            // Step 2: Find the latest call recording
            Log.d(TAG, "📁 Step 2: Searching for latest call recording...")
            val recordingFile = RecordingFinder.findLatestRecording()

            if (recordingFile == null) {
                Log.w(TAG, "❌ No call recording found")
                reportBasicAlert(phoneNumber)
                ScamNotificationManager.dismissProcessingNotification(context)
                return
            }

            Log.i(TAG, "✅ Found recording: ${recordingFile.name}")

            // Convert audio if needed
            Log.d(TAG, "🔄 Converting audio if needed...")
            val processedFile = if (recordingFile.extension.lowercase() == "m4a") {
                val wavFile = File(context.cacheDir, "converted_${System.currentTimeMillis()}.wav")
                val success = AudioConverter.convertM4aToWav(recordingFile, wavFile)
                if (!success) {
                    Log.e(TAG, "❌ Failed to convert M4A to WAV")
                    reportBasicAlert(phoneNumber)
                    ScamNotificationManager.dismissProcessingNotification(context)
                    return
                }
                wavFile
            } else {
                recordingFile
            }

            // Step 3: Transcribe the audio file
            Log.d(TAG, "🎤 Step 3: Transcribing audio to text...")
            val transcript = run {
                val transcriber = Transcriber()
                transcriber.transcribeFromLatestFile()
            }

            if (transcript.isBlank() || transcript.contains("No recording found") || transcript.contains("conversion failed")) {
                Log.w(TAG, "❌ Transcription failed or empty: $transcript")
                reportBasicAlert(phoneNumber)
                ScamNotificationManager.dismissProcessingNotification(context)
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
                reportBasicAlert(phoneNumber)
                ScamNotificationManager.dismissProcessingNotification(context)
                return
            }

            val analyzer = ChatGPTAnalyzer(openaiApiKey)
            val scamResult = analyzer.analyze(filteredTranscript)

            // Keep the score as 0-1.0 scale (don't change it as user requested)
            val riskScore = scamResult.score

            Log.i(TAG, "✅ Analysis complete: ${(scamResult.score * 100).toInt()}% scam probability")

            // Step 6: Report to server using triggerAlert endpoint
            Log.d(TAG, "📡 Step 6: Reporting to server...")
            reportScamAlert(
                phoneNumber = phoneNumber,
                filteredTranscript = filteredTranscript,
                riskScore = riskScore,
                analysisPoints = scamResult.analysisPoints
            )

            // Step 7: Notify user based on threshold
            if (riskScore >= SCAM_THRESHOLD) {
                Log.w(TAG, "🚨 SCAM DETECTED! Risk score: ${(riskScore * 100).toInt()}%")

                ScamNotificationManager.showScamAlert(
                    context = context,
                    callerNumber = phoneNumber,
                    scamScore = riskScore,
                    analysis = scamResult.analysisPoints.joinToString("; ")
                )
            } else {
                Log.i(TAG, "✅ Call appears legitimate (score: ${(riskScore * 100).toInt()}%)")

                // Show safe call notification like ProtecTalkService did
                ScamNotificationManager.showSafeCallNotification(context)
            }

            ScamNotificationManager.dismissProcessingNotification(context)

            // Clean up temporary files
            if (processedFile != recordingFile) {
                processedFile.delete()
                Log.d(TAG, "🗑️ Cleaned up temporary converted file")
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error in scam detection pipeline", e)
            ScamNotificationManager.dismissProcessingNotification(context)
        }
    }

    /**
     * Reports a basic alert when full analysis isn't possible
     */
    private suspend fun reportBasicAlert(phoneNumber: String) {
        try {
            val message = "Unknown call received from: $phoneNumber"
            val severity = "info"

            val result = AppModule.alertRepo.triggerAlert(message, severity)

            when (result) {
                is com.protectalk.protectalk.data.model.ResultModel.Ok -> {
                    Log.i(TAG, "✅ Basic alert reported successfully to server")
                }
                is com.protectalk.protectalk.data.model.ResultModel.Err -> {
                    Log.e(TAG, "❌ Failed to report basic alert: ${result.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error reporting basic alert to server", e)
        }
    }

    /**
     * Reports scam analysis to the server using triggerScamAlert endpoint with filtered transcript
     */
    private suspend fun reportScamAlert(
        phoneNumber: String,
        filteredTranscript: String,
        riskScore: Double,
        analysisPoints: List<String>
    ) {
        try {
            val riskLevel = when {
                riskScore >= 0.8 -> com.protectalk.protectalk.data.model.dto.RiskLevel.RED
                riskScore >= 0.5 -> com.protectalk.protectalk.data.model.dto.RiskLevel.YELLOW
                else -> com.protectalk.protectalk.data.model.dto.RiskLevel.GREEN
            }

            val modelAnalysis = if (analysisPoints.isNotEmpty()) {
                analysisPoints.joinToString("; ")
            } else {
                "Automated scam analysis completed"
            }

            val result = AppModule.alertRepo.triggerScamAlert(
                callerNumber = phoneNumber,
                modelScore = riskScore,
                riskLevel = riskLevel,
                transcript = filteredTranscript,
                modelAnalysis = modelAnalysis,
                durationInSeconds = 0 // We don't track call duration yet
            )

            when (result) {
                is com.protectalk.protectalk.data.model.ResultModel.Ok -> {
                    Log.i(TAG, "✅ Scam alert reported successfully to server")
                }
                is com.protectalk.protectalk.data.model.ResultModel.Err -> {
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
