package com.protectalk.protectalk.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.protectalk.protectalk.R
import com.protectalk.protectalk.alert.ProtectionStatusManager
import com.protectalk.protectalk.domain.RegisterDeviceUseCase
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
private fun HomeScreen_Preview() {
    HomeScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val registerDeviceUseCase = remember { RegisterDeviceUseCase() }

    // State for protection status
    var protectionStatus by remember { mutableStateOf<ProtectionStatusManager.ProtectionStatus?>(null) }
    var isCheckingStatus by remember { mutableStateOf(false) }

    // Register device when HomeScreen is first composed (for existing users who sign in)
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                registerDeviceUseCase(context)
                // Don't need to handle the result here - it's best-effort
            } catch (_: Exception) {
                // Silently handle errors - device registration is not critical for app functionality
            }
        }
    }

    // Check protection status when home screen appears
    LaunchedEffect(Unit) {
        scope.launch {
            isCheckingStatus = true
            try {
                val status = ProtectionStatusManager.checkProtectionStatus(context)
                protectionStatus = status
            } catch (_: Exception) {
                // Handle error by showing a fallback status
                protectionStatus = ProtectionStatusManager.ProtectionStatus(
                    allPermissionsGranted = false,
                    callRecordingStatus = ProtectionStatusManager.CallRecordingStatus.CANNOT_CHECK,
                    isFullyProtected = false,
                    statusMessage = "Protection disabled",
                    detailMessage = "Unable to check protection status"
                )
            } finally {
                isCheckingStatus = false
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 40.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ProtectTalk Logo
            Image(
                painter = painterResource(id = R.drawable.protectalk),
                contentDescription = "ProtectTalk Logo",
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Use 90% of available width
                    .padding(bottom = 32.dp)
            )

            // Protection Status Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Protection Status",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isCheckingStatus) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            protectionStatus?.let { status ->
                                // Binary status indicator - green for ON, red for OFF
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = if (status.isFullyProtected) Color(0xFF4CAF50) else Color(0xFFF44336),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isCheckingStatus) {
                        Text(
                            "Analyzing protection configuration...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        protectionStatus?.let { status ->
                            // Binary status message - either ON or OFF
                            Text(
                                status.statusMessage,
                                color = if (status.isFullyProtected) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )

                            Spacer(Modifier.height(16.dp))

                            // Simple binary status indicator with appropriate messages
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Single status dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (status.isFullyProtected) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )

                                Spacer(Modifier.width(8.dp))

                                Column {
                                    Text(
                                        status.detailMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Show call recording guidance only when we detect it's NOT working (not when we can't check)
                                    if (status.callRecordingStatus == ProtectionStatusManager.CallRecordingStatus.NOT_WORKING) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "If call recording is enabled, status will update after your next call",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

