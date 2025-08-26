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

    suspend fun refreshIdToken(): String? {
        Log.d(TAG, "Attempting to refresh ID token...")
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w(TAG, "No current user found, cannot refresh ID token")
            return null
        }

        return try {
            val token = user.getIdToken(true).await().token
            cachedIdToken = token
            Log.d(TAG, "ID token refreshed successfully. Token length: ${token?.length}")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh ID token", e)
            null
        }
    }

    suspend fun fetchFcmToken(): String? {
        Log.d(TAG, "Fetching FCM token...")
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            cachedFcmToken = token
            Log.d(TAG, "FCM token fetched successfully: ${token?.take(20)}...${token?.takeLast(10)}")
            Log.d(TAG, "FCM token cached. Full length: ${token?.length}")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch FCM token", e)
            null
        }
    }

    suspend fun sendTokenToServer(repo: AccountRepository, deviceId: String): ResultModel<Unit> {
        Log.d(TAG, "Attempting to send token to server...")
        Log.d(TAG, "Cached FCM token exists: ${cachedFcmToken != null}")
        Log.d(TAG, "Using device ID: $deviceId")

        val token = cachedFcmToken ?: fetchFcmToken()
        if (token == null) {
            Log.e(TAG, "No FCM token available to send to server")
            return ResultModel.Err("No FCM token")
        }

        Log.d(TAG, "Using FCM token: ${token.take(20)}...${token.takeLast(10)}")

        if (cachedIdToken.isNullOrBlank()) {
            Log.d(TAG, "ID token is blank, attempting to refresh...")
            refreshIdToken() // best-effort
        } else {
            Log.d(TAG, "Using cached ID token")
        }

        return try {
            val result = repo.registerDeviceToken(token, deviceId)
            when (result) {
                is ResultModel.Ok -> Log.d(TAG, "Successfully sent token to server")
                is ResultModel.Err -> Log.e(TAG, "Failed to send token to server: ${result.message}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending token to server", e)
            ResultModel.Err("Exception: ${e.message}")
        }
    }

    fun onNewFcmToken(token: String) {
        Log.i(TAG, "New FCM token received!")
        Log.d(TAG, "New FCM token: ${token.take(20)}...${token.takeLast(10)}")
        Log.d(TAG, "New FCM token length: ${token.length}")

        val oldToken = cachedFcmToken
        cachedFcmToken = token

        if (oldToken != null && oldToken != token) {
            Log.i(TAG, "FCM token has changed from previous token")
        } else if (oldToken == null) {
            Log.i(TAG, "This is the first FCM token received")
        } else {
            Log.d(TAG, "FCM token is the same as cached token")
        }

        // TODO: enqueue upload if user is signed in
        Log.d(TAG, "FCM token cached successfully")
    }
}
