package com.example.data.export

import android.content.Context
import android.net.Uri
import com.example.data.model.CustomExercise
import com.example.data.model.DailyGoal
import com.example.data.model.DateUtils
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class ExportFormat(
    val title: String,
    val subtitle: String,
    val mimeType: String,
    val extension: String
) {
    CSV(
        title = "CSV Spreadsheet",
        subtitle = "Opens cleanly in Google Sheets & Drive",
        mimeType = "text/csv",
        extension = "csv"
    ),
    JSON(
        title = "JSON Full Backup",
        subtitle = "Workouts, daily goals, custom exercises & profile",
        mimeType = "application/json",
        extension = "json"
    )
}

object WorkoutExportHelper {

    fun suggestedFileName(format: ExportFormat): String {
        val date = DateUtils.todayKey()
        return when (format) {
            ExportFormat.CSV -> "fittrack_workouts_$date.csv"
            ExportFormat.JSON -> "fittrack_backup_$date.json"
        }
    }

    fun buildCsvContent(workouts: List<WorkoutRecord>): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Category,Duration (min),Duration (sec),Calories Burned,Intensity,Notes")
        workouts.forEach { w ->
            val mins = String.format(java.util.Locale.US, "%.1f", w.durationSeconds / 60.0)
            val row = listOf(
                escapeCsv(w.dateKey),
                escapeCsv(w.title),
                escapeCsv(w.category),
                mins,
                w.durationSeconds.toString(),
                w.caloriesBurned.toString(),
                escapeCsv(w.intensity),
                escapeCsv(w.notes.orEmpty())
            ).joinToString(",")
            sb.appendLine(row)
        }
        return sb.toString()
    }

    fun buildJsonBackupContent(
        profile: UserProfile?,
        workouts: List<WorkoutRecord>,
        goals: List<DailyGoal>,
        customExercises: List<CustomExercise>
    ): String {
        val root = JSONObject()
        root.put("app", "FitTrack")
        root.put("exportedAtDate", DateUtils.todayKey())

        if (profile != null) {
            root.put(
                "profile",
                JSONObject().apply {
                    put("displayName", profile.displayName)
                    put("age", profile.age)
                    put("fitnessGoal", profile.fitnessGoal)
                    put("preferredIntensity", profile.preferredIntensity)
                    put("preferredCategories", JSONArray(profile.preferredCategories))
                    put("dailyMinutesGoal", profile.dailyMinutesGoal)
                    put("dailyCaloriesGoal", profile.dailyCaloriesGoal)
                    put("weeklyWorkoutsGoal", profile.weeklyWorkoutsGoal)
                }
            )
        }

        val workoutsArr = JSONArray()
        workouts.forEach { w ->
            workoutsArr.put(
                JSONObject().apply {
                    put("dateKey", w.dateKey)
                    put("title", w.title)
                    put("category", w.category)
                    put("durationSeconds", w.durationSeconds)
                    put("caloriesBurned", w.caloriesBurned)
                    put("intensity", w.intensity)
                    if (!w.notes.isNullOrBlank()) put("notes", w.notes)
                }
            )
        }
        root.put("workouts", workoutsArr)

        val goalsArr = JSONArray()
        goals.forEach { g ->
            goalsArr.put(
                JSONObject().apply {
                    put("title", g.title)
                    put("targetValue", g.targetValue)
                    put("currentValue", g.currentValue)
                    put("unit", g.unit)
                    put("dateKey", g.dateKey)
                    put("completed", g.completed)
                }
            )
        }
        root.put("dailyGoals", goalsArr)

        val exercisesArr = JSONArray()
        customExercises.forEach { ex ->
            exercisesArr.put(
                JSONObject().apply {
                    put("name", ex.name)
                    put("category", ex.category)
                    put("instructions", ex.instructions)
                    put("difficulty", ex.difficulty)
                    put("caloriesPerMinute", ex.caloriesPerMinute)
                    put("defaultRestSeconds", ex.defaultRestSeconds)
                }
            )
        }
        root.put("customExercises", exercisesArr)

        return root.toString(2)
    }

    suspend fun writeContentToUri(
        context: Context,
        uri: Uri,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open destination file stream")
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ")
        return if (clean.contains(',') || clean.contains('"')) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }
}
