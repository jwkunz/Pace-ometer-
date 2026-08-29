package com.example.pace_ometer.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pace_ometer.data.ActivityType
import com.example.pace_ometer.data.db.entity.PersonalRecordCategory
import com.example.pace_ometer.data.db.entity.PersonalRecordEntity
import com.example.pace_ometer.data.db.entity.PersonalRecordScope
import com.example.pace_ometer.util.formatDistanceMeters
import com.example.pace_ometer.util.formatDurationMs
import com.example.pace_ometer.util.formatSpeedFromPaceSecPerKm

private fun categoryLabel(category: String): String = when (category) {
    PersonalRecordCategory.FASTEST_1K -> "Fastest 1K"
    PersonalRecordCategory.FASTEST_5K -> "Fastest 5K"
    PersonalRecordCategory.FASTEST_10K -> "Fastest 10K"
    PersonalRecordCategory.FASTEST_MILE -> "Fastest Mile"
    PersonalRecordCategory.LONGEST_DISTANCE -> "Longest Distance"
    PersonalRecordCategory.FASTEST_OVERALL_PACE -> "Fastest Overall Pace"
    else -> category
}

private fun formatRecordValue(category: String, value: Double, activityType: ActivityType): String = when (category) {
    PersonalRecordCategory.LONGEST_DISTANCE -> formatDistanceMeters(value)
    PersonalRecordCategory.FASTEST_OVERALL_PACE ->
        if (activityType.usesPaceDisplay) "${formatDurationMs((value * 1000).toLong())} /km"
        else formatSpeedFromPaceSecPerKm(value)
    else -> formatDurationMs((value * 1000).toLong())
}

@Composable
fun PersonalRecordsScreen(
    onBack: () -> Unit,
    viewModel: PersonalRecordsViewModel = viewModel()
) {
    val seasons by viewModel.seasons.collectAsState()
    val selectedScope by viewModel.selectedScope.collectAsState()
    val selectedActivityType by viewModel.selectedActivityType.collectAsState()
    val records by viewModel.records.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Personal Records") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ActivityType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, ActivityType.entries.size),
                        selected = selectedActivityType == type,
                        onClick = { viewModel.selectActivityType(type) }
                    ) { Text(type.displayName) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedScope == PersonalRecordScope.ALL_TIME,
                    onClick = { viewModel.selectScope(PersonalRecordScope.ALL_TIME) },
                    label = { Text("All-Time") }
                )
                seasons.forEach { season ->
                    val scope = PersonalRecordScope.season(season.id)
                    FilterChip(
                        selected = selectedScope == scope,
                        onClick = { viewModel.selectScope(scope) },
                        label = { Text(season.name) }
                    )
                }
            }

            if (records.isEmpty()) {
                Text("No records in this scope yet. Save a run to start setting personal records.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(records, key = { it.category }) { record: PersonalRecordEntity ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(categoryLabel(record.category), style = MaterialTheme.typography.titleSmall)
                                Text(formatRecordValue(record.category, record.value, selectedActivityType))
                            }
                        }
                    }
                }
            }
        }
    }
}
