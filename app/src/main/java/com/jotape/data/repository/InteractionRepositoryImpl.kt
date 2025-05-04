package com.jotape.data.repository

import android.util.Log
import com.jotape.data.remote.api.JotapeApiService
import com.jotape.data.remote.dto.ProcessCommandRequest
import com.jotape.data.remote.dto.ProcessCommandResponse
import com.jotape.domain.model.Interaction
import com.jotape.domain.model.Resource
import com.jotape.domain.repository.InteractionRepository
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

// Definir a classe DTO diretamente aqui para simplicidade
@Serializable
data class InteractionData(
    val id: String, // Supabase geralmente usa UUID como String por padrão
    val user_id: String,
    val user_input: String? = null,
    val assistant_response: String? = null,
    val timestamp: String // Timestamp como String (ISO 8601 format)
)

/**
 * Implementation of the InteractionRepository that uses Supabase Edge Function for sending/receiving
 * individual messages and Supabase Realtime/Postgrest for fetching history.
 */
@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val realtime: Realtime,
    private val postgrest: Postgrest,
    private val jotapeApiService: JotapeApiService,
    private val externalScope: CoroutineScope
) : InteractionRepository {

    private val TAG = "InteractionRepoImpl"

    private val _interactionsFlow = MutableStateFlow<Resource<List<Interaction>>>(Resource.Loading)
    private val interactionsStateFlow: StateFlow<Resource<List<Interaction>>> = _interactionsFlow.asStateFlow()

    init {
        Log.d(TAG, "Initializing InteractionRepositoryImpl and setting up Realtime.")
        externalScope.launch(Dispatchers.IO) {
            auth.sessionStatus.collectLatest { status ->
                if (status is SessionStatus.Authenticated) {
                    Log.d(TAG, "User authenticated, setting up Realtime channel.")
                    setupRealtimeChannelAndFetchFullList(status.session.user?.id)
                } else {
                     Log.d(TAG, "User status changed to non-authenticated, clearing interactions flow.")
                    _interactionsFlow.value = Resource.Success(emptyList())
                     // Considerar cancelar o canal realtime aqui se necessário
                }
            }
        }
    }

    override suspend fun sendMessage(message: String): Resource<Unit> {
        val currentUserId = auth.currentUserOrNull()?.id ?: return Resource.Error("User not authenticated.")
        Log.d(TAG, "Processing user message via Edge Function: $message")
        val functionName = "process-user-command"

        return try {
            val request = ProcessCommandRequest(prompt = message, userId = currentUserId)
            Log.d(TAG, "Invoking Edge Function '$functionName' with payload: $request")

            // A chamada da API Retrofit retorna Response<ProcessCommandResponse>
            val retrofitResponse: Response<ProcessCommandResponse> = jotapeApiService.processUserCommand(request)

            if (retrofitResponse.isSuccessful) {
                val responseBody = retrofitResponse.body()
                if (responseBody != null) {
                    // Usar responseBody.response que é o campo do nosso DTO
                    Log.d(TAG, "Edge Function '$functionName' call successful. Response: ${responseBody.response}")
                    Resource.Success(Unit)
                } else {
                    // Corpo da resposta veio nulo mesmo com sucesso (inesperado para esta função)
                    Log.e(TAG, "Edge Function '$functionName' returned successful status but null body.")
                    Resource.Error("Resposta inesperada do servidor.")
                }
            } else {
                // A chamada HTTP falhou (status não-2xx)
                val errorBody = retrofitResponse.errorBody()?.string() ?: "No error body"
                Log.e(TAG, "Edge Function '$functionName' call failed with code: ${retrofitResponse.code()}. Body: $errorBody")
                Resource.Error("Erro ${retrofitResponse.code()}: ${errorBody.take(100)}") // Limitar tamanho da msg de erro
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception invoking Edge Function '$functionName': ${e.message}", e)
            Resource.Error("Erro ao comunicar com o assistente: ${e.localizedMessage}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun setupRealtimeChannelAndFetchFullList(userId: String?) {
        if (userId == null) {
            _interactionsFlow.value = Resource.Error("User ID is null, cannot setup Realtime.")
            return
        }

        Log.d(TAG, "Setting up Realtime channel for interactions for user: $userId")
        val channel = realtime.channel("interactions_user_$userId")

        try {
            // 1. Buscar a lista inicial antes de começar a ouvir mudanças
            _interactionsFlow.value = Resource.Loading
            val initialResult = fetchAllInteractionsForSupabase(userId)
            _interactionsFlow.value = initialResult
            Log.d(TAG, "Initial fetch completed. Result: $initialResult")

            // 2. Ouvir mudanças e re-buscar a lista completa quando uma ação ocorrer
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "interactions"
                filter = "user_id=eq.$userId"
            }.onCompletion { cause -> // Tratar onCompletion primeiro
                if (cause != null && cause !is kotlinx.coroutines.CancellationException) {
                    Log.e(TAG, "Realtime flow completed with error: $cause")
                    _interactionsFlow.value = Resource.Error("Realtime connection error: ${cause.message}")
                } else {
                    Log.d(TAG, "Realtime flow completed (Cause: $cause).")
                }
            }.catch { e -> // Tratar catch depois
                Log.e(TAG, "Error in Realtime flow: ${e.message}", e)
                _interactionsFlow.value = Resource.Error("Error listening for messages: ${e.message}")
            }.collect { action -> // Coletar a ação (INSERT, UPDATE, DELETE)
                 Log.d(TAG, "Realtime action received: $action. Re-fetching list...")
                 // Disparar a busca em background para não bloquear o coletor do Realtime
                 externalScope.launch(Dispatchers.IO) {
                     val updatedResult = fetchAllInteractionsForSupabase(userId)
                     _interactionsFlow.value = updatedResult // Atualiza o StateFlow com o novo resultado
                     Log.i(TAG, "Realtime emitting updated list result after action: ${updatedResult}")
                 }
            }

            // Inscrever após configurar o coletor
            channel.subscribe()
            Log.d(TAG, "Subscribed to Realtime channel: ${channel.topic}")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Realtime channel or initial fetch: ${e.message}", e)
            _interactionsFlow.value = Resource.Error("Failed to setup Realtime listener or fetch initial data: ${e.message}")
        }
    }

    private suspend fun fetchAllInteractionsForSupabase(userId: String): Resource<List<Interaction>> {
        Log.d(TAG, "Executing fetch full list for user: $userId")
        return try {
            val response = postgrest.from("interactions")
                .select { // Usar lambda para o select
                    // Corrigir sintaxe do filtro
                    filter { eq("user_id", userId) }
                    order("timestamp", Order.ASCENDING)
                }
                .decodeList<InteractionData>()
            Log.d(TAG, "Fetched ${response.size} interactions from Supabase.")
            Resource.Success(response.map { it.toDomainModel() })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching full interaction list: ${e.message}", e)
            Resource.Error("Failed to fetch messages: ${e.message}")
        }
    }

    private fun InteractionData.toDomainModel(): Interaction {
        val isFromUser = user_input != null
        val text = user_input ?: assistant_response ?: "[Mensagem vazia]"
        val parsedTimestamp = try {
            Instant.parse(this.timestamp)
        } catch (e: DateTimeParseException) {
            Log.w(TAG, "Failed to parse timestamp '${this.timestamp}', using Instant.now(). Error: ${e.message}")
            Instant.now()
        }
        return Interaction(
            id = this.id,
            isFromUser = isFromUser,
            text = text,
            timestamp = parsedTimestamp
        )
    }

    override fun getInteractionsStream(): StateFlow<Resource<List<Interaction>>> {
        return interactionsStateFlow
    }

} 