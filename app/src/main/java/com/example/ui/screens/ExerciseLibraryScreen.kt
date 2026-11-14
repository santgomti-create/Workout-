package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BuiltInExercises
import com.example.data.model.CustomExercise
import com.example.data.model.LibraryExercise
import com.example.data.model.WorkoutCategory
import com.example.ui.components.AnimatedGoalProgressBar
import com.example.ui.components.CategoryBadge
import com.example.ui.components.categoryIcon
import com.example.ui.components.formatStopwatch
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.RestTimerState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    customExercisesState: UiState<List<CustomExercise>>,
    restTimerState: RestTimerState,
    onSetRestDuration: (Int) -> Unit,
    onStartRestTimer: (Int?) -> Unit,
    onPauseRestTimer: () -> Unit,
    onResetRestTimer: () -> Unit,
    onAddCustomExercise: (name: String, category: WorkoutCategory, instructions: String, difficulty: String, caloriesPerMinute: Int, restSeconds: Int) -> Unit,
    onDeleteCustomExercise: (String) -> Unit,
    onStartExerciseWorkout: (LibraryExercise) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<WorkoutCategory?>(null) }
    var showCustomOnly by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var inspectedExercise by remember { mutableStateOf<LibraryExercise?>(null) }

    val customExercises = (customExercisesState as? UiState.Success)?.data ?: emptyList()

    val allExercises = remember(customExercises) {
        val mappedCustom = customExercises.map { ce ->
            LibraryExercise(
                id = ce.id,
                name = ce.name,
                category = WorkoutCategory.fromName(ce.category),
                instructions = ce.instructions.split("\n").filter { it.isNotBlank() }.ifEmpty {
                    listOf(ce.instructions)
                },
                difficulty = ce.difficulty,
                caloriesPerMinute = ce.caloriesPerMinute,
                defaultRestSeconds = ce.defaultRestSeconds,
                targetMuscles = "Custom Exercise",
                isCustom = true
            )
        }
        mappedCustom + BuiltInExercises.items
    }

    val filteredExercises = remember(allExercises, searchQuery, selectedCategory, showCustomOnly) {
        allExercises.filter { ex ->
            val matchesQuery = searchQuery.isBlank() ||
                ex.name.contains(searchQuery, ignoreCase = true) ||
                ex.targetMuscles.contains(searchQuery, ignoreCase = true) ||
                ex.category.displayName.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || ex.category == selectedCategory
            val matchesCustom = !showCustomOnly || ex.isCustom
            matchesQuery && matchesCat && matchesCustom
        }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Add Custom Exercise CTA
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Exercise Library",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Beginner-friendly guides & built-in rest timer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("library_add_custom_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            // Built-in Rest Timer Card
            item {
                RestTimerCard(
                    state = restTimerState,
                    onSelectDuration = onSetRestDuration,
                    onStart = { onStartRestTimer(null) },
                    onPause = onPauseRestTimer,
                    onReset = onResetRestTimer
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Search exercises, muscles, or categories") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input")
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null && !showCustomOnly,
                            onClick = {
                                selectedCategory = null
                                showCustomOnly = false
                            },
                            label = { Text("All (${allExercises.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = showCustomOnly,
                            onClick = {
                                showCustomOnly = !showCustomOnly
                                if (showCustomOnly) selectedCategory = null
                            },
                            label = { Text("Custom (${customExercises.size})") }
                        )
                    }
                    items(WorkoutCategory.libraryCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat && !showCustomOnly,
                            onClick = {
                                showCustomOnly = false
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = { Text(cat.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Exercises List
            items(filteredExercises, key = { it.id }) { exercise ->
                ExerciseLibraryCard(
                    exercise = exercise,
                    onInspect = { inspectedExercise = exercise },
                    onStartRest = { onStartRestTimer(exercise.defaultRestSeconds) },
                    onDeleteCustom = if (exercise.isCustom) {
                        { onDeleteCustomExercise(exercise.id) }
                    } else null
                )
            }

            if (filteredExercises.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No matching exercises",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Try clearing filters or add your own custom exercise.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showAddDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, cat, instructions, diff, cals, rest ->
                onAddCustomExercise(name, cat, instructions, diff, cals, rest)
                showAddDialog = false
            }
        )
    }

    inspectedExercise?.let { exercise ->
        ExerciseDetailDialog(
            exercise = exercise,
            onDismiss = { inspectedExercise = null },
            onStartRestTimer = {
                onStartRestTimer(exercise.defaultRestSeconds)
                inspectedExercise = null
            },
            onTrackExercise = {
                onStartExerciseWorkout(exercise)
                inspectedExercise = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimerCard(
    state: RestTimerState,
    onSelectDuration: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    val progress = if (state.totalSeconds > 0) {
        1f - (state.remainingSeconds.toFloat() / state.totalSeconds.toFloat())
    } else 0f

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Rest Timer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formatStopwatch(state.remainingSeconds),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    fontWeight = FontWeight.Bold,
                    color = if (state.isFinished) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("rest_timer_readout")
                )
            }

            AnimatedGoalProgressBar(
                progress = if (state.isFinished) 1f else progress,
                height = 8.dp
            )

            AnimatedVisibility(visible = state.isFinished) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Rest complete! Ready for your next set.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(30, 45, 60, 90, 120).forEach { sec ->
                    FilterChip(
                        selected = state.totalSeconds == sec,
                        onClick = { onSelectDuration(sec) },
                        label = { Text("${sec}s") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("rest_preset_$sec")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.isRunning) {
                    FilledTonalButton(
                        onClick = onPause,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rest_pause_button")
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rest_start_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (state.isFinished) "Restart" else "Start Rest")
                    }
                }
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("rest_reset_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryCard(
    exercise: LibraryExercise,
    onInspect: () -> Unit,
    onStartRest: () -> Unit,
    onDeleteCustom: (() -> Unit)?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onInspect)
            .testTag("exercise_card_${exercise.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(exercise.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${exercise.targetMuscles} • ${exercise.caloriesPerMinute} kcal/min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onDeleteCustom != null) {
                    IconButton(
                        onClick = onDeleteCustom,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete custom exercise",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryBadge(category = exercise.category)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape
                    ) {
                        Text(
                            text = exercise.difficulty,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                TextButton(
                    onClick = onStartRest,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${exercise.defaultRestSeconds}s rest")
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailDialog(
    exercise: LibraryExercise,
    onDismiss: () -> Unit,
    onStartRestTimer: () -> Unit,
    onTrackExercise: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryBadge(category = exercise.category)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape
                    ) {
                        Text(
                            text = exercise.difficulty,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Beginner Step-by-Step Guide",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                exercise.instructions.forEachIndexed { idx, step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Targets: ${exercise.targetMuscles} • Est. ${exercise.caloriesPerMinute} kcal/min • Rest: ${exercise.defaultRestSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onTrackExercise,
                modifier = Modifier.testTag("detail_track_exercise_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Track Now")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onStartRestTimer) {
                    Text("Rest ${exercise.defaultRestSeconds}s")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
