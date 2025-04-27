package com.jotape.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for interactions fetched from/sent to Supabase.
 * Matches the 'interactions' table structure.
 */
@Serializable // Requires kotlinx-serialization plugin and library
data class InteractionDto(
    val id: Long? = null, // Nullable for inserts where DB generates ID

    @SerialName("user_id") // Map to snake_case column name in Supabase
    val userId: String, // UUID stored as String

    @SerialName("user_input")
    val userInput: String? = null, // User's message text

    @SerialName("assistant_response")
    val assistantResponse: String? = null, // Assistant's message text

    val timestamp: String, // Supabase timestamp with time zone as String

    @SerialName("feedback_rating")
    val feedbackRating: Int? = null
) 