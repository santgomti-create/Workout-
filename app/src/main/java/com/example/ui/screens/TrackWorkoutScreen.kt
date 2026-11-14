package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutIntensity
import com.example.ui.components.categoryIcon
import com.example.ui.components.formatStopwatch
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.ActiveWorkoutTimerState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackWorkoutScreen(
    timerState: ActiveWorkoutTimerState,
    isSaving: Boolean,
    onSelectCategory: (WorkoutCategory) -> Unit,
    onSetTitle: (String) -> Unit,
    onSetIntensity: (WorkoutIntensity) -> Unit,
    onSetNotes: (String) -> Unit,
    onStartWorkout: () -> Unit,
    onPauseWorkout: () -> Unit,
    onResumeWorkout: () -> Unit,
    onStopAndSave: () -> Unit,
    onDiscard: () -> Unit,
    onQuickLogWorkout: (title: String, category: WorkoutCategory, minutes: Int, intensity: WorkoutIntensity, notes: String) -> Unit,
    onAddCustomExercise: (name: String, category: WorkoutCategory, instructions: String, difficulty: String, caloriesPerMinute: Int, restSeconds: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showQuickLogDialog by remember { mutableStateOf(false) }
    var showCustomExerciseDialog by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "timer_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Workout Tracker",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pick an activity, press Start, or quick-log a completed session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Timer Hero Card
            item {
                val activeRunning = timerState.isRunning && !timerState.isPaused
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = if (activeRunning) 0.16f else 0.06f
                                        ),
                                        MaterialTheme.colorScheme.surfaceContainer
                                    )
                                )
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Status Badge
                        Surface(
                            color = if (activeRunning) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            } else if (timerState.isPaused) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = when {
                                    activeRunning -> "LIVE SESSION • ${timerState.selectedCategory.displayName.uppercase()}"
                                    timerState.isPaused -> "PAUSED • ${timerState.selectedCategory.displayName.uppercase()}"
                                    else -> "READY • ${timerState.selectedCategory.displayName.uppercase()}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeRunning) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        // Circular Pulse Readout
                        Box(
                            modifier = Modifier
                                .size(196.dp)
                                .scale(if (activeRunning) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(
                                    width = 4.dp,
                                    color = if (activeRunning) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = formatStopwatch(timerState.elapsedSeconds),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("workout_timer_readout")
                                )
                                Text(
                                    text = "~${timerState.estimatedCalories} kcal est.",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Timer Control Buttons (Start / Pause / Resume / Stop & Save)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!timerState.isRunning) {
                                Button(
                                    onClick = onStartWorkout,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .testTag("timer_start_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Start Timer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                if (timerState.isPaused) {
                                    Button(
                                        onClick = onResumeWorkout,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .testTag("timer_resume_button")
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Resume")
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = onPauseWorkout,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .testTag("timer_pause_button")
                                    ) {
                                        Icon(Icons.Filled.Pause, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pause")
                                    }
                                }

                                Button(
                                    onClick = onStopAndSave,
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .testTag("timer_stop_save_button")
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isSaving) "Saving..." else "Stop & Save")
                                }
                            }
                        }

                        AnimatedVisibility(visible = timerState.isRunning || timerState.elapsedSeconds > 0) {
                            TextButton(
                                onClick = onDiscard,
                                modifier = Modifier.testTag("timer_discard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Timer")
                            }
                        }
                    }
                }
            }

            // Workout Category Selector (Walking, Running, Cycling, Yoga, Gym, Home Workout, Custom)
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
                            text = "Workout Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkoutCategory.entries.forEach { category ->
                                val selected = timerState.selectedCategory == category
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectCategory(category) },
                                    label = { Text(category.displayName) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = categoryIcon(category),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = timerState.workoutTitle,
                            onValueChange = onSetTitle,
                            label = { Text("Workout Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("workout_title_input")
                        )

                        Text(
                            text = "Intensity",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkoutIntensity.entries.forEach { level ->
                                val selected = timerState.intensity == level
                                Surface(
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSetIntensity(level) }
                                        .testTag("intensity_${level.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = level.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = timerState.notes,
                            onValueChange = onSetNotes,
                            label = { Text("Session Notes (optional)") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Quick Log & Custom Exercise Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showQuickLogDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("quick_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quick Log")
                    }

                    FilledTonalButton(
                        onClick = { showCustomExerciseDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("track_add_custom_exercise_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Exercise")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showQuickLogDialog) {
        QuickLogWorkoutDialog(
            initialCategory = timerState.selectedCategory,
            initialIntensity = timerState.intensity,
            onDismiss = { showQuickLogDialog = false },
            onSave = { title, cat, mins, intensity, notes ->
                onQuickLogWorkout(title, cat, mins, intensity, notes)
                showQuickLogDialog = false
            }
        )
    }

    if (showCustomExerciseDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showCustomExerciseDialog = false },
            onSave = { name, cat, instructions, diff, cals, rest ->
                onAddCustomExercise(name, cat, instructions, diff, cals, rest)
                showCustomExerciseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLogWorkoutDialog(
    initialCategory: WorkoutCategory,
    initialIntensity: WorkoutIntensity,
    onDismiss: () -> Unit,
    onSave: (title: String, category: WorkoutCategory, minutes: Int, intensity: WorkoutIntensity, notes: String) -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var title by remember { mutableStateOf("${initialCategory.displayName} Session") }
    var minutesStr by remember { mutableStateOf("25") }
    var intensity by remember { mutableStateOf(initialIntensity) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Log Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WorkoutCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                title = "${cat.displayName} Session"
                            },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Workout Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minutesStr,
                    onValueChange = { minutesStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkoutIntensity.entries.forEach { level ->
                        FilterChip(
                            selected = intensity == level,
                            onClick = { intensity = level },
                            label = { Text(level.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = minutesStr.toIntOrNull()?.coerceIn(1, 600) ?: 20
                    onSave(title, category, mins, intensity, notes)
                },
                enabled = title.isNotBlank() && (minutesStr.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.testTag("confirm_quick_log_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Workout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCustomExerciseDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: WorkoutCategory,
        instructions: String,
        difficulty: String,
        caloriesPerMinute: Int,
        restSeconds: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(WorkoutCategory.HOME_WORKOUT) }
    var instructions by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Beginner") }
    var calsStr by remember { mutableStateOf("6") }
    var restStr by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_name_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WorkoutCategory.libraryCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Beginner Instructions") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_instructions_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calsStr,
                        onValueChange = { calsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("kcal / min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = restStr,
                        onValueChange = { restStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Rest (sec)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cals = calsStr.toIntOrNull()?.coerceIn(1, 30) ?: 5
                    val rest = restStr.toIntOrNull()?.coerceIn(5, 300) ?: 60
                    onSave(name, category, instructions, difficulty, cals, rest)
                },
                enabled = name.isNotBlank() && instructions.isNotBlank(),
                modifier = Modifier.testTag("confirm_custom_exercise_button")
            ) {
                Text("Save Exercise")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
