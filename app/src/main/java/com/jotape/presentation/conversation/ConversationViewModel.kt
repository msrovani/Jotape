package com.jotape.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.domain.model.Interaction
import com.jotape.domain.model.Resource
import com.jotape.domain.repository.InteractionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val interactionRepository: InteractionRepository
) : ViewModel() {

    private val TAG = "ConversationViewModel"

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized, starting to collect interactions stream.")
        observeInteractions()
    }

    private fun observeInteractions() {
        viewModelScope.launch {
            interactionRepository.getInteractionsStream()
                .collectLatest { resource ->
                    Log.d(TAG, "Received interactions resource: $resource")
                    when (resource) {
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }
                        is Resource.Success -> {
                            val interactions = resource.data
                            _uiState.update {
                                it.copy(
                                    interactions = interactions,
                                    isLoading = false,
                                    error = null
                                )
                            }
                            Log.d(TAG, "Updated UI state with ${interactions.size} interactions.")
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = resource.message
                                )
                            }
                            Log.e(TAG, "Error observing interactions: ${resource.message}")
                        }
                    }
                }
        }
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return

        _uiState.update { it.copy(isSending = true) }

        viewModelScope.launch {
            val result = interactionRepository.sendMessage(message = messageText)
            _uiState.update { it.copy(isSending = false) }

            if (result is Resource.Error) {
                Log.e(TAG, "Error sending message: ${result.message}")
                _uiState.update { it.copy(error = "Erro ao enviar: ${result.message}") }
            } else {
                Log.d(TAG, "Message sent successfully (processed by Edge Function).")
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    data class ConversationUiState(
        val interactions: List<Interaction> = emptyList(),
        val isLoading: Boolean = true,
        val isSending: Boolean = false,
        val error: String? = null
    )
} 