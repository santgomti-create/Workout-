package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CustomExercise
import com.example.data.model.DailyGoal
import com.example.data.model.DateUtils
import com.example.data.model.WorkoutRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FitTrackRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun profile_ensureAndObserve_createsAndReadsOwnerProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FitTrackRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureDefaultProfile(uid, "Alice Runner")
        }
        assertTrue(result.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("Alice Runner", profile?.displayName)
        assertEquals(uid, profile?.userId)
    }

    @Test
    fun workout_addObserveAndDelete_persistsUnderUserScope() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FitTrackRepository(firestore)

        val workout = WorkoutRecord(
            userId = uid,
            title = "Evening Jog",
            category = "Running",
            durationSeconds = 1200,
            caloriesBurned = 195,
            intensity = "Moderate",
            dateKey = DateUtils.todayKey(),
            notes = "Great pace"
        )
        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addWorkout(uid, workout)
        }
        assertTrue(createRes.isSuccess)
        val workoutId = createRes.getOrThrow()
        assertTrue(workoutId.isNotEmpty())

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkouts(uid).first { items -> items.any { it.id == workoutId } }
        }
        assertTrue(list.any { it.id == workoutId && it.title == "Evening Jog" })

        val deleteRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteWorkout(uid, workoutId)
        }
        assertTrue(deleteRes.isSuccess)
    }

    @Test
    fun dailyGoalsAndCustomExercises_crudFlow_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FitTrackRepository(firestore)

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addDailyGoal(
                uid,
                DailyGoal(
                    userId = uid,
                    title = "Hydration & Stretch",
                    targetValue = 20,
                    currentValue = 10,
                    unit = "min",
                    dateKey = DateUtils.todayKey(),
                    completed = false
                )
            ).getOrThrow()
        }

        val goals = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDailyGoals(uid).first { list -> list.any { it.id == goalId } }
        }
        val createdGoal = goals.first { it.id == goalId }
        assertFalse(createdGoal.completed)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateDailyGoalProgress(uid, createdGoal, 20).getOrThrow()
        }

        val updatedGoals = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDailyGoals(uid).first { list ->
                list.any { it.id == goalId && it.completed }
            }
        }
        assertTrue(updatedGoals.first { it.id == goalId }.completed)

        val exerciseId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addCustomExercise(
                uid,
                CustomExercise(
                    userId = uid,
                    name = "Kettlebell Swing",
                    category = "Gym",
                    instructions = "Hinge at hips and snap glutes forward.",
                    difficulty = "Intermediate",
                    caloriesPerMinute = 9,
                    defaultRestSeconds = 60
                )
            ).getOrThrow()
        }
        val customList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomExercises(uid).first { list -> list.any { it.id == exerciseId } }
        }
        assertTrue(customList.any { it.id == exerciseId && it.name == "Kettlebell Swing" })
    }

    @Test
    fun crossUserAccess_bobCannotReadOrWriteAliceWorkouts() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FitTrackRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addWorkout(
                aliceUid,
                WorkoutRecord(
                    userId = aliceUid,
                    title = "Alice Yoga",
                    category = "Yoga",
                    durationSeconds = 900,
                    caloriesBurned = 55,
                    intensity = "Beginner",
                    dateKey = DateUtils.todayKey()
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FitTrackRepository(firestore)
        val exception = assertFailsWith<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeWorkouts(aliceUid).first()
            }
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun unauthenticatedCaller_observeWorkouts_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FitTrackRepository(firestore)

        val exception = assertFailsWith<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWorkouts("some_uid").first()
            }
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit): T {
        try {
            block()
        } catch (t: Throwable) {
            if (t is T) return t
            throw t
        }
        fail("Expected ${T::class.java.simpleName} to be thrown")
        throw AssertionError("Unreachable")
    }

    private companion object {
        const val ALICE_EMAIL = "alice_fit@test.com"
        const val BOB_EMAIL = "bob_fit@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
