package com.protectalk.client.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.protectalk.client.ui.home.HomeScreen
import com.protectalk.client.ui.protection.ProtectionScreen
import com.protectalk.client.ui.registration.RegistrationScreen
import com.protectalk.client.ui.registration.VerificationScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Registration
    ) {
        // Registration -> user enters phone, we navigate to verification with the phone string
        composable(Routes.Registration) {
            RegistrationScreen(
                onContinue = { phone ->
                    // TODO(auth): kick off Firebase verifyPhoneNumber(phone) before navigating,
                    //             stash verificationId/resendToken in a ViewModel.
                    navController.navigate("verification/$phone")
                }
            )
        }

        // Verification -> user enters 6-digit code, on success go to Home and clear auth from back stack
        composable(
            route = Routes.Verification,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            VerificationScreen(
                phoneNumber = phone,
                onVerified = {
                    navController.navigate(Routes.Home) {
                        // Remove Registration & Verification from history so back can't return to them
                        popUpTo(Routes.Registration) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { /* no-op: we don't allow going back in auth flow */ }
            )
        }

        // Home (placeholder for now)
        composable(Routes.Home) {
            HomeScreen()
        }

        // Protection (placeholder for now)
        composable(Routes.Protection) {
            ProtectionScreen()
        }
    }
}
