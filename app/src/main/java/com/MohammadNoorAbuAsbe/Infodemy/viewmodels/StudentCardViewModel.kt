package com.MohammadNoorAbuAsbe.Infodemy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.StudentCard
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.StudentCardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StudentCardViewModel(
    private val repository: StudentCardRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentCardUiState())
    val uiState: StateFlow<StudentCardUiState> = _uiState.asStateFlow()

    private val _currentDateTime = MutableStateFlow("")
    val currentDateTime: StateFlow<String> = _currentDateTime.asStateFlow()

    init {
        loadStudentCard()
        loadServerDateTime()
    }

    private fun loadStudentCard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                tokenManager.token.collectLatest { token ->
                    if (token == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No authentication token found"
                        )
                        return@collectLatest
                    }

                    // First, get the student card data (SNL data)
                    val studentCardData = repository.fetchStudentCardData(token)
                    if (studentCardData == null || studentCardData.snlsData.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to fetch student card data"
                        )
                        return@collectLatest
                    }

                    // Use the first (selected) SNL data to get the actual student card
                    val selectedSnlData = studentCardData.snlsData.first { it.selected }
                    val studentCard = repository.fetchStudentCard(token, selectedSnlData)

                    if (studentCard == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to fetch student card"
                        )
                        return@collectLatest
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        studentCard = studentCard,
                        error = null
                    )

                    // Load institution logo after student card is loaded
                    loadInstituteLogo(studentCard.instituteLogo)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun loadServerDateTime() {
        viewModelScope.launch {
            try {
                tokenManager.token.collectLatest { token ->
                    if (token != null) {
                        val serverDateTime = repository.fetchServerDateTime(token)
                        if (serverDateTime != null) {
                            // Parse and format the server date time
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'+03:00'")
                            val localDateTime = LocalDateTime.parse(serverDateTime, formatter)
                            val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm:ss")
                            _currentDateTime.value = localDateTime.format(displayFormatter)
                        } else {
                            // Fallback to local time
                            val localDateTime = LocalDateTime.now()
                            val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm:ss")
                            _currentDateTime.value = localDateTime.format(displayFormatter)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to local time
                val localDateTime = LocalDateTime.now()
                val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm:ss")
                _currentDateTime.value = localDateTime.format(displayFormatter)
            }
        }
    }

    private fun loadInstituteLogo(logoUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLogoLoading = true)

            try {
                val logoBytes = repository.fetchInstituteLogo(logoUrl)
                _uiState.value = _uiState.value.copy(
                    instituteLogo = logoBytes,
                    isLogoLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    instituteLogo = null,
                    isLogoLoading = false
                )
            }
        }
    }

    fun refreshStudentCard() {
        loadStudentCard()
        loadServerDateTime()
    }
}

data class StudentCardUiState(
    val isLoading: Boolean = false,
    val studentCard: StudentCard? = null,
    val error: String? = null,
    val instituteLogo: ByteArray? = null,
    val isLogoLoading: Boolean = false
)
