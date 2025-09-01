package com.protectalk.protectalk.data.remote.network

import com.protectalk.protectalk.data.model.dto.CompleteRegistrationRequest
import com.protectalk.protectalk.data.model.dto.RegisterTokenRequest
import com.protectalk.protectalk.data.model.dto.TriggerAlertRequest
import com.protectalk.protectalk.data.model.dto.ContactRequestDto
import com.protectalk.protectalk.data.model.dto.UserProfileResponse
import com.protectalk.protectalk.data.model.dto.DeleteContactRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/device-tokens/register")
    suspend fun registerDevice(@Body body: RegisterTokenRequest): Response<Unit>

    @POST("api/users/complete-registration")
    suspend fun completeRegistration(@Body body: CompleteRegistrationRequest): Response<Unit>

    @POST("api/v1/alerts")
    suspend fun triggerAlert(@Body body: TriggerAlertRequest): Response<Unit>

    @POST("api/users/contact-request")
    suspend fun sendContactRequest(@Body body: ContactRequestDto): Response<Unit>

    @GET("api/users/profile")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @POST("api/users/requests/{requestId}/approve")
    suspend fun approveRequest(@Path("requestId") requestId: String): Response<Unit>

    @POST("api/users/requests/{requestId}/deny")
    suspend fun denyRequest(@Path("requestId") requestId: String): Response<Unit>

    @DELETE("api/users/contacts")
    suspend fun deleteLinkedContact(
        @Query("phoneNumber") phoneNumber: String,
        @Query("contactType") contactType: String
    ): Response<Unit>

    @POST("api/users/requests/{requestId}/cancel")
    suspend fun cancelRequest(@Path("requestId") requestId: String): Response<Unit>

}
