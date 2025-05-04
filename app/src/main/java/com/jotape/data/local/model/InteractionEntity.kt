package com.jotape.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an interaction stored in the local Room database.
 */
@Entity(tableName = "interactions")
data class InteractionEntity(
    @PrimaryKey val id: String, // Unique ID, potentially UUID
    val text: String,           // Message content
    val sender: String,         // Who sent the message ("USER" or "ASSISTANT")
    val timestamp: Long,        // Time of the message (milliseconds since epoch)
    val status: String,         // Sync status ("PENDING", "SYNCED", "ERROR")
    val userId: String          // ID of the user associated with this interaction
) 