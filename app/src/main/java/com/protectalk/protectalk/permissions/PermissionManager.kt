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
            Manifest.permission.READ_CONTACTS, // Include READ_CONTACTS as essential (used by AlertManager)
            // Only request POST_NOTIFICATIONS on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else null
        )

        // Audio processing permissions for scam detection
        private val AUDIO_PERMISSIONS = listOfNotNull(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            // Only request READ_MEDIA_AUDIO on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else null
        )

        // Optional enhanced permissions
        private val ENHANCED_PERMISSIONS = listOfNotNull(
            // Only request ANSWER_PHONE_CALLS on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Manifest.permission.ANSWER_PHONE_CALLS
            } else null,
            Manifest.permission.CALL_PHONE
        )

        // =========================
        // CENTRALIZED STATIC METHODS
        // These can be used by any component without creating a PermissionManager instance
        // =========================

        /**
         * Gets essential permissions needed for basic monitoring (what AlertManager uses)
         */
        fun getEssentialPermissions(): Array<String> {
            return ESSENTIAL_PERMISSIONS.toTypedArray()
        }

        /**
         * Gets audio permissions needed for scam detection
         */
        fun getAudioPermissions(): Array<String> {
            return AUDIO_PERMISSIONS.toTypedArray()
        }

        /**
         * Gets all required permissions for full functionality
         */
        fun getAllRequiredPermissions(): Array<String> {
            return (ESSENTIAL_PERMISSIONS + AUDIO_PERMISSIONS + ENHANCED_PERMISSIONS).toTypedArray()
        }

        /**
         * Checks if essential permissions are granted (for basic monitoring)
         * This is what AlertManager should use
         */
        fun checkEssentialPermissions(context: android.content.Context): Boolean {
            val result = ESSENTIAL_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
            Log.d(TAG, "Essential permissions check: $result")
            return result
        }

        /**
         * Checks if audio permissions are granted (for scam detection)
         */
        fun checkAudioPermissions(context: android.content.Context): Boolean {
            val result = AUDIO_PERMISSIONS.any { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
            Log.d(TAG, "Audio permissions check: $result")
            return result
        }

        /**
         * Checks if all permissions are granted
         */
        fun checkAllPermissions(context: android.content.Context): Boolean {
            val regularPermissions = ESSENTIAL_PERMISSIONS + AUDIO_PERMISSIONS + ENHANCED_PERMISSIONS
            val systemAlert = Settings.canDrawOverlays(context)
            val result = regularPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            } && systemAlert
            Log.d(TAG, "All permissions check: $result")
            return result
        }

        /**
         * Gets missing essential permissions
         */
        fun getMissingEssentialPermissions(context: android.content.Context): List<String> {
            return ESSENTIAL_PERMISSIONS.filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Gets missing audio permissions
         */
        fun getMissingAudioPermissions(context: android.content.Context): List<String> {
            return AUDIO_PERMISSIONS.filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Gets all missing permissions
         */
        fun getAllMissingPermissions(context: android.content.Context): List<String> {
            val allPermissions = ESSENTIAL_PERMISSIONS + AUDIO_PERMISSIONS + ENHANCED_PERMISSIONS
            return allPermissions.filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get permission status summary for debugging (centralized version)
         */
        fun getPermissionStatusStatic(context: android.content.Context): Map<String, Boolean> {
            return mapOf(
                "Essential" to checkEssentialPermissions(context),
                "Audio" to checkAudioPermissions(context),
                "SystemAlert" to Settings.canDrawOverlays(context),
                "AllGranted" to checkAllPermissions(context)
            )
        }
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
