package com.jotape.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jotape.data.local.model.InteractionEntity

/**
 * Data Access Object (DAO) for the interactions table.
 */
@Dao
interface InteractionDao {

    /**
     * Inserts or updates an interaction. If an interaction with the same ID exists, it replaces it.
     * Returns the row ID of the inserted/updated item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInteraction(interaction: InteractionEntity): Long

    /**
     * Retrieves all interactions from the table, ordered by timestamp descending (newest first).
     */
    @Query("SELECT * FROM interactions ORDER BY timestamp DESC")
    suspend fun getAllInteractions(): List<InteractionEntity>

    /**
     * Retrieves all interactions that are not yet synced (isSynced = false),
     * ordered by timestamp ascending (oldest first) for syncing.
     */
    @Query("SELECT * FROM interactions WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedInteractions(): List<InteractionEntity>

    /**
     * Updates an existing interaction entity.
     * Useful for marking an item as synced.
     */
    @Update
    suspend fun updateInteraction(interaction: InteractionEntity)

    /**
     * Marks a specific interaction as synced.
     * Uses a direct query for potentially better efficiency than loading and updating the entity.
     * @param id The ID of the interaction to mark as synced.
     */
    @Query("UPDATE interactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    /**
     * Deletes all interactions from the table.
     */
    @Query("DELETE FROM interactions")
    suspend fun clearAllInteractions()

} 