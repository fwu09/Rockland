package com.example.rockland.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rockland.ui.screens.LoginScreen
import com.example.rockland.ui.screens.MainScreen
import com.example.rockland.ui.screens.RegisterScreen
import com.example.rockland.ui.screens.SettingsScreen
import com.example.rockland.ui.screens.WelcomeScreen
import com.example.rockland.viewmodel.UserViewModel

// Application routes
object AppRoutes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

// Application navigation component
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel(factory = UserViewModel.Factory())
    val isLoggedIn by userViewModel.isLoggedIn.collectAsState()
    val error by userViewModel.error.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val successMessage by userViewModel.successMessage.collectAsState()


    NavHost(
        navController = navController,
        startDestination = AppRoutes.WELCOME
    ) {
        // Welcome screen
        composable(AppRoutes.WELCOME) {
            WelcomeScreen(
                onSignInClick = {
                    navController.navigate(AppRoutes.LOGIN)
                },
                onSignUpClick = {
                    navController.navigate(AppRoutes.REGISTER)
                },
                onSkipClick = {
                    navController.navigate(AppRoutes.MAIN) {
                        popUpTo(AppRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        // Login screen
        composable(AppRoutes.LOGIN) {
            LaunchedEffect(Unit) {
                userViewModel.clearError()
            }

            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    navController.navigate(AppRoutes.MAIN) {
                        popUpTo(AppRoutes.WELCOME) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                isLoading = isLoading,
                errorMessage = error,
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = { email, password ->
                    userViewModel.loginUser(email, password)
                },
                onRegisterClick = {
                    navController.navigate(AppRoutes.REGISTER) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onClearError = {
                    userViewModel.clearError()
                }
            )
        }

        // Register screen
        composable(AppRoutes.REGISTER) {
            LaunchedEffect(Unit) {
                userViewModel.clearError()
            }

            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    navController.navigate(AppRoutes.MAIN) {
                        popUpTo(AppRoutes.WELCOME) { inclusive = true }
                    }
                }
            }

            RegisterScreen(
                isLoading = isLoading,
                errorMessage = error,
                onBackClick = {
                    navController.popBackStack()
                },
                onRegisterClick = { email, password, firstName, lastName ->
                    userViewModel.registerUser(email, password, firstName, lastName)
                },
                onLoginClick = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.REGISTER) { inclusive = true }
                    }
                },
                onClearError = {
                    userViewModel.clearError()
                }
            )
        }

        // Main screen
        composable(AppRoutes.MAIN) {
            MainScreen(
                onSettingsClick = {
                    navController.navigate(AppRoutes.SETTINGS)
                },
                onLogoutClick = {
                    userViewModel.logout()
                    navController.navigate(AppRoutes.WELCOME) {
                        popUpTo(AppRoutes.MAIN) { inclusive = true }
                    }
                },
                userViewModel = userViewModel
            )
        }

        // Settings screen
        composable(AppRoutes.SETTINGS) {
            val userData by userViewModel.userData.collectAsState()

            SettingsScreen(
                userData = userData,
                onBackClick = {
                    // Ensure we navigate back to MAIN screen explicitly
                    if (!navController.popBackStack()) {
                        // If popBackStack fails (no previous screen), navigate to MAIN
                        navController.navigate(AppRoutes.MAIN) {
                            popUpTo(AppRoutes.MAIN) { inclusive = true }
                        }
                    }
                },
                onSaveClick = { firstName, lastName, _ ->
                    userViewModel.updateUserProfile(firstName, lastName)
                    // Navigate back to MAIN after saving
                    if (!navController.popBackStack()) {
                        navController.navigate(AppRoutes.MAIN) {
                            popUpTo(AppRoutes.MAIN) { inclusive = true }
                        }
                    }
                },
                onLogoutClick = {
                    userViewModel.logout()
                    navController.navigate(AppRoutes.WELCOME) {
                        popUpTo(AppRoutes.MAIN) { inclusive = true }
                    }
                }
            )
        }
    }
}


