package com.protectalk.protectalk.domain

import android.util.Log
import com.protectalk.protectalk.app.di.AppModule
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.data.model.dto.UserProfileResponse

class GetUserProfileUseCase {
    suspend operator fun invoke(): ResultModel<UserProfileResponse> {
        Log.d("GetUserProfile", "Fetching user profile...")

        return try {
            val result = AppModule.protectionRepo.getUserProfile()

            when (result) {
                is ResultModel.Ok -> Log.d("GetUserProfile", "Profile fetched successfully")
                is ResultModel.Err -> Log.e("GetUserProfile", "Failed to fetch profile: ${result.message}")
            }

            result
        } catch (e: Exception) {
            Log.e("GetUserProfile", "Exception while fetching profile", e)
            ResultModel.Err("Failed to fetch profile: ${e.message}")
        }
    }
}
