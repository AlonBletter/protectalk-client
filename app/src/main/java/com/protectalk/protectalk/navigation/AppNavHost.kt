package com.protectalk.protectalk.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.protectalk.protectalk.ui.home.HomeScreen
import com.protectalk.protectalk.ui.protection.ProtectionScreen
import com.protectalk.protectalk.ui.registration.RegistrationScreen
import com.protectalk.protectalk.ui.settings.SettingsScreen
import com.protectalk.protectalk.ui.splash.SplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.protectalk.protectalk.ui.login.LoginScreen
import com.protectalk.protectalk.ui.registration.AuthViewModel

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomItems = listOf(
    NavItem(Routes.Home, "Home", Icons.Default.Home),
    NavItem(Routes.Protection, "Protection", Icons.Default.Shield),
    NavItem(Routes.Settings, "Settings", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val isSignedIn = authViewModel.ui.collectAsState().value.isSignedIn

    Scaffold(
        bottomBar = {
            val route = currentDest?.route
            val showBar = route != Routes.Splash && route != Routes.Registration
            if (showBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentDest.isRouteInHierarchy(item.route)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
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
            startDestination = Routes.Splash,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Splash) {
                SplashScreen()
                LaunchedEffect(isSignedIn) {
                    navController.navigate(if (isSignedIn) Routes.Home else Routes.Registration) {
                        popUpTo(Routes.Splash) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            composable(Routes.Registration) {
                val ui = authViewModel.ui.collectAsState().value
                RegistrationScreen(
                    onRegister = { email, password ->
                        authViewModel.signUp(
                            email = email,
                            password = password,
                            onSuccess = {
                                navController.navigate(Routes.Home) {
                                    popUpTo(Routes.Registration) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onError = { /* ui.error already reflected */ }
                        )
                    },
                    onNavigateToLogin = { navController.navigate(Routes.Login) },
                    serverError = ui.error,
                    isSubmittingExternal = ui.isSubmitting
                )
            }

            composable(Routes.Login) {
                val ui = authViewModel.ui.collectAsState().value
                LoginScreen(
                    onLogin = { email, password ->
                        authViewModel.signIn(
                            email = email,
                            password = password,
                            onSuccess = {
                                navController.navigate(Routes.Home) {
                                    popUpTo(Routes.Registration) { inclusive = true } // clear auth stack
                                    launchSingleTop = true
                                }
                            },
                            onError = { /* ui.error already reflected */ }
                        )
                    },
                    onBackToRegister = { navController.popBackStack() },
                    onForgotPassword = {
                        // TODO(Firebase): FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        // Optionally show a snackbar/toast on success/failure
                    },
                    serverError = ui.error,
                    isSubmittingExternal = ui.isSubmitting
                )
            }

            composable(Routes.Home) { HomeScreen() }
            composable(Routes.Protection) { ProtectionScreen() }
            composable(Routes.Settings) {
                val email = FirebaseAuth.getInstance().currentUser?.email
                SettingsScreen(
                    email = email,
                    onLogout = {
                        // 1) Sign out in VM (source of truth for Splash)
                        authViewModel.signOut() // sets isSignedIn = false

                        // 2) Navigate to Registration and clear back stack
                        navController.navigate(Routes.Registration) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

// helper to highlight the selected tab
private fun NavDestination?.isRouteInHierarchy(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true
