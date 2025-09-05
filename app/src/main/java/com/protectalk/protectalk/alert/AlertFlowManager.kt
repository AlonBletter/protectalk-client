package com.protectalk.protectalk.alert

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.protectalk.protectalk.alert.scam.ScamNotificationManager
import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.BuildConfig
import com.protectalk.protectalk.data.remote.analysis.ChatGPTAnalyzer
import com.protectalk.protectalk.data.remote.transcription.Transcriber
import com.protectalk.protectalk.utils.AudioConverter
import com.protectalk.protectalk.utils.RecordingFinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

object AlertFlowManager {

    private const val TAG = "AlertFlowManager"
    private const val SCAM_THRESHOLD = 80 // 80% scam probability threshold (your implementation uses 0-100 scale)

    // Coroutine scope for background operations
    private val alertScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Handles the alert flow when a call from an unknown number ends
     * This is the main entry point for the scam detection system
     *
     * @param context The application context
     * @param phoneNumber The phone number of the unknown caller
     */
    @RequiresApi(Build.VERSION_CODES.S)
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
     * Complete scam detection pipeline using existing sophisticated implementations
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun processUnknownCallAlert(context: Context, phoneNumber: String) {
        Log.d(TAG, "🔍 Starting scam detection pipeline for $phoneNumber")

        // Show processing notification
        ScamNotificationManager.showProcessingNotification(context, phoneNumber)

        try {
            // Step 1: Find the latest call recording using your existing RecordingFinder
            Log.d(TAG, "📁 Step 1: Searching for latest call recording...")
            val recordingFile =
                RecordingFinder.findLatestRecording()

            if (recordingFile == null) {
                Log.w(TAG, "❌ No call recording found or API level too low")
                // Still report a basic alert without transcript analysis
                reportBasicAlert(phoneNumber)
                ScamNotificationManager.dismissProcessingNotification(context)
                return
            }

            Log.i(TAG, "✅ Found recording: ${recordingFile.name}")

            // Step 2: Convert audio if needed using your existing AudioConverter
            Log.d(TAG, "🔄 Step 2: Converting audio if needed...")
            val processedFile = if (recordingFile.extension.lowercase() == "m4a") {
                val wavFile = File(
                    context.cacheDir,
                    "converted_${System.currentTimeMillis()}.wav"
                )

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

            // Step 3: Transcribe using your existing Transcriber with Google Speech-to-Text
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

            // Step 4: Analyze for scam using your existing ChatGPTAnalyzer
            Log.d(TAG, "🧠 Step 4: Analyzing transcript for scam indicators...")
            val openaiApiKey = BuildConfig.OPENAI_API_KEY
            if (openaiApiKey.isBlank()) {
                Log.e(TAG, "❌ OpenAI API key not configured")
                reportBasicAlert(phoneNumber)
                ScamNotificationManager.dismissProcessingNotification(context)
                return
            }

            val analyzer = ChatGPTAnalyzer(openaiApiKey)
            val scamResult = analyzer.analyze(transcript)

            Log.i(TAG, "✅ Analysis complete: ${scamResult.score}% scam probability")

            // Step 5: Report to server using existing triggerAlert endpoint
            Log.d(TAG, "📡 Step 5: Reporting to server...")
            reportScamAlert(
                phoneNumber = phoneNumber,
                transcript = transcript,
                scamScore = scamResult.score,
                analysisPoints = scamResult.analysisPoints
            )

            // Step 6: Notify user if threshold exceeded
            if (scamResult.score >= SCAM_THRESHOLD) {
                Log.w(TAG, "🚨 SCAM DETECTED! Score: ${scamResult.score}%")

                ScamNotificationManager.showScamAlert(
                    context = context,
                    callerNumber = phoneNumber,
                    scamScore = scamResult.score / 100.0, // Convert to 0.0-1.0 for notification
                    analysis = scamResult.analysisPoints.joinToString("; ")
                )
            } else {
                Log.i(TAG, "✅ Call appears legitimate (score: ${scamResult.score}%)")
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
     * Reports scam analysis to the server using existing triggerAlert endpoint
     */
    private suspend fun reportScamAlert(
        phoneNumber: String,
        transcript: String,
        scamScore: Int,
        analysisPoints: List<String>
    ) {
        try {
            val severity = if (scamScore >= SCAM_THRESHOLD) "urgent" else "info"

            val message = buildString {
                append("Scam Analysis Report\n")
                append("Caller: $phoneNumber\n")
                append("Scam Score: $scamScore%\n")
                append("Risk Level: $severity\n")
                if (analysisPoints.isNotEmpty()) {
                    append("Analysis:\n")
                    analysisPoints.forEach { point ->
                        append("• $point\n")
                    }
                }
                append("\nTranscript: ${transcript.take(500)}...")
            }

            val result = AppModule.alertRepo.triggerAlert(message, severity)

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
}
