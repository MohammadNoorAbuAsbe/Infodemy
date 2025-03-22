package com.MohammadNoorAbuAsbe.myruppin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.MohammadNoorAbuAsbe.myruppin.data.models.DaySchedule

@Composable
fun DayScheduleList(
    daySchedules: List<DaySchedule>,
    formatTimeFromDateTime: (String) -> String,
    modifier: Modifier = Modifier
) {
    // Detect overlapping schedules
    val overlappingIndices = remember(daySchedules) {
        daySchedules.indices.filter { i ->
            daySchedules.any { other ->
                other != daySchedules[i] &&
                        daySchedules[i].startTime < other.endTime &&
                        daySchedules[i].endTime > other.startTime
            }
        }.toSet()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (daySchedules.isEmpty()) {
            Text(
                text = "No events scheduled for this day",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp).align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            ) {
                items(daySchedules) { schedule ->
                    val isOverlapping = overlappingIndices.contains(daySchedules.indexOf(schedule))
                    val overlappingTimes = if (isOverlapping) {
                        daySchedules.filter { other ->
                            other != schedule &&
                                    schedule.startTime < other.endTime &&
                                    schedule.endTime > other.startTime
                        }.joinToString(separator = "\n") { other ->
                            "Overlaps with: ${formatTimeFromDateTime(other.startTime)} - ${formatTimeFromDateTime(other.endTime)}"
                        }
                    } else null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOverlapping) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isOverlapping) {
                                    Text(
                                        text = "⚠ Overlap",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                                    )
                                }
                                Text(
                                    text = schedule.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = "${formatTimeFromDateTime(schedule.startTime)} - ${formatTimeFromDateTime(schedule.endTime)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!schedule.place.isNullOrBlank()) {
                                Text(
                                    text = "Location: ${schedule.place}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (!schedule.moreInfo.isNullOrBlank()) {
                                Text(
                                    text = "Instructor: ${schedule.moreInfo}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (isOverlapping && overlappingTimes != null) {
                                Text(
                                    text = overlappingTimes,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}