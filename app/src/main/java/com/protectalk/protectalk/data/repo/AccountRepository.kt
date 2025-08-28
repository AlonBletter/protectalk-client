package com.protectalk.protectalk.data.repo

import android.util.Log
import com.protectalk.protectalk.data.model.dto.CompleteRegistrationRequest
import com.protectalk.protectalk.data.model.dto.RegisterTokenRequest
import com.protectalk.protectalk.data.remote.network.ApiService
import com.protectalk.protectalk.data.model.ResultModel

class AccountRepository(private val api: ApiService) {
    suspend fun registerDeviceToken(token: String, deviceId: String): ResultModel<Unit> = try {
        val request = RegisterTokenRequest(
            deviceId = deviceId,
            fcmToken = token,
            platform = "android",
            appVersion = "1.0"
        )

        val response = api.registerDevice(request)

        if (response.isSuccessful) {
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMsg = "Failed to register device: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}"
            Log.e("AccountRepository", errorMsg)
            ResultModel.Err(errorMsg)
        }
    } catch (t: Throwable) {
        Log.e("AccountRepository", "Exception during device registration", t)
        ResultModel.Err("Failed to register device", t)
    }

    suspend fun completeRegistration(
        name: String,
        phoneNumber: String,
        fcmToken: String,
        deviceId: String
    ): ResultModel<Unit> = try {
        val request = CompleteRegistrationRequest(
            name = name,
            phoneNumber = phoneNumber,
            registerTokenRequest = RegisterTokenRequest(
                deviceId = deviceId,
                fcmToken = fcmToken,
                platform = "android",
                appVersion = "1.0"
            )
        )

        val response = api.completeRegistration(request)

        if (response.isSuccessful) {
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMsg = "Failed to complete registration: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}"
            Log.e("AccountRepository", errorMsg)
            ResultModel.Err(errorMsg)
        }
    } catch (t: Throwable) {
        Log.e("AccountRepository", "Exception during registration completion", t)
        ResultModel.Err("Failed to complete registration", t)
    }
}
