package com.protectalk.protectalk

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.protectalk.protectalk.navigation.AppNavHost
import com.protectalk.protectalk.permissions.PermissionManager
import com.protectalk.protectalk.push.PushManager
import com.protectalk.protectalk.alert.AlertManager
import com.protectalk.protectalk.ui.theme.ProtectTalkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission manager for comprehensive permission handling
        permissionManager = PermissionManager(this)

        // Initialize FCM token early in app lifecycle
        initializeFcmToken()

        // Request all necessary permissions for scam detection functionality
        requestAllPermissions()

        setContent {
            ProtectTalkTheme { // Custom light green theme
                Surface {
                    val navController = rememberNavController()
                    AppNavHost(navController)
                }
            }
        }
    }

    private fun initializeFcmToken() {
        // Fetch FCM token asynchronously on app start
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PushManager.fetchFcmToken()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to initialize FCM token", e)
            }
        }
    }

    /**
     * Request all permissions required for scam detection functionality
     * This includes essential, audio processing, and enhanced permissions
     */
    private fun requestAllPermissions() {
        Log.d("MainActivity", "Starting comprehensive permission request for scam detection")

        // Log current permission status for debugging
        val currentStatus = permissionManager.getPermissionStatus()
        Log.d("MainActivity", "Current permission status: $currentStatus")

        permissionManager.requestAllPermissions { allCriticalGranted ->
            if (allCriticalGranted) {
                Log.i("MainActivity", "✅ All critical permissions granted - scam detection fully enabled")

                // Start alert monitoring with full functionality
                AlertManager.startAlertMonitoring(this)

                // Log final permission status
                val finalStatus = permissionManager.getPermissionStatus()
                Log.d("MainActivity", "Final permission status: $finalStatus")

            } else {
                Log.w("MainActivity", "⚠️ Some critical permissions denied - limited functionality")

                // Still try to start monitoring with available permissions
                if (permissionManager.checkEssentialPermissions()) {
                    Log.i("MainActivity", "Starting limited alert monitoring with essential permissions only")
                    AlertManager.startAlertMonitoring(this)
                } else {
                    Log.e("MainActivity", "❌ Essential permissions missing - cannot start alert monitoring")
                }

                // Show which permissions are missing
                val finalStatus = permissionManager.getPermissionStatus()
                Log.w("MainActivity", "Missing permissions status: $finalStatus")
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if permissions have changed (user might have granted them in settings)
        if (permissionManager.checkEssentialPermissions()) {
            Log.d("MainActivity", "Essential permissions available - ensuring monitoring is active")
            AlertManager.startAlertMonitoring(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Note: We don't stop alert monitoring here because we want it to continue
        // even when the app is closed. The monitoring will stop when user logs out.
    }
}
