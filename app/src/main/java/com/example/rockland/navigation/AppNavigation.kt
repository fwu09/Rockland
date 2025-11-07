package com.example.rockland.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rockland.ui.components.NotificationDialog
import com.example.rockland.ui.screens.LoginScreen
import com.example.rockland.ui.screens.MainScreen
import com.example.rockland.ui.screens.RegisterScreen
import com.example.rockland.ui.screens.SettingsScreen
import com.example.rockland.ui.screens.WelcomeScreen
import com.example.rockland.viewmodel.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    // Dialog state
    var showSuccessDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }

    // Navigate after dialog dismiss
    var navigateToLogin by remember { mutableStateOf(false) }

    // Show error toast if needed
    error?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        userViewModel.clearError()
    }

    // Show success dialog only for registration
    successMessage?.let {
        if (it.contains("Registration successful")) {
            dialogTitle = "Success"
            dialogMessage = it
            showSuccessDialog = true
            navigateToLogin = true
        }
        userViewModel.clearSuccessMessage()
    }

    // Success dialog
    if (showSuccessDialog) {
        NotificationDialog(
            title = dialogTitle,
            message = dialogMessage,
            onDismiss = {
                showSuccessDialog = false
                // Navigate to login if registration was successful
                if (navigateToLogin) {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.REGISTER) { inclusive = true }
                    }
                    navigateToLogin = false
                }
            }
        )
    }

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
            LoginScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLoginClick = { email, password ->
                    userViewModel.loginUser(email, password)
                    // Navigate to main screen after a short delay to allow login to complete
                    CoroutineScope(Dispatchers.Main).launch {
                        if (userViewModel.isLoggedIn.value) {
                            navController.navigate(AppRoutes.MAIN) {
                                popUpTo(AppRoutes.WELCOME) { inclusive = true }
                            }
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate(AppRoutes.REGISTER) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Register screen
        composable(AppRoutes.REGISTER) {
            RegisterScreen(
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
                    navController.popBackStack()
                },
                onSaveClick = { firstName, lastName, _ ->
                    userViewModel.updateUserProfile(firstName, lastName)
                    navController.popBackStack()
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


