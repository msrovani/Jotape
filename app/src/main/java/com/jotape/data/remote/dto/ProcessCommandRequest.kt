package com.jotape.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the request body sent to the process-user-command Edge Function.
 * Uses kotlinx.serialization.
 */
@Serializable
data class ProcessCommandRequest(
    @SerialName("prompt") val prompt: String,
    @SerialName("userId") val userId: String
) 