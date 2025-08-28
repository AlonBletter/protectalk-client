package com.protectalk.protectalk

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.protectalk.protectalk.navigation.AppNavHost
import com.protectalk.protectalk.push.PushManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FCM token early in app lifecycle
        initializeFcmToken()

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
}
