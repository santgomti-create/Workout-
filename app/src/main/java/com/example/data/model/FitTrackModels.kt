package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class WorkoutCategory(
    val displayName: String,
    val caloriesPerMinute: Double,
    val defaultRestSeconds: Int,
    val isLibraryCategory: Boolean = true
) {
    WALKING("Walking", 4.2, 45),
    RUNNING("Running", 9.8, 60),
    CYCLING("Cycling", 7.8, 60),
    YOGA("Yoga", 3.2, 30),
    GYM("Gym", 6.5, 90),
    HOME_WORKOUT("Home Workout", 5.8, 60),
    CUSTOM("Custom", 5.5, 60, isLibraryCategory = false);

    companion object {
        fun fromName(name: String): WorkoutCategory {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: CUSTOM
        }

        val libraryCategories: List<WorkoutCategory>
            get() = entries.filter { it.isLibraryCategory }
    }
}

enum class WorkoutIntensity(
    val label: String,
    val multiplier: Double,
    val description: String
) {
    BEGINNER("Beginner", 0.85, "Comfortable pace, easy breathing"),
    MODERATE("Moderate", 1.0, "Steady effort, slight sweat"),
    VIGOROUS("Vigorous", 1.25, "Challenging pace, high energy");

    companion object {
        fun fromLabel(label: String): WorkoutIntensity {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: MODERATE
        }
    }
}

enum class FitnessGoalType(val label: String) {
    GENERAL_WELLNESS("General Wellness"),
    WEIGHT_LOSS("Weight Loss"),
    ENDURANCE("Endurance"),
    STRENGTH_TONE("Strength & Tone"),
    FLEXIBILITY("Flexibility");

    companion object {
        fun fromLabel(label: String): FitnessGoalType {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: GENERAL_WELLNESS
        }
    }
}

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromKey(key: String): ThemeMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "Athlete",
    val age: Int = 26,
    val fitnessGoal: String = FitnessGoalType.GENERAL_WELLNESS.label,
    val preferredIntensity: String = WorkoutIntensity.BEGINNER.label,
    val preferredCategories: List<String> = listOf("Walking", "Home Workout", "Yoga"),
    val dailyMinutesGoal: Int = 30,
    val dailyCaloriesGoal: Int = 250,
    val weeklyWorkoutsGoal: Int = 4,
    val themePreference: String = ThemeMode.SYSTEM.key,
    val privacyAnalyticsOptOut: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkoutRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = WorkoutCategory.WALKING.displayName,
    val durationSeconds: Int = 0,
    val caloriesBurned: Int = 0,
    val intensity: String = WorkoutIntensity.MODERATE.label,
    val dateKey: String = DateUtils.todayKey(),
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetValue: Int = 30,
    val currentValue: Int = 0,
    val unit: String = "min",
    val dateKey: String = DateUtils.todayKey(),
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomExercise(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = WorkoutCategory.HOME_WORKOUT.displayName,
    val instructions: String = "",
    val difficulty: String = "Beginner",
    val caloriesPerMinute: Int = 5,
    val defaultRestSeconds: Int = 60,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class LibraryExercise(
    val id: String,
    val name: String,
    val category: WorkoutCategory,
    val instructions: List<String>,
    val difficulty: String,
    val caloriesPerMinute: Int,
    val defaultRestSeconds: Int,
    val targetMuscles: String,
    val isCustom: Boolean = false
)

data class CalendarDayCell(
    val dateKey: String,
    val dayOfMonth: Int,
    val isToday: Boolean
)

object DateUtils {
    private val dateFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val shortDayFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEE", Locale.US)

    private val displayDateFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMM d, yyyy", Locale.US)

    private val monthYearFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMMM yyyy", Locale.US)

    private val fullDayDateFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)

    fun todayKey(): String = dateKeyDaysAgo(0)

    fun dateKeyDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateFormat.format(cal.time)
    }

    fun shortDayLabel(dateKey: String): String {
        val parsed = runCatching { dateFormat.parse(dateKey) }.getOrNull() ?: return dateKey
        return shortDayFormat.format(parsed)
    }

    fun displayDate(dateKey: String): String {
        if (dateKey == todayKey()) return "Today"
        if (dateKey == dateKeyDaysAgo(1)) return "Yesterday"
        val parsed = runCatching { dateFormat.parse(dateKey) }.getOrNull() ?: return dateKey
        return displayDateFormat.format(parsed)
    }

    fun fullDisplayDate(dateKey: String): String {
        val parsed = runCatching { dateFormat.parse(dateKey) }.getOrNull() ?: return dateKey
        val formatted = fullDayDateFormat.format(parsed)
        return when (dateKey) {
            todayKey() -> "Today • $formatted"
            dateKeyDaysAgo(1) -> "Yesterday • $formatted"
            else -> formatted
        }
    }

    fun lastNDaysKeys(n: Int): List<String> {
        return (n - 1 downTo 0).map { dateKeyDaysAgo(it) }
    }

    fun currentYearMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun shiftMonth(year: Int, monthZeroBased: Int, delta: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            add(Calendar.MONTH, delta)
        }
        return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun monthTitle(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
        }
        return monthYearFormat.format(cal.time)
    }

    fun buildMonthGrid(year: Int, monthZeroBased: Int): List<CalendarDayCell?> {
        val today = todayKey()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val leadingEmpty = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<CalendarDayCell?>()
        repeat(leadingEmpty) { cells.add(null) }
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val key = dateFormat.format(cal.time)
            cells.add(CalendarDayCell(dateKey = key, dayOfMonth = day, isToday = key == today))
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }
}

