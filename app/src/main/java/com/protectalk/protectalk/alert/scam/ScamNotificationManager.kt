package com.protectalk.protectalk.alert.scam

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.protectalk.protectalk.MainActivity
import com.protectalk.protectalk.R

/**
 * Manages scam detection notifications
 */
object ScamNotificationManager {

    private const val CHANNEL_ID = "scam_alerts"
    private const val CHANNEL_NAME = "Scam Alerts"
    private const val CHANNEL_DESCRIPTION = "High-priority notifications for detected scam calls"
    private const val NOTIFICATION_ID = 2001

    /**
     * Shows a high-priority notification when a scam is detected
     */
    fun showScamAlert(
        context: Context,
        callerNumber: String,
        scamScore: Double,
        analysis: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scorePercentage = (scamScore * 100).toInt()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🚨 Scam Call Detected")
            .setContentText("$scorePercentage% scam risk from $callerNumber")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Scam risk: $scorePercentage%\nCaller: $callerNumber\n\nAnalysis: $analysis"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF0000.toInt()) // Red color for danger
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Shows a notification for processing status
     */
    fun showProcessingNotification(context: Context, callerNumber: String) {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Analyzing Call")
            .setContentText("Checking call from $callerNumber for scam indicators...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Dismisses the processing notification
     */
    fun dismissProcessingNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID + 1)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
