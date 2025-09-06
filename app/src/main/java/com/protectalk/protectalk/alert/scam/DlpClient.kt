package com.protectalk.protectalk.alert.scam

import android.util.Log
import com.protectalk.protectalk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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

            // Define what infoTypes to inspect - using proper JSON arrays
            val infoTypesArray = JSONArray().apply {
                put(JSONObject().put("name", "PHONE_NUMBER"))
                put(JSONObject().put("name", "EMAIL_ADDRESS"))
                put(JSONObject().put("name", "CREDIT_CARD_NUMBER"))
                put(JSONObject().put("name", "PERSON_NAME"))
                put(JSONObject().put("name", "US_SOCIAL_SECURITY_NUMBER"))
            }

            val inspectConfig = JSONObject().apply {
                put("infoTypes", infoTypesArray)
            }

            // Define how to mask sensitive data - using proper JSON arrays
            val transformationsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("primitiveTransformation", JSONObject().apply {
                        put("replaceWithInfoTypeConfig", JSONObject())
                    })
                })
            }

            val deidentifyConfig = JSONObject().apply {
                put("infoTypeTransformations", JSONObject().apply {
                    put("transformations", transformationsArray)
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

            Log.d("DlpClient", "Request body: ${json.toString()}")

            // REST endpoint
            val url = "https://dlp.googleapis.com/v2/projects/$projectId/locations/global/content:deidentify?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d("DlpClient", "Response code: ${response.code}")
                Log.d("DlpClient", "Response body: $responseBody")

                if (!response.isSuccessful) {
                    Log.e("DlpClient", "DLP error: ${response.code} ${response.message}")
                    Log.e("DlpClient", "Error response body: $responseBody")
                    return@withContext text // Return original text if DLP fails
                }

                val jsonResp = JSONObject(responseBody)
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
