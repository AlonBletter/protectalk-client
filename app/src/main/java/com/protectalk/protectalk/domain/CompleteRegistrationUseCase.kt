package com.protectalk.protectalk.domain

import android.content.Context
import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.push.PushManager
import java.util.UUID
import androidx.core.content.edit

class CompleteRegistrationUseCase {
    suspend operator fun invoke(
        context: Context,
        name: String,
        phoneNumber: String
    ): ResultModel<Unit> {
        // Get or generate device ID
        val deviceId = getOrCreateDeviceId(context)

        // Get FCM token
        val fcmToken = PushManager.cachedFcmToken ?: PushManager.fetchFcmToken()
        if (fcmToken == null) {
            return ResultModel.Err("Failed to get FCM token")
        }

        // Refresh Firebase ID token for authentication
        PushManager.refreshIdToken()

        // Complete registration with user profile and device data
        return AppModule.accountRepo.completeRegistration(
            name = name,
            phoneNumber = phoneNumber,
            fcmToken = fcmToken,
            deviceId = deviceId
        )
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
