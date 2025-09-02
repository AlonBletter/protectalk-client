package com.protectalk.protectalk.alert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

object AlertManager {

    private const val TAG = "AlertManager"

    /**
     * Required permissions for the alert flow
     */
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    )

    /**
     * Gets all required permissions based on current Android version
     */
    private fun getAllRequiredPermissions(): Array<String> {
        return REQUIRED_PERMISSIONS // No API-specific permissions needed
    }

    /**
     * Starts the alert monitoring system
     * This will start the background service that monitors calls
     */
    fun startAlertMonitoring(context: Context): Boolean {
        Log.i(TAG, "=== ATTEMPTING TO START ALERT MONITORING ===")
        Log.d(TAG, "Android API Level: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Required permissions: ${getAllRequiredPermissions().joinToString()}")

        if (!hasRequiredPermissions(context)) {
            val missing = getMissingPermissions(context)
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
     * Stops the alert monitoring system
     */
    fun stopAlertMonitoring(context: Context) {
        Log.d(TAG, "Stopping alert monitoring system")

        try {
            CallMonitoringService.stopService(context)
            Log.i(TAG, "Alert monitoring stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop alert monitoring", e)
        }
    }

    /**
     * Checks if the alert monitoring system is currently active
     * TODO: Implement proper service state checking
     */
    fun isAlertMonitoringActive(context: Context): Boolean {
        // TODO: Implement proper service state checking
        // For now, check if we have permissions as a proxy
        return hasRequiredPermissions(context)
    }

    /**
     * Checks if all required permissions are granted
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        return getAllRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Gets the list of required permissions that are not yet granted
     */
    fun getMissingPermissions(context: Context): List<String> {
        return getAllRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Gets all required permissions for requesting
     */
    fun getRequiredPermissions(): Array<String> {
        return getAllRequiredPermissions()
    }
}
