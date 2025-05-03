package com.jotape.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.domain.model.DomainResult
import com.jotape.domain.model.Interaction
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

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()
    private val TAG = "ConversationViewModel"

    init {
        Log.d(TAG, "ViewModel initialized, starting to collect interactions.")
        collectInteractions()
    }

    private fun collectInteractions() {
        viewModelScope.launch {
            interactionRepository.getAllInteractions()
                .onStart {
                    Log.d(TAG, "Interaction collection started.")
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
                .catch { e ->
                    Log.e(TAG, "Error collecting interactions flow", e)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao observar atualizações: ${e.message}") }
                }
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            Log.d(TAG, "Received interactions order (${result.data.size} items):")
                            result.data.forEachIndexed { index, interaction ->
                                Log.d(TAG, "  [$index]: ${interaction.timestamp} - [User: ${interaction.isFromUser}] - ${interaction.text.take(30)}")
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    interactions = result.data,
                                    errorMessage = null
                                )
                            }
                        }
                        is DomainResult.Error -> {
                            Log.w(TAG, "Interaction flow collected an error: ${result.message}")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = result.message
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage() {
        val currentInputText = _uiState.value.inputText.trim()
        if (currentInputText.isEmpty() || _uiState.value.isSending) {
            return
        }

        _uiState.update { it.copy(inputText = "", isSending = true, errorMessage = null) }

        viewModelScope.launch {
            val result = interactionRepository.sendMessage(userMessage = currentInputText)

            _uiState.update { it.copy(isSending = false) }

            if (result is DomainResult.Error) {
                Log.e(TAG, "Error sending message: ${result.message}")
                _uiState.update { it.copy(errorMessage = result.message) }
            } else {
                Log.d(TAG, "Message sent successfully via repository.")
            }
        }
    }

    fun clearConversationHistory() {
        viewModelScope.launch {
            Log.d(TAG, "Clear history requested from ViewModel")
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = interactionRepository.clearHistory()) {
                is DomainResult.Success -> {
                    Log.i(TAG, "History cleared successfully via repository")
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                }
                is DomainResult.Error -> {
                    Log.e(TAG, "Failed to clear history: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
} 