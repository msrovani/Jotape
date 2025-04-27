package com.jotape.data.remote.mapper

import com.jotape.data.remote.dto.InteractionDto
import com.jotape.domain.model.Interaction
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Maps InteractionDto (data layer - remote) to Interaction (domain layer).
 */
fun InteractionDto.toDomainModel(): Interaction? {
    // Determine text and isFromUser based on which field is non-null
    val text = this.userInput ?: this.assistantResponse
    val isFromUser = this.userInput != null

    if (text == null || this.id == null) {
        // Invalid state, should not happen if data is consistent
        return null
    }

    // Safely parse timestamp
    val parsedTimestamp = try {
        Instant.parse(this.timestamp)
    } catch (e: DateTimeParseException) {
        // Log error or return null if timestamp is critical and invalid
        // For now, fallback to current time, but logging is recommended
        Instant.now() 
    }

    return Interaction(
        id = this.id!!,
        isFromUser = isFromUser,
        text = text,
        timestamp = parsedTimestamp,
        isPersistedRemotely = true
    )
}

/**
 * Maps a list of InteractionDto to a list of Interaction, filtering out invalid entries.
 */
fun List<InteractionDto>.toDomainModelList(): List<Interaction> {
    return this.mapNotNull { it.toDomainModel() }
}

// Note: Mapping from Domain to DTO for inserts might be handled differently,
// often by creating a map or a specific insert DTO without the ID. 