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
     * Data class representing the overall protection status - binary ON/OFF only
     */
    data class ProtectionStatus(
        val allPermissionsGranted: Boolean,
        val callRecordingStatus: CallRecordingStatus,
        val isFullyProtected: Boolean,
        val statusMessage: String,
        val detailMessage: String
    )

    /**
     * Represents the call recording status
     */
    enum class CallRecordingStatus {
        WORKING,        // Call recording is confirmed working
        NOT_WORKING,    // Call recording is confirmed not working
        CANNOT_CHECK    // Cannot check due to missing permissions
    }

    /**
     * Checks the complete protection status including permissions and call recording functionality.
     * Protection is either fully ON (all requirements met) or completely OFF.
     * No partial protection states.
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

        // Check call recording functionality (nullable result)
        val callRecordingResult = checkCallRecordingStatus(context)
        val callRecordingStatus = when (callRecordingResult) {
            true -> CallRecordingStatus.WORKING
            false -> CallRecordingStatus.NOT_WORKING
            null -> CallRecordingStatus.CANNOT_CHECK
        }

        // Protection is binary: either fully ON or completely OFF
        // If we can't check call recording due to permissions, protection is OFF
        val isProtectionOn = allPermissionsGranted && (callRecordingStatus == CallRecordingStatus.WORKING)

        // Generate binary status message
        val statusMessage = if (isProtectionOn) {
            "Protection active"
        } else {
            "Protection disabled"
        }

        // Generate detailed message based on the specific issue
        val detailMessage = when {
            !allPermissionsGranted -> {
                val missingItems = mutableListOf<String>()

                // Check each essential permission individually
                if (!essentialPermissionsGranted) {
                    val missingEssential = PermissionManager.getMissingEssentialPermissions(context)

                    // Add each missing essential permission as a separate item
                    if (missingEssential.any { it.contains("READ_PHONE_STATE") }) {
                        missingItems.add("phone access")
                    }
                    if (missingEssential.any { it.contains("READ_CALL_LOG") }) {
                        missingItems.add("call log access")
                    }
                    if (missingEssential.any { it.contains("READ_CONTACTS") }) {
                        missingItems.add("contact access")
                    }
                    if (missingEssential.any { it.contains("POST_NOTIFICATIONS") }) {
                        missingItems.add("notification permissions")
                    }
                }

                // Check each audio permission individually
                if (!audioPermissionsGranted) {
                    val missingAudio = PermissionManager.getMissingAudioPermissions(context)
                    if (missingAudio.any { it.contains("READ_EXTERNAL_STORAGE") }) {
                        missingItems.add("storage access")
                    }
                    if (missingAudio.any { it.contains("READ_MEDIA_AUDIO") }) {
                        missingItems.add("media access")
                    }
                }

                // If no specific permissions identified, show generic message
                if (missingItems.isEmpty()) {
                    "Grant required app permissions"
                } else {
                    "Grant ${missingItems.joinToString(", ")} to continue"
                }
            }
            callRecordingStatus == CallRecordingStatus.CANNOT_CHECK -> "Enable permissions to verify call recording"
            callRecordingStatus == CallRecordingStatus.NOT_WORKING -> "Enable call recording in your phone settings"
            else -> "Everything looks great! You're protected"
        }

        val status = ProtectionStatus(
            allPermissionsGranted = allPermissionsGranted,
            callRecordingStatus = callRecordingStatus,
            isFullyProtected = isProtectionOn,
            statusMessage = statusMessage,
            detailMessage = detailMessage
        )

        Log.i(TAG, "Protection status check complete: ${if (isProtectionOn) "ON" else "OFF"}")
        Log.d(TAG, "Permission details: Essential=$essentialPermissionsGranted, Audio=$audioPermissionsGranted, Recording=$callRecordingStatus")

        return status
    }

    /**
     * Checks if call recording is working by comparing the latest recording
     * with the most recent call from the call log.
     * Returns null if we can't check due to missing permissions.
     *
     * @param context The application context
     * @return True if recording is working, false if not working, null if can't check due to permissions
     */
    private fun checkCallRecordingStatus(context: Context): Boolean? {
        try {
            // First check if we have permission to read call log
            if (!PermissionManager.checkEssentialPermissions(context)) {
                Log.d(TAG, "📞 Cannot check call recording - missing essential permissions")
                return null // Can't check without permissions
            }

            // Get the most recent call from call log
            val lastCallTime = getLastCallTime(context)
            if (lastCallTime == null) {
                Log.d(TAG, "📞 No recent calls found in call log - cannot verify recording")
                // If no calls found, we can't verify but don't mark as broken
                return true
            }

            // Use RecordingFinder to check if recording is working
            return RecordingFinder.isCallRecordingWorking(context, lastCallTime)

        } catch (e: SecurityException) {
            Log.w(TAG, "❌ Cannot check call recording due to permission denial: ${e.message}")
            return null // Can't check due to permissions
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking call recording status: ${e.message}", e)
            return false // Actual error - mark as not working
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
}
