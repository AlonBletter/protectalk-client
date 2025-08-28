package com.protectalk.protectalk.domain

import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel

class TriggerAlertUseCase {
    suspend operator fun invoke(message: String, severity: String): ResultModel<Unit> {
        return AppModule.alertRepo.triggerAlert(message, severity)
    }
}
