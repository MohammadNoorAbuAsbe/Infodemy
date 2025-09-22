package com.MohammadNoorAbuAsbe.Infodemy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.ScheduleCourse
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

    val error by viewModel.error.collectAsState()

    // Debounce state to prevent multiple rapid navigation actions
    var isNavigating by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "My Schedule",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isNavigating) {
                            isNavigating = true
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            // Tab navigation
            PrimaryTabRow(
                selectedTabIndex = if (showSchedule) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = showSchedule,
                    onClick = { viewModel.setShowSchedule(true) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (showSchedule) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Schedule")
                        }
                    }
                )
                Tab(
                    selected = !showSchedule,
                    onClick = { viewModel.setShowSchedule(false) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (!showSchedule) Icons.Filled.Event else Icons.Outlined.Event,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calendar")
                        }
                    }
                )
            }

            // Loading indicator at top
            if (isLoading && scheduleData.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

if (showSchedule) {
                // Schedule View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ScheduleFilters(viewModel, scheduleData, selectedFilter)
                }

                // Schedule List
                if (error != null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "We're having trouble connecting. Please check your internet connection and try again.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Retry")
                        }
                    }
                } else if (isLoading) {
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
                if (error != null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "We're having trouble connecting. Please check your internet connection and try again.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Retry")
                        }
                    }
                } else if (isLoading) {
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


@Composable
private fun ScheduleFilters(
    viewModel: ScheduleViewModel,
    scheduleData: List<ScheduleCourse>,
    selectedFilter: Pair<String, String>?
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

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(combinedFilters.count()) { filter ->
            FilterChip(
                selected = selectedFilter == combinedFilters[filter],
                onClick = { viewModel.setFilter(combinedFilters[filter]) },
                label = { Text("${combinedFilters[filter].first} - ${combinedFilters[filter].second}") },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}