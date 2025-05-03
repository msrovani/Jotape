package com.jotape.presentation.conversation

import com.jotape.domain.model.Interaction

/**
 * Represents the state of the Conversation screen.
 */
data class ConversationUiState(
    val interactions: List<Interaction> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
) 