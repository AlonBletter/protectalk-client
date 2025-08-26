package com.protectalk.protectalk.data.remote.network

import com.protectalk.protectalk.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {
    fun build(authInterceptor: AuthInterceptor): ApiService {
        val ok = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(ok)
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
