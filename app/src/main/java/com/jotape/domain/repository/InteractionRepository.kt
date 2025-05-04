package com.jotape.domain.repository

import com.jotape.domain.model.Interaction
import com.jotape.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining operations for managing interactions (chat messages).
 */
interface InteractionRepository {

    /**
     * Sends a user message to the backend for processing.
     *
     * @param message The text message input by the user.
     * @return A [Resource] indicating the success or failure of the operation.
     */
    suspend fun sendMessage(message: String): Resource<Unit>

    /**
     * Provides a stream of interactions for the current session.
     * The stream emits updates reflecting the current state of the conversation.
     *
     * @return A [StateFlow] emitting [Resource] updates containing the list of [Interaction]s.
     */
    fun getInteractionsStream(): StateFlow<Resource<List<Interaction>>>

}