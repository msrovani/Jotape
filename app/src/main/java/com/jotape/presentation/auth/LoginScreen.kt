package com.jotape.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToSignUp: () -> Unit, // Callback to navigate to SignUp
    onLoginSuccess: () -> Unit // Callback on successful login
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess() // Trigger navigation
            viewModel.resetLoginStatus() // Reset flag in ViewModel
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login - Jotape", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = uiState.errorMessage != null
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = uiState.errorMessage != null
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = viewModel::login,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Login")
            }
        }

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Google Sign In Button ---
        Button(
            onClick = viewModel::signInWithGoogle, // Call the ViewModel function
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            // Optional: Add specific colors or an icon for Google button
            // colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            // Optional: Add Google Icon
            // Icon(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = "Google logo")
            // Spacer(modifier = Modifier.width(8.dp))
            Text("Entrar com Google")
        }
        // --- End Google Sign In Button ---

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text("Não tem uma conta? Cadastre-se")
        }
    }
} 