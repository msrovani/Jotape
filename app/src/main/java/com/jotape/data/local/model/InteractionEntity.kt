package com.jotape.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Represents a single interaction (a user message or an assistant response)
 * stored in the local Room database.
 */
@Entity(tableName = "interactions")
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isFromUser: Boolean,
    val text: String,
    val timestamp: Instant, // Store timestamp for ordering
    val isSynced: Boolean = false // Flag to track sync status with Supabase
) 