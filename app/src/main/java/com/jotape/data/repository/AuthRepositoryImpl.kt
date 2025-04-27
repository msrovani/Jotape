package com.jotape.data.repository

import com.jotape.domain.model.DomainResult
import com.jotape.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import android.util.Log

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseAuth: Auth
) : AuthRepository {

    private val TAG = "AuthRepositoryImpl"

    override val sessionStatus: Flow<SessionStatus>
        get() = supabaseAuth.sessionStatus

    override suspend fun signInWithEmail(email: String, password: String): DomainResult<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseAuth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            DomainResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Exception during sign in (${e::class.simpleName}): ${e.message}", e)

            val errorMessage = if (e.message?.contains("Invalid login credentials", ignoreCase = true) == true ||
                                  e.message?.contains("400", ignoreCase = true) == true ||
                                  e.message?.contains("401", ignoreCase = true) == true) {
                "Email ou senha inválidos."
            } else {
                e.message ?: "Ocorreu um erro inesperado."
            }
            DomainResult.Error(errorMessage)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): DomainResult<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting signUpWithEmail for: $email")
        try {
            supabaseAuth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Log.i(TAG, "signUpWithEmail successful for: $email")
            DomainResult.Success(Unit)
        } catch (e: CancellationException) {
            Log.w(TAG, "signUpWithEmail cancelled for: $email", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "signUpWithEmail failed for: $email. Error: ${e.message}", e)
            DomainResult.Error(e.message ?: "Unknown sign-up error")
        }
    }

    override suspend fun signOut(): DomainResult<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseAuth.signOut()
            DomainResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DomainResult.Error(e.message ?: "Unknown sign-out error")
        }
    }

    override suspend fun signInWithGoogle(): DomainResult<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseAuth.signInWith(Google)
            DomainResult.Success(Unit)
        } catch (e: CancellationException) {
            DomainResult.Error("Google Sign-In cancelled")
        } catch (e: Exception) {
            DomainResult.Error(e.message ?: "Unknown Google Sign-In error")
        }
    }

    override fun getCurrentUserId(): String? {
        return try {
            supabaseAuth.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }
} 