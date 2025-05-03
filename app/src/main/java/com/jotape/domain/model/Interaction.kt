package com.jotape.domain.model

import java.time.Instant

/**
 * Represents a single interaction (message) in the chat.
 */
data class Interaction(
    val id: String,           // Unique identifier (can be from local DB or remote)
    val isFromUser: Boolean, // True if the message is from the user, false if from the AI
    val text: String,         // The content of the message
    val timestamp: Instant    // When the interaction occurred
    // Removed: val isSynced: Boolean   // True if the interaction is synced with the remote backend
) 