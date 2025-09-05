package com.protectalk.protectalk.alert.scam

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles scam analysis using OpenAI's ChatGPT API
 */
class ChatGPTAnalyzer(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ChatGPTAnalyzer"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"

        private const val SCAM_ANALYSIS_PROMPT = """
You are an expert scam detection AI. Analyze the following phone call transcript and determine if it's a scam call.

Please provide:
1. A scam probability score from 0.0 to 1.0 (where 1.0 = definitely a scam)
2. A brief analysis explaining your reasoning
3. Key indicators that led to your conclusion

Consider these scam indicators:
- Urgency tactics ("act now", "limited time")
- Requests for personal/financial information
- Threats or intimidation
- Too-good-to-be-true offers
- Impersonation of authorities/banks
- Technical support scams
- Prize/lottery scams
- Robocall patterns

Respond in JSON format:
{
  "scamScore": 0.0-1.0,
  "analysis": "Brief explanation",
  "indicators": ["list", "of", "key", "indicators"],
  "riskLevel": "LOW|MEDIUM|HIGH|RED"
}

Transcript to analyze:
"""
    }

    /**
     * Analyzes a transcript for scam indicators using ChatGPT
     */
    suspend fun analyzeForScam(transcript: String, callerNumber: String): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting scam analysis for transcript from $callerNumber")

            if (transcript.isBlank()) {
                Log.w(TAG, "Empty transcript provided")
                return@withContext AnalysisResult.Error("Empty transcript")
            }

            val result = performAnalysis(transcript, callerNumber)
            Log.i(TAG, "Scam analysis completed")
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error during scam analysis", e)
            AnalysisResult.Error("Analysis failed: ${e.message}")
        }
    }

    private suspend fun performAnalysis(transcript: String, callerNumber: String): AnalysisResult {
        return try {
            val fullPrompt = "$SCAM_ANALYSIS_PROMPT\n\n\"$transcript\""

            val requestJson = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are a professional scam detection expert.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", fullPrompt)
                    })
                })
                put("max_tokens", 500)
                put("temperature", 0.3) // Lower temperature for more consistent analysis
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(OPENAI_API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseAnalysisResponse(responseBody)
            } else {
                val errorBody = response.body?.string()
                Log.e(TAG, "OpenAI API request failed: ${response.code} - $errorBody")
                AnalysisResult.Error("API request failed: ${response.code}")
            }

        } catch (e: IOException) {
            Log.e(TAG, "Network error during analysis", e)
            AnalysisResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during analysis", e)
            AnalysisResult.Error("Analysis error: ${e.message}")
        }
    }

    private fun parseAnalysisResponse(responseBody: String?): AnalysisResult {
        return try {
            if (responseBody.isNullOrEmpty()) {
                return AnalysisResult.Error("Empty response from OpenAI API")
            }

            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                val message = error.optString("message", "Unknown OpenAI API error")
                Log.e(TAG, "OpenAI API returned error: $message")
                return AnalysisResult.Error("API error: $message")
            }

            val choices = jsonResponse.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return AnalysisResult.Error("No analysis choices returned")
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content")

            // Parse the JSON response from ChatGPT
            parseScamAnalysisJson(content)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing analysis response", e)
            AnalysisResult.Error("Error parsing response: ${e.message}")
        }
    }

    private fun parseScamAnalysisJson(content: String): AnalysisResult {
        return try {
            // Extract JSON from the response (ChatGPT sometimes adds extra text)
            val jsonStart = content.indexOf("{")
            val jsonEnd = content.lastIndexOf("}") + 1

            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                Log.w(TAG, "No JSON found in response, treating as low-confidence analysis")
                return AnalysisResult.Success(
                    scamScore = 0.1,
                    analysis = content.take(200),
                    indicators = emptyList(),
                    riskLevel = "LOW"
                )
            }

            val jsonContent = content.substring(jsonStart, jsonEnd)
            val analysisJson = JSONObject(jsonContent)

            val scamScore = analysisJson.optDouble("scamScore", 0.0)
            val analysis = analysisJson.optString("analysis", "No analysis provided")
            val riskLevel = analysisJson.optString("riskLevel", "LOW")

            val indicators = mutableListOf<String>()
            val indicatorsArray = analysisJson.optJSONArray("indicators")
            if (indicatorsArray != null) {
                for (i in 0 until indicatorsArray.length()) {
                    indicators.add(indicatorsArray.getString(i))
                }
            }

            Log.d(TAG, "Parsed analysis: score=$scamScore, risk=$riskLevel")

            AnalysisResult.Success(
                scamScore = scamScore.coerceIn(0.0, 1.0),
                analysis = analysis,
                indicators = indicators,
                riskLevel = riskLevel
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing scam analysis JSON", e)
            AnalysisResult.Error("Error parsing analysis: ${e.message}")
        }
    }

    sealed class AnalysisResult {
        data class Success(
            val scamScore: Double,
            val analysis: String,
            val indicators: List<String>,
            val riskLevel: String
        ) : AnalysisResult()

        data class Error(val message: String) : AnalysisResult()
    }
}
