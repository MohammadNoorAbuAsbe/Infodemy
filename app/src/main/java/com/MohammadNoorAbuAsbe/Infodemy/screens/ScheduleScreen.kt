package com.MohammadNoorAbuAsbe.Infodemy.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.ScheduleRepository
import com.MohammadNoorAbuAsbe.Infodemy.ui.components.CalendarGridView
import com.MohammadNoorAbuAsbe.Infodemy.ui.components.CourseList
import com.MohammadNoorAbuAsbe.Infodemy.ui.components.DayScheduleList
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.ScheduleViewModel
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.ScheduleViewModelFactory
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val client = remember { OkHttpClient() }
    val repository = remember { ScheduleRepository(client) }

    // Create ViewModel with factory
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(repository, tokenManager)
    )

    // Collect state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val showSchedule by viewModel.showSchedule.collectAsState()
    val scheduleData by viewModel.scheduleData.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val monthSchedule by viewModel.monthSchedule.collectAsState()

    // Local UI state
    var filterExpanded by remember { mutableStateOf(false) }

    // Debounce state to prevent multiple rapid navigation actions
    var isNavigating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Schedule") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isNavigating) {
                            isNavigating = true
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.toggleScheduleView()
                        },
                        modifier = Modifier.padding(end = 16.dp)) {
                        Text(if (showSchedule) "Show Calendar View" else "Show Schedule View")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (showSchedule) {
                // Schedule View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val studyYears = scheduleData.map { it.studyYear }.distinct()
                    val semesters = scheduleData.map { it.semester }.distinct().reversed()
                    val combinedFilters = studyYears.flatMap { year ->
                        semesters.map { semester -> year to semester }
                    }.filter { filter ->
                        scheduleData.any { course ->
                            course.studyYear == filter.first && course.semester == filter.second
                        }
                    }

                    TextButton(onClick = { filterExpanded = !filterExpanded }) {
                        Text("Filter: ${selectedFilter?.let { "${it.first} - ${it.second}" } ?: "None"}")
                    }
                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        combinedFilters.forEach { (year, semester) ->
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.setFilter(year to semester)
                                    filterExpanded = false
                                },
                                text = { Text(text = "$year - $semester") }
                            )
                        }
                    }
                }

                // Schedule List
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    val filteredCourses = scheduleData.filter { course ->
                        selectedFilter == null || (course.studyYear == selectedFilter!!.first && course.semester == selectedFilter!!.second)
                    }

                    CourseList(
                        courses = filteredCourses,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Calendar View
                CalendarGridView(
                    currentMonth = currentMonth,
                    onMonthChanged = { viewModel.changeMonth(it) },
                    onDaySelected = { viewModel.selectDay(it) },
                    selectedDay = selectedDay,
                    hasEvents = { date -> viewModel.getScheduleForDay(date).isNotEmpty() }
                )

                // Display events for selected day
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    selectedDay?.let { date ->
                        val daySchedule = viewModel.getScheduleForDay(date)
                        DayScheduleList(
                            daySchedules = daySchedule,
                            formatTimeFromDateTime = { dateTimeStr ->
                                try {
                                    if (dateTimeStr.length >= 16) {
                                        dateTimeStr.substring(11, 16)
                                    } else {
                                        dateTimeStr
                                    }
                                } catch (e: Exception) {
                                    dateTimeStr
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true)
                        )
                    } ?: run {
                        Text(
                            text = "Select a day to view events",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}