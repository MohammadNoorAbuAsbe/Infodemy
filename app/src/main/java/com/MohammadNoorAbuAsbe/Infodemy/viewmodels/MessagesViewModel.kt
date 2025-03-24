package com.MohammadNoorAbuAsbe.Infodemy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.Message
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessagesViewModel(private val repository: MessagesRepository,
                        private val tokenManager: TokenManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchMessages()
    }

    private fun fetchMessages() {
        viewModelScope.launch {
            tokenManager.token.collectLatest { token ->
                token?.let { currentToken ->
                    _isLoading.value = true
                    _error.value = null
                    try {
                        _messages.value = repository.fetchMessages(currentToken)
                    } catch (e: Exception) {
                        _error.value = e.message
                    } finally {
                        _isLoading.value = false
                    }
                } ?: run {
                    _error.value = "Authentication token not found"
                    _isLoading.value = false
                }
            }
        }
    }
}
