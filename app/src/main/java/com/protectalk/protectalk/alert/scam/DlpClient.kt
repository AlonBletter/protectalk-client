package com.protectalk.protectalk.alert.scam

import android.util.Log
import com.protectalk.protectalk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class DlpClient {
    private val client = OkHttpClient()

    /**
     * Deidentifies sensitive information in text using Google DLP API
     * Uses API key and project ID from BuildConfig
     */
    suspend fun deidentifyText(text: String): String? = withContext(Dispatchers.IO) {
        try {
            // Check if DLP configuration is available
            val apiKey = getGoogleApiKey()
            val projectId = getProjectId()

            if (apiKey.isBlank() || projectId.isBlank()) {
                Log.w("DlpClient", "DLP configuration missing, returning original text")
                return@withContext text
            }

            // Define what infoTypes to inspect
            val inspectConfig = JSONObject().apply {
                put("infoTypes", listOf(
                    JSONObject().put("name", "PHONE_NUMBER"),
                    JSONObject().put("name", "EMAIL_ADDRESS"),
                    JSONObject().put("name", "CREDIT_CARD_NUMBER"),
                    JSONObject().put("name", "PERSON_NAME"),
                    JSONObject().put("name", "US_SOCIAL_SECURITY_NUMBER")
                ))
            }

            // Define how to mask sensitive data
            val deidentifyConfig = JSONObject().apply {
                put("infoTypeTransformations", JSONObject().apply {
                    put("transformations", listOf(
                        JSONObject().apply {
                            put("primitiveTransformation", JSONObject().apply {
                                put("replaceWithInfoTypeConfig", JSONObject())
                            })
                        }
                    ))
                })
            }

            // Build request body
            val json = JSONObject().apply {
                put("item", JSONObject().put("value", text))
                put("inspectConfig", inspectConfig)
                put("deidentifyConfig", deidentifyConfig)
            }

            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())

            // REST endpoint
            val url = "https://dlp.googleapis.com/v2/projects/$projectId/locations/global/content:deidentify?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("DlpClient", "DLP error: ${response.code} ${response.message}")
                    return@withContext text // Return original text if DLP fails
                }

                val body = response.body?.string()
                val jsonResp = JSONObject(body ?: "{}")
                return@withContext jsonResp
                    .optJSONObject("item")
                    ?.optString("value") ?: text
            }
        } catch (e: Exception) {
            Log.e("DlpClient", "Exception: ${e.message}", e)
            return@withContext text // Return original text if exception occurs
        }
    }

    private fun getGoogleApiKey(): String {
        return try {
            BuildConfig.GOOGLE_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    private fun getProjectId(): String {
        return try {
            BuildConfig.GOOGLE_PROJECT_ID
        } catch (e: Exception) {
            "protectalk"
        }
    }
}
