package com.protectalk.protectalk.data.repo

import android.util.Log
import com.protectalk.protectalk.data.model.dto.ContactRequestDto
import com.protectalk.protectalk.data.model.dto.UserProfileResponse
import com.protectalk.protectalk.data.remote.network.ApiService
import com.protectalk.protectalk.data.model.ResultModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun sendInvite(body: Map<String, Any>): ResultModel<Unit> = try {
        val response = apiService.sendInvite(body)
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
        val response = apiService.respondInvite(body)
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
        val response = apiService.getLinks()
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

    suspend fun sendContactRequest(contactRequest: ContactRequestDto): ResultModel<Unit> = try {
        Log.d("ProtectionRepository", "Sending contact request to server...")
        Log.d("ProtectionRepository", "Request: $contactRequest")

        val response = apiService.sendContactRequest(contactRequest)

        if (response.isSuccessful) {
            Log.d("ProtectionRepository", "Contact request sent successfully")
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMsg = "Failed to send contact request: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}"
            Log.e("ProtectionRepository", errorMsg)
            ResultModel.Err(errorMsg)
        }
    } catch (t: Throwable) {
        Log.e("ProtectionRepository", "Exception during contact request", t)
        ResultModel.Err("Failed to send contact request", t)
    }

    suspend fun getUserProfile(): ResultModel<UserProfileResponse> = try {
        Log.d("ProtectionRepository", "Fetching user profile from server...")

        val response = apiService.getUserProfile()

        if (response.isSuccessful) {
            val profile = response.body()
            if (profile != null) {
                Log.d("ProtectionRepository", "User profile fetched successfully")
                ResultModel.Ok(profile)
            } else {
                Log.e("ProtectionRepository", "Profile response body is null")
                ResultModel.Err("Empty profile response")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMsg = "Failed to fetch profile: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}"
            Log.e("ProtectionRepository", errorMsg)
            ResultModel.Err(errorMsg)
        }
    } catch (t: Throwable) {
        Log.e("ProtectionRepository", "Exception during profile fetch", t)
        ResultModel.Err("Failed to fetch profile", t)
    }

    suspend fun approveRequest(requestId: String): ResultModel<Unit> = try {
        val response = apiService.approveRequest(requestId)
        if (response.isSuccessful) {
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            ResultModel.Err("Failed to approve request: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}")
        }
    } catch (t: Throwable) {
        ResultModel.Err("Failed to approve request", t)
    }

    suspend fun denyRequest(requestId: String): ResultModel<Unit> = try {
        val response = apiService.denyRequest(requestId)
        if (response.isSuccessful) {
            ResultModel.Ok(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            ResultModel.Err("Failed to deny request: HTTP ${response.code()} - ${response.message()}${if (errorBody != null) " - $errorBody" else ""}")
        }
    } catch (t: Throwable) {
        ResultModel.Err("Failed to deny request", t)
    }
}
