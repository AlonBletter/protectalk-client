package com.protectalk.protectalk.data.model.dto

data class RegisterTokenRequest(
    val fcmToken: String,
    val deviceId: String,
    val platform: String = "android",
    val appVersion: String? = null
)
