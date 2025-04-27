package com.jotape.data.repository

import com.jotape.data.remote.dto.InteractionDto
import com.jotape.data.remote.mapper.toDomainModelList
import com.jotape.domain.model.DomainResult
import com.jotape.domain.model.Interaction
import com.jotape.domain.repository.AuthRepository
import com.jotape.domain.repository.InteractionRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import android.util.Log
import com.jotape.data.local.dao.InteractionDao
import com.jotape.data.local.mapper.toDomainModelList as localToDomainModelList
import com.jotape.data.local.mapper.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Implementation of the InteractionRepository that uses Supabase Postgrest for fetching, adding, and clearing interactions.
 */
@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val supabasePostgrest: Postgrest,
    private val authRepository: AuthRepository,
    private val interactionDao: InteractionDao
) : InteractionRepository {

    private val interactionsTable = "interactions"
    private val TAG = "InteractionRepository"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    override suspend fun getAllInteractions(): DomainResult<List<Interaction>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting all interactions from local Room DB")
        try {
            val entities = interactionDao.getAllInteractions()
            val interactions = entities.localToDomainModelList()
            Log.d(TAG, "Loaded ${interactions.size} interactions from Room.")
            DomainResult.Success(interactions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting interactions from Room: ${e.message}", e)
            DomainResult.Error("Falha ao carregar interações do banco de dados local.")
        }
    }

    override suspend fun addInteraction(text: String, isFromUser: Boolean): DomainResult<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Adding interaction locally: isFromUser=$isFromUser, text=$text")
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Log.w(TAG, "Cannot add interaction: User not logged in.")
            return@withContext DomainResult.Error("Usuário não está logado")
        }

        val interaction = Interaction(
            id = 0, // Let Room auto-generate the ID
            isFromUser = isFromUser,
            text = text,
            timestamp = Instant.now(),
            isSynced = false // Assumes Interaction model has this field
        )

        try {
            val localId = interactionDao.upsertInteraction(interaction.toEntity())
            Log.d(TAG, "Interaction saved locally with ID: $localId")

            // Trigger background sync for this specific new interaction
            repositoryScope.launch {
                Log.d(TAG, "Starting background sync for newly added local interaction ID: $localId")
                syncSingleInteraction(localId, interaction, currentUserId) // Delegate sync logic
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving interaction locally: ${e.message}", e)
            DomainResult.Error("Falha ao salvar interação localmente.")
        }
    }

    override suspend fun clearHistory(): DomainResult<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting to clear history in repository (Supabase + Room)...")
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Log.w(TAG, "Clear history failed: User not logged in.")
            return@withContext DomainResult.Error("User not logged in")
        }

        var supabaseError: String? = null
        try {
            Log.d(TAG, "Clearing history from Supabase for user ID: $currentUserId")
            supabasePostgrest.from(interactionsTable).delete {
                filter { eq("user_id", currentUserId) }
            }
            Log.i(TAG, "Successfully cleared history from Supabase for user ID: $currentUserId")
        } catch (e: CancellationException) {
            Log.w(TAG, "Clear history from Supabase cancelled", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history from Supabase for user ID: $currentUserId. Error: ${e.message}", e)
            supabaseError = "Falha ao limpar histórico remoto: ${e.message}"
        }

        try {
            Log.d(TAG, "Clearing history from local Room DB")
            interactionDao.clearAllInteractions()
            Log.i(TAG, "Successfully cleared history from Room DB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history from Room DB: ${e.message}", e)
            val finalError = supabaseError?.let { "$it\nFalha ao limpar histórico local." } ?: "Falha ao limpar histórico local."
            return@withContext DomainResult.Error(finalError)
        }

        if (supabaseError != null) {
            return@withContext DomainResult.Error(supabaseError)
        }

        Log.i(TAG, "Clear history finished successfully for both sources.")
        DomainResult.Success(Unit)
    }

    /**
     * Attempts to sync a single interaction identified by its local ID.
     * Should be called from a background coroutine.
     */
    private suspend fun syncSingleInteraction(localId: Long, interaction: Interaction, userId: String) {
        val interactionDataForSupabase = mapOf(
            "user_id" to userId,
            "user_input" to if (interaction.isFromUser) interaction.text else null,
            "assistant_response" to if (!interaction.isFromUser) interaction.text else null,
            "timestamp" to interaction.timestamp.toString()
        )
        try {
            supabasePostgrest.from(interactionsTable).insert(interactionDataForSupabase)
            interactionDao.markAsSynced(localId)
            Log.i(TAG, "Successfully synced interaction (local ID: $localId) with Supabase.")
        } catch (e: CancellationException) {
            Log.w(TAG, "Sync cancelled for local interaction ID: $localId", e)
            throw e // Rethrow cancellation
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for local interaction ID: $localId. Error: ${e.message}", e)
            // Keep isSynced = false. Will be retried by syncUnsynced.
        }
    }

    /**
     * Fetches all unsynced interactions from the local DB and attempts to sync them with Supabase.
     * This should be triggered when connectivity is available.
     */
    suspend fun syncUnsyncedInteractions() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting batch sync for unsynced interactions...")
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Log.w(TAG, "Cannot sync: User not logged in.")
            return@withContext
        }

        val unsyncedEntities = try {
            interactionDao.getUnsyncedInteractions()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching unsynced interactions from Room: ${e.message}", e)
            return@withContext // Stop sync if we can't read from DB
        }

        if (unsyncedEntities.isEmpty()) {
            Log.d(TAG, "No unsynced interactions found.")
            return@withContext
        }

        Log.d(TAG, "Found ${unsyncedEntities.size} unsynced interactions. Attempting sync...")
        var successfulSyncs = 0
        var failedSyncs = 0

        for (entity in unsyncedEntities) {
            // Reconstruct domain model or map data needed for Supabase
            val interactionDataForSupabase = mapOf(
                "user_id" to currentUserId,
                "user_input" to if (entity.isFromUser) entity.text else null,
                "assistant_response" to if (!entity.isFromUser) entity.text else null,
                "timestamp" to entity.timestamp.toString()
            )
            try {
                supabasePostgrest.from(interactionsTable).insert(interactionDataForSupabase)
                interactionDao.markAsSynced(entity.id)
                Log.i(TAG, "Successfully synced batch interaction (local ID: ${entity.id})")
                successfulSyncs++
            } catch (e: CancellationException) {
                Log.w(TAG, "Batch sync cancelled during interaction (local ID: ${entity.id})", e)
                throw e // Rethrow cancellation
            } catch (e: Exception) {
                Log.e(TAG, "Batch sync failed for interaction (local ID: ${entity.id}). Error: ${e.message}", e)
                failedSyncs++
                // Continue to next item, don't stop the whole batch
            }
        }
        Log.d(TAG, "Batch sync finished. Success: $successfulSyncs, Failed: $failedSyncs")
    }
} 