package com.jotape.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jotape.data.local.dao.InteractionDao
import com.jotape.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncInteractionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val interactionDao: InteractionDao,
    private val supabasePostgrest: Postgrest,
    private val authRepository: AuthRepository // Para obter o user ID
) : CoroutineWorker(context, workerParams) {

    private val interactionsTable = "interactions"
    private val TAG = "SyncInteractionWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting SyncInteractionWorker...")

        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            Log.w(TAG, "Sync failed: User not logged in.")
            return@withContext Result.failure() // Falha se não há usuário
        }

        val unsyncedEntities = try {
            interactionDao.getUnsyncedInteractions()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching unsynced interactions from Room: ${e.message}", e)
            return@withContext Result.retry() // Tentar novamente depois
        }

        if (unsyncedEntities.isEmpty()) {
            Log.i(TAG, "No unsynced interactions found. Worker finished successfully.")
            return@withContext Result.success()
        }

        Log.d(TAG, "Found ${unsyncedEntities.size} unsynced interactions. Attempting sync...")
        var allSucceeded = true

        for (entity in unsyncedEntities) {
            // TODO: Consider mapping to InteractionDto instead of Map for type safety
            val interactionDataForSupabase = mapOf(
                "user_id" to currentUserId,
                "user_input" to (if (entity.isFromUser) entity.text else null),
                "assistant_response" to (if (!entity.isFromUser) entity.text else null),
                "timestamp" to entity.timestamp.toString()
            )
            try {
                supabasePostgrest.from(interactionsTable).insert(interactionDataForSupabase)
                interactionDao.markAsSynced(entity.id)
                Log.i(TAG, "Successfully synced interaction (local ID: ${entity.id})")
            } catch (e: CancellationException) {
                Log.w(TAG, "Sync cancelled during interaction (local ID: ${entity.id})", e)
                allSucceeded = false
                throw e // Permite que o WorkManager cancele corretamente
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed for interaction (local ID: ${entity.id}). Error: ${e.message}", e)
                allSucceeded = false
                // Não marcar como sincronizado, tentar novamente na próxima execução
            }
        }

        return@withContext if (allSucceeded) {
            Log.i(TAG, "SyncInteractionWorker finished successfully.")
            Result.success()
        } else {
            Log.w(TAG, "SyncInteractionWorker finished with some failures. Retrying later.")
            Result.retry() // Se alguma falha ocorreu, tentar novamente mais tarde
        }
    }
} 