package com.jotape.presentation.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.domain.model.DomainResult
import com.jotape.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Define the UI state for authentication screens
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false, // Flag to trigger navigation on success
    val isSignUpSuccess: Boolean = false, // Flag to indicate sign up requires confirmation
    val isLoggedIn: Boolean? = null // Null initially, true/false after check
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Observe session status to determine initial logged-in state
    val isLoggedIn: StateFlow<Boolean?> = authRepository.sessionStatus
        .map { status ->
            // Map SessionStatus to a simple Boolean?
            when (status) {
                is SessionStatus.Authenticated -> true // User is logged in
                is SessionStatus.NotAuthenticated -> false // User is logged out
                // Handle Loading/Error states if needed, mapping to null for initial state
                else -> null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // Start as null before first emission
        )

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email.trim(), errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val email = _uiState.value.email
        val password = _uiState.value.password
        // Basic validation (can be improved)
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email e senha não podem estar vazios") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signInWithEmail(email, password)) {
                is DomainResult.Success -> {
                    // isLoginSuccess will be set by observing isLoggedIn state flow
                    // _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) } -> No longer needed here
                     _uiState.update { it.copy(isLoading = false) } // Just update loading state
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun signUp() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        // --- Start Enhanced Validation ---
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email e senha não podem estar vazios") }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "Formato de email inválido") }
            return
        }

        if (password.length < 6) { // Example: Minimum 6 characters
            _uiState.update { it.copy(errorMessage = "A senha deve ter pelo menos 6 caracteres") }
            return
        }
        // Add more password strength rules if needed (e.g., require numbers, symbols)
        // --- End Enhanced Validation ---

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signUpWithEmail(email, password)) {
                is DomainResult.Success -> {
                    // Indicate success flag. The UI will handle showing the confirmation message.
                    _uiState.update { it.copy(isLoading = false, isSignUpSuccess = true, errorMessage = null) } // Remove generic message from here
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Result indicates if flow started or failed immediately.
            // Actual login success/failure is observed via isLoggedIn StateFlow.
            when (val result = authRepository.signInWithGoogle()) {
                is DomainResult.Success -> {
                    // Don't need to do much here, wait for sessionStatus update
                     _uiState.update { it.copy(isLoading = false) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun resetLoginStatus() {
        // This might not be needed anymore as navigation relies on isLoggedIn StateFlow
        // _uiState.update { it.copy(isLoginSuccess = false) }
    }

    fun resetSignUpStatus() {
       _uiState.update { it.copy(isSignUpSuccess = false, errorMessage = null) } // Also clear message
    }

    // Function to handle user logout
    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Show loading indicator
            when (val result = authRepository.signOut()) {
                is DomainResult.Success -> {
                    // Successfully signed out. isLoggedIn StateFlow will automatically update,
                    // triggering navigation if observed correctly.
                    // Clear sensitive fields from UI state just in case.
                    _uiState.update { AuthUiState() } // Reset to initial state
                }
                is DomainResult.Error -> {
                    // Handle potential (though unlikely) sign-out errors
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    // No explicit signOut function here, it's usually triggered from another screen
    // but could be added if needed for profile screen etc.

} 