package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CustomExercise
import com.example.data.model.DailyGoal
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutRecord
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FitTrackRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun workoutsCol(userId: String) = userDoc(userId).collection("workouts")
    private fun goalsCol(userId: String) = userDoc(userId).collection("daily_goals")
    private fun customExercisesCol(userId: String) = userDoc(userId).collection("custom_exercises")

    fun observeProfile(userId: String): Flow<UserProfile?> {
        val docRef = userDoc(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    fun observeWorkouts(userId: String): Flow<List<WorkoutRecord>> {
        val colRef = workoutsCol(userId)
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(WorkoutRecord::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path)
                throw error
            }
    }

    fun observeDailyGoals(userId: String): Flow<List<DailyGoal>> {
        val colRef = goalsCol(userId)
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(DailyGoal::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path)
                throw error
            }
    }

    fun observeCustomExercises(userId: String): Flow<List<CustomExercise>> {
        val colRef = customExercisesCol(userId)
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustomExercise::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, colRef.path)
                throw error
            }
    }

    suspend fun ensureDefaultProfile(userId: String, defaultName: String): Result<Unit> {
        val docRef = userDoc(userId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = defaultName.trim().ifBlank { "FitTrack Athlete" }.take(60)
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "age" to 26,
                    "fitnessGoal" to "General Wellness",
                    "preferredIntensity" to "Beginner",
                    "preferredCategories" to listOf("Walking", "Home Workout", "Yoga"),
                    "dailyMinutesGoal" to 30,
                    "dailyCaloriesGoal" to 250,
                    "weeklyWorkoutsGoal" to 4,
                    "themePreference" to "system",
                    "privacyAnalyticsOptOut" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveProfile(userId: String, profile: UserProfile): Result<Unit> {
        val docRef = userDoc(userId)
        return try {
            val existing = docRef.get().await()
            val safeName = profile.displayName.trim().ifBlank { "FitTrack Athlete" }.take(60)
            val safeAge = profile.age.coerceIn(13, 120)
            val safeCategories = profile.preferredCategories.take(10)
            if (existing.exists()) {
                val updates = mapOf(
                    "displayName" to safeName,
                    "age" to safeAge,
                    "fitnessGoal" to profile.fitnessGoal,
                    "preferredIntensity" to profile.preferredIntensity,
                    "preferredCategories" to safeCategories,
                    "dailyMinutesGoal" to profile.dailyMinutesGoal.coerceIn(5, 600),
                    "dailyCaloriesGoal" to profile.dailyCaloriesGoal.coerceIn(50, 5000),
                    "weeklyWorkoutsGoal" to profile.weeklyWorkoutsGoal.coerceIn(1, 14),
                    "themePreference" to profile.themePreference,
                    "privacyAnalyticsOptOut" to profile.privacyAnalyticsOptOut,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "age" to safeAge,
                    "fitnessGoal" to profile.fitnessGoal,
                    "preferredIntensity" to profile.preferredIntensity,
                    "preferredCategories" to safeCategories,
                    "dailyMinutesGoal" to profile.dailyMinutesGoal.coerceIn(5, 600),
                    "dailyCaloriesGoal" to profile.dailyCaloriesGoal.coerceIn(50, 5000),
                    "weeklyWorkoutsGoal" to profile.weeklyWorkoutsGoal.coerceIn(1, 14),
                    "themePreference" to profile.themePreference,
                    "privacyAnalyticsOptOut" to profile.privacyAnalyticsOptOut,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addWorkout(userId: String, workout: WorkoutRecord): Result<String> {
        val docRef = workoutsCol(userId).document()
        return try {
            val payload = mapOf(
                "userId" to userId,
                "title" to workout.title.trim().ifBlank { workout.category }.take(80),
                "category" to workout.category,
                "durationSeconds" to workout.durationSeconds.coerceIn(1, 86400),
                "caloriesBurned" to workout.caloriesBurned.coerceIn(0, 10000),
                "intensity" to workout.intensity,
                "dateKey" to workout.dateKey,
                "notes" to workout.notes?.trim()?.takeIf { it.isNotEmpty() }?.take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(userId: String, workoutId: String): Result<Unit> {
        val docRef = workoutsCol(userId).document(workoutId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addDailyGoal(userId: String, goal: DailyGoal): Result<String> {
        val docRef = goalsCol(userId).document()
        return try {
            val target = goal.targetValue.coerceIn(1, 10000)
            val current = goal.currentValue.coerceIn(0, 10000)
            val payload = mapOf(
                "userId" to userId,
                "title" to goal.title.trim().take(80),
                "targetValue" to target,
                "currentValue" to current,
                "unit" to goal.unit.trim().ifBlank { "min" }.take(20),
                "dateKey" to goal.dateKey,
                "completed" to (current >= target || goal.completed),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDailyGoalProgress(userId: String, goal: DailyGoal, newValue: Int): Result<Unit> {
        val docRef = goalsCol(userId).document(goal.id)
        return try {
            val clamped = newValue.coerceIn(0, 10000)
            val updates = mapOf(
                "currentValue" to clamped,
                "completed" to (clamped >= goal.targetValue),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDailyGoal(userId: String, goalId: String): Result<Unit> {
        val docRef = goalsCol(userId).document(goalId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addCustomExercise(userId: String, exercise: CustomExercise): Result<String> {
        val docRef = customExercisesCol(userId).document()
        return try {
            val payload = mapOf(
                "userId" to userId,
                "name" to exercise.name.trim().take(80),
                "category" to exercise.category,
                "instructions" to exercise.instructions.trim().take(500),
                "difficulty" to exercise.difficulty,
                "caloriesPerMinute" to exercise.caloriesPerMinute.coerceIn(1, 30),
                "defaultRestSeconds" to exercise.defaultRestSeconds.coerceIn(5, 300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomExercise(userId: String, exerciseId: String): Result<Unit> {
        val docRef = customExercisesCol(userId).document(exerciseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearAllUserData(userId: String): Result<Unit> {
        return try {
            val collections = listOf(
                workoutsCol(userId),
                goalsCol(userId),
                customExercisesCol(userId)
            )
            for (col in collections) {
                val snap = col.get().await()
                val chunks = snap.documents.chunked(400)
                for (chunk in chunks) {
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            Result.failure(e)
        }
    }

    private fun logFlowError(error: Throwable, operationType: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
