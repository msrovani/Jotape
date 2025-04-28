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
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.net.UnknownHostException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseAuth: Auth
) : AuthRepository {

    private val TAG = "AuthRepositoryImpl"

    override val sessionStatus: Flow<SessionStatus>
        get() = supabaseAuth.sessionStatus

    override suspend fun signInWithEmail(email: String, password: String): DomainResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            supabaseAuth.signInWith(Email) { this.email = email; this.password = password }
            Log.i(TAG, "Sign in successful for email: $email")
            DomainResult.Success(Unit)
        } catch (e: UnauthorizedRestException) {
            Log.w(TAG, "Sign in failed (Unauthorized): ${e.message}")
            DomainResult.Error("Email ou senha inválidos.")
        } catch (e: BadRequestRestException) {
             Log.w(TAG, "Sign in failed (Bad Request): ${e.message}")
             if (e.message?.contains("Email not confirmed") == true) {
                 DomainResult.Error("Seu email ainda não foi confirmado. Verifique sua caixa de entrada.")
             } else {
                 DomainResult.Error("Dados inválidos fornecidos.")
             }
        } catch (e: HttpRequestTimeoutException) {
            Log.e(TAG, "Sign in failed (Timeout): ${e.message}", e)
            DomainResult.Error("Tempo limite de conexão excedido. Verifique sua internet.")
        } catch (e: HttpRequestException) {
            Log.e(TAG, "Sign in failed (Network): ${e.message}", e)
             if (e.cause is UnknownHostException) {
                 DomainResult.Error("Não foi possível conectar ao servidor. Verifique sua internet.")
             } else {
                 DomainResult.Error("Erro de rede ao tentar fazer login.")
             }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed (Unknown): ${e.message}", e)
            DomainResult.Error("Ocorreu um erro inesperado durante o login.")
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): DomainResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            supabaseAuth.signUpWith(Email) { this.email = email; this.password = password }
            Log.i(TAG, "Sign up successful for email: $email. Confirmation email sent.")
            DomainResult.Success(Unit) // Indica sucesso no INÍCIO do processo (email enviado)
        } catch (e: BadRequestRestException) {
            Log.w(TAG, "Sign up failed (Bad Request): ${e.message}")
            if (e.message?.contains("User already registered") == true) {
                DomainResult.Error("Este email já está cadastrado.")
            } else if (e.message?.contains("Password should be at least 6 characters") == true){
                 DomainResult.Error("A senha deve ter pelo menos 6 caracteres.")
            } else {
                DomainResult.Error("Dados inválidos para cadastro.")
            }
        } catch (e: HttpRequestTimeoutException) {
            Log.e(TAG, "Sign up failed (Timeout): ${e.message}", e)
            DomainResult.Error("Tempo limite de conexão excedido. Verifique sua internet.")
        } catch (e: HttpRequestException) {
             Log.e(TAG, "Sign up failed (Network): ${e.message}", e)
             if (e.cause is UnknownHostException) {
                 DomainResult.Error("Não foi possível conectar ao servidor. Verifique sua internet.")
             } else {
                 DomainResult.Error("Erro de rede ao tentar cadastrar.")
             }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed (Unknown): ${e.message}", e)
            DomainResult.Error("Ocorreu um erro inesperado durante o cadastro.")
        }
    }

    override suspend fun signOut(): DomainResult<Unit> = withContext(Dispatchers.IO) {
         return@withContext try {
             supabaseAuth.signOut()
             Log.i(TAG, "Sign out successful.")
             DomainResult.Success(Unit)
         } catch (e: Exception) {
             Log.e(TAG, "Sign out failed: ${e.message}", e)
             DomainResult.Error("Erro ao tentar sair: ${e.message}") // Manter erro técnico aqui?
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