package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.CalendarDayCell
import com.example.data.model.CustomExercise
import com.example.data.model.DailyGoal
import com.example.data.model.DateUtils
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutRecord
import com.example.ui.components.AnimatedGoalProgressBar
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ChartBarPoint
import com.example.ui.components.InteractiveWorkoutBarChart
import com.example.ui.components.SaveToDriveDialog
import com.example.ui.components.categoryAccentColor
import com.example.ui.components.categoryIcon
import com.example.ui.components.formatDurationMinutes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.UiState

private enum class ReportPeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

@Composable
fun HistoryReportsScreen(
    workoutsState: UiState<List<WorkoutRecord>>,
    profileState: UiState<UserProfile>,
    dailyGoalsState: UiState<List<DailyGoal>>,
    customExercisesState: UiState<List<CustomExercise>>,
    onDeleteWorkout: (String) -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.WEEKLY) }
    var filterCategory by remember { mutableStateOf<WorkoutCategory?>(null) }
    var pendingDeleteWorkout by remember { mutableStateOf<WorkoutRecord?>(null) }
    var showSaveToDriveDialog by remember { mutableStateOf(false) }

    val initialYm = remember { DateUtils.currentYearMonth() }
    var calendarYear by remember { mutableIntStateOf(initialYm.first) }
    var calendarMonth by remember { mutableIntStateOf(initialYm.second) }
    var selectedDateKey by remember { mutableStateOf(DateUtils.todayKey()) }

    val workouts = (workoutsState as? UiState.Success)?.data ?: emptyList()
    val profile = (profileState as? UiState.Success)?.data
    val dailyGoals = (dailyGoalsState as? UiState.Success)?.data ?: emptyList()
    val customExercises = (customExercisesState as? UiState.Success)?.data ?: emptyList()
    val workoutsByDate = remember(workouts) { workouts.groupBy { it.dateKey } }

    val reportPoints = remember(workouts, selectedPeriod) {
        if (selectedPeriod == ReportPeriod.WEEKLY) {
            DateUtils.lastNDaysKeys(7).map { key ->
                val list = workouts.filter { it.dateKey == key }
                ChartBarPoint(
                    label = DateUtils.shortDayLabel(key),
                    minutes = list.sumOf { it.durationSeconds } / 60,
                    calories = list.sumOf { it.caloriesBurned },
                    sessions = list.size
                )
            }
        } else {
            val all28 = DateUtils.lastNDaysKeys(28)
            (0 until 4).map { weekIdx ->
                val slice = all28.subList(weekIdx * 7, (weekIdx + 1) * 7).toSet()
                val list = workouts.filter { it.dateKey in slice }
                ChartBarPoint(
                    label = "Wk ${weekIdx + 1}",
                    minutes = list.sumOf { it.durationSeconds } / 60,
                    calories = list.sumOf { it.caloriesBurned },
                    sessions = list.size
                )
            }
        }
    }

    val periodMinutes = remember(reportPoints) { reportPoints.sumOf { it.minutes } }
    val periodCalories = remember(reportPoints) { reportPoints.sumOf { it.calories } }
    val periodSessions = remember(reportPoints) { reportPoints.sumOf { it.sessions } }

    val periodDateKeys = remember(selectedPeriod) {
        DateUtils.lastNDaysKeys(if (selectedPeriod == ReportPeriod.WEEKLY) 7 else 28).toSet()
    }
    val categoryTotals = remember(workouts, periodDateKeys) {
        val scoped = workouts.filter { it.dateKey in periodDateKeys }
        val totalMin = scoped.sumOf { it.durationSeconds }.coerceAtLeast(1)
        WorkoutCategory.entries.mapNotNull { cat ->
            val catSec = scoped.filter { it.category == cat.displayName }.sumOf { it.durationSeconds }
            if (catSec <= 0) null
            else Triple(cat, catSec / 60, catSec.toFloat() / totalMin.toFloat())
        }.sortedByDescending { it.second }
    }

    val filteredHistory = remember(workouts, filterCategory) {
        if (filterCategory == null) workouts
        else workouts.filter { it.category == filterCategory!!.displayName }
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "History & Reports",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track your weekly & monthly consistency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { showSaveToDriveDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_save_to_drive_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = "Save to Drive",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Drive")
                    }
                }
            }

            // Visual Workout Calendar Card
            item {
                WorkoutCalendarCard(
                    year = calendarYear,
                    monthZeroBased = calendarMonth,
                    selectedDateKey = selectedDateKey,
                    workoutsByDate = workoutsByDate,
                    onSelectDate = { selectedDateKey = it },
                    onPreviousMonth = {
                        val (y, m) = DateUtils.shiftMonth(calendarYear, calendarMonth, -1)
                        calendarYear = y
                        calendarMonth = m
                    },
                    onNextMonth = {
                        val (y, m) = DateUtils.shiftMonth(calendarYear, calendarMonth, 1)
                        calendarYear = y
                        calendarMonth = m
                    },
                    onJumpToToday = {
                        val (y, m) = DateUtils.currentYearMonth()
                        calendarYear = y
                        calendarMonth = m
                        selectedDateKey = DateUtils.todayKey()
                    }
                )
            }

            // Weekly / Monthly Switcher + Interactive Chart Card
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ReportPeriod.entries.forEachIndexed { idx, period ->
                                SegmentedButton(
                                    selected = selectedPeriod == period,
                                    onClick = { selectedPeriod = period },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = idx,
                                        count = ReportPeriod.entries.size
                                    ),
                                    modifier = Modifier.testTag("report_period_${period.name.lowercase()}")
                                ) {
                                    Text("${period.label} Report")
                                }
                            }
                        }

                        // Summary Totals Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReportStatChip(
                                icon = Icons.Filled.Timer,
                                value = "${periodMinutes}m",
                                label = "Active Time",
                                modifier = Modifier.weight(1f)
                            )
                            ReportStatChip(
                                icon = Icons.Filled.LocalFireDepartment,
                                value = "$periodCalories",
                                label = "kcal Burned",
                                modifier = Modifier.weight(1f)
                            )
                            ReportStatChip(
                                icon = Icons.Filled.BarChart,
                                value = "$periodSessions",
                                label = "Sessions",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        InteractiveWorkoutBarChart(points = reportPoints)
                    }
                }
            }

            // Category Breakdown Card
            if (categoryTotals.isNotEmpty()) {
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
                                text = "${selectedPeriod.label} Mix by Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            categoryTotals.forEach { (cat, mins, fraction) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat.displayName,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = "${mins}m (${(fraction * 100).toInt()}%)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    AnimatedGoalProgressBar(
                                        progress = fraction,
                                        height = 8.dp,
                                        fillColor = categoryAccentColor(cat)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // History Filter Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Workout Records (${filteredHistory.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterCategory == null,
                                onClick = { filterCategory = null },
                                label = { Text("All") }
                            )
                        }
                        items(WorkoutCategory.entries) { cat ->
                            FilterChip(
                                selected = filterCategory == cat,
                                onClick = {
                                    filterCategory = if (filterCategory == cat) null else cat
                                },
                                label = { Text(cat.displayName) }
                            )
                        }
                    }
                }
            }

            if (filteredHistory.isEmpty()) {
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
                                imageVector = Icons.Filled.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "No saved workouts here yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Complete or quick-log a workout in the Track tab to build your permanent history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredHistory, key = { it.id }) { workout ->
                    HistoryRecordCard(
                        workout = workout,
                        onRequestDelete = { pendingDeleteWorkout = workout }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    pendingDeleteWorkout?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteWorkout = null },
            title = { Text("Delete Workout?") },
            text = { Text("Remove \"${target.title}\" from your permanent workout history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWorkout(target.id)
                        pendingDeleteWorkout = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteWorkout = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSaveToDriveDialog) {
        SaveToDriveDialog(
            profile = profile,
            workouts = workouts,
            dailyGoals = dailyGoals,
            customExercises = customExercises,
            onDismiss = { showSaveToDriveDialog = false },
            onResultMessage = onNotify
        )
    }
}

@Composable
private fun ReportStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRecordCard(
    workout: WorkoutRecord,
    onRequestDelete: () -> Unit
) {
    val category = WorkoutCategory.fromName(workout.category)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(category = category)
                        Text(
                            text = "${DateUtils.displayDate(workout.dateKey)} • ${workout.intensity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete workout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration: ${formatDurationMinutes(workout.durationSeconds)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${workout.caloriesBurned} kcal burned",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (!workout.notes.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = workout.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutCalendarCard(
    year: Int,
    monthZeroBased: Int,
    selectedDateKey: String,
    workoutsByDate: Map<String, List<WorkoutRecord>>,
    onSelectDate: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit
) {
    val cells = remember(year, monthZeroBased) {
        DateUtils.buildMonthGrid(year, monthZeroBased)
    }
    val weeks = remember(cells) { cells.chunked(7) }
    val monthTitle = remember(year, monthZeroBased) {
        DateUtils.monthTitle(year, monthZeroBased)
    }
    val activeDaysInMonth = remember(cells, workoutsByDate) {
        cells.filterNotNull().count { !workoutsByDate[it.dateKey].isNullOrEmpty() }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workout_calendar_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Workout Calendar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$activeDaysInMonth active day${if (activeDaysInMonth == 1) "" else "s"} this month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (selectedDateKey != DateUtils.todayKey()) {
                    TextButton(
                        onClick = onJumpToToday,
                        modifier = Modifier.testTag("calendar_today_button")
                    ) {
                        Text("Today")
                    }
                }
            }

            // Month Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("calendar_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("calendar_month_title")
                )
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("calendar_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Calendar Grid
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { cell ->
                            if (cell == null) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val hasWorkouts = !workoutsByDate[cell.dateKey].isNullOrEmpty()
                                val isSelected = cell.dateKey == selectedDateKey
                                CalendarDayItem(
                                    cell = cell,
                                    hasWorkouts = hasWorkouts,
                                    isSelected = isSelected,
                                    onClick = { onSelectDate(cell.dateKey) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "Workout completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Selected Date Summary Panel
            AnimatedContent(
                targetState = selectedDateKey,
                label = "calendar_day_summary"
            ) { dateKey ->
                val dayWorkouts = workoutsByDate[dateKey].orEmpty()
                val dayMinutes = dayWorkouts.sumOf { it.durationSeconds } / 60
                val dayCalories = dayWorkouts.sumOf { it.caloriesBurned }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_selected_summary")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = DateUtils.fullDisplayDate(dateKey),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (dayWorkouts.isEmpty()) {
                                        "Rest day • No workouts logged"
                                    } else {
                                        "${dayWorkouts.size} workout${if (dayWorkouts.size == 1) "" else "s"} completed"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (dayWorkouts.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${dayMinutes}m",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$dayCalories",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Text(
                                            text = "kcal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        dayWorkouts.forEach { workout ->
                            val category = WorkoutCategory.fromName(workout.category)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(category),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = workout.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${category.displayName} • ${workout.intensity}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${formatDurationMinutes(workout.durationSeconds)} • ${workout.caloriesBurned} kcal",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayItem(
    cell: CalendarDayCell,
    hasWorkouts: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasWorkouts -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        hasWorkouts -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderModifier = if (cell.isToday && !isSelected) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .testTag("calendar_day_${cell.dateKey}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = cell.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                fontWeight = if (isSelected || hasWorkouts || cell.isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasWorkouts && isSelected -> MaterialTheme.colorScheme.onPrimary
                            hasWorkouts -> MaterialTheme.colorScheme.primary
                            else -> bgColor
                        }
                    )
            )
        }
    }
}
