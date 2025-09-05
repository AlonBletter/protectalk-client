package com.protectalk.protectalk.alert.scam

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents the result of a scam analysis using a language model.
 */
data class ScamResult(
    val score: Double, // Changed from Int to Double for 0.0-1.0 range
    val analysisPoints: List<String>
)

/**
 * Handles scam analysis logic by invoking the OpenAI GPT API with a specialized prompt.
 *
 * @param apiKey The OpenAI API key used for authentication.
 */
class ChatGPTAnalyzer(private val apiKey: String) {

    companion object {
        private const val LOG_TAG: String = "ChatGPTAnalyzer"

        private const val OPENAI_CHAT_COMPLETION_ENDPOINT: String =
            "https://api.openai.com/v1/chat/completions"

        private const val MODEL_NAME: String = "gpt-4-turbo"
        private const val TEMPERATURE: Double = 0.2

        private const val SCAM_SCORE_KEY = "scam_score"
        private const val ANALYSIS_KEY = "analysis"

        private val JSON_MEDIA_TYPE = "application/json".toMediaTypeOrNull()

        private const val MAX_ANALYSIS_POINTS = 3

        private const val SYSTEM_PROMPT = """
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

    private val httpClient: OkHttpClient = OkHttpClient()

    /**
     * Submits a transcript to GPT for scam analysis and returns a [ScamResult].
     *
     * @param transcript The call transcript to analyze.
     * @return The scam score and analysis bullet points.
     */
    suspend fun analyze(transcript: String): ScamResult = withContext(Dispatchers.IO) {
        Log.d(LOG_TAG, "🧠 Analyzing transcript (preview): ${transcript.take(50)}...")

        val requestMessages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            put(JSONObject().put("role", "user").put("content", transcript))
        }

        val requestPayload = JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", requestMessages)
            put("temperature", TEMPERATURE)
        }

        val httpRequest = Request.Builder()
            .url(OPENAI_CHAT_COMPLETION_ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val rawResponseText: String = httpClient.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            Log.d(LOG_TAG, "🔗 GPT HTTP ${response.code}: $responseBody")
            responseBody
        }

        val messageContent: String = try {
            JSONObject(rawResponseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "❌ Failed to extract message from response: ${e.message}", e)
            return@withContext ScamResult(
                score = 0.0,
                analysisPoints = listOf(
                    "⚠️ GPT response format error.",
                    "Raw response: ${rawResponseText.take(150)}"
                )
            )
        }

        val resultJson: JSONObject = try {
            JSONObject(messageContent)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "❌ Failed to parse GPT message as JSON: ${e.message}", e)
            return@withContext ScamResult(
                score = 0.0,
                analysisPoints = listOf("⚠️ Could not parse GPT response.", "Raw content:", messageContent)
            )
        }

        val scamScore: Double = resultJson.optDouble("scamScore", 0.0)
        val analysisJsonArray: JSONArray = resultJson.optJSONArray("indicators") ?:
                                          resultJson.optJSONArray("analysis") ?: JSONArray()

        val bulletPoints: List<String> = List(analysisJsonArray.length()) { i ->
            analysisJsonArray.optString(i)
        }.take(MAX_ANALYSIS_POINTS)

        ScamResult(score = scamScore, analysisPoints = bulletPoints)
    }
}
