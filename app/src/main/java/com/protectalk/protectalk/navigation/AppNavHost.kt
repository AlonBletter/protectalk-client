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
import com.protectalk.protectalk.ui.registration.UserProfileScreen

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
    val isSignedIn by authViewModel.ui.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBar = currentRoute?.startsWith(Graph.Main) == true ||
            currentRoute in listOf(Routes.Home, Routes.Protection, Routes.Settings)

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    listOf(
                        Routes.Home to Icons.Default.Home,
                        Routes.Protection to Icons.Default.Shield,
                        Routes.Settings to Icons.Default.Settings
                    ).forEach { (route, icon) ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(icon, null) },
                            label = { Text(route.replaceFirstChar(Char::uppercase)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash,
            modifier = Modifier.padding(padding),
            route = Graph.Root
        ) {
            // Splash decides graph
            composable(Routes.Splash) {
                SplashScreen()
                LaunchedEffect(isSignedIn.isSignedIn) {
                    navController.navigate(if (isSignedIn.isSignedIn) Graph.Main else Graph.Auth) {
                        popUpTo(Graph.Root) { inclusive = false } // keep root
                        launchSingleTop = true
                    }
                }
            }

            // ---------- AUTH GRAPH ----------
            navigation(
                route = Graph.Auth,
                startDestination = Routes.Registration
            ) {
                composable(Routes.Registration) {
                    val ui = authViewModel.ui.collectAsState().value
                    RegistrationScreen(
                        onRegister = { email, password ->
                            authViewModel.signUp(
                                email = email,
                                password = password,
                                onSuccess = {
                                    // Navigate to profile setup instead of main app
                                    navController.navigate(Routes.UserProfile) {
                                        launchSingleTop = true
                                    }
                                },
                                onError = { /* ui.error already surfaces */ }
                            )
                        },
                        onNavigateToLogin = { navController.navigate(Routes.Login) },
                        serverError = ui.error,
                        isSubmittingExternal = ui.isSubmitting
                    )
                }

                composable(Routes.UserProfile) {
                    UserProfileScreen(
                        onComplete = {
                            // After profile completion, go to main app
                            navController.navigate(Graph.Main) {
                                popUpTo(Graph.Auth) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
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
                                    navController.navigate(Graph.Main) {
                                        popUpTo(Graph.Auth) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onError = { /* ui.error already surfaces */ }
                            )
                        },
                        onBackToRegister = { navController.popBackStack() },
                        onForgotPassword = {
                            // TODO(Firebase): FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        },
                        serverError = ui.error,
                        isSubmittingExternal = ui.isSubmitting
                    )
                }
            }

            // ---------- MAIN GRAPH ----------
            navigation(
                route = Graph.Main,
                startDestination = Routes.Home
            ) {
                composable(Routes.Home) { HomeScreen() }
                composable(Routes.Protection) { ProtectionScreen() }
                composable(Routes.Settings) {
                    val email = FirebaseAuth.getInstance().currentUser?.email
                    SettingsScreen(
                        email = email,
                        onLogout = {
                            authViewModel.signOut()
                            // enter Auth graph and clear Main graph
                            navController.navigate(Graph.Auth) {
                                popUpTo(Graph.Main) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}

// helper to highlight the selected tab
private fun NavDestination?.isRouteInHierarchy(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true
