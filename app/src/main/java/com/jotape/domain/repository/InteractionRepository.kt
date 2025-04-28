package com.jotape.domain.repository

import com.jotape.domain.model.Interaction
import com.jotape.domain.model.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing interactions.
 * Defines the contract for data operations related to conversations.
 */
interface InteractionRepository {

    /**
     * Gets the list of all interactions for the current user.
     * Fetches the current state from the remote source.
     */
    suspend fun getAllInteractions(): DomainResult<List<Interaction>>

    /**
     * Adds a new interaction to the repository.
     * In Phase 1, this saves locally. Later, it might trigger network calls.
     *
     * @param text The content of the message.
     * @param isFromUser True if the message is from the user, false if from the assistant.
     */
    suspend fun addInteraction(text: String, isFromUser: Boolean): DomainResult<Unit>

    // /**
    //  * Clears all interactions from the history for the current user.
    //  */
    // suspend fun clearHistory(): DomainResult<Unit>

    // We might add pagination methods later if needed:
    // suspend fun getPagedInteractions(limit: Int, offset: Int): List<Interaction>
    // suspend fun getInteractionCount(): Int
} 