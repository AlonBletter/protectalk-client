package com.protectalk.protectalk.alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.protectalk.protectalk.MainActivity
import com.protectalk.protectalk.R

class CallMonitoringService : Service() {

    companion object {
        private const val TAG = "CallMonitoringService"
        private const val CHANNEL_ID = "call_monitoring_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, CallMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CallMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var callStateReceiver: CallStateReceiver
    private var isReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "=== CallMonitoringService CREATED ===")
        Log.d(TAG, "Process ID: ${android.os.Process.myPid()}")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Initialize and register the call state receiver
        callStateReceiver = CallStateReceiver()
        registerCallStateReceiver()

        Log.i(TAG, "=== CallMonitoringService FULLY INITIALIZED ===")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "=== CallMonitoringService STARTED ===")
        Log.d(TAG, "Intent: $intent, Flags: $flags, StartId: $startId")
        return START_STICKY // Restart service if it gets killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "=== CallMonitoringService DESTROYED ===")

        unregisterCallStateReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors calls for protection alerts"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ProtectTalk Protection Active")
            .setContentText("Monitoring calls for your safety")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun registerCallStateReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            }

            registerReceiver(callStateReceiver, filter)
            isReceiverRegistered = true
            Log.d(TAG, "Call state receiver registered")
        }
    }

    private fun unregisterCallStateReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(callStateReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Call state receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver was not registered", e)
            }
        }
    }
}
