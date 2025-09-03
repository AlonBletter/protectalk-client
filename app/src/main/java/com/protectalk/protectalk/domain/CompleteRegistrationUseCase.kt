package com.protectalk.protectalk.domain

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.push.PushManager
import java.util.UUID

class CompleteRegistrationUseCase {
    suspend operator fun invoke(
        context: Context,
        name: String,
        phoneNumber: String
    ): ResultModel<Unit> {
        // Get or generate device ID
        val deviceId = getOrCreateDeviceId(context)

        // Force fetch a fresh FCM token for each new user registration
        // This ensures each user gets a unique token, not a cached one from previous users
        val fcmToken = PushManager.fetchFcmToken(forceRefresh = true)
        if (fcmToken == null) {
            Log.e("CompleteRegistration", "Failed to get FCM token")
            return ResultModel.Err("Failed to get FCM token")
        }

        Log.d("CompleteRegistration", "Using fresh FCM token for new user: ${fcmToken.take(10)}...")

        // Refresh Firebase ID token for authentication
        val idToken = PushManager.refreshIdToken()
        if (idToken == null) {
            Log.e("CompleteRegistration", "Failed to refresh ID token")
            return ResultModel.Err("Failed to refresh authentication token")
        }

        // Complete registration with user profile and device data
        val result = AppModule.accountRepo.completeRegistration(
            name = name,
            phoneNumber = phoneNumber,
            fcmToken = fcmToken,
            deviceId = deviceId
        )

        if (result is ResultModel.Err) {
            Log.e("CompleteRegistration", "Registration failed: ${result.message}")
        }

        return result
    }

    private fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        val existingDeviceId = prefs.getString("device_id", null)

        return if (existingDeviceId != null) {
            existingDeviceId
        } else {
            // Generate a new UUID-based device ID
            val newDeviceId = "android_${UUID.randomUUID()}"
            prefs.edit { putString("device_id", newDeviceId) }
            newDeviceId
        }
    }
}
