package com.MohammadNoorAbuAsbe.Infodemy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.DaySchedule
import com.MohammadNoorAbuAsbe.Infodemy.data.models.ScheduleCourse
import com.MohammadNoorAbuAsbe.Infodemy.data.models.ScheduleParams
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.ScheduleRepository
import com.MohammadNoorAbuAsbe.Infodemy.utils.DateUtils.formatTimeFromDateTime
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.text.format

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // Add a CoroutineExceptionHandler
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _error.value = "Unexpected error: ${exception.message}"
    }

    // UI State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showSchedule = MutableStateFlow(false)
    val showSchedule: StateFlow<Boolean> = _showSchedule.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Schedule Data
    private val _scheduleData = MutableStateFlow<List<ScheduleCourse>>(emptyList())
    val scheduleData: StateFlow<List<ScheduleCourse>> = _scheduleData.asStateFlow()

    private val _scheduleParams = MutableStateFlow<ScheduleParams?>(null)
    val scheduleParams: StateFlow<ScheduleParams?> = _scheduleParams.asStateFlow()

    // Filter State
    private val _selectedFilter = MutableStateFlow<Pair<String, String>?>(null)
    val selectedFilter: StateFlow<Pair<String, String>?> = _selectedFilter.asStateFlow()

    // Calendar State
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDay = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDay: StateFlow<LocalDate?> = _selectedDay.asStateFlow()

    private val _monthSchedule = MutableStateFlow<List<DaySchedule>>(emptyList())
    val monthSchedule: StateFlow<List<DaySchedule>> = _monthSchedule.asStateFlow()

    // Cache for all fetched schedules
    private val _allMonthSchedules = MutableStateFlow<Map<String, List<DaySchedule>>>(emptyMap())
    private val _fetchedDateRanges = MutableStateFlow<Set<String>>(emptySet())
    private val _isFetchingMonthData = MutableStateFlow(false)

    init {
        loadScheduleData()
    }

    private fun loadScheduleData() {
        viewModelScope.launch(exceptionHandler) { // Use the exception handler
            tokenManager.token.collectLatest { token ->
                token?.let { currentToken ->
                    try {
                        _isLoading.value = true
                        _error.value = null // Reset error before starting
                        if (token == TokenManager.DEMO_TOKEN_VALUE) {
                            loadDemoScheduleData()
                        } else {

                            val paramsDeferred =
                                async { repository.fetchScheduleParams(currentToken) }
                            val params = paramsDeferred.await()
                            _scheduleParams.value = params

                            val coursesDeferred =
                                async { repository.fetchSchedule(currentToken, params) }
                            val courses = coursesDeferred.await()
                            _scheduleData.value = courses

                            if (_selectedFilter.value == null && courses.isNotEmpty()) {
                                val studyYears = courses.map { it.studyYear }.distinct()
                                val semesters = courses.map { it.semester }.distinct().reversed()
                                if (studyYears.isNotEmpty() && semesters.isNotEmpty()) {
                                    _selectedFilter.value = studyYears.first() to semesters.first()
                                }
                            }

                            fetchMonthSchedule(currentToken, _currentMonth.value, params)
                        }
                    } catch (e: IOException) {
                        _error.value = "Network error: ${e.message}. Please check your internet connection."
                    } catch (e: Exception) {
                        _error.value = "Error loading schedule: ${e.message}"
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

    private fun loadDemoScheduleData() {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            _error.value = null

            _scheduleParams.value = ScheduleParams(
                hash = "demo_hash_string_123",
                pt = 1, // Demo program type
                ptMsl = 101, // Demo program track
                shl = 20232 // Demo academic year/semester code
            )

            val demoCourses = listOf(
                ScheduleCourse(name = "Demo Mobile Dev", instructor = "Dr. Demo Droid", startTime = "09:00", endTime = "11:00", day = "MONDAY", location = "Room D101", semester = "A", studyYear = "Year 3"),
                ScheduleCourse(name = "Demo Algorithms Lab", instructor = "TA Demo Byte", startTime = "11:00", endTime = "13:00", day = "MONDAY", location = "Lab D102", semester = "A", studyYear = "Year 3"),
                ScheduleCourse(name = "Demo Web Tech", instructor = "Prof. Demo Web", startTime = "14:00", endTime = "16:00", day = "WEDNESDAY", location = "Room D201", semester = "A", studyYear = "Year 3"),
                ScheduleCourse(name = "Demo Databases", instructor = "Dr. Demo SQL", startTime = "10:00", endTime = "12:00", day = "FRIDAY", location = "Room D301", semester = "B", studyYear = "Year 2")
            )
            _scheduleData.value = demoCourses

            // 3. Set initial filter if not set
            if (_selectedFilter.value == null && demoCourses.isNotEmpty()) {
                val studyYears = demoCourses.map { it.studyYear }.distinct()
                val semesters = demoCourses.map { it.semester }.distinct().reversed()
                if (studyYears.isNotEmpty() && semesters.isNotEmpty()) {
                    _selectedFilter.value = studyYears.first() to semesters.first()
                }
            }

            // 4. Fetch/Generate Demo Month Schedule
            // For demo, we'll generate some static data for the current month
            // and simulate the caching mechanism.
            _scheduleParams.value?.let { params ->
                fetchDemoMonthSchedule(_currentMonth.value, params)
            }
            _isLoading.value = false
            _showSchedule.value = true // Assuming we always show schedule if data is loaded
        }
    }

    private fun fetchDemoMonthSchedule(yearMonth: YearMonth, params: ScheduleParams) {
        // Prevent concurrent demo data generation for the same month if already running
        if (_isFetchingMonthData.value && _fetchedDateRanges.value.contains("${yearMonth}-DEMO")) return
        _isFetchingMonthData.value = true
        _isLoading.value = true // Indicate loading for month data generation

        val demoDaySchedulesForMonth = mutableListOf<DaySchedule>()
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()

        // Formatter for time strings (HH:mm)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        // Formatter for the date string in DaySchedule (yyyy-MM-ddTHH:mm:ss)
        // The time part can be arbitrary (e.g., start of day) if DaySchedule.date is just for the date.
        // Or, it can be specific if the 'date' field is meant to be a full timestamp.
        // Let's assume DaySchedule.date is a full timestamp representing the start of the event.
        val dayScheduleDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")


        var currentDayIterator = firstDayOfMonth
        while (!currentDayIterator.isAfter(lastDayOfMonth)) {
            val dayOfWeek = currentDayIterator.dayOfWeek

            // Only add events for weekdays for this demo (e.g., Monday to Friday)
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                // Let's add 1 to 2 random events per demo weekday
                val numberOfEvents = Random.nextInt(1, 3)
                for (i in 1..numberOfEvents) {
                    val startHour = Random.nextInt(8, 16) // Events start between 8 AM and 4 PM
                    val startMinute = if (Random.nextBoolean()) 0 else 30 // On the hour or half hour
                    val eventStartTime = LocalTime.of(startHour, startMinute)
                    // Event duration 1 or 2 hours
                    val eventEndTime = eventStartTime.plusHours(Random.nextLong(1, 3))

                    // Ensure end time does not exceed a reasonable time (e.g., 18:00)
                    val maxEndTime = LocalTime.of(18, 0)
                    val finalEndTime = if (eventEndTime.isAfter(maxEndTime)) maxEndTime else eventEndTime


                    // Construct the full date-time string for DaySchedule's 'date' field
                    val eventFullDateTimeStr = currentDayIterator.atTime(eventStartTime).format(dayScheduleDateFormatter)

                    demoDaySchedulesForMonth.add(
                        DaySchedule(
                            date = eventFullDateTimeStr, // e.g., "2023-01-15T09:00:00"
                            title = "Demo ${if (Random.nextBoolean()) "Lecture" else "Lab"} ${Random.nextInt(1, 20)}",
                            startTime = eventStartTime.format(timeFormatter), // "09:00"
                            endTime = finalEndTime.format(timeFormatter),   // "11:00"
                            place = "Demo Room ${Random.nextInt(100, 305)}",
                            moreInfo = "Instructor: Prof. Demo ${Random.nextInt(1, 10)}\nTopic: Introduction to Demo Topic ${Random.nextInt(1,5)}"
                        )
                    )
                }
            }
            currentDayIterator = currentDayIterator.plusDays(1)
        }

        // Simulate caching mechanism: update _allMonthSchedules
        // Group schedules by their date part (yyyy-MM-dd) for the cache key
        val newCache = _allMonthSchedules.value.toMutableMap()
        demoDaySchedulesForMonth.groupBy { it.date.substring(0, 10) } // Group by "yyyy-MM-dd"
            .forEach { (dateKey, schedulesOnDate) ->
                // If there are existing schedules for this date (e.g. from another demo fetch), append.
                // Or, replace if that's the desired behavior for demo. Let's append for now.
                val existingSchedules = newCache[dateKey] ?: emptyList()
                newCache[dateKey] = existingSchedules + schedulesOnDate
            }
        _allMonthSchedules.value = newCache

        // Simulate fetched date ranges (mark the whole month as "fetched" for demo)
        _fetchedDateRanges.value = _fetchedDateRanges.value + "${yearMonth}-DEMO"

        updateMonthScheduleFromCache(yearMonth) // This will filter from _allMonthSchedules for the current _monthSchedule

        _isFetchingMonthData.value = false
        _isLoading.value = false
    }

    fun setFilter(filter: Pair<String, String>) {
        _selectedFilter.value = filter
    }

    fun changeMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _currentMonth.value = yearMonth
            _scheduleParams.value?.let { params ->
                // Get the current token and use it
                tokenManager.token.collectLatest { token ->
                    token?.let { currentToken ->
                        fetchMonthSchedule(currentToken, yearMonth, params)
                    }
                }
            }
        }
    }

    fun selectDay(date: LocalDate) {
        _selectedDay.value = date
    }

    fun getScheduleForDay(date: LocalDate): List<DaySchedule> {
        return _monthSchedule.value
            .filter { it.date.startsWith(date.toString()) }
            .sortedBy { formatTimeFromDateTime(it.startTime) }
    }

    private suspend fun fetchMonthSchedule(token: String, yearMonth: YearMonth, params: ScheduleParams) {
        if (_isFetchingMonthData.value) return
        _isFetchingMonthData.value = true
        _isLoading.value = true

        try {
            val firstDayOfMonth = yearMonth.atDay(1)
            val lastDayOfMonth = yearMonth.atEndOfMonth()

            // Calculate which weeks we need to fetch
            val weeksToFetch = mutableListOf<LocalDate>()
            var currentDay = firstDayOfMonth.minusDays(firstDayOfMonth.dayOfWeek.value % 7L)

            // Continue fetching weeks until we've covered the entire month
            while (currentDay.isBefore(lastDayOfMonth) || currentDay.isEqual(lastDayOfMonth)) {
                val weekStartKey = "${currentDay}T00:00:00.000Z"
                if (!_fetchedDateRanges.value.contains(weekStartKey)) {
                    weeksToFetch.add(currentDay)
                    // Add to fetched ranges immediately to prevent duplicate requests
                    _fetchedDateRanges.value = _fetchedDateRanges.value + weekStartKey
                }
                currentDay = currentDay.plusDays(7)
            }

            // Add one more week if the last day of the month is not in the last fetched week
            if (currentDay.minusDays(7).isBefore(lastDayOfMonth)) {
                val weekStartKey = "${currentDay}T00:00:00.000Z"
                if (!_fetchedDateRanges.value.contains(weekStartKey)) {
                    weeksToFetch.add(currentDay)
                    _fetchedDateRanges.value = _fetchedDateRanges.value + weekStartKey
                }
            }

            if (weeksToFetch.isEmpty()) {
                // All weeks for this month have been fetched already
                updateMonthScheduleFromCache(yearMonth)
                _isFetchingMonthData.value = false
                _isLoading.value = false
                return
            }

            // Fetch all needed weeks concurrently
            val weekSchedules = weeksToFetch.map { startDay ->
                viewModelScope.async { repository.fetchWeekSchedule(token, params, startDay) }
            }.awaitAll()

            // Add to our global cache
            val newCache = _allMonthSchedules.value.toMutableMap()
            weekSchedules.flatten().forEach { schedule ->
                val dateKey = schedule.date.split("T")[0] // Get just the date part
                val existingList = newCache[dateKey] ?: emptyList()
                newCache[dateKey] = existingList + schedule
            }
            _allMonthSchedules.value = newCache

            // Update the current month's schedule from the cache
            updateMonthScheduleFromCache(yearMonth)
        } catch (e: Exception) {
            // Handle overall fetch failure
        } finally {
            _isFetchingMonthData.value = false
            _isLoading.value = false
        }
    }

    private fun updateMonthScheduleFromCache(yearMonth: YearMonth) {
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()

        val relevantDates = mutableListOf<String>()
        var currentDay = firstDayOfMonth

        while (currentDay.isBefore(lastDayOfMonth) || currentDay.isEqual(lastDayOfMonth)) {
            relevantDates.add(currentDay.toString())
            currentDay = currentDay.plusDays(1)
        }

        val relevantSchedules = mutableListOf<DaySchedule>()
        for (date in relevantDates) {
            _allMonthSchedules.value[date]?.let { schedules ->
                relevantSchedules.addAll(schedules)
            }
        }

        _monthSchedule.value = relevantSchedules
    }

    fun refreshData() {
        loadScheduleData()
    }

    fun setShowSchedule(b: Boolean) {
        _showSchedule.value = b;
    }
}