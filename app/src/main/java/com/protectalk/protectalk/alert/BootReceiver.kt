package com.protectalk.protectalk.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver to restart alert monitoring after device boot.
 * This ensures scam detection continues working after device restarts.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "📱 Device boot completed - checking if alert monitoring should restart")

            try {
                // Only restart monitoring if we have the required permissions
                // and the user was previously using the app
                if (AlertManager.hasRequiredPermissions(context)) {
                    Log.i(TAG, "✅ Permissions available - restarting alert monitoring after boot")
                    AlertManager.startAlertMonitoring(context)
                } else {
                    Log.w(TAG, "⚠️ Required permissions not available - cannot restart monitoring")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error restarting alert monitoring after boot", e)
            }
        }
    }
}
