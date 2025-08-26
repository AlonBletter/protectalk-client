package com.protectalk.protectalk.domain

import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel

class GetLinksUseCase {
    suspend operator fun invoke(): ResultModel<Map<String, Any>> {
        return AppModule.protectionRepo.getLinks()
    }
}
