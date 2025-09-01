package com.protectalk.protectalk.push

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.protectalk.protectalk.data.repo.AccountRepository
import com.protectalk.protectalk.data.model.ResultModel
import kotlinx.coroutines.tasks.await

object PushManager {
    private const val TAG = "PushManager"

    @Volatile var cachedFcmToken: String? = null
    @Volatile var cachedIdToken: String? = null

    // Clear cached tokens when user logs out or switches
    fun clearTokens() {
        Log.d(TAG, "Clearing cached tokens")
        cachedFcmToken = null
        cachedIdToken = null
    }

    suspend fun refreshIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w(TAG, "No current user found, cannot refresh ID token")
            return null
        }

        return try {
            val token = user.getIdToken(true).await().token
            cachedIdToken = token
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh ID token", e)
            null
        }
    }

    suspend fun fetchFcmToken(forceRefresh: Boolean = false): String? {
        // Always fetch fresh token if forced or no cached token
        if (forceRefresh || cachedFcmToken == null) {
            return try {
                Log.d(TAG, "Fetching fresh FCM token...")
                val token = FirebaseMessaging.getInstance().token.await()
                cachedFcmToken = token
                Log.d(TAG, "Fresh FCM token obtained: ${token.take(10)}...")
                token
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch FCM token", e)
                null
            }
        }

        Log.d(TAG, "Using cached FCM token: ${cachedFcmToken?.take(10)}...")
        return cachedFcmToken
    }

    suspend fun sendTokenToServer(repo: AccountRepository, deviceId: String): ResultModel<Unit> {
        val token = cachedFcmToken ?: fetchFcmToken()
        if (token == null) {
            Log.e(TAG, "No FCM token available to send to server")
            return ResultModel.Err("No FCM token")
        }

        if (cachedIdToken.isNullOrBlank()) {
            refreshIdToken() // best-effort
        }

        return try {
            val result = repo.registerDeviceToken(token, deviceId)
            if (result is ResultModel.Err) {
                Log.e(TAG, "Failed to send token to server: ${result.message}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending token to server", e)
            ResultModel.Err("Exception: ${e.message}")
        }
    }

    fun onNewFcmToken(token: String) {
        val oldToken = cachedFcmToken
        cachedFcmToken = token

        if (oldToken != null && oldToken != token) {
            Log.i(TAG, "FCM token has changed from ${oldToken.take(10)}... to ${token.take(10)}...")
        } else {
            Log.d(TAG, "FCM token received: ${token.take(10)}...")
        }

        // Token is now cached and will be uploaded by the WorkManager
        // The PushService handles enqueuing the upload work when user is signed in
    }
}
