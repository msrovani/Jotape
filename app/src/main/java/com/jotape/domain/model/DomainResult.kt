package com.jotape.domain.model

/**
 * A generic class that holds a value with its loading status.
 * Used to represent the result of an operation that can succeed or fail.
 */
sealed interface DomainResult<out T> {
    data class Success<out T>(val data: T) : DomainResult<T>
    data class Error(val message: String, val exception: Throwable? = null) : DomainResult<Nothing> // Changed from Exception to String message
    // data object Loading : DomainResult<Nothing> // Optional: Add a loading state if needed
} 