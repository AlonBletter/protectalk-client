package com.protectalk.protectalk.push

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.data.remote.network.ApiClient
import com.protectalk.protectalk.data.repo.AccountRepository

class FcmTokenUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "FcmTokenUploadWorker"
        const val KEY_FCM_TOKEN = "fcm_token"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting FCM token upload work")

        // Check if user is signed in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "No signed-in user, skipping FCM token upload")
            return Result.success()
        }

        // Get the FCM token from input data
        val fcmToken = inputData.getString(KEY_FCM_TOKEN)
        if (fcmToken.isNullOrBlank()) {
            Log.e(TAG, "No FCM token provided to worker")
            return Result.failure()
        }

        // Get device ID
        val deviceId = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        try {
            // Create repository instance
            val accountRepository = AccountRepository(ApiClient.apiService)

            // Upload token to server
            val result = PushManager.sendTokenToServer(accountRepository, deviceId)

            return when (result) {
                is ResultModel.Ok -> {
                    Log.d(TAG, "FCM token upload successful")
                    Result.success()
                }
                is ResultModel.Err -> {
                    Log.e(TAG, "FCM token upload failed: ${result.message}")
                    // Retry on failure
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during FCM token upload", e)
            return Result.retry()
        }
    }
}