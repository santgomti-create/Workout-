package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ThemeMode
import com.example.data.preferences.ThemePreferencesRepository
import com.example.data.repository.FitTrackRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExerciseLibraryScreen
import com.example.ui.screens.HistoryReportsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TrackWorkoutScreen
import com.example.ui.theme.FitTrackTheme
import com.example.ui.viewmodel.FitTrackViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private object FitTrackRoutes {
    const val DASHBOARD = "dashboard"
    const val TRACK = "track"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val PROFILE = "profile"
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    NavDestinationItem(
        route = FitTrackRoutes.DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_dashboard"
    ),
    NavDestinationItem(
        route = FitTrackRoutes.TRACK,
        labelRes = R.string.nav_track,
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer,
        testTag = "nav_track"
    ),
    NavDestinationItem(
        route = FitTrackRoutes.LIBRARY,
        labelRes = R.string.nav_library,
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter,
        testTag = "nav_library"
    ),
    NavDestinationItem(
        route = FitTrackRoutes.HISTORY,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_history"
    ),
    NavDestinationItem(
        route = FitTrackRoutes.PROFILE,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_profile"
    )
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themePrefs = remember { ThemePreferencesRepository(context.applicationContext) }
            val themeMode by themePrefs.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )

            FitTrackTheme(themeMode = themeMode) {
                FitTrackRoot(
                    themePrefs = themePrefs,
                    currentThemeMode = themeMode
                )
            }
        }
    }
}

@Composable
private fun FitTrackRoot(
    themePrefs: ThemePreferencesRepository,
    currentThemeMode: ThemeMode
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var hasAttemptedAutoSignIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasAttemptedAutoSignIn && auth.currentUser == null) {
            hasAttemptedAutoSignIn = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {}
        )
    } else {
        val uid = user.uid
        val defaultName = user.displayName ?: "FitTrack Athlete"
        val userEmail = user.email
        key(uid) {
            FitTrackMainApp(
                currentUserId = uid,
                defaultDisplayName = defaultName,
                userEmail = userEmail,
                themePrefs = themePrefs,
                currentThemeMode = currentThemeMode,
                onSignOut = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun FitTrackMainApp(
    currentUserId: String,
    defaultDisplayName: String,
    userEmail: String?,
    themePrefs: ThemePreferencesRepository,
    currentThemeMode: ThemeMode,
    onSignOut: () -> Unit
) {
    val viewModel: FitTrackViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                } as Application
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FitTrackViewModel(
                    repository = FitTrackRepository(db),
                    themePrefs = themePrefs,
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val workoutsState by viewModel.workoutsState.collectAsStateWithLifecycle()
    val dailyGoalsState by viewModel.dailyGoalsState.collectAsStateWithLifecycle()
    val customExercisesState by viewModel.customExercisesState.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val restTimerState by viewModel.restTimerState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    // Sync profile theme preference if stored in cloud
    LaunchedEffect(profileState) {
        val cloudTheme = (profileState as? UiState.Success)?.data?.themePreference
        if (cloudTheme != null) {
            val mode = ThemeMode.fromKey(cloudTheme)
            if (mode != currentThemeMode) {
                themePrefs.setThemeMode(mode)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: FitTrackRoutes.DASHBOARD

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = FitTrackRoutes.DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(FitTrackRoutes.DASHBOARD) {
                        DashboardScreen(
                            profileState = profileState,
                            workoutsState = workoutsState,
                            dailyGoalsState = dailyGoalsState,
                            isWorkoutTimerActive = timerState.isRunning || timerState.elapsedSeconds > 0,
                            onNavigateToTrack = { navigateToTab(FitTrackRoutes.TRACK) },
                            onNavigateToHistory = { navigateToTab(FitTrackRoutes.HISTORY) },
                            onAddDailyGoal = viewModel::addDailyGoal,
                            onIncrementGoal = viewModel::incrementDailyGoal,
                            onToggleGoalComplete = viewModel::toggleDailyGoalComplete,
                            onDeleteGoal = viewModel::deleteDailyGoal
                        )
                    }
                    composable(FitTrackRoutes.TRACK) {
                        TrackWorkoutScreen(
                            timerState = timerState,
                            isSaving = isSaving,
                            onSelectCategory = viewModel::selectWorkoutCategory,
                            onSetTitle = viewModel::setWorkoutTitle,
                            onSetIntensity = viewModel::setWorkoutIntensity,
                            onSetNotes = viewModel::setWorkoutNotes,
                            onStartWorkout = viewModel::startWorkout,
                            onPauseWorkout = viewModel::pauseWorkout,
                            onResumeWorkout = viewModel::resumeWorkout,
                            onStopAndSave = viewModel::stopAndSaveWorkout,
                            onDiscard = viewModel::discardWorkout,
                            onQuickLogWorkout = viewModel::logQuickWorkout,
                            onAddCustomExercise = viewModel::addCustomExercise
                        )
                    }
                    composable(FitTrackRoutes.LIBRARY) {
                        ExerciseLibraryScreen(
                            customExercisesState = customExercisesState,
                            restTimerState = restTimerState,
                            onSetRestDuration = viewModel::setRestDuration,
                            onStartRestTimer = viewModel::startRestTimer,
                            onPauseRestTimer = viewModel::pauseRestTimer,
                            onResetRestTimer = viewModel::resetRestTimer,
                            onAddCustomExercise = viewModel::addCustomExercise,
                            onDeleteCustomExercise = viewModel::deleteCustomExercise,
                            onStartExerciseWorkout = { ex ->
                                viewModel.prepareWorkoutFromExercise(
                                    title = ex.name,
                                    category = ex.category,
                                    caloriesPerMin = ex.caloriesPerMinute,
                                    restSeconds = ex.defaultRestSeconds
                                )
                                navigateToTab(FitTrackRoutes.TRACK)
                            }
                        )
                    }
                    composable(FitTrackRoutes.HISTORY) {
                        HistoryReportsScreen(
                            workoutsState = workoutsState,
                            profileState = profileState,
                            dailyGoalsState = dailyGoalsState,
                            customExercisesState = customExercisesState,
                            onDeleteWorkout = viewModel::deleteWorkout,
                            onNotify = viewModel::notifyMessage
                        )
                    }
                    composable(FitTrackRoutes.PROFILE) {
                        ProfileScreen(
                            profileState = profileState,
                            workoutsState = workoutsState,
                            dailyGoalsState = dailyGoalsState,
                            customExercisesState = customExercisesState,
                            currentThemeMode = currentThemeMode,
                            userEmail = userEmail,
                            isSaving = isSaving,
                            onChangeThemeMode = viewModel::updateThemeMode,
                            onSaveProfile = viewModel::saveProfile,
                            onClearAllData = viewModel::clearAllUserData,
                            onNotify = viewModel::notifyMessage,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}
