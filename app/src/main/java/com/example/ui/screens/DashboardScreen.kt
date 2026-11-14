package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyGoal
import com.example.data.model.DateUtils
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutRecord
import com.example.ui.components.AnimatedActivityRing
import com.example.ui.components.AnimatedGoalProgressBar
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ChartBarPoint
import com.example.ui.components.InteractiveWorkoutBarChart
import com.example.ui.components.categoryIcon
import com.example.ui.components.formatDurationMinutes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.UiState

@Composable
fun DashboardScreen(
    profileState: UiState<UserProfile>,
    workoutsState: UiState<List<WorkoutRecord>>,
    dailyGoalsState: UiState<List<DailyGoal>>,
    isWorkoutTimerActive: Boolean,
    onNavigateToTrack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onAddDailyGoal: (title: String, target: Int, unit: String) -> Unit,
    onIncrementGoal: (DailyGoal, Int) -> Unit,
    onToggleGoalComplete: (DailyGoal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddGoalDialog by remember { mutableStateOf(false) }

    val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
    val workouts = (workoutsState as? UiState.Success)?.data ?: emptyList()
    val goals = (dailyGoalsState as? UiState.Success)?.data ?: emptyList()

    val todayKey = remember { DateUtils.todayKey() }
    val todayWorkouts = remember(workouts, todayKey) {
        workouts.filter { it.dateKey == todayKey }
    }
    val todayMinutes = remember(todayWorkouts) {
        todayWorkouts.sumOf { it.durationSeconds } / 60
    }
    val todayCalories = remember(todayWorkouts) {
        todayWorkouts.sumOf { it.caloriesBurned }
    }
    val totalAllTimeMinutes = remember(workouts) {
        workouts.sumOf { it.durationSeconds } / 60
    }

    val last7DaysPoints = remember(workouts) {
        DateUtils.lastNDaysKeys(7).map { key ->
            val dayList = workouts.filter { it.dateKey == key }
            ChartBarPoint(
                label = DateUtils.shortDayLabel(key),
                minutes = dayList.sumOf { it.durationSeconds } / 60,
                calories = dayList.sumOf { it.caloriesBurned },
                sessions = dayList.size
            )
        }
    }
    val weeklySessions = remember(last7DaysPoints) {
        last7DaysPoints.sumOf { it.sessions }
    }
    val weeklyMinutes = remember(last7DaysPoints) {
        last7DaysPoints.sumOf { it.minutes }
    }

    val todayGoals = remember(goals, todayKey) {
        goals.filter { it.dateKey == todayKey }
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
            // Hero Greeting Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dashboard_hero),
                        contentDescription = "FitTrack activity banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF070C09).copy(alpha = 0.94f),
                                        Color(0xFF070C09).copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = profile.fitnessGoal,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Hi, ${profile.displayName}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (todayWorkouts.isEmpty()) {
                                    "Ready for a fresh session today?"
                                } else {
                                    "${todayWorkouts.size} workout${if (todayWorkouts.size == 1) "" else "s"} logged today!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCDE2D4)
                            )
                        }

                        Button(
                            onClick = onNavigateToTrack,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("dashboard_start_workout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWorkoutTimerActive) "Resume Active Timer" else "Start Workout",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Loading / Error banner if needed
            if (workoutsState is UiState.Loading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Syncing your workouts...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (workoutsState is UiState.Error) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = workoutsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Daily Workout Summary Card with Animated Activity Ring
            item {
                val minuteProgress = (todayMinutes.toFloat() / profile.dailyMinutesGoal.coerceAtLeast(1))
                val calorieProgress = (todayCalories.toFloat() / profile.dailyCaloriesGoal.coerceAtLeast(1))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Activity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(minuteProgress * 100).toInt().coerceAtMost(999)}% of daily goal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            AnimatedActivityRing(
                                progress = minuteProgress,
                                centerPrimaryText = "${todayMinutes}m",
                                centerSecondaryText = "of ${profile.dailyMinutesGoal}m"
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryMetricRow(
                                    icon = Icons.Filled.LocalFireDepartment,
                                    label = "Calories Burned",
                                    value = "$todayCalories / ${profile.dailyCaloriesGoal} kcal",
                                    accent = MaterialTheme.colorScheme.tertiary
                                )
                                SummaryMetricRow(
                                    icon = Icons.Filled.Timer,
                                    label = "Total Time (All-Time)",
                                    value = "$totalAllTimeMinutes min",
                                    accent = MaterialTheme.colorScheme.primary
                                )
                                SummaryMetricRow(
                                    icon = Icons.Filled.FitnessCenter,
                                    label = "Today's Sessions",
                                    value = "${todayWorkouts.size} completed",
                                    accent = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Animated Progress Bars for Active Minutes & Calories
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Active Time Goal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$todayMinutes / ${profile.dailyMinutesGoal} min",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                AnimatedGoalProgressBar(progress = minuteProgress)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Calorie Burn Goal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$todayCalories / ${profile.dailyCaloriesGoal} kcal",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                AnimatedGoalProgressBar(
                                    progress = calorieProgress,
                                    fillColor = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            // Daily Goals Checklist Section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Fitness Goals",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${todayGoals.count { it.completed }}/${todayGoals.size} custom goals completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { showAddGoalDialog = true },
                                modifier = Modifier.testTag("add_daily_goal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Goal")
                            }
                        }

                        if (todayGoals.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Add a custom daily target like \"15 min Stretching\", \"8,000 steps\", or \"20 Push-ups\" to track with animated progress bars.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                todayGoals.forEach { goal ->
                                    DailyGoalRow(
                                        goal = goal,
                                        onIncrement = { onIncrementGoal(goal, it) },
                                        onToggleComplete = { onToggleGoalComplete(goal) },
                                        onDelete = { onDeleteGoal(goal.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Weekly Workout Overview Interactive Chart
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Weekly Overview",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$weeklySessions/${profile.weeklyWorkoutsGoal} workouts • $weeklyMinutes min total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onNavigateToHistory) {
                                Text("Reports")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        InteractiveWorkoutBarChart(points = last7DaysPoints)
                    }
                }
            }

            // Recent Workouts Quick List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Workouts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (workouts.isNotEmpty()) {
                        TextButton(onClick = onNavigateToHistory) {
                            Text("See All (${workouts.size})")
                        }
                    }
                }
            }

            if (workouts.isEmpty()) {
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
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No workouts recorded yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap Start Workout above to begin a beginner-friendly guided session or quick-log your activity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(workouts.take(3), key = { it.id }) { workout ->
                    RecentWorkoutItem(workout = workout)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showAddGoalDialog) {
        AddDailyGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, target, unit ->
                onAddDailyGoal(title, target, unit)
                showAddGoalDialog = false
            }
        )
    }
}

@Composable
private fun SummaryMetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DailyGoalRow(
    goal: DailyGoal,
    onIncrement: (Int) -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (goal.currentValue.toFloat() / goal.targetValue.coerceAtLeast(1).toFloat())
    val stepDelta = if (goal.targetValue >= 1000) 500 else if (goal.targetValue >= 100) 25 else 5

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_goal_${goal.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (goal.completed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.AnimatedVisibility(visible = goal.completed) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Goal completed",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Column {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${goal.currentValue} / ${goal.targetValue} ${goal.unit}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { onIncrement(stepDelta) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("+$stepDelta", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete goal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            AnimatedGoalProgressBar(progress = progress, height = 8.dp)
        }
    }
}

@Composable
private fun RecentWorkoutItem(workout: WorkoutRecord) {
    val category = WorkoutCategory.fromName(workout.category)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = workout.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(category = category)
                    Text(
                        text = DateUtils.displayDate(workout.dateKey),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDurationMinutes(workout.durationSeconds),
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${workout.caloriesBurned} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddDailyGoalDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, target: Int, unit: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("20") }
    var unit by remember { mutableStateOf("min") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Daily Fitness Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Name (e.g., Morning Stretch)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(16) },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetStr.toIntOrNull()?.coerceIn(1, 10000) ?: 20
                    if (title.isNotBlank()) {
                        onSave(title, target, unit.ifBlank { "min" })
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_goal_button")
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
