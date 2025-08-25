package com.protectalk.protectalk.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.protectalk.protectalk.ui.home.HomeScreen
import com.protectalk.protectalk.ui.protection.ProtectionScreen
import com.protectalk.protectalk.ui.registration.RegistrationScreen
import com.protectalk.protectalk.ui.registration.VerificationScreen
import com.protectalk.protectalk.ui.settings.SettingsScreen
import com.protectalk.protectalk.ui.splash.SplashScreen // ← add this import
import androidx.lifecycle.viewmodel.compose.viewModel
import com.protectalk.protectalk.ui.registration.AuthViewModel

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomItems = listOf(
    NavItem(Routes.Home, "Home", Icons.Default.Home),
    NavItem(Routes.Protection, "Protection", Icons.Default.Add),
    NavItem(Routes.Settings, "Settings", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest: NavDestination? = navBackStackEntry?.destination

    val authViewModel: AuthViewModel = viewModel()
    val isSignedIn = authViewModel.ui.collectAsState().value.isSignedIn

    Scaffold(
        bottomBar = {
            // Show bottom bar only when not in splash/auth flow
            val route = currentDest?.route
            val showBar =
                route?.startsWith("verification") == false &&
                        route != Routes.Registration &&
                        route != Routes.Splash

            if (showBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentDest.isRouteInHierarchy(item.route)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        // Pop up to the start of the graph to avoid building a large stack
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash, // ← start at Splash now
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            // Splash decides where to go based on isSignedIn
            composable(Routes.Splash) {
                SplashScreen()
                androidx.compose.runtime.LaunchedEffect(isSignedIn) {
                    navController.navigate(if (isSignedIn) Routes.Home else Routes.Registration) {
                        popUpTo(Routes.Splash) { inclusive = true } // remove Splash from back stack
                        launchSingleTop = true
                    }
                }
            }

            composable(Routes.Registration) {
                RegistrationScreen(
                    onContinue = { phone ->
                        // TODO(auth): start Firebase verifyPhoneNumber(phone) before nav; stash verificationId/resendToken
                        navController.navigate("verification/$phone")
                    }
                )
            }

            composable(
                route = Routes.Verification,
                arguments = listOf(navArgument("phone") { type = NavType.StringType })
            ) { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                VerificationScreen(
                    phoneNumber = phone,
                    onVerified = {
                        // NOTE: Once Firebase is wired, rely on FirebaseAuth.getInstance().currentUser instead
                        authViewModel.markSignedIn() // UI-only session gate for now
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Registration) { inclusive = true } // clear auth screens
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.Home) { HomeScreen() }
            composable(Routes.Protection) { ProtectionScreen() }
            composable(Routes.Settings) { SettingsScreen() }
        }
    }
}

// tiny helper to highlight the selected tab when nested graphs are used (safe here too)
private fun NavDestination?.isRouteInHierarchy(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true
