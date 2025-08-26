package com.protectalk.protectalk.data.model.dto

data class CompleteRegistrationRequest(
    val name: String,
    val phoneNumber: String,
    val RegisterTokenRequest: RegisterTokenRequest
)
