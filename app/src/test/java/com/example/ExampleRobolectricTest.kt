package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.WorkoutExportHelper
import com.example.data.model.CustomExercise
import com.example.data.model.DailyGoal
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutRecord
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FitTrack", appName)
  }

  @Test
  fun `buildJsonBackupContent serializes profile workouts goals and exercises`() {
    val jsonStr = WorkoutExportHelper.buildJsonBackupContent(
      profile = UserProfile(displayName = "Alex", age = 28, dailyMinutesGoal = 45),
      workouts = listOf(
        WorkoutRecord(
          id = "w1",
          title = "Evening Yoga",
          category = "Yoga",
          durationSeconds = 1500,
          caloriesBurned = 110,
          intensity = "Light",
          dateKey = "2026-11-12"
        )
      ),
      goals = listOf(
        DailyGoal(id = "g1", title = "Drink Water", targetValue = 8, currentValue = 6, unit = "cups")
      ),
      customExercises = listOf(
        CustomExercise(id = "e1", name = "Kettlebell Swing", category = "Gym")
      )
    )
    val parsed = JSONObject(jsonStr)
    assertEquals("FitTrack", parsed.getString("app"))
    assertEquals("Alex", parsed.getJSONObject("profile").getString("displayName"))
    assertEquals(1, parsed.getJSONArray("workouts").length())
    assertEquals("Evening Yoga", parsed.getJSONArray("workouts").getJSONObject(0).getString("title"))
    assertEquals(1, parsed.getJSONArray("dailyGoals").length())
    assertEquals(1, parsed.getJSONArray("customExercises").length())
  }
}
