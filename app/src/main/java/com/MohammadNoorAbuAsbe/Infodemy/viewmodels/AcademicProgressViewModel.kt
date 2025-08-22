package com.MohammadNoorAbuAsbe.Infodemy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.MaazanData
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.MaazanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

class AcademicProgressViewModel(
    private val repository: MaazanRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // UI State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Maazan Data
    private val _maazanData = MutableStateFlow<MaazanData?>(null)
    val maazanData: StateFlow<MaazanData?> = _maazanData.asStateFlow()

    // Error State
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMaazanData()
    }

    /**
     * Loads Maazan data using the token from TokenManager
     */
    private fun loadMaazanData() {
        viewModelScope.launch {
            tokenManager.token.collectLatest { token ->
                token?.let { currentToken ->
                    try {
                        _isLoading.value = true
                        _error.value = null

                        val data = repository.fetchMaazanData(currentToken)
                        _maazanData.value = data
                    } catch (e: IOException) {
                        _error.value = "Network error: ${e.message}"
                    } catch (e: Exception) {
                        _error.value = "Error loading academic progress: ${e.message}"
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

    /**
     * Refreshes the Maazan data
     */
    fun refreshData() {
        loadMaazanData()
    }
}