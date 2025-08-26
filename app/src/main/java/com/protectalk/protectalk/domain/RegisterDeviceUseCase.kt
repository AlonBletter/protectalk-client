package com.protectalk.protectalk.domain

import android.content.Context
import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.push.PushManager
import java.util.UUID
import androidx.core.content.edit

class RegisterDeviceUseCase {
    suspend operator fun invoke(context: Context): ResultModel<Unit> {
        // Get or generate device ID
        val deviceId = getOrCreateDeviceId(context)

        // Best-effort refresh & upload
        PushManager.refreshIdToken()
        return PushManager.sendTokenToServer(AppModule.accountRepo, deviceId)
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
