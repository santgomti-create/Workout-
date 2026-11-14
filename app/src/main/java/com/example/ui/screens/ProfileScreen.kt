package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomExercise
import com.example.data.model.DailyGoal
import com.example.data.model.FitnessGoalType
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutIntensity
import com.example.data.model.WorkoutRecord
import com.example.ui.components.SaveToDriveDialog
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile>,
    workoutsState: UiState<List<WorkoutRecord>>,
    dailyGoalsState: UiState<List<DailyGoal>>,
    customExercisesState: UiState<List<CustomExercise>>,
    currentThemeMode: ThemeMode,
    userEmail: String?,
    isSaving: Boolean,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onSaveProfile: (
        displayName: String,
        age: Int,
        fitnessGoal: FitnessGoalType,
        preferredIntensity: WorkoutIntensity,
        preferredCategories: List<String>,
        dailyMinutesGoal: Int,
        dailyCaloriesGoal: Int,
        weeklyWorkoutsGoal: Int,
        themeMode: ThemeMode,
        privacyAnalyticsOptOut: Boolean
    ) -> Unit,
    onClearAllData: () -> Unit,
    onNotify: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loadedProfile = (profileState as? UiState.Success)?.data ?: UserProfile()
    val workouts = (workoutsState as? UiState.Success)?.data ?: emptyList()
    val dailyGoals = (dailyGoalsState as? UiState.Success)?.data ?: emptyList()
    val customExercises = (customExercisesState as? UiState.Success)?.data ?: emptyList()

    var displayName by remember(loadedProfile.displayName) { mutableStateOf(loadedProfile.displayName) }
    var ageStr by remember(loadedProfile.age) { mutableStateOf(loadedProfile.age.toString()) }
    var fitnessGoal by remember(loadedProfile.fitnessGoal) {
        mutableStateOf(FitnessGoalType.fromLabel(loadedProfile.fitnessGoal))
    }
    var preferredIntensity by remember(loadedProfile.preferredIntensity) {
        mutableStateOf(WorkoutIntensity.fromLabel(loadedProfile.preferredIntensity))
    }
    var preferredCategories by remember(loadedProfile.preferredCategories) {
        mutableStateOf(loadedProfile.preferredCategories)
    }
    var dailyMinutesGoal by remember(loadedProfile.dailyMinutesGoal) {
        mutableIntStateOf(loadedProfile.dailyMinutesGoal)
    }
    var dailyCaloriesGoal by remember(loadedProfile.dailyCaloriesGoal) {
        mutableIntStateOf(loadedProfile.dailyCaloriesGoal)
    }
    var weeklyWorkoutsGoal by remember(loadedProfile.weeklyWorkoutsGoal) {
        mutableIntStateOf(loadedProfile.weeklyWorkoutsGoal)
    }
    var privacyMinimalMode by remember(loadedProfile.privacyAnalyticsOptOut) {
        mutableStateOf(loadedProfile.privacyAnalyticsOptOut)
    }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSaveToDriveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loadedProfile) {
        displayName = loadedProfile.displayName
        ageStr = loadedProfile.age.toString()
        fitnessGoal = FitnessGoalType.fromLabel(loadedProfile.fitnessGoal)
        preferredIntensity = WorkoutIntensity.fromLabel(loadedProfile.preferredIntensity)
        preferredCategories = loadedProfile.preferredCategories
        dailyMinutesGoal = loadedProfile.dailyMinutesGoal
        dailyCaloriesGoal = loadedProfile.dailyCaloriesGoal
        weeklyWorkoutsGoal = loadedProfile.weeklyWorkoutsGoal
        privacyMinimalMode = loadedProfile.privacyAnalyticsOptOut
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.trim().take(1).uppercase().ifEmpty { "F" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = displayName.ifBlank { "FitTrack Athlete" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail ?: "Private Google Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${workouts.size} workouts saved privately",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Appearance / Theme Switcher (Dark, Light, System)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Appearance Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = currentThemeMode == mode,
                                    onClick = { onChangeThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = idx,
                                        count = ThemeMode.entries.size
                                    ),
                                    icon = {
                                        when (mode) {
                                            ThemeMode.DARK -> Icon(
                                                Icons.Filled.DarkMode,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            ThemeMode.LIGHT -> Icon(
                                                Icons.Filled.LightMode,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            ThemeMode.SYSTEM -> Unit
                                        }
                                    },
                                    modifier = Modifier.testTag("theme_mode_${mode.key}")
                                ) {
                                    Text(mode.label)
                                }
                            }
                        }
                    }
                }
            }

            // Personal Info & Fitness Targets Form
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Profile & Fitness Goals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it.take(60) },
                                label = { Text("Your Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(2f)
                                    .testTag("profile_name_input")
                            )
                            OutlinedTextField(
                                value = ageStr,
                                onValueChange = { ageStr = it.filter { ch -> ch.isDigit() }.take(3) },
                                label = { Text("Age") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_age_input")
                            )
                        }

                        Text(
                            text = "Primary Fitness Goal",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FitnessGoalType.entries.forEach { goal ->
                                FilterChip(
                                    selected = fitnessGoal == goal,
                                    onClick = { fitnessGoal = goal },
                                    label = { Text(goal.label) }
                                )
                            }
                        }

                        Text(
                            text = "Daily Active Minutes Target: $dailyMinutesGoal min",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = dailyMinutesGoal.toFloat(),
                            onValueChange = { dailyMinutesGoal = (it / 5f).roundToInt() * 5 },
                            valueRange = 10f..180f
                        )

                        Text(
                            text = "Daily Calorie Burn Target: $dailyCaloriesGoal kcal",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = dailyCaloriesGoal.toFloat(),
                            onValueChange = { dailyCaloriesGoal = (it / 25f).roundToInt() * 25 },
                            valueRange = 100f..1500f
                        )

                        Text(
                            text = "Weekly Workout Days Goal: $weeklyWorkoutsGoal days/week",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = weeklyWorkoutsGoal.toFloat(),
                            onValueChange = { weeklyWorkoutsGoal = it.roundToInt().coerceIn(1, 7) },
                            valueRange = 1f..7f,
                            steps = 5
                        )
                    }
                }
            }

            // Workout Preferences
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Workout Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Preferred Pace",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WorkoutIntensity.entries.forEach { level ->
                                FilterChip(
                                    selected = preferredIntensity == level,
                                    onClick = { preferredIntensity = level },
                                    label = { Text(level.label) }
                                )
                            }
                        }
                        Text(
                            text = "Favorite Activities",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkoutCategory.libraryCategories.forEach { cat ->
                                val isSelected = preferredCategories.contains(cat.displayName)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        preferredCategories = if (isSelected) {
                                            preferredCategories - cat.displayName
                                        } else {
                                            (preferredCategories + cat.displayName).take(10)
                                        }
                                    },
                                    label = { Text(cat.displayName) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val parsedAge = ageStr.toIntOrNull()?.coerceIn(13, 120) ?: 26
                                onSaveProfile(
                                    displayName,
                                    parsedAge,
                                    fitnessGoal,
                                    preferredIntensity,
                                    preferredCategories,
                                    dailyMinutesGoal,
                                    dailyCaloriesGoal,
                                    weeklyWorkoutsGoal,
                                    currentThemeMode,
                                    privacyMinimalMode
                                )
                            },
                            enabled = !isSaving && displayName.isNotBlank(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_profile_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSaving) "Saving..." else "Save Profile & Preferences")
                        }
                    }
                }
            }

            // Privacy-Friendly Data Handling Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Privacy & Data Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Your workout logs, goals, and custom exercises are isolated exclusively to your signed-in account. FitTrack collects zero ad trackers or background location.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Strict Minimal Data Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Keep only essential workout metrics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = privacyMinimalMode,
                                onCheckedChange = { privacyMinimalMode = it }
                            )
                        }

                        FilledTonalButton(
                            onClick = { showSaveToDriveDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_save_to_drive_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download to Drive / Device")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val totalMins = workouts.sumOf { it.durationSeconds } / 60
                                    val totalCals = workouts.sumOf { it.caloriesBurned }
                                    val summary = buildString {
                                        appendLine("FitTrack Summary for $displayName")
                                        appendLine("Goal: ${fitnessGoal.label}")
                                        appendLine("Total Workouts: ${workouts.size}")
                                        appendLine("Total Active Time: $totalMins min")
                                        appendLine("Total Est. Calories: $totalCals kcal")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, summary)
                                        putExtra(Intent.EXTRA_SUBJECT, "FitTrack Progress Summary")
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Export Summary"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_summary_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Summary")
                            }

                            OutlinedButton(
                                onClick = { showClearConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("clear_data_button")
                            ) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Data")
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Workout Data?") },
            text = {
                Text("This permanently deletes all saved workout records, daily goals, and custom exercises from your account. Your profile settings remain intact.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSaveToDriveDialog) {
        SaveToDriveDialog(
            profile = (profileState as? UiState.Success)?.data,
            workouts = workouts,
            dailyGoals = dailyGoals,
            customExercises = customExercises,
            onDismiss = { showSaveToDriveDialog = false },
            onResultMessage = onNotify
        )
    }
}
