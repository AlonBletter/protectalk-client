package com.protectalk.protectalk.domain

import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel

class RespondToInviteUseCase {
    suspend operator fun invoke(requestId: String, accept: Boolean): ResultModel<Unit> {
        val body = mapOf("requestId" to requestId, "accept" to accept)
        return AppModule.protectionRepo.respondInvite(body)
    }
}
