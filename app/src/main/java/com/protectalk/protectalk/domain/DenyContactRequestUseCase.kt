package com.protectalk.protectalk.domain

import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel

class DenyContactRequestUseCase {
    suspend operator fun invoke(requestId: String): ResultModel<Unit> {
        return AppModule.protectionRepo.denyRequest(requestId)
    }
}
