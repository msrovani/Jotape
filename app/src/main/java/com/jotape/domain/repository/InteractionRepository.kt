package com.jotape.domain.repository

import com.jotape.domain.model.Interaction
import com.jotape.domain.model.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing chat interactions.
 */
interface InteractionRepository {

    /**
     * Retrieves all interactions for the current user as a Flow.
     * The Flow emits updates whenever the interaction list changes (e.g., new messages).
     */
    fun getAllInteractions(): Flow<DomainResult<List<Interaction>>>

    /**
     * Sends a user message to the backend for processing.
     * The implementation should handle communication with the AI and persistence.
     *
     * @param userMessage The text message sent by the user.
     * @return A DomainResult indicating success or failure of the send operation.
     */
    suspend fun sendMessage(userMessage: String): DomainResult<Unit>

    /**
     * Clears the entire interaction history for the current user.
     */
    suspend fun clearHistory(): DomainResult<Unit>

    // /**
    //  * Clears all interactions from the history for the current user.
    //  */
    // suspend fun clearHistory(): DomainResult<Unit>

    // We might add pagination methods later if needed:
    // suspend fun getPagedInteractions(limit: Int, offset: Int): List<Interaction>
    // suspend fun getInteractionCount(): Int
} 