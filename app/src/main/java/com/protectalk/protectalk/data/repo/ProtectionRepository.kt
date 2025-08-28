package com.protectalk.protectalk.data.repo

import com.protectalk.protectalk.data.remote.network.ApiService
import com.protectalk.protectalk.data.model.ResultModel

class ProtectionRepository(private val api: ApiService) {
    suspend fun sendInvite(body: Map<String, Any>): ResultModel<Unit> = try {
        val response = api.sendInvite(body)
        if (response.isSuccessful) {
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            ResultModel.Err("Failed to send invite: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}")
        }
    } catch (t: Throwable) {
        ResultModel.Err("Failed to send invite", t)
    }

    suspend fun respondInvite(body: Map<String, Any>): ResultModel<Unit> = try {
        val response = api.respondInvite(body)
        if (response.isSuccessful) {
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            ResultModel.Err("Failed to respond to invite: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}")
        }
    } catch (t: Throwable) {
        ResultModel.Err("Failed to respond to invite", t)
    }

    suspend fun getLinks(): ResultModel<Map<String, Any>> = try {
        val response = api.getLinks()
        if (response.isSuccessful) {
            val data = response.body() ?: emptyMap()
            ResultModel.Ok(data)
        } else {
            val errorBody = response.errorBody()?.string()
            ResultModel.Err("Failed to load links: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}")
        }
    } catch (t: Throwable) {
        ResultModel.Err("Failed to load links", t)
    }
}
