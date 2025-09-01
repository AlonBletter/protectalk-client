package com.protectalk.protectalk.data.model.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeleteContactRequestDto(
    val phoneNumber: String,
    val contactType: String // "TRUSTED_CONTACT" or "PROTEGEE"
)
