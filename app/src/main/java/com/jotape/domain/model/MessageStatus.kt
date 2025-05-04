package com.jotape.domain.model

/**
 * Represents the synchronization status of a message.
 */
enum class MessageStatus {
    PENDING, // Message saved locally, not yet sent/synced
    SENT,    // Message sent to backend (intermediate state, might be removed)
    SYNCED,  // Message confirmed synced with the backend
    ERROR    // Error occurred during sending/syncing
} 