package com.jotape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jotape.presentation.auth.AuthViewModel
import com.jotape.presentation.auth.LoginScreen
import com.jotape.presentation.auth.SignUpScreen
import com.jotape.presentation.conversation.ConversationScreen
import com.jotape.presentation.navigation.Routes
import com.jotape.ui.theme.JotapeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JotapeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                    val startDestination = remember(isLoggedIn) {
                        when (isLoggedIn) {
                            true -> Routes.CONVERSATION
                            false -> Routes.LOGIN
                            null -> null
                        }
                    }

                    if (startDestination != null) {
                        NavHost(navController = navController, startDestination = startDestination) {
                            composable(Routes.LOGIN) {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) },
                                    onLoginSuccess = {
                                        navController.navigate(Routes.CONVERSATION) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable(Routes.SIGN_UP) {
                                SignUpScreen(
                                    viewModel = authViewModel,
                                    onNavigateToLogin = { navController.popBackStack() },
                                    onSignUpSuccess = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable(Routes.CONVERSATION) {
                                ConversationScreen()
                            }
                        }
                    } else {
                        // Optional: Show a loading indicator while checking auth state
                        // CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
} 