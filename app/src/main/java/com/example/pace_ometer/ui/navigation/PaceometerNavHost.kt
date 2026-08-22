package com.example.pace_ometer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.ui.activerun.ActiveRunScreen
import com.example.pace_ometer.ui.help.HelpScreen
import com.example.pace_ometer.ui.history.HistoryScreen
import com.example.pace_ometer.ui.home.HomeScreen
import com.example.pace_ometer.ui.legal.LegalScreen
import com.example.pace_ometer.ui.onboarding.OnboardingScreen
import com.example.pace_ometer.ui.settings.SettingsScreen
import com.example.pace_ometer.ui.summary.RunSummaryScreen
import kotlinx.coroutines.flow.map

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ACTIVE_RUN = "active_run"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val LEGAL = "legal"
    const val RUN_SUMMARY = "run_summary/{runId}"
    fun runSummary(runId: Long) = "run_summary/$runId"
}

@Composable
fun PaceometerNavHost() {
    val context = LocalContext.current
    val app = context.applicationContext as PaceometerApp
    val onboardingCompleted by app.settingsRepository.userSettings
        .map { it.onboardingCompleted }
        .collectAsState(initial = null)

    val completed = onboardingCompleted
    if (completed == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = if (completed) Routes.HOME else Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onComplete = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onStartRun = { navController.navigate(Routes.ACTIVE_RUN) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onOpenLegal = { navController.navigate(Routes.LEGAL) }
            )
        }
        composable(Routes.ACTIVE_RUN) {
            ActiveRunScreen(onFinished = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenRun = { runId -> navController.navigate(Routes.runSummary(runId)) }
            )
        }
        composable(
            Routes.RUN_SUMMARY,
            arguments = listOf(navArgument("runId") { type = NavType.LongType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getLong("runId") ?: return@composable
            RunSummaryScreen(runId = runId, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LEGAL) {
            LegalScreen(onBack = { navController.popBackStack() })
        }
    }
}
