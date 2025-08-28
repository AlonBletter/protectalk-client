package com.protectalk.protectalk.data.model.dto

data class RegisterTokenRequest(
    val deviceId: String,
    val fcmToken: String,
    val platform: String = "ANDROID",
    val appVersion: String? = null
)
