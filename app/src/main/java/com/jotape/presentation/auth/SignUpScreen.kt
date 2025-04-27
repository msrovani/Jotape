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
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.stringResource
import com.jotape.R

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit // Keep this for potential Snackbar fallback if needed
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmPassword by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val passwordsMatch = uiState.password == confirmPassword
    val isSignUpEnabled = uiState.email.isNotBlank() &&
                          uiState.password.isNotBlank() &&
                          confirmPassword.isNotBlank() &&
                          passwordsMatch &&
                          !uiState.isLoading

    // Trigger dialog display when sign up success flag is true
    LaunchedEffect(uiState.isSignUpSuccess) {
        if (uiState.isSignUpSuccess) {
            showConfirmationDialog = true
        }
    }

    // --- Confirmation Dialog ---
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing by clicking outside */ },
            title = { Text("Confirmação Necessária") }, // stringResource(R.string.signup_confirmation_title)
            text = { Text("Conta criada com sucesso!\n\nEnviamos um email de confirmação para ${uiState.email}. Por favor, verifique sua caixa de entrada (e spam) e clique no link para ativar sua conta. Depois, volte e faça login.") }, // stringResource(R.string.signup_confirmation_message, uiState.email)
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false // Dismiss dialog
                        viewModel.resetSignUpStatus() // Reset the flag in ViewModel
                        onNavigateToLogin() // Navigate back to Login screen
                    }
                ) {
                    Text("OK") // stringResource(R.string.ok)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cadastro - Jotape", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = uiState.errorMessage != null && !uiState.isSignUpSuccess // Don't show as error on success message
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
            isError = uiState.errorMessage != null && !uiState.isSignUpSuccess
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword, // Use local state
            onValueChange = { confirmPassword = it }, // Update local state
            label = { Text("Confirmar Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = !passwordsMatch && confirmPassword.isNotEmpty() // Show error if not matching and field isn't empty
        )
        if (!passwordsMatch && confirmPassword.isNotEmpty()) {
             Text(
                 text = "As senhas não conferem",
                 color = MaterialTheme.colorScheme.error,
                 style = MaterialTheme.typography.bodySmall,
                 modifier = Modifier.padding(start = 16.dp)
             )
         }
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = viewModel::signUp,
                modifier = Modifier.fillMaxWidth(),
                enabled = isSignUpEnabled
            ) {
                Text("Cadastrar")
            }
        }

        // Show only actual errors here
        if (uiState.errorMessage != null && !uiState.isSignUpSuccess) { // Check !isSignUpSuccess
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error, // Always show error color
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("Já tem uma conta? Faça login")
        }
    }
} 