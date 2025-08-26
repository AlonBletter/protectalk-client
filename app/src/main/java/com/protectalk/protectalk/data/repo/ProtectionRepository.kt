package com.protectalk.protectalk.data.repo

import com.protectalk.protectalk.data.remote.network.ApiService
import com.protectalk.protectalk.data.model.ResultModel

class ProtectionRepository(private val api: ApiService) {
    suspend fun sendInvite(body: Map<String, Any>): ResultModel<Unit> = try {
        api.sendInvite(body)
        ResultModel.Ok(Unit)
    } catch (t: Throwable) {
        ResultModel.Err("Failed to send invite", t)
    }

    suspend fun respondInvite(body: Map<String, Any>): ResultModel<Unit> = try {
        api.respondInvite(body)
        ResultModel.Ok(Unit)
    } catch (t: Throwable) {
        ResultModel.Err("Failed to respond to invite", t)
    }

    suspend fun getLinks(): ResultModel<Map<String, Any>> = try {
        val data = api.getLinks()
        ResultModel.Ok(data)
    } catch (t: Throwable) {
        ResultModel.Err("Failed to load links", t)
    }
}
