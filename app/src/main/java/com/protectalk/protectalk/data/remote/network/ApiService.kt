package com.protectalk.protectalk.data.remote.network

import com.protectalk.protectalk.data.model.dto.CompleteRegistrationRequest
import com.protectalk.protectalk.data.model.dto.RegisterTokenRequest
import com.protectalk.protectalk.data.model.dto.TriggerAlertRequest
import com.protectalk.protectalk.data.model.dto.ContactRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("api/device-tokens/register")
    suspend fun registerDevice(@Body body: RegisterTokenRequest): Response<Unit>

    @POST("api/users/complete-registration")
    suspend fun completeRegistration(@Body body: CompleteRegistrationRequest): Response<Unit>

    @POST("api/v1/alerts")
    suspend fun triggerAlert(@Body body: TriggerAlertRequest): Response<Unit>

    @POST("api/users/contact-request")
    suspend fun sendContactRequest(@Body body: ContactRequestDto): Response<Unit>

    // Protection flows (stubs for later phases)
    @POST("api/v1/links/invite")
    suspend fun sendInvite(@Body body: Map<String, Any>): Response<Unit>

    @POST("api/v1/links/respond")
    suspend fun respondInvite(@Body body: Map<String, Any>): Response<Unit>

    @GET("api/v1/links")
    suspend fun getLinks(): Response<Map<String, Any>> // TODO: replace with DTOs
}
