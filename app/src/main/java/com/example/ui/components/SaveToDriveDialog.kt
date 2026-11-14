package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportFormat
import com.example.data.export.WorkoutExportHelper
import com.example.data.model.CustomExercise
import com.example.data.model.DailyGoal
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutRecord
import kotlinx.coroutines.launch

@Composable
fun SaveToDriveDialog(
    profile: UserProfile?,
    workouts: List<WorkoutRecord>,
    dailyGoals: List<DailyGoal>,
    customExercises: List<CustomExercise>,
    onDismiss: () -> Unit,
    onResultMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType)
    ) { uri ->
        if (uri != null) {
            val content = WorkoutExportHelper.buildCsvContent(workouts)
            scope.launch {
                val res = WorkoutExportHelper.writeContentToUri(context, uri, content)
                if (res.isSuccess) {
                    onResultMessage("Saved ${workouts.size} workouts as CSV to Drive/Device")
                } else {
                    onResultMessage("Could not save CSV file. Please try again.")
                }
                onDismiss()
            }
        }
    }

    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.JSON.mimeType)
    ) { uri ->
        if (uri != null) {
            val content = WorkoutExportHelper.buildJsonBackupContent(
                profile = profile,
                workouts = workouts,
                goals = dailyGoals,
                customExercises = customExercises
            )
            scope.launch {
                val res = WorkoutExportHelper.writeContentToUri(context, uri, content)
                if (res.isSuccess) {
                    onResultMessage("Saved full FitTrack backup (JSON) to Drive/Device")
                } else {
                    onResultMessage("Could not save backup file. Please try again.")
                }
                onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Download to Drive / Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose a format, then pick Google Drive or your Downloads folder in the system file picker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExportFormat.entries.forEach { format ->
                    val selected = selectedFormat == format
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedFormat = format }
                            .testTag("export_format_${format.extension}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { selectedFormat = format }
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = format.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = format.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fileName = WorkoutExportHelper.suggestedFileName(selectedFormat)
                    when (selectedFormat) {
                        ExportFormat.CSV -> csvLauncher.launch(fileName)
                        ExportFormat.JSON -> jsonLauncher.launch(fileName)
                    }
                },
                modifier = Modifier.testTag("confirm_download_drive_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Choose Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
