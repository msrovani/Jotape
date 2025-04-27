package com.jotape.domain.repository

import com.jotape.domain.model.DomainResult // Assuming a generic Result wrapper
import io.github.jan.supabase.gotrue.SessionStatus // Import SessionStatus
// import io.github.jan.supabase.gotrue.user.UserSession // Remove old import if not directly needed
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for handling authentication operations.
 */
interface AuthRepository {

    /**
     * Gets the current user session status as a flow.
     * Emits SessionStatus indicating the current auth state (Authenticated, NotAuthenticated, etc.).
     */
    val sessionStatus: Flow<SessionStatus> // Updated type to Flow<SessionStatus>

    /**
     * Attempts to sign in a user with email and password.
     *
     * @return DomainResult indicating success or failure.
     */
    suspend fun signInWithEmail(email: String, password: String): DomainResult<Unit>

    /**
     * Attempts to sign up a new user with email and password.
     *
     * @return DomainResult indicating success or failure (e.g., user exists, weak password).
     */
    suspend fun signUpWithEmail(email: String, password: String): DomainResult<Unit>

    /**
     * Signs out the current user.
     *
     * @return DomainResult indicating success or failure.
     */
    suspend fun signOut(): DomainResult<Unit>

    /**
     * Initiates the Google Sign-In flow.
     * The actual token exchange and session creation are handled by the Supabase plugin.
     *
     * @return DomainResult indicating success (flow initiated) or immediate failure.
     */
    suspend fun signInWithGoogle(): DomainResult<Unit> // Simplified signature

    /**
     * Gets the current logged-in user's ID, if available.
     */
    fun getCurrentUserId(): String?

} 