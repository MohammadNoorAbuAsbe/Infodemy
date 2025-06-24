package com.MohammadNoorAbuAsbe.Infodemy.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.Krs
import com.MohammadNoorAbuAsbe.Infodemy.data.models.MaazanData
import com.MohammadNoorAbuAsbe.Infodemy.data.models.MasHit
import com.MohammadNoorAbuAsbe.Infodemy.data.models.Tchum
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.MaazanRepository
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.MaazanViewModel
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.MaazanViewModelFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaazanScreen(navController: NavController) {
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

    val repository = remember { MaazanRepository(client) }
    val viewModel: MaazanViewModel = viewModel(
        factory = MaazanViewModelFactory(repository, tokenManager)
    )

    // Collect state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val maazanData by viewModel.maazanData.collectAsState()
    val error by viewModel.error.collectAsState()
    val expandedSections by remember { derivedStateOf { viewModel.expandedSections } }

    // UI state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var isNavigating by remember { mutableStateOf(false) }

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
                        "Academic Progress",
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
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
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

                maazanData == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No academic progress data available")
                }

                else -> {
                    MaazanContent(
                        maazanData = maazanData!!,
                        expandedSections = expandedSections,
                        onSectionToggle = { viewModel.toggleSection(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MaazanContent(
    maazanData: MaazanData,
    expandedSections: Map<String, Boolean>,
    onSectionToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(maazanData.masHits) { masHit ->
            MasHitSection(
                masHit = masHit,
                isExpanded = expandedSections[masHit.name] ?: false,
                onToggle = { onSectionToggle(masHit.name) }
            )
        }
    }
}

@Composable
private fun MasHitSection(
    masHit: MasHit,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (masHit.isSumUpRecord)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = masHit.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (masHit.ahuz.isNotBlank()) {
                    ProgressBadge(value = masHit.ahuz.toFloatOrNull() ?: 0f)
                }

                if (masHit.tchums!!.isNotEmpty()) {
                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            // Summary metrics
            ProgressMetrics(
                zin = masHit.zin,
                nidrash = masHit.nidrash,
                nirsham = masHit.nirsham,
                nilmad = masHit.nilmad,
                ptor = masHit.ptor,
                notar = masHit.notar,
                ahuz = masHit.ahuz
            )

            // Incomplete warning
            if (masHit.isNotComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                IncompleteWarning()
            }

            // Description
            masHit.description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tchums (sub-sections)
            if (isExpanded && masHit.tchums!!.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    masHit.tchums.forEach { tchum ->
                        TchumSection(tchum = tchum)
                    }
                }
            }
        }
    }
}

@Composable
private fun TchumSection(tchum: Tchum) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Text(
                text = tchum.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics
            ProgressMetrics(
                zin = tchum.zin,
                nidrash = tchum.nidrash,
                nirsham = tchum.nirsham,
                nilmad = tchum.nilmad,
                ptor = tchum.ptor,
                notar = tchum.notar,
                ahuz = tchum.ahuz,
                isCompact = true
            )

            // Incomplete warning
            if (tchum.isNotComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                IncompleteWarning()
            }

            // Description
            tchum.description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Courses
            if (tchum.krss!!.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tchum.krss.forEach { krs ->
                        KrsItem(krs = krs)
                    }
                }
            }
        }
    }
}

@Composable
private fun KrsItem(krs: Krs) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (krs.style == "error")
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        else
            null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = krs.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                krs.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (krs.ahuz.isNotBlank()) {
                Text(
                    text = "${krs.ahuz}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = progressColor(krs.ahuz.toFloatOrNull() ?: 0f)
                )
            }
        }
    }
}

@Composable
private fun ProgressMetrics(
    zin: String,
    nidrash: String,
    nirsham: String,
    nilmad: String,
    ptor: String,
    notar: String,
    ahuz: String,
    isCompact: Boolean = false
) {
    val metrics = listOf(
        "ציון" to zin,
        "נדרש" to nidrash,
        "נרשם" to nirsham,
        "נלמד" to nilmad,
        "פטור" to ptor,
        "נותר" to notar,
        "אחוז" to ahuz
    ).filter { (_, value) -> value.isNotBlank() }

    if (isCompact) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(metrics) { (label, value) ->
                MetricChip(label = label, value = value)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            metrics.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("$label: $value", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ProgressBadge(value: Float) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = progressColor(value),
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${value.toInt()}%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun IncompleteWarning() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Incomplete",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Requirements incomplete",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private fun progressColor(value: Float): Color {
    return when {
        value >= 85 -> Color(0xFF4CAF50) // Green
        value >= 70 -> Color(0xFF2196F3) // Blue
        value >= 50 -> Color(0xFFFFC107) // Amber
        else -> Color(0xFFF44336) // Red
    }
}