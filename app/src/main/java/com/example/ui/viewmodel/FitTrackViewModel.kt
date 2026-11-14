package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalorieEstimator
import com.example.data.model.CustomExercise
import com.example.data.model.DailyGoal
import com.example.data.model.DateUtils
import com.example.data.model.FitnessGoalType
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutIntensity
import com.example.data.model.WorkoutRecord
import com.example.data.preferences.ThemePreferencesRepository
import com.example.data.repository.FitTrackRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ActiveWorkoutTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val selectedCategory: WorkoutCategory = WorkoutCategory.WALKING,
    val workoutTitle: String = "Morning Walk",
    val intensity: WorkoutIntensity = WorkoutIntensity.BEGINNER,
    val notes: String = "",
    val customCaloriesPerMin: Int? = null
) {
    val estimatedCalories: Int
        get() = CalorieEstimator.estimateCalories(
            category = selectedCategory,
            intensity = intensity,
            durationSeconds = elapsedSeconds,
            customCaloriesPerMin = customCaloriesPerMin
        )
}

data class RestTimerState(
    val totalSeconds: Int = 60,
    val remainingSeconds: Int = 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class FitTrackViewModel(
    private val repository: FitTrackRepository,
    private val themePrefs: ThemePreferencesRepository,
    private val currentUserId: String,
    private val defaultDisplayName: String
) : ViewModel() {

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            UiState.Success(
                profile ?: UserProfile(
                    userId = currentUserId,
                    displayName = defaultDisplayName.ifBlank { "FitTrack Athlete" }
                )
            )
        }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load profile")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val workoutsState: StateFlow<UiState<List<WorkoutRecord>>> = repository.observeWorkouts(currentUserId)
        .map<List<WorkoutRecord>, UiState<List<WorkoutRecord>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load workouts")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val dailyGoalsState: StateFlow<UiState<List<DailyGoal>>> = repository.observeDailyGoals(currentUserId)
        .map<List<DailyGoal>, UiState<List<DailyGoal>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load daily goals")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val customExercisesState: StateFlow<UiState<List<CustomExercise>>> =
        repository.observeCustomExercises(currentUserId)
            .map<List<CustomExercise>, UiState<List<CustomExercise>>> { UiState.Success(it) }
            .catch { error ->
                emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load exercises")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _timerState = MutableStateFlow(ActiveWorkoutTimerState())
    val timerState: StateFlow<ActiveWorkoutTimerState> = _timerState.asStateFlow()

    private val _restTimerState = MutableStateFlow(RestTimerState())
    val restTimerState: StateFlow<RestTimerState> = _restTimerState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var workoutTickerJob: Job? = null
    private var restTickerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfile(currentUserId, defaultDisplayName)
        }
    }

    private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, fallback, root)
        return root.message ?: fallback
    }

    fun notifyMessage(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    // --- Live Workout Timer Actions ---

    fun selectWorkoutCategory(category: WorkoutCategory) {
        _timerState.update { state ->
            val updatedTitle = if (state.workoutTitle.isBlank() ||
                WorkoutCategory.entries.any { "${it.displayName} Session" == state.workoutTitle } ||
                state.workoutTitle == "Morning Walk"
            ) {
                "${category.displayName} Session"
            } else {
                state.workoutTitle
            }
            state.copy(
                selectedCategory = category,
                workoutTitle = updatedTitle,
                customCaloriesPerMin = null
            )
        }
    }

    fun setWorkoutTitle(title: String) {
        _timerState.update { it.copy(workoutTitle = title.take(80)) }
    }

    fun setWorkoutIntensity(intensity: WorkoutIntensity) {
        _timerState.update { it.copy(intensity = intensity) }
    }

    fun setWorkoutNotes(notes: String) {
        _timerState.update { it.copy(notes = notes.take(300)) }
    }

    fun prepareWorkoutFromExercise(
        title: String,
        category: WorkoutCategory,
        caloriesPerMin: Int,
        restSeconds: Int
    ) {
        _timerState.update {
            it.copy(
                selectedCategory = category,
                workoutTitle = title,
                customCaloriesPerMin = caloriesPerMin
            )
        }
        setRestDuration(restSeconds)
    }

    fun startWorkout() {
        if (_timerState.value.isRunning && !_timerState.value.isPaused) return
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        startWorkoutTicker()
    }

    fun pauseWorkout() {
        if (!_timerState.value.isRunning) return
        workoutTickerJob?.cancel()
        _timerState.update { it.copy(isPaused = true) }
    }

    fun resumeWorkout() {
        if (!_timerState.value.isRunning || !_timerState.value.isPaused) return
        _timerState.update { it.copy(isPaused = false) }
        startWorkoutTicker()
    }

    private fun startWorkoutTicker() {
        workoutTickerJob?.cancel()
        workoutTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _timerState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun discardWorkout() {
        workoutTickerJob?.cancel()
        _timerState.update {
            ActiveWorkoutTimerState(
                selectedCategory = it.selectedCategory,
                workoutTitle = "${it.selectedCategory.displayName} Session",
                intensity = it.intensity
            )
        }
        _snackbarMessage.tryEmit("Workout timer reset")
    }

    fun stopAndSaveWorkout() {
        val current = _timerState.value
        val duration = current.elapsedSeconds.coerceAtLeast(1)
        workoutTickerJob?.cancel()

        val calories = CalorieEstimator.estimateCalories(
            category = current.selectedCategory,
            intensity = current.intensity,
            durationSeconds = duration,
            customCaloriesPerMin = current.customCaloriesPerMin
        )
        val record = WorkoutRecord(
            userId = currentUserId,
            title = current.workoutTitle.ifBlank { "${current.selectedCategory.displayName} Session" },
            category = current.selectedCategory.displayName,
            durationSeconds = duration,
            caloriesBurned = calories,
            intensity = current.intensity.label,
            dateKey = DateUtils.todayKey(),
            notes = current.notes.trim().takeIf { it.isNotEmpty() }
        )

        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.addWorkout(currentUserId, record)
            _isSaving.value = false
            if (res.isSuccess) {
                _timerState.value = ActiveWorkoutTimerState(
                    selectedCategory = current.selectedCategory,
                    workoutTitle = "${current.selectedCategory.displayName} Session",
                    intensity = current.intensity
                )
                _snackbarMessage.emit("Saved ${record.title} (+${calories} kcal)")
            } else {
                _snackbarMessage.emit("Could not save workout. Please try again.")
            }
        }
    }

    fun logQuickWorkout(
        title: String,
        category: WorkoutCategory,
        durationMinutes: Int,
        intensity: WorkoutIntensity,
        notes: String
    ) {
        val durationSec = (durationMinutes.coerceIn(1, 600)) * 60
        val calories = CalorieEstimator.estimateCalories(category, intensity, durationSec)
        val record = WorkoutRecord(
            userId = currentUserId,
            title = title.trim().ifBlank { "${category.displayName} Session" },
            category = category.displayName,
            durationSeconds = durationSec,
            caloriesBurned = calories,
            intensity = intensity.label,
            dateKey = DateUtils.todayKey(),
            notes = notes.trim().takeIf { it.isNotEmpty() }
        )

        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.addWorkout(currentUserId, record)
            _isSaving.value = false
            if (res.isSuccess) {
                _snackbarMessage.emit("Logged ${record.title} (${durationMinutes}m, ${calories} kcal)")
            } else {
                _snackbarMessage.emit("Failed to log workout")
            }
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            val res = repository.deleteWorkout(currentUserId, workoutId)
            if (res.isSuccess) {
                _snackbarMessage.emit("Workout deleted")
            } else {
                _snackbarMessage.emit("Could not delete workout")
            }
        }
    }

    // --- Rest Timer Actions ---

    fun setRestDuration(seconds: Int) {
        val clamped = seconds.coerceIn(5, 300)
        restTickerJob?.cancel()
        _restTimerState.value = RestTimerState(
            totalSeconds = clamped,
            remainingSeconds = clamped,
            isRunning = false,
            isFinished = false
        )
    }

    fun startRestTimer(seconds: Int? = null) {
        if (seconds != null) {
            val clamped = seconds.coerceIn(5, 300)
            _restTimerState.value = RestTimerState(
                totalSeconds = clamped,
                remainingSeconds = clamped,
                isRunning = true,
                isFinished = false
            )
        } else {
            val cur = _restTimerState.value
            val startFrom = if (cur.remainingSeconds <= 0) cur.totalSeconds else cur.remainingSeconds
            _restTimerState.value = cur.copy(
                remainingSeconds = startFrom,
                isRunning = true,
                isFinished = false
            )
        }

        restTickerJob?.cancel()
        restTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val next = _restTimerState.value.remainingSeconds - 1
                if (next <= 0) {
                    _restTimerState.update {
                        it.copy(remainingSeconds = 0, isRunning = false, isFinished = true)
                    }
                    _snackbarMessage.emit("Rest complete! Ready for your next set.")
                    break
                } else {
                    _restTimerState.update { it.copy(remainingSeconds = next) }
                }
            }
        }
    }

    fun pauseRestTimer() {
        restTickerJob?.cancel()
        _restTimerState.update { it.copy(isRunning = false) }
    }

    fun resetRestTimer() {
        restTickerJob?.cancel()
        _restTimerState.update {
            it.copy(remainingSeconds = it.totalSeconds, isRunning = false, isFinished = false)
        }
    }

    // --- Daily Goals Actions ---

    fun addDailyGoal(title: String, targetValue: Int, unit: String) {
        val goal = DailyGoal(
            userId = currentUserId,
            title = title.trim(),
            targetValue = targetValue.coerceIn(1, 10000),
            currentValue = 0,
            unit = unit.trim().ifBlank { "min" },
            dateKey = DateUtils.todayKey(),
            completed = false
        )
        viewModelScope.launch {
            val res = repository.addDailyGoal(currentUserId, goal)
            if (res.isSuccess) {
                _snackbarMessage.emit("Daily goal added")
            } else {
                _snackbarMessage.emit("Could not add goal")
            }
        }
    }

    fun incrementDailyGoal(goal: DailyGoal, delta: Int) {
        val nextValue = (goal.currentValue + delta).coerceIn(0, goal.targetValue * 2)
        viewModelScope.launch {
            repository.updateDailyGoalProgress(currentUserId, goal, nextValue)
        }
    }

    fun toggleDailyGoalComplete(goal: DailyGoal) {
        val nextValue = if (goal.completed) 0 else goal.targetValue
        viewModelScope.launch {
            repository.updateDailyGoalProgress(currentUserId, goal, nextValue)
        }
    }

    fun deleteDailyGoal(goalId: String) {
        viewModelScope.launch {
            val res = repository.deleteDailyGoal(currentUserId, goalId)
            if (res.isSuccess) {
                _snackbarMessage.emit("Goal removed")
            }
        }
    }

    // --- Custom Exercise Actions ---

    fun addCustomExercise(
        name: String,
        category: WorkoutCategory,
        instructions: String,
        difficulty: String,
        caloriesPerMinute: Int,
        defaultRestSeconds: Int
    ) {
        val libCat = if (category.isLibraryCategory) category else WorkoutCategory.HOME_WORKOUT
        val exercise = CustomExercise(
            userId = currentUserId,
            name = name.trim(),
            category = libCat.displayName,
            instructions = instructions.trim(),
            difficulty = difficulty,
            caloriesPerMinute = caloriesPerMinute.coerceIn(1, 30),
            defaultRestSeconds = defaultRestSeconds.coerceIn(5, 300)
        )
        viewModelScope.launch {
            val res = repository.addCustomExercise(currentUserId, exercise)
            if (res.isSuccess) {
                _snackbarMessage.emit("Added ${exercise.name} to Exercise Library")
            } else {
                _snackbarMessage.emit("Could not save custom exercise")
            }
        }
    }

    fun deleteCustomExercise(exerciseId: String) {
        viewModelScope.launch {
            val res = repository.deleteCustomExercise(currentUserId, exerciseId)
            if (res.isSuccess) {
                _snackbarMessage.emit("Custom exercise removed")
            }
        }
    }

    // --- Profile, Preferences & Privacy Actions ---

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePrefs.setThemeMode(mode)
            val currentProfile = (profileState.value as? UiState.Success)?.data
            if (currentProfile != null) {
                repository.saveProfile(
                    currentUserId,
                    currentProfile.copy(themePreference = mode.key)
                )
            }
        }
    }

    fun saveProfile(
        displayName: String,
        age: Int,
        fitnessGoal: FitnessGoalType,
        preferredIntensity: WorkoutIntensity,
        preferredCategories: List<String>,
        dailyMinutesGoal: Int,
        dailyCaloriesGoal: Int,
        weeklyWorkoutsGoal: Int,
        themeMode: ThemeMode,
        privacyAnalyticsOptOut: Boolean
    ) {
        val currentProfile = (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        val updated = currentProfile.copy(
            userId = currentUserId,
            displayName = displayName.trim().ifBlank { "FitTrack Athlete" },
            age = age.coerceIn(13, 120),
            fitnessGoal = fitnessGoal.label,
            preferredIntensity = preferredIntensity.label,
            preferredCategories = preferredCategories.take(10),
            dailyMinutesGoal = dailyMinutesGoal.coerceIn(5, 600),
            dailyCaloriesGoal = dailyCaloriesGoal.coerceIn(50, 5000),
            weeklyWorkoutsGoal = weeklyWorkoutsGoal.coerceIn(1, 14),
            themePreference = themeMode.key,
            privacyAnalyticsOptOut = privacyAnalyticsOptOut
        )
        viewModelScope.launch {
            _isSaving.value = true
            themePrefs.setThemeMode(themeMode)
            val res = repository.saveProfile(currentUserId, updated)
            _isSaving.value = false
            if (res.isSuccess) {
                _snackbarMessage.emit("Profile & fitness goals saved")
            } else {
                _snackbarMessage.emit("Failed to save profile")
            }
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.clearAllUserData(currentUserId)
            _isSaving.value = false
            if (res.isSuccess) {
                _snackbarMessage.emit("All workout records, goals, and custom exercises cleared")
            } else {
                _snackbarMessage.emit("Could not clear data. Please try again.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        workoutTickerJob?.cancel()
        restTickerJob?.cancel()
    }

    private companion object {
        const val TAG = "FitTrackVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
