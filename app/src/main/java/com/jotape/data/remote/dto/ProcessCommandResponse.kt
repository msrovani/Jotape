package com.jotape.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the response body received from the process-user-command Edge Function.
 * Uses kotlinx.serialization.
 */
@Serializable
data class ProcessCommandResponse(
    @SerialName("response") val response: String?
) 