package com.protectalk.protectalk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.protectalk.protectalk.navigation.AppNavHost
import com.protectalk.protectalk.push.PushManager
import com.protectalk.protectalk.alert.AlertManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    private val requestAlertPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedPermissions = permissions.filterValues { it }.keys
        val deniedPermissions = permissions.filterValues { !it }.keys

        Log.d("MainActivity", "Alert permissions granted: $grantedPermissions")
        Log.d("MainActivity", "Alert permissions denied: $deniedPermissions")

        // Check if we have the essential permissions needed for the current Android version
        val hasEssentialPermissions = AlertManager.hasRequiredPermissions(this)
        val missingCritical = AlertManager.getMissingPermissions(this)

        if (hasEssentialPermissions) {
            Log.i("MainActivity", "All required alert permissions granted for API ${android.os.Build.VERSION.SDK_INT}, starting monitoring")
            AlertManager.startAlertMonitoring(this)
        } else {
            Log.w("MainActivity", "Missing critical permissions: $missingCritical - alert monitoring cannot start")
            // You might want to show a dialog explaining why these permissions are needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FCM token early in app lifecycle
        initializeFcmToken()

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Request alert monitoring permissions
        requestAlertPermissions()

        setContent {
            MaterialTheme { // default Material3 theme is fine for now
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    Log.d("MainActivity", "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun requestAlertPermissions() {
        val missingPermissions = AlertManager.getMissingPermissions(this)

        if (missingPermissions.isEmpty()) {
            Log.d("MainActivity", "All alert permissions already granted")
            // Start monitoring if permissions are already granted
            AlertManager.startAlertMonitoring(this)
        } else {
            Log.d("MainActivity", "Requesting alert permissions: $missingPermissions")
            requestAlertPermissionsLauncher.launch(AlertManager.getRequiredPermissions())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Note: We don't stop alert monitoring here because we want it to continue
        // even when the app is closed. The monitoring will stop when user logs out.
    }
}
