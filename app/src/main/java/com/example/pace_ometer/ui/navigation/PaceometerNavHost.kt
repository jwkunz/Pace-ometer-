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
import com.example.pace_ometer.data.ActivityType
import com.example.pace_ometer.ui.activerun.ActiveRunScreen
import com.example.pace_ometer.ui.equipment.EquipmentDetailScreen
import com.example.pace_ometer.ui.equipment.EquipmentScreen
import com.example.pace_ometer.ui.help.HelpScreen
import com.example.pace_ometer.ui.history.HistoryScreen
import com.example.pace_ometer.ui.home.HomeScreen
import com.example.pace_ometer.ui.legal.LegalScreen
import com.example.pace_ometer.ui.onboarding.OnboardingScreen
import com.example.pace_ometer.ui.records.PersonalRecordsScreen
import com.example.pace_ometer.ui.settings.SettingsScreen
import com.example.pace_ometer.ui.summary.RunSummaryScreen
import kotlinx.coroutines.flow.map

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ACTIVE_RUN = "active_run/{activityType}"
    fun activeRun(activityType: ActivityType) = "active_run/${activityType.name}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val LEGAL = "legal"
    const val RECORDS = "records"
    const val RUN_SUMMARY = "run_summary/{runId}"
    fun runSummary(runId: Long) = "run_summary/$runId"
    const val EQUIPMENT = "equipment"
    const val EQUIPMENT_DETAIL = "equipment/{equipmentId}"
    fun equipmentDetail(equipmentId: Long) = "equipment/$equipmentId"
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
                onStartRun = { activityType -> navController.navigate(Routes.activeRun(activityType)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenRecords = { navController.navigate(Routes.RECORDS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onOpenLegal = { navController.navigate(Routes.LEGAL) }
            )
        }
        composable(
            Routes.ACTIVE_RUN,
            arguments = listOf(navArgument("activityType") { type = NavType.StringType })
        ) { backStackEntry ->
            val activityType = backStackEntry.arguments?.getString("activityType")
                ?.let { runCatching { ActivityType.valueOf(it) }.getOrNull() }
                ?: ActivityType.RUNNING
            ActiveRunScreen(
                activityType = activityType,
                onFinished = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
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
        composable(Routes.RECORDS) {
            PersonalRecordsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetComplete = {
                    navController.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
                },
                onOpenEquipment = { navController.navigate(Routes.EQUIPMENT) }
            )
        }
        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LEGAL) {
            LegalScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EQUIPMENT) {
            EquipmentScreen(
                onBack = { navController.popBackStack() },
                onOpenEquipment = { equipmentId -> navController.navigate(Routes.equipmentDetail(equipmentId)) }
            )
        }
        composable(
            Routes.EQUIPMENT_DETAIL,
            arguments = listOf(navArgument("equipmentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val equipmentId = backStackEntry.arguments?.getLong("equipmentId") ?: return@composable
            EquipmentDetailScreen(equipmentId = equipmentId, onBack = { navController.popBackStack() })
        }
    }
}
