package com.protectalk.protectalk.data.repo

import com.protectalk.protectalk.data.model.dto.TriggerAlertRequest
import com.protectalk.protectalk.data.remote.network.ApiService
import com.protectalk.protectalk.data.model.ResultModel

class AlertRepository(private val api: ApiService) {
    suspend fun triggerAlert(message: String, severity: String): ResultModel<Unit> = try {
        api.triggerAlert(TriggerAlertRequest(message, severity))
        ResultModel.Ok(Unit)
    } catch (t: Throwable) {
        ResultModel.Err("Failed to trigger alert", t)
    }
}
