package com.jotape.domain.model

import java.time.Instant

/**
 * Domain model representing a single interaction (user or assistant message).
 */
data class Interaction(
    val id: Long,
    val isFromUser: Boolean,
    val text: String,
    val timestamp: Instant,
    val isSynced: Boolean = false
) 