object CalorieEstimator {
    fun estimateCalories(
        category: WorkoutCategory,
        intensity: WorkoutIntensity,
        durationSeconds: Int,
        customCaloriesPerMin: Int? = null
    ): Int {
        val baseRate = customCaloriesPerMin?.toDouble() ?: category.caloriesPerMinute
        val minutes = durationSeconds / 60.0
        return (baseRate * intensity.multiplier * minutes).toInt().coerceAtLeast(if (durationSeconds >= 10) 1 else 0)
    }
}

object BuiltInExercises {
    val items: List<LibraryExercise> = listOf(
        LibraryExercise(
            id = "builtin_brisk_walk",
            name = "Brisk Power Walk",
            category = WorkoutCategory.WALKING,
            instructions = listOf(
                "Stand tall with relaxed shoulders and eyes looking ahead.",
                "Swing your arms naturally at a 90-degree angle.",
                "Land smoothly on your heel and roll through to your toes at a conversational pace."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 4,
            defaultRestSeconds = 45,
            targetMuscles = "Legs, Core, Heart"
        ),
        LibraryExercise(
            id = "builtin_incline_walk",
            name = "Incline Stride Walk",
            category = WorkoutCategory.WALKING,
            instructions = listOf(
                "Choose a gentle hill or set treadmill incline between 3% and 6%.",
                "Take slightly shorter steps and drive smoothly through your glutes.",
                "Keep your torso upright without leaning heavily forward."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 6,
            defaultRestSeconds = 60,
            targetMuscles = "Glutes, Calves, Endurance"
        ),
        LibraryExercise(
            id = "builtin_walk_run_intervals",
            name = "Beginner Walk-Jog Intervals",
            category = WorkoutCategory.RUNNING,
            instructions = listOf(
                "Warm up with 3 minutes of easy walking.",
                "Alternate 1 minute of light jogging with 2 minutes of brisk walking.",
                "Focus on soft, quiet footfalls and steady nasal-mouth breathing."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 8,
            defaultRestSeconds = 60,
            targetMuscles = "Cardio, Quads, Calves"
        ),
        LibraryExercise(
            id = "builtin_steady_jog",
            name = "Steady Aerobic Jog",
            category = WorkoutCategory.RUNNING,
            instructions = listOf(
                "Maintain a comfortable pace where you can speak in short sentences.",
                "Keep cadence light and quick rather than over-striding.",
                "Cool down with 3 minutes of slow walking afterwards."
            ),
            difficulty = "Intermediate",
            caloriesPerMinute = 10,
            defaultRestSeconds = 60,
            targetMuscles = "Cardio, Full Lower Body"
        ),
        LibraryExercise(
            id = "builtin_easy_spin",
            name = "Smooth Cadence Spin",
            category = WorkoutCategory.CYCLING,
            instructions = listOf(
                "Adjust saddle height so your knee has a slight bend at the bottom of the stroke.",
                "Set light-to-moderate resistance and keep a smooth circular pedal stroke.",
                "Keep shoulders relaxed away from your ears."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 7,
            defaultRestSeconds = 45,
            targetMuscles = "Quads, Hamstrings, Cardio"
        ),
        LibraryExercise(
            id = "builtin_hill_climb_ride",
            name = "Seated Climb Intervals",
            category = WorkoutCategory.CYCLING,
            instructions = listOf(
                "Increase bike resistance to feel like riding up a steady hill.",
                "Push and pull smoothly through 45-second efforts followed by 45 seconds of easy spin.",
                "Engage your core to stay stable on the saddle."
            ),
            difficulty = "Intermediate",
            caloriesPerMinute = 9,
            defaultRestSeconds = 60,
            targetMuscles = "Glutes, Quads, Stamina"
        ),
        LibraryExercise(
            id = "builtin_sun_salutation",
            name = "Gentle Sun Salutation Flow",
            category = WorkoutCategory.YOGA,
            instructions = listOf(
                "Start in Mountain Pose, inhale and reach arms overhead.",
                "Exhale to fold forward softly with bent knees if hamstrings feel tight.",
                "Step back into tabletop or plank, then flow into Cat-Cow or Downward Dog."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 3,
            defaultRestSeconds = 30,
            targetMuscles = "Spine, Shoulders, Flexibility"
        ),
        LibraryExercise(
            id = "builtin_mobility_stretch",
            name = "Hip & Spine Mobility Reset",
            category = WorkoutCategory.YOGA,
            instructions = listOf(
                "Hold a low lunge for 30 seconds per side while keeping hips square.",
                "Transition to Child's Pose with deep belly breaths for 45 seconds.",
                "Finish with gentle seated spinal twists on each side."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 3,
            defaultRestSeconds = 30,
            targetMuscles = "Hips, Lower Back, Balance"
        ),
        LibraryExercise(
            id = "builtin_goblet_squat",
            name = "Goblet Squat",
            category = WorkoutCategory.GYM,
            instructions = listOf(
                "Hold a light dumbbell or kettlebell vertically against your chest.",
                "Stand with feet shoulder-width apart and toes slightly turned out.",
                "Lower hips back and down until thighs are comfortable, then press evenly through heels."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 7,
            defaultRestSeconds = 90,
            targetMuscles = "Quads, Glutes, Core"
        ),
        LibraryExercise(
            id = "builtin_lat_pulldown",
            name = "Seated Lat Pulldown",
            category = WorkoutCategory.GYM,
            instructions = listOf(
                "Grip the bar slightly wider than shoulder width and lock thighs under the pad.",
                "Pull elbows down toward your sides until the bar reaches upper chest level.",
                "Control the weight slowly back up without shrugging your shoulders."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 6,
            defaultRestSeconds = 75,
            targetMuscles = "Upper Back, Biceps, Posture"
        ),
        LibraryExercise(
            id = "builtin_dumbbell_press",
            name = "Incline Dumbbell Chest Press",
            category = WorkoutCategory.GYM,
            instructions = listOf(
                "Lie on a slight incline bench with feet planted firmly on the floor.",
                "Press dumbbells up smoothly over your chest without locking elbows.",
                "Lower slowly for 2 seconds until elbows form roughly a 45-degree angle to torso."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 6,
            defaultRestSeconds = 90,
            targetMuscles = "Chest, Shoulders, Triceps"
        ),
        LibraryExercise(
            id = "builtin_bodyweight_circuit",
            name = "Beginner Bodyweight Circuit",
            category = WorkoutCategory.HOME_WORKOUT,
            instructions = listOf(
                "Perform 10 bodyweight squats, 8 incline push-ups (against wall or couch), and a 20-second plank.",
                "Move with controlled form rather than rushing reps.",
                "Rest 60 seconds between rounds and repeat 3 times."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 6,
            defaultRestSeconds = 60,
            targetMuscles = "Full Body, Core"
        ),
        LibraryExercise(
            id = "builtin_glute_bridge",
            name = "Floor Glute Bridge & March",
            category = WorkoutCategory.HOME_WORKOUT,
            instructions = listOf(
                "Lie on your back with knees bent and feet hip-width apart near your hips.",
                "Press through your heels to lift hips until shoulders, hips, and knees align.",
                "Pause 2 seconds at the top, squeeze glutes, and lower smoothly."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 5,
            defaultRestSeconds = 45,
            targetMuscles = "Glutes, Hamstrings, Lower Back"
        ),
        LibraryExercise(
            id = "builtin_dead_bug",
            name = "Core Stability Dead Bug",
            category = WorkoutCategory.HOME_WORKOUT,
            instructions = listOf(
                "Lie flat on your back with arms pointing to the ceiling and knees bent at 90 degrees.",
                "Gently press your lower back into the mat.",
                "Slowly extend opposite arm and leg toward the floor, then return and switch sides."
            ),
            difficulty = "Beginner",
            caloriesPerMinute = 4,
            defaultRestSeconds = 45,
            targetMuscles = "Deep Core, Stability"
        )
    )
}
