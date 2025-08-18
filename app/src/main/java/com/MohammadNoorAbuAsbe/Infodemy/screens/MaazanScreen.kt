package com.MohammadNoorAbuAsbe.Infodemy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.models.*
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.MaazanRepository
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.MaazanViewModel
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.MaazanViewModelFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// User-friendly label mappings
object AcademicLabels {
    val fieldLabels = mapOf(
        "zin" to "ציון", // Grade
        "nidrash" to "נדרש", // Required
        "nirsham" to "נרשם", // Registered
        "nilmad" to "נלמד", // Studied
        "ptor" to "פטור", // Exempted
        "notar" to "נותר", // Remaining
        "ahuz" to "אחוז", // Percentage
        "track" to "מסלול", // Track/Program
        "speciality" to "התמחות", // Speciality
        "mashit" to "משיט", // Academic track
        "tchum" to "תחום", // Field/Domain
        "krs" to "קורס" // Course
    )

    // Additional context-based translations for common academic terms
    val contextLabels = mapOf(
        "Forms.Modules.Maazan.Track" to "מסלול",
        "Forms.Modules.Maazan.Speciality" to "התמחות",
        "מעזן אקדמי" to "מעזן אקדמי",
        "סיכום כללי" to "סיכום כללי",
        "תחומי לימוד" to "תחומי לימוד",
        "קורסים" to "קורסים"
    )

    fun getFieldLabel(field: String): String {
        // First check exact matches
        fieldLabels[field]?.let { return it }

        // Check context-based matches
        contextLabels[field]?.let { return it }

        // Handle compound field names (like forms.Moduls.mazaan.track)
        when {
            field.contains("Track") -> return "מסלול"
            field.contains("speciality") || field.contains("specialty") -> return "התמחות"
            field.contains("mashit") || field.contains("masHit") -> return "משיט"
            field.contains("tchum") -> return "תחום"
            field.contains("krs") || field.contains("course") -> return "קורס"
        }

        // Default fallback
        return field.replaceFirstChar { it.uppercase() }
    }

    fun getGradeColor(value: String): Color {
        return when {
            value == "0" || value.isEmpty() -> Color.Gray
            value.toIntOrNull()?.let { it >= 85 } == true -> Color(0xFF4CAF50) // Green
            value.toIntOrNull()?.let { it >= 70 } == true -> Color(0xFF2196F3) // Blue
            value.toIntOrNull()?.let { it >= 60 } == true -> Color(0xFFFF9800) // Orange
            else -> Color(0xFFF44336) // Red
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaazanScreen(navController: NavController) {
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
    val maazanData by viewModel.maazanData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "מעזן אקדמי",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                isLoading -> CenterLoader()
                error != null -> ErrorMessage(error!!) { viewModel.refreshData() }
                maazanData == null -> EmptyState()
                else -> EnhancedMaazanContent(maazanData!!)
            }
        }
    }
}

@Composable
private fun CenterLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "טוען נתונים אקדמיים...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "שגיאה בטעינת הנתונים",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("נסה שוב")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "אין נתונים אקדמיים זמינים",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnhancedMaazanContent(data: MaazanData) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add summary header
        item {
            AcademicSummaryCard(data)
        }

        // Add each masHit
        items(data.masHits.filter { !it.isSumUpRecord }, key = { it.name }) { masHit ->
            EnhancedMasHitCard(masHit = masHit)
        }
    }
}

@Composable
private fun AcademicSummaryCard(data: MaazanData) {
    val summaryData = data.masHits.find { it.isSumUpRecord }

    summaryData?.let { summary ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "סיכום כללי",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                EnhancedProgressGrid(
                    items = listOf(
                        AcademicLabels.getFieldLabel("zin") to summary.zin,
                        AcademicLabels.getFieldLabel("nidrash") to summary.nidrash,
                        AcademicLabels.getFieldLabel("nirsham") to summary.nirsham,
                        AcademicLabels.getFieldLabel("nilmad") to summary.nilmad,
                        AcademicLabels.getFieldLabel("ptor") to summary.ptor,
                        AcademicLabels.getFieldLabel("notar") to summary.notar,
                        AcademicLabels.getFieldLabel("ahuz") to summary.ahuz
                    ),
                    isHighlighted = true,
                    isSingleRow = true
                )
            }
        }
    }
}

