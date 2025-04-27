package com.jotape.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.domain.model.DomainResult
import com.jotape.domain.repository.InteractionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        Log.d(TAG, "ViewModel initialized, loading interactions.")
        loadInteractions()
    }

    private fun loadInteractions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = interactionRepository.getAllInteractions()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, interactions = result.data) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage() {
        val currentInputText = _uiState.value.inputText.trim()
        if (currentInputText.isEmpty() || _uiState.value.isLoading) {
            return
        }

        _uiState.update { it.copy(inputText = "", isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val userResult = interactionRepository.addInteraction(text = currentInputText, isFromUser = true)

            if (userResult is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = userResult.message) }
                return@launch
            }

            delay(1000)
            val botResponse = "Entendido (Supabase): '$currentInputText'"
            val botResult = interactionRepository.addInteraction(text = botResponse, isFromUser = false)

            if (botResult is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = botResult.message) }
                loadInteractions()
                return@launch
            }
            
            loadInteractions()
        }
    }

    fun clearChatHistory() {
        Log.d(TAG, "Attempting to clear chat history...")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = interactionRepository.clearHistory()) {
                is DomainResult.Success -> {
                    Log.i(TAG, "Clear history successful in repository. Reloading interactions.")
                    _uiState.update { it.copy(interactions = emptyList(), isLoading = false) }
                }
                is DomainResult.Error -> {
                    Log.e(TAG, "Clear history failed: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
            Log.d(TAG, "Clear chat history process finished.")
        }
    }
} 