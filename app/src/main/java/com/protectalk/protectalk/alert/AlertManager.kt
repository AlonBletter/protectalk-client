package com.protectalk.protectalk.alert

import android.content.Context
import android.os.Build
import android.util.Log
import com.protectalk.protectalk.permissions.PermissionManager

object AlertManager {

    private const val TAG = "AlertManager"

    /**
     * Starts the alert monitoring system
     * This will start the background service that monitors calls
     */
    fun startAlertMonitoring(context: Context): Boolean {
        Log.i(TAG, "=== ATTEMPTING TO START ALERT MONITORING ===")
        Log.d(TAG, "Android API Level: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Required permissions: ${PermissionManager.getEssentialPermissions().joinToString()}")

        if (!PermissionManager.checkEssentialPermissions(context)) {
            val missing = PermissionManager.getMissingEssentialPermissions(context)
            Log.e(TAG, "❌ CANNOT START - Missing required permissions: $missing")
            return false
        }

        Log.i(TAG, "✅ All permissions granted, starting CallMonitoringService...")

        try {
            CallMonitoringService.startService(context)
            Log.i(TAG, "🚀 CallMonitoringService.startService() called successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "💥 FAILED to start CallMonitoringService", e)
            return false
        }
    }

    /**
     * Checks if all required permissions are granted
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        return PermissionManager.checkEssentialPermissions(context)
    }
}
