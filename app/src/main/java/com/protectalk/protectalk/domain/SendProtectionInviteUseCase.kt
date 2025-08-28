package com.protectalk.protectalk.domain

import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel

class SendProtectionInviteUseCase {
    suspend operator fun invoke(
        target: String,           // email/phone
        relation: String,         // "family" | "friend" | "other"
        role: String              // "protegee" | "trusted"
    ): ResultModel<Unit> {
        val body = mapOf("target" to target, "relation" to relation, "role" to role)
        return AppModule.protectionRepo.sendInvite(body)
    }
}