@Composable
private fun EnhancedMasHitCard(masHit: MasHit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with expand/collapse
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = masHit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        CompletionStatusBadge(isComplete = !masHit.isNotComplete)
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "צמצם" else "הרחב",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick summary - now showing all 7 fields in a single row
            EnhancedProgressGrid(
                items = listOf(
                    AcademicLabels.getFieldLabel("zin") to masHit.zin,
                    AcademicLabels.getFieldLabel("nidrash") to masHit.nidrash,
                    AcademicLabels.getFieldLabel("nirsham") to masHit.nirsham,
                    AcademicLabels.getFieldLabel("nilmad") to masHit.nilmad,
                    AcademicLabels.getFieldLabel("ptor") to masHit.ptor,
                    AcademicLabels.getFieldLabel("notar") to masHit.notar,
                    AcademicLabels.getFieldLabel("ahuz") to masHit.ahuz + "%"
                ),
                isHighlighted = false,
                isSingleRow = true
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Show Tchums when expanded (removed duplicate fields section)
                masHit.tchums?.let { tchums ->
                    if (tchums.isNotEmpty()) {
                        Text(
                            text = "תחומי לימוד",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        tchums.forEach { tchum ->
                            EnhancedTchumCard(tchum = tchum)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedTchumCard(tchum: Tchum) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tchum.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        CompletionStatusBadge(isComplete = !tchum.isNotComplete)
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "צמצם" else "הרחב",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick summary
            EnhancedProgressGrid(
                items = listOf(
                    AcademicLabels.getFieldLabel("zin") to tchum.zin,
                    AcademicLabels.getFieldLabel("nidrash") to tchum.nidrash,
                    AcademicLabels.getFieldLabel("nirsham") to tchum.nirsham,
                    AcademicLabels.getFieldLabel("nilmad") to tchum.nilmad,
                    AcademicLabels.getFieldLabel("ptor") to tchum.ptor,
                    AcademicLabels.getFieldLabel("notar") to tchum.notar,
                    AcademicLabels.getFieldLabel("ahuz") to tchum.ahuz + "%"
                ),
                isHighlighted = false,
                isSingleRow = true
            )

            if (expanded) {
                // Show courses (Krss) when expanded
                tchum.krss?.let { courses ->
                    if (courses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "קורסים",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        courses.forEach { krs ->
                            EnhancedKrsCard(krs = krs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedKrsCard(krs: Krs) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = krs.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                CompletionStatusBadge(isComplete = !krs.isNotComplete)
            }

            Spacer(modifier = Modifier.height(8.dp))

            EnhancedProgressGrid(
                items = listOf(
                    AcademicLabels.getFieldLabel("zin") to krs.zin,
                    AcademicLabels.getFieldLabel("nidrash") to krs.nidrash,
                    AcademicLabels.getFieldLabel("nirsham") to krs.nirsham,
                    AcademicLabels.getFieldLabel("nilmad") to krs.nilmad,
                    AcademicLabels.getFieldLabel("ptor") to krs.ptor,
                    AcademicLabels.getFieldLabel("notar") to krs.notar,
                    AcademicLabels.getFieldLabel("ahuz") to krs.ahuz + "%"
                ),
                isHighlighted = false,
                isSingleRow = true
            )

            if (!krs.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = krs.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompletionStatusBadge(isComplete: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isComplete) Color(0xFF4CAF50) else Color(0xFFFF9800),
        modifier = Modifier.size(8.dp)
    ) {}
}

@Composable
private fun EnhancedProgressGrid(
    items: List<Pair<String, String>>,
    isHighlighted: Boolean = false,
    isCompact: Boolean = false,
    isSingleRow: Boolean = false
) {
    val chunkedItems = if (isSingleRow) {
        listOf(items) // All items in one row
    } else {
        items.chunked(if (isCompact) 2 else 3)
    }

    val backgroundColor = if (isHighlighted)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(if (isCompact) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
    ) {
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isSingleRow) Arrangement.SpaceEvenly else Arrangement.SpaceBetween
            ) {
                rowItems.forEach { (label, value) ->
                    EnhancedProgressStat(
                        label = label,
                        value = value,
                        isHighlighted = isHighlighted,
                        isCompact = isCompact || isSingleRow
                    )
                }

                // Fill empty spaces if needed (only for non-single-row layouts)
                if (!isSingleRow) {
                    repeat((if (isCompact) 2 else 3) - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedProgressStat(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    isCompact: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = if (isCompact) 2.dp else 4.dp)
    ) {
        Text(
            text = value,
            style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer
                   else AcademicLabels.getGradeColor(value)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
