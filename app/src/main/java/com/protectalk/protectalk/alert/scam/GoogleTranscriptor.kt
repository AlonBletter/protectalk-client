package com.protectalk.protectalk.alert.scam

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles transcription of audio files using Google Speech-to-Text API
 */
class GoogleTranscriptor(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GoogleTranscriptor"
        private const val SPEECH_API_URL = "https://speech.googleapis.com/v1/speech:recognize"
    }

    /**
     * Transcribes an audio file to text using Google Speech-to-Text API
     */
    suspend fun transcribeAudio(audioFile: File): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting transcription for file: ${audioFile.name}")

            if (!audioFile.exists() || audioFile.length() == 0) {
                Log.e(TAG, "Audio file is empty or doesn't exist")
                return@withContext TranscriptionResult.Error("Audio file is empty or doesn't exist")
            }

            // For files larger than 10MB, we'd need to use Google Cloud Storage
            // For now, we'll handle smaller files with direct upload
            if (audioFile.length() > 10 * 1024 * 1024) {
                Log.w(TAG, "Audio file is too large for direct upload (${audioFile.length()} bytes)")
                return@withContext TranscriptionResult.Error("Audio file too large for transcription")
            }

            val result = performTranscription(audioFile)
            Log.i(TAG, "Transcription completed successfully")
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error during transcription", e)
            TranscriptionResult.Error("Transcription failed: ${e.message}")
        }
    }

    private suspend fun performTranscription(audioFile: File): TranscriptionResult {
        return try {
            // Convert audio file to base64 for API
            val audioBytes = audioFile.readBytes()
            val audioBase64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)

            // Prepare request JSON
            val requestJson = JSONObject().apply {
                put("config", JSONObject().apply {
                    put("encoding", getEncodingForFile(audioFile))
                    put("sampleRateHertz", 16000) // Standard rate for phone calls
                    put("languageCode", "en-US")
                    put("enableAutomaticPunctuation", true)
                    put("enableWordTimeOffsets", false)
                    put("model", "phone_call") // Optimized for phone call audio
                })
                put("audio", JSONObject().apply {
                    put("content", audioBase64)
                })
            }

            val requestBody = RequestBody.create(
                "application/json".toMediaType(),
                requestJson.toString()
            )

            val request = Request.Builder()
                .url("$SPEECH_API_URL?key=$apiKey")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseTranscriptionResponse(responseBody)
            } else {
                val errorBody = response.body?.string()
                Log.e(TAG, "API request failed: ${response.code} - $errorBody")
                TranscriptionResult.Error("API request failed: ${response.code}")
            }

        } catch (e: IOException) {
            Log.e(TAG, "Network error during transcription", e)
            TranscriptionResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during transcription", e)
            TranscriptionResult.Error("Transcription error: ${e.message}")
        }
    }

    private fun getEncodingForFile(file: File): String {
        return when (file.extension.lowercase()) {
            "wav" -> "LINEAR16"
            "mp3" -> "MP3"
            "m4a" -> "MP3" // Fallback
            "3gp" -> "AMR"
            else -> "LINEAR16" // Default
        }
    }

    private fun parseTranscriptionResponse(responseBody: String?): TranscriptionResult {
        return try {
            if (responseBody.isNullOrEmpty()) {
                return TranscriptionResult.Error("Empty response from API")
            }

            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                val message = error.optString("message", "Unknown API error")
                Log.e(TAG, "API returned error: $message")
                return TranscriptionResult.Error("API error: $message")
            }

            val results = jsonResponse.optJSONArray("results")
            if (results == null || results.length() == 0) {
                Log.w(TAG, "No transcription results found")
                return TranscriptionResult.Success("", 0.0)
            }

            val firstResult = results.getJSONObject(0)
            val alternatives = firstResult.getJSONArray("alternatives")

            if (alternatives.length() > 0) {
                val bestAlternative = alternatives.getJSONObject(0)
                val transcript = bestAlternative.getString("transcript")
                val confidence = bestAlternative.optDouble("confidence", 0.0)

                Log.d(TAG, "Transcription: '$transcript' (confidence: $confidence)")
                TranscriptionResult.Success(transcript, confidence)
            } else {
                TranscriptionResult.Error("No transcription alternatives found")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing transcription response", e)
            TranscriptionResult.Error("Error parsing response: ${e.message}")
        }
    }

    sealed class TranscriptionResult {
        data class Success(val transcript: String, val confidence: Double) : TranscriptionResult()
        data class Error(val message: String) : TranscriptionResult()
    }
}
