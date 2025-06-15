package com.MohammadNoorAbuAsbe.Infodemy.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.ScheduleCourse
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.GradesRepository
import com.MohammadNoorAbuAsbe.Infodemy.ui.components.CourseCard
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.GradesViewModel
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.GradesViewModelFactory
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.ScheduleViewModel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(navController: NavController) {
    // Setup dependencies
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val repository = remember { GradesRepository(client) }
    val viewModel: GradesViewModel = viewModel(
        factory = GradesViewModelFactory(repository, tokenManager)
    )

    // Collect state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val uniqueKrsSnl by viewModel.uniqueKrsSnl.collectAsState()
    val error by viewModel.error.collectAsState()

    // UI state
    var filterDropdownExpanded by remember { mutableStateOf(false) }

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
                        "My Grades",
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Filter chips and average row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Average display
                Text(
                    text = "ממוצע: ${viewModel.getDisplayAverage()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 16.dp)
                )

                // Filter chips
                GradesFilter(
                    viewModel = viewModel,
                    uniqueKrsSnl = uniqueKrsSnl,
                    selectedFilter = selectedFilter
                )
            }

            // Content
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    val filteredCourses = viewModel.getFilteredCourses()

                    if (filteredCourses.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No courses found for the selected filter")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCourses) { course ->
                                CourseCard(course)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradesFilter(
    viewModel: GradesViewModel,
    uniqueKrsSnl: List<String>,
    selectedFilter: String?
) {
    // Add "All" as the first filter option
    val filterOptions = listOf("All") + uniqueKrsSnl.reversed()

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filterOptions) { filterOption ->
            FilterChip(
                selected = selectedFilter == filterOption,
                onClick = { viewModel.setFilter(filterOption) },
                label = { Text(filterOption) },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}