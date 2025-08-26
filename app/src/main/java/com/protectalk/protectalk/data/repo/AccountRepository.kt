package com.protectalk.protectalk.data.repo

import com.protectalk.protectalk.data.model.dto.CompleteRegistrationRequest
import com.protectalk.protectalk.data.model.dto.RegisterTokenRequest
import com.protectalk.protectalk.data.remote.network.ApiService
import com.protectalk.protectalk.data.model.ResultModel

class AccountRepository(private val api: ApiService) {
    suspend fun registerDeviceToken(token: String, deviceId: String): ResultModel<Unit> = try {
        val response = api.registerDevice(
            RegisterTokenRequest(
                fcmToken = token,
                deviceId = deviceId,
                platform = "android",
                appVersion = "1.0"
            )
        )

        if (response.isSuccessful) {
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            ResultModel.Err("Failed to register device: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}")
        }
    } catch (t: Throwable) {
        ResultModel.Err("Failed to register device", t)
    }

    suspend fun completeRegistration(
        name: String,
        phoneNumber: String,
        fcmToken: String,
        deviceId: String
    ): ResultModel<Unit> = try {
        val response = api.completeRegistration(
            CompleteRegistrationRequest(
                name = name,
                phoneNumber = phoneNumber,
                fcmToken = fcmToken,
                deviceId = deviceId,
                platform = "android",
                appVersion = "1.0"
            )
        )

        if (response.isSuccessful) {
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            ResultModel.Err("Failed to complete registration: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}")
        }
    } catch (t: Throwable) {
        ResultModel.Err("Failed to complete registration", t)
    }
}
