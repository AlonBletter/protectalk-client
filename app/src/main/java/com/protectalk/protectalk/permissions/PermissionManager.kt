package com.protectalk.protectalk.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Manages all runtime permissions required for scam detection functionality.
 * Handles both install-time and runtime permission requests with proper fallbacks.
 */
class PermissionManager(private val activity: ComponentActivity) {

    companion object {
        private const val TAG = "PermissionManager"

        // Essential permissions for basic functionality
        private val ESSENTIAL_PERMISSIONS = listOfNotNull(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            // Only request POST_NOTIFICATIONS on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else null
        )

        // Audio processing permissions for scam detection
        private val AUDIO_PERMISSIONS = listOfNotNull(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            // Only request READ_MEDIA_AUDIO on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else null
        )

        // Optional enhanced permissions
        private val ENHANCED_PERMISSIONS = listOfNotNull(
            Manifest.permission.READ_CONTACTS,
            // Only request ANSWER_PHONE_CALLS on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Manifest.permission.ANSWER_PHONE_CALLS
            } else null,
            Manifest.permission.CALL_PHONE
        )
    }

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    // Permission launchers
    private val essentialPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allEssentialGranted = ESSENTIAL_PERMISSIONS.all { permissions[it] == true }
            Log.d(TAG, "Essential permissions result: $allEssentialGranted")

            if (allEssentialGranted) {
                requestAudioPermissions()
            } else {
                Log.w(TAG, "Essential permissions are required for the app to function")
                onPermissionResult?.invoke(false)
            }
        }

    private val audioPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val audioGranted = AUDIO_PERMISSIONS.any { permissions[it] == true }
            Log.d(TAG, "Audio permissions result: $audioGranted")

            requestEnhancedPermissions()
        }

    private val enhancedPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val enhancedGranted = ENHANCED_PERMISSIONS.count { permissions[it] == true }
            Log.d(TAG, "Enhanced permissions granted: $enhancedGranted/${ENHANCED_PERMISSIONS.size}")

            requestSystemAlertPermission()
        }

    private val systemAlertPermissionLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val hasSystemAlertPermission = Settings.canDrawOverlays(activity)
            Log.d(TAG, "System alert permission: $hasSystemAlertPermission")

            val allCriticalGranted = checkEssentialPermissions() && checkAudioPermissions()
            onPermissionResult?.invoke(allCriticalGranted)
        }

    /**
     * Requests all necessary permissions in stages for optimal user experience
     */
    fun requestAllPermissions(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult

        when {
            checkAllPermissions() -> {
                Log.d(TAG, "All permissions already granted")
                onResult(true)
            }
            else -> {
                Log.d(TAG, "Starting permission request flow")
                requestEssentialPermissions()
            }
        }
    }

    /**
     * Stage 1: Request essential permissions (required for basic functionality)
     */
    private fun requestEssentialPermissions() {
        val missingEssential = ESSENTIAL_PERMISSIONS.filter { !isPermissionGranted(it) }

        if (missingEssential.isNotEmpty()) {
            Log.d(TAG, "Requesting essential permissions: $missingEssential")
            essentialPermissionLauncher.launch(missingEssential.toTypedArray())
        } else {
            requestAudioPermissions()
        }
    }

    /**
     * Stage 2: Request audio processing permissions (critical for scam detection)
     */
    private fun requestAudioPermissions() {
        val missingAudio = AUDIO_PERMISSIONS.filter { !isPermissionGranted(it) }

        if (missingAudio.isNotEmpty()) {
            Log.d(TAG, "Requesting audio permissions: $missingAudio")
            audioPermissionLauncher.launch(missingAudio.toTypedArray())
        } else {
            requestEnhancedPermissions()
        }
    }

    /**
     * Stage 3: Request enhanced permissions (optional but recommended)
     */
    private fun requestEnhancedPermissions() {
        val missingEnhanced = ENHANCED_PERMISSIONS.filter { !isPermissionGranted(it) }

        if (missingEnhanced.isNotEmpty()) {
            Log.d(TAG, "Requesting enhanced permissions: $missingEnhanced")
            enhancedPermissionLauncher.launch(missingEnhanced.toTypedArray())
        } else {
            requestSystemAlertPermission()
        }
    }

    /**
     * Stage 4: Request system alert window permission (special permission)
     */
    private fun requestSystemAlertPermission() {
        if (!Settings.canDrawOverlays(activity)) {
            Log.d(TAG, "Requesting system alert window permission")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            systemAlertPermissionLauncher.launch(intent)
        } else {
            val allCriticalGranted = checkEssentialPermissions() && checkAudioPermissions()
            onPermissionResult?.invoke(allCriticalGranted)
        }
    }

    /**
     * Check if a specific permission is granted
     */
    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all essential permissions are granted
     */
    fun checkEssentialPermissions(): Boolean {
        return ESSENTIAL_PERMISSIONS.all { isPermissionGranted(it) }
    }

    /**
     * Check if audio permissions are granted (at least one for compatibility)
     */
    fun checkAudioPermissions(): Boolean {
        return AUDIO_PERMISSIONS.any { isPermissionGranted(it) }
    }

    /**
     * Check if all permissions are granted
     */
    private fun checkAllPermissions(): Boolean {
        val regularPermissions = ESSENTIAL_PERMISSIONS + AUDIO_PERMISSIONS + ENHANCED_PERMISSIONS
        val systemAlert = Settings.canDrawOverlays(activity)

        return regularPermissions.all { isPermissionGranted(it) } && systemAlert
    }

    /**
     * Get permission status summary for debugging
     */
    fun getPermissionStatus(): Map<String, Boolean> {
        return mapOf(
            "Essential" to checkEssentialPermissions(),
            "Audio" to checkAudioPermissions(),
            "SystemAlert" to Settings.canDrawOverlays(activity),
            "AllGranted" to checkAllPermissions()
        )
    }
}
