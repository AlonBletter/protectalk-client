package com.protectalk.protectalk.alert.scam

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.protectalk.protectalk.R

/**
 * Handles creation and display of scam and safe call notifications for ProtecTalk.
 */
object ScamNotificationManager {

    // == Notification Channel Constants ==
    const val SCAM_ALERT_NOTIFICATION_CHANNEL_ID: String = "protectalk_alerts_v2"
    private const val SCAM_ALERT_NOTIFICATION_CHANNEL_NAME: String = "ProtecTalk Scam Alerts"
    private const val SCAM_ALERT_NOTIFICATION_CHANNEL_DESCRIPTION: String = "Urgent scam call alerts"

    // == Notification ID Constants ==
    private const val NOTIFICATION_ID_SCAM_CALL_DETECTED: Int = 2
    private const val NOTIFICATION_ID_SAFE_CALL: Int = 3

    // == Notification Text Constants ==
    private const val NOTIFICATION_TITLE_SCAM_CALL: String = "⚠️ Potential Scam Call Detected!"
    private const val NOTIFICATION_TITLE_SAFE_CALL: String = "✅ Call Safe"
    private const val NOTIFICATION_BODY_SAFE_CALL: String = "No scam risk detected. This call is safe!"

    /**
     * Creates the notification channel required for scam alerts on Android O+.
     */
    fun createScamAlertNotificationChannel(applicationContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val scamAlertNotificationChannel: NotificationChannel = NotificationChannel(
                SCAM_ALERT_NOTIFICATION_CHANNEL_ID,
                SCAM_ALERT_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = SCAM_ALERT_NOTIFICATION_CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                lightColor = Color.RED
            }

            val systemNotificationManager: NotificationManager =
                applicationContext.getSystemService(NotificationManager::class.java)

            systemNotificationManager.createNotificationChannel(scamAlertNotificationChannel)
        }
    }

    /**
     * Shows a scam alert notification with risk score and analysis.
     * Consolidated method that handles all scam notification display logic.
     */
    fun showScamAlert(
        context: Context,
        callerNumber: String,
        scamScore: Double,
        analysis: String
    ) {
        // Ensure notification channel is created first
        createScamAlertNotificationChannel(context)

        val riskPercentage = (scamScore * 100).toInt()
        val analysisPoints = analysis.split("; ").filter { it.isNotBlank() }

        // Create detailed message with phone number
        val detailedScamMessage = buildString {
            append("Phone: $callerNumber\n")
            append("Risk Score: $riskPercentage%")
            if (analysisPoints.isNotEmpty()) {
                append("\n\nAnalysis:")
                analysisPoints.forEach { point ->
                    append("\n• $point")
                }
            }
        }

        val scamAlertNotification = NotificationCompat.Builder(
            context,
            SCAM_ALERT_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle(NOTIFICATION_TITLE_SCAM_CALL)
            .setContentText("Phone: $callerNumber - Risk: $riskPercentage%")
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedScamMessage))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        val systemNotificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        systemNotificationManager.notify(
            NOTIFICATION_ID_SCAM_CALL_DETECTED,
            scamAlertNotification
        )

        Log.d("ScamNotificationManager", "Scam alert notification shown for $callerNumber with $riskPercentage% risk")
    }

    /**
     * Shows a safe call notification to reassure the user.
     */
    fun showSafeCallNotification(context: Context, phoneNumber: String? = null) {
        // Ensure notification channel is created first
        createScamAlertNotificationChannel(context)

        val message = if (phoneNumber != null) {
            "Call from $phoneNumber appears to be safe."
        } else {
            NOTIFICATION_BODY_SAFE_CALL
        }

        val safeCallNotification = NotificationCompat.Builder(
            context,
            SCAM_ALERT_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle(NOTIFICATION_TITLE_SAFE_CALL)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200))
            .build()

        val systemNotificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        systemNotificationManager.notify(
            NOTIFICATION_ID_SAFE_CALL,
            safeCallNotification
        )

        Log.d("ScamNotificationManager", "Safe call notification shown${phoneNumber?.let { " for $it" } ?: ""}")
    }
}
