package com.protectalk.protectalk.domain

import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel

class DeleteLinkedContactUseCase {
    suspend operator fun invoke(phoneNumber: String, contactType: String): ResultModel<Unit> {
        return AppModule.protectionRepo.deleteLinkedContact(phoneNumber, contactType)
    }
}
