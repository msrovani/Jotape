package com.jotape.data.remote.api

import com.jotape.data.remote.dto.ProcessCommandRequest
import com.jotape.data.remote.dto.ProcessCommandResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service interface for interacting with backend APIs,
 * specifically the Supabase Edge Functions.
 */
interface JotapeApiService {

    /**
     * Sends a user prompt to the backend Edge Function for processing
     * and receives the AI-generated response.
     *
     * Note: The Authorization header is typically added via an Interceptor.
     */
    @POST("process-user-command") // Corrected relative path
    suspend fun processUserCommand(
        // @Header("Authorization") bearerToken: String, // Added by Interceptor
        @Body request: ProcessCommandRequest
    ): Response<ProcessCommandResponse>

} 