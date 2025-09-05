package com.protectalk.protectalk.alert.scam

import android.os.Build
import android.util.Base64
import android.util.Log
import com.protectalk.protectalk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeoutException

/**
 * Finds the latest recording and transcribes it using Google's Speech-to-Text API
 * WITHOUT speaker diarization. Returns a single plain transcript string.
 *
 * Language auto-detection enabled with alternativeLanguageCodes:
 *   - Primary: en-US
 *   - Alternatives: he-IL (Hebrew), ru-RU (Russian)
 *
 * If the 'phone_call' model rejects alternativeLanguageCodes,
 * the request is retried without specifying the model (fallback).
 */
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class Transcriber {

    companion object {
        // == Logging ==
        private const val TAG: String = "RealTimeTranscriber"

        // == HTTP ==
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val httpClient: OkHttpClient = OkHttpClient()

        // == Google STT ==
        private fun sttEndpoint(): String =
            "https://speech.googleapis.com/v1p1beta1/speech:longrunningrecognize?key=${BuildConfig.GOOGLE_API_KEY}"

        private const val MAX_POLLING_ATTEMPTS: Int = 120
        private const val POLLING_DELAY_MS: Long = 1500L
    }

    /**
     * Transcribes the latest call recording using Google Speech-to-Text API
     * Compatible with API level 24+
     */
    suspend fun transcribeFromLatestFile(): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Searching for latest call recording...")

        val latestRecordingFile: File? = RecordingFinder.findLatestRecording()
        if (latestRecordingFile == null) {
            Log.w(TAG, "No recent recording found.")
            return@withContext "No recording found."
        }

        Log.d(TAG, "Found recording: ${latestRecordingFile.absolutePath}")

        // Convert m4a -> wav for safer/STT-friendly input
        val inputAudioFile: File = when (latestRecordingFile.extension.lowercase()) {
            "m4a" -> {
                val wavFile = File(latestRecordingFile.parent, "${latestRecordingFile.nameWithoutExtension}.wav")
                val ok: Boolean = AudioConverter.convertM4aToWav(latestRecordingFile, wavFile)
                if (!ok) return@withContext "WAV conversion failed ❗"
                wavFile
            }
            else -> latestRecordingFile
        }

        val audioBytes: ByteArray = inputAudioFile.readBytes()
        val audioBase64: String = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val audioFormatExtension: String = inputAudioFile.extension.lowercase()
        // Keep LINEAR16 when we feed wav; leave others unspecified unless you know exact codec.
        val encodingType: String = when (audioFormatExtension) {
            "wav" -> "LINEAR16"
            "flac" -> "FLAC"
            "amr" -> "AMR"
            "mp3" -> "MP3"
            else -> "ENCODING_UNSPECIFIED"
        }

        // Build payload with language detection and (first try) phone_call model
        fun buildPayload(usePhoneCallModel: Boolean): String {
            val recognitionConfigJson = JSONObject().apply {
                put("encoding", encodingType)
                put("sampleRateHertz", 48000) // If you downsample, use 16000 accordingly.
                // Primary language guess
                put("languageCode", "en-US")
                // 👇 Let STT auto-pick best match among these (max 3 total including primary)
                put("alternativeLanguageCodes", JSONArray().apply {
                    put("he-IL") // Hebrew
                    put("ru-RU") // Russian
                })
                put("enableAutomaticPunctuation", true)

                if (usePhoneCallModel) {
                    // Some accounts/models reject alternativeLanguageCodes with 'phone_call'.
                    // We'll catch that and retry without the model if needed.
                    put("useEnhanced", true)
                    put("model", "phone_call")
                }
                // NO diarization here for speed
            }

            return JSONObject().apply {
                put("config", recognitionConfigJson)
                put("audio", JSONObject().put("content", audioBase64))
            }.toString()
        }

        fun startOperation(payload: String): Pair<Int, String> {
            val req = Request.Builder()
                .url(sttEndpoint())
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                return response.code to responseBody
            }
        }

        // First try with 'phone_call' model
        var (code, body) = startOperation(buildPayload(usePhoneCallModel = true))

        // If model rejects alternativeLanguageCodes, retry without model
        if (code !in 200..299) {
            val lower = body.lowercase()
            val looksLikeAltLangUnsupported =
                "alternative_language" in lower && "not supported" in lower
            if (looksLikeAltLangUnsupported) {
                Log.w(TAG, "Model 'phone_call' rejected alternativeLanguageCodes. Retrying without model.")
                val retry = startOperation(buildPayload(usePhoneCallModel = false))
                code = retry.first
                body = retry.second
            }
        }

        if (code !in 200..299) {
            Log.e(TAG, "Failed to start STT: $code / $body")
            throw IllegalStateException("STT request failed with code $code")
        }

        val operationId: String = JSONObject(body).getString("name")
        val operationStatusUrl =
            "https://speech.googleapis.com/v1/operations/$operationId?key=${BuildConfig.GOOGLE_API_KEY}"

        // Poll until done
        var detectedLanguage: String? = null
        repeat(MAX_POLLING_ATTEMPTS) {
            delay(POLLING_DELAY_MS)

            httpClient.newCall(Request.Builder().url(operationStatusUrl).build()).execute().use { pollingResponse ->
                val responseJsonString = pollingResponse.body!!.string()
                if (pollingResponse.isSuccessful) {
                    val pollingJson = JSONObject(responseJsonString)
                    Log.d(TAG, "Polling STT: $pollingJson")

                    if (pollingJson.optBoolean("done", false)) {
                        val responseJson = pollingJson.optJSONObject("response") ?: return@use
                        val resultsJsonArray = responseJson.optJSONArray("results") ?: return@use

                        // Build a single transcript from the best alternative of each chunk
                        val transcriptBuilder = StringBuilder()
                        for (i in 0 until resultsJsonArray.length()) {
                            val resultObj = resultsJsonArray.getJSONObject(i)

                            // Try to capture detected language (appears in some responses)
                            detectedLanguage = resultObj.optString("languageCode", detectedLanguage)
                            val alternatives = resultObj.optJSONArray("alternatives")
                            if (alternatives != null && alternatives.length() > 0) {
                                val bestAlt = alternatives.getJSONObject(0)
                                detectedLanguage = bestAlt.optString("languageCode", detectedLanguage)

                                val piece = bestAlt.optString("transcript", "")
                                if (piece.isNotBlank()) {
                                    if (transcriptBuilder.isNotEmpty()) transcriptBuilder.append(' ')
                                    transcriptBuilder.append(piece.trim())
                                }
                            }
                        }

                        val finalTranscript = transcriptBuilder.toString().trim()
                            .ifBlank { "No transcript returned." }

                        Log.d(TAG, "Detected language: ${detectedLanguage ?: "unknown"}")
                        Log.d(TAG, "Final transcript (no diarization):\n$finalTranscript")
                        return@withContext finalTranscript
                    }
                } else {
                    Log.e(TAG, "Polling failed: ${pollingResponse.code} / ${pollingResponse.body?.string()}")
                }
            }
        }

        throw TimeoutException("Transcription timed out after $MAX_POLLING_ATTEMPTS attempts.")
    }
}
