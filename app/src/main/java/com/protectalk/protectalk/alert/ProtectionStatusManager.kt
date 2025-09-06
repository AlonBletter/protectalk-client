package com.protectalk.protectalk.alert

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.util.Log
import com.protectalk.protectalk.alert.scam.RecordingFinder
import com.protectalk.protectalk.permissions.PermissionManager

/**
 * Manages and checks the overall protection status of the app.
 * This includes verifying that all necessary permissions are granted
 * and that call recording functionality is working properly.
 * Uses centralized PermissionManager for all permission checking.
 */
object ProtectionStatusManager {

    private const val TAG = "ProtectionStatusManager"

    /**
     * Data class representing the overall protection status
     */
    data class ProtectionStatus(
        val allPermissionsGranted: Boolean,
        val callRecordingEnabled: Boolean,
        val isFullyProtected: Boolean = allPermissionsGranted && callRecordingEnabled,
        val statusMessage: String
    )

    /**
     * Checks the complete protection status including permissions and call recording functionality.
     * This should be called when the home screen appears to provide real-time status.
     * Uses the same permission checking logic as AlertManager and the monitoring service.
     *
     * @param context The application context
     * @return ProtectionStatus containing all status information
     */
    fun checkProtectionStatus(context: Context): ProtectionStatus {
        Log.d(TAG, "🔍 Checking complete protection status...")

        // Use centralized PermissionManager for permission checking
        val essentialPermissionsGranted = PermissionManager.checkEssentialPermissions(context)
        val audioPermissionsGranted = PermissionManager.checkAudioPermissions(context)
        val allPermissionsGranted = essentialPermissionsGranted && audioPermissionsGranted

        // Check call recording functionality
        val callRecordingEnabled = checkCallRecordingStatus(context)

        // Generate status message
        val statusMessage = generateStatusMessage(allPermissionsGranted, callRecordingEnabled)

        val status = ProtectionStatus(
            allPermissionsGranted = allPermissionsGranted,
            callRecordingEnabled = callRecordingEnabled,
            statusMessage = statusMessage
        )

        Log.i(TAG, "✅ Protection status check complete: $status")
        Log.d(TAG, "Permission details: Essential=$essentialPermissionsGranted, Audio=$audioPermissionsGranted")

        return status
    }

    /**
     * Checks if call recording is working by comparing the latest recording
     * with the most recent call from the call log.
     *
     * @param context The application context
     * @return True if call recording appears to be working, false otherwise
     */
    private fun checkCallRecordingStatus(context: Context): Boolean {
        try {
            // Get the most recent call from call log
            val lastCallTime = getLastCallTime(context)
            if (lastCallTime == null) {
                Log.d(TAG, "📞 No recent calls found in call log - cannot verify recording")
                // If no calls found, we can't verify but don't mark as broken
                return true
            }

            // Use RecordingFinder to check if recording is working
            return RecordingFinder.isCallRecordingWorking(context, lastCallTime)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking call recording status: ${e.message}", e)
            return false
        }
    }

    /**
     * Retrieves the start time of the most recent call from the call log.
     *
     * @param context The application context
     * @return The start time of the last call in milliseconds, or null if no calls found
     */
    private fun getLastCallTime(context: Context): Long? {
        var cursor: Cursor? = null
        try {
            // Query the call log for the most recent call
            cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.DATE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                val dateColumnIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                if (dateColumnIndex != -1) {
                    val lastCallTime = cursor.getLong(dateColumnIndex)
                    Log.d(TAG, "📞 Last call time: $lastCallTime")
                    return lastCallTime
                }
            }

            Log.d(TAG, "📞 No calls found in call log")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading call log: ${e.message}", e)
            return null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Generates a user-friendly status message based on the protection status.
     *
     * @param allPermissionsGranted Whether all required permissions are granted
     * @param callRecordingEnabled Whether call recording is working
     * @return A descriptive status message
     */
    private fun generateStatusMessage(
        allPermissionsGranted: Boolean,
        callRecordingEnabled: Boolean
    ): String {
        return when {
            allPermissionsGranted && callRecordingEnabled ->
                "✅ Full protection active - All permissions granted and call recording working"

            allPermissionsGranted && !callRecordingEnabled ->
                "⚠️ Partial protection - Permissions granted but call recording not detected"

            !allPermissionsGranted && callRecordingEnabled ->
                "⚠️ Partial protection - Call recording working but some permissions missing"

            else ->
                "❌ Protection disabled - Missing permissions and call recording not working"
        }
    }

    /**
     * Quick check method that only verifies permissions (lighter operation).
     * Useful for frequent status checks without the overhead of recording verification.
     *
     * @param context The application context
     * @return True if all essential permissions are granted
     */
    fun arePermissionsGranted(context: Context): Boolean {
        return try {
            PermissionManager.checkEssentialPermissions(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions: ${e.message}", e)
            false
        }
    }
}
