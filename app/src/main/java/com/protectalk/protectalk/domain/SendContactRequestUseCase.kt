package com.protectalk.protectalk.domain

import android.util.Log
import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.data.model.dto.ContactRequestDto
import com.protectalk.protectalk.data.model.dto.ContactType

class SendContactRequestUseCase {
    suspend operator fun invoke(
        name: String,
        phoneNumber: String,
        relationship: String,
        contactType: ContactType
    ): ResultModel<Unit> {
        Log.d("SendContactRequest", "Sending contact request: $name, $phoneNumber, $relationship, $contactType")

        return try {
            val result = AppModule.protectionRepo.sendContactRequest(
                ContactRequestDto(
                    name = name,
                    phoneNumber = phoneNumber,
                    relationship = relationship,
                    contactType = contactType
                )
            )

            when (result) {
                is ResultModel.Ok -> Log.d("SendContactRequest", "Contact request sent successfully")
                is ResultModel.Err -> Log.e("SendContactRequest", "Failed to send contact request: ${result.message}")
            }

            result
        } catch (e: Exception) {
            Log.e("SendContactRequest", "Exception while sending contact request", e)
            ResultModel.Err("Failed to send contact request: ${e.message}")
        }
    }
}
