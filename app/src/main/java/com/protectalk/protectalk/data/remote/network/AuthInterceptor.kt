package com.protectalk.protectalk.data.remote.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    companion object {
        private const val TAG = "AuthInterceptor"

        @Volatile
        private var cachedToken: String? = null

        @Volatile
        private var tokenExpiryTime: Long = 0L

        // Refresh token 5 minutes before expiry to be safe
        private const val REFRESH_BUFFER_MS = 5 * 60 * 1000L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Get token (cached or fresh)
        val idToken = getValidToken()

        val req = if (!idToken.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $idToken")
                .build()
        } else {
            Log.w(TAG, "No valid ID token available for request")
            original
        }

        return chain.proceed(req)
    }

    private fun getValidToken(): String? {
        val currentTime = System.currentTimeMillis()

        // Check if we have a cached token that's still valid
        if (cachedToken != null && currentTime < (tokenExpiryTime - REFRESH_BUFFER_MS)) {
            Log.d(TAG, "Using cached ID token (expires in ${(tokenExpiryTime - currentTime) / 1000}s)")
            return cachedToken
        }

        // Need to fetch a new token
        Log.d(TAG, "Fetching fresh ID token (cached token ${if (cachedToken == null) "missing" else "expired"})")

        return try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                runBlocking {
                    val tokenResult = user.getIdToken(false).await()
                    val token = tokenResult.token
                    val expirationTimestamp = tokenResult.expirationTimestamp

                    // Cache the token and its expiry time
                    cachedToken = token
                    tokenExpiryTime = expirationTimestamp * 1000L // Convert to milliseconds

                    Log.d(TAG, "Cached new ID token (expires at ${java.util.Date(tokenExpiryTime)})")
                    token
                }
            } else {
                Log.w(TAG, "No current user found, cannot get ID token")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ID token", e)
            null
        }
    }

    // Method to clear cached token (useful for logout)
    fun clearCachedToken() {
        Log.d(TAG, "Clearing cached ID token")
        cachedToken = null
        tokenExpiryTime = 0L
    }
}
