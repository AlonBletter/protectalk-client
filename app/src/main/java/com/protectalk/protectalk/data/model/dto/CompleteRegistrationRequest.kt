package com.protectalk.protectalk.data.model.dto

data class CompleteRegistrationRequest(
    val name: String,
    val phoneNumber: String,
    val registerTokenRequest: RegisterTokenRequest
)
