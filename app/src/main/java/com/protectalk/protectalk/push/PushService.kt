package com.protectalk.protectalk.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.protectalk.protectalk.MainActivity
import com.protectalk.protectalk.R
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class PushService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "PushService"
        private const val CHANNEL_ID = "protectalk_notifications"
        private const val CHANNEL_NAME = "ProtectTalk Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for protection alerts and contact requests"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token received")
        PushManager.onNewFcmToken(token)

        // Enqueue background upload (WorkManager) if signed in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d(TAG, "User is signed in, enqueuing FCM token upload work")
            enqueueTokenUploadWork(token)
        } else {
            Log.d(TAG, "No signed-in user, FCM token will be uploaded on next sign-in")
        }
    }

    private fun enqueueTokenUploadWork(token: String) {
        val inputData = Data.Builder()
            .putString(FcmTokenUploadWorker.KEY_FCM_TOKEN, token)
            .build()

        val uploadWork = OneTimeWorkRequestBuilder<FcmTokenUploadWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueue(uploadWork)
        Log.d(TAG, "FCM token upload work enqueued")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM message received from: ${message.from}")

        // Log message data for debugging
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
        }

        message.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")
        }

        // Handle different types of messages based on server notification types
        when (message.data["type"]) {
            "contact_request_received" -> handleContactRequestReceived(message)
            "contact_request_approved" -> handleContactRequestApproved(message)
            "contact_request_denied" -> handleContactRequestDenied(message)
            "protection_alert" -> handleProtectionAlert(message)
            "contact_response" -> handleContactResponse(message) // Legacy support
            else -> handleGenericNotification(message)
        }
    }

    private fun handleContactRequestReceived(message: RemoteMessage) {
        Log.d(TAG, "Handling contact request received notification")

        // Use server-provided title and body instead of composing locally
        val title = message.data["title"] ?: message.notification?.title ?: "Contact Request"
        val body = message.data["body"] ?: message.notification?.body ?: "You have a new contact request"

        showNotification(
            title = title,
            body = body,
            data = message.data
        )
    }

    private fun handleContactRequestApproved(message: RemoteMessage) {
        Log.d(TAG, "Handling contact request approved notification")

        // Use server-provided title and body instead of composing locally
        val title = message.data["title"] ?: message.notification?.title ?: "Request Approved"
        val body = message.data["body"] ?: message.notification?.body ?: "Your request was approved"

        showNotification(
            title = title,
            body = body,
            data = message.data
        )
    }

    private fun handleContactRequestDenied(message: RemoteMessage) {
        Log.d(TAG, "Handling contact request denied notification")

        // Use server-provided title and body instead of composing locally
        val title = message.data["title"] ?: message.notification?.title ?: "Request Denied"
        val body = message.data["body"] ?: message.notification?.body ?: "Your request was denied"

        showNotification(
            title = title,
            body = body,
            data = message.data
        )
    }

    private fun handleProtectionAlert(message: RemoteMessage) {
        Log.d(TAG, "Handling protection alert notification")

        // Use server-provided title and body instead of composing locally
        val title = message.data["title"] ?: message.notification?.title ?: "🚨 Emergency Alert"
        val body = message.data["body"] ?: message.notification?.body ?: "Emergency assistance needed"

        showNotification(
            title = title,
            body = body,
            data = message.data,
            isHighPriority = true
        )
    }

    private fun handleContactResponse(message: RemoteMessage) {
        Log.d(TAG, "Handling contact response notification")

        // Use server-provided title and body instead of composing locally
        val title = message.data["title"] ?: message.notification?.title ?: "Contact Request Response"
        val body = message.data["body"] ?: message.notification?.body ?: "Someone responded to your request"

        showNotification(
            title = title,
            body = body,
            data = message.data
        )
    }

    private fun handleGenericNotification(message: RemoteMessage) {
        Log.d(TAG, "Handling generic notification")

        val title = message.notification?.title ?: message.data["title"] ?: "ProtectTalk"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new message"

        showNotification(
            title = title,
            body = body,
            data = message.data
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        isHighPriority: Boolean = false
    ) {
        Log.d(TAG, "Showing notification: $title - $body")

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add any extra data from the FCM message
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        if (isHighPriority) {
            notificationBuilder
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
