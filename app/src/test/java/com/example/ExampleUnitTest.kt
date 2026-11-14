package com.example

import com.example.data.export.ExportFormat
import com.example.data.export.WorkoutExportHelper
import com.example.data.model.CalorieEstimator
import com.example.data.model.DateUtils
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutIntensity
import com.example.data.model.WorkoutRecord
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun buildMonthGrid_producesAlignedWeeksWithCorrectDayCount() {
        // November 2026 has 30 days and starts on Sunday
        val grid = DateUtils.buildMonthGrid(2026, Calendar.NOVEMBER)
        assertEquals(0, grid.size % 7)
        val nonNullDays = grid.filterNotNull()
        assertEquals(30, nonNullDays.size)
        assertEquals("2026-11-01", nonNullDays.first().dateKey)
        assertEquals("2026-11-30", nonNullDays.last().dateKey)
    }

    @Test
    fun shiftMonth_crossesYearBoundariesCorrectly() {
        assertEquals(2025 to Calendar.DECEMBER, DateUtils.shiftMonth(2026, Calendar.JANUARY, -1))
        assertEquals(2027 to Calendar.JANUARY, DateUtils.shiftMonth(2026, Calendar.DECEMBER, 1))
    }

    @Test
    fun calorieEstimator_scalesWithDurationAndIntensity() {
        val moderate = CalorieEstimator.estimateCalories(
            category = WorkoutCategory.RUNNING,
            intensity = WorkoutIntensity.MODERATE,
            durationSeconds = 1800
        )
        val vigorous = CalorieEstimator.estimateCalories(
            category = WorkoutCategory.RUNNING,
            intensity = WorkoutIntensity.VIGOROUS,
            durationSeconds = 1800
        )
        assertTrue(moderate > 200)
        assertTrue(vigorous > moderate)
    }

    @Test
    fun buildCsvContent_includesHeadersAndEscapesCommasAndQuotes() {
        val workouts = listOf(
            WorkoutRecord(
                id = "w1",
                title = "Morning Run, Park Loop",
                category = "Running",
                durationSeconds = 1800,
                caloriesBurned = 320,
                intensity = "Moderate",
                notes = "Felt \"strong\" today",
                dateKey = "2026-11-10"
            )
        )
        val csv = WorkoutExportHelper.buildCsvContent(workouts)
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("Date,Title,Category,Duration (min)"))
        assertTrue(lines[1].contains("\"Morning Run, Park Loop\""))
        assertTrue(lines[1].contains("\"Felt \"\"strong\"\" today\""))
        assertTrue(lines[1].contains(",30.0,1800,320,"))
    }

    @Test
    fun suggestedFileName_usesExpectedExtension() {
        assertTrue(WorkoutExportHelper.suggestedFileName(ExportFormat.CSV).endsWith(".csv"))
        assertTrue(WorkoutExportHelper.suggestedFileName(ExportFormat.JSON).endsWith(".json"))
    }
}
