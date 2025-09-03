package com.protectalk.protectalk.app.di

import com.protectalk.protectalk.data.remote.network.ApiClient
import com.protectalk.protectalk.data.remote.network.AuthInterceptor
import com.protectalk.protectalk.data.repo.AccountRepository
import com.protectalk.protectalk.data.repo.AlertRepository
import com.protectalk.protectalk.data.repo.ProtectionRepository

object AppModule {
    private val api by lazy { ApiClient.build(AuthInterceptor.instance) }

    val accountRepo by lazy { AccountRepository(api) }
    val alertRepo by lazy { AlertRepository(api) }
    val protectionRepo by lazy { ProtectionRepository(api) }
}
