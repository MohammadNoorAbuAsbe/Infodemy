package com.MohammadNoorAbuAsbe.Infodemy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.Exam
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.ExamsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExamsViewModel(
    private val repository: ExamsRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // UI State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Exams Data
    private val _allExams = MutableStateFlow<List<Exam>>(emptyList())
    private val _upcomingExams = MutableStateFlow<List<Exam>>(emptyList())
    private val _pastExams = MutableStateFlow<List<Exam>>(emptyList())

    val upcomingExams: StateFlow<List<Exam>> = _upcomingExams.asStateFlow()
    val pastExams: StateFlow<List<Exam>> = _pastExams.asStateFlow()

    // Tab state
    private val _selectedTab = MutableStateFlow(0) // 0 = Upcoming, 1 = Past
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Error State
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Date formatter
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        loadExamsData()
    }

    /**
     * Loads exams data using the token from TokenManager
     */
    private fun loadExamsData() {
        viewModelScope.launch {
            tokenManager.token.collectLatest { token ->
                token?.let { currentToken ->
                    try {
                        _isLoading.value = true
                        _error.value = null

                        val examsData = repository.fetchExamsData(currentToken)
                        updateExamsData(examsData)
                    } catch (e: IOException) {
                        _error.value = "Network error: ${e.message}"
                    } catch (e: Exception) {
                        _error.value = "Error loading exams: ${e.message}"
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
     * Updates the UI state with the fetched exams data
     */
    private fun updateExamsData(examsData: List<Exam>) {
        _allExams.value = examsData

        // Separate exams into upcoming and past
        val now = LocalDateTime.now()

        val upcoming = mutableListOf<Exam>()
        val past = mutableListOf<Exam>()

        examsData.forEach { exam ->
            try {
                // Parse the full date-time string
                val examDateTime = LocalDateTime.parse(exam.date, dateFormatter)

                if (examDateTime.isAfter(now)) {
                    upcoming.add(exam)
                } else {
                    past.add(exam)
                }
            } catch (e: Exception) {
                // If full datetime parsing fails, try with date only
                try {
                    val examDateStr = exam.date.substringBefore("T")
                    val examDate = LocalDate.parse(examDateStr, dateOnlyFormatter)
                    val today = LocalDate.now()

                    if (!examDate.isBefore(today)) {
                        upcoming.add(exam)
                    } else {
                        past.add(exam)
                    }
                } catch (e2: Exception) {
                    // If date parsing still fails, treat as past exam
                    past.add(exam)
                }
            }
        }

        // Sort upcoming exams: closest first
        upcoming.sortBy { exam ->
            try {
                LocalDateTime.parse(exam.date, dateFormatter)
            } catch (e: Exception) {
                try {
                    // Use start of day if we only have date
                    LocalDate.parse(exam.date.substringBefore("T"), dateOnlyFormatter)
                        .atStartOfDay()
                } catch (e2: Exception) {
                    // Fallback to distant future if parsing fails
                    LocalDateTime.MAX
                }
            }
        }

        // Sort past exams: most recent first
        past.sortByDescending { exam ->
            try {
                LocalDateTime.parse(exam.date, dateFormatter)
            } catch (e: Exception) {
                try {
                    // Use start of day if we only have date
                    LocalDate.parse(exam.date.substringBefore("T"), dateOnlyFormatter)
                        .atStartOfDay()
                } catch (e2: Exception) {
                    // Fallback to distant past if parsing fails
                    LocalDateTime.MIN
                }
            }
        }

        _upcomingExams.value = upcoming
        _pastExams.value = past
    }

    /**
     * Sets the selected tab
     */
    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    /**
     * Refreshes the exams data
     */
    fun refreshData() {
        loadExamsData()
    }
}