package com.cloudxtreme.pro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cloudxtreme.pro.ui.screens.apkmanager.ApkManagerScreen
import com.cloudxtreme.pro.ui.screens.cloudphone.CloudPhoneScreen
import com.cloudxtreme.pro.ui.screens.dashboard.DashboardScreen
import com.cloudxtreme.pro.ui.screens.instances.ConfigureInstanceScreen
import com.cloudxtreme.pro.ui.screens.instances.InstancesScreen
import com.cloudxtreme.pro.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToInstances = { navController.navigate(Screen.Instances.route) },
                onNavigateToCloudPhone = { navController.navigate(Screen.CloudPhone.route) },
                onNavigateToApkManager = { navController.navigate(Screen.ApkManager.route) },
                onConfigureInstance = { instanceId ->
                    navController.navigate(Screen.ConfigureInstance.createRoute(instanceId))
                }
            )
        }

        composable(Screen.Instances.route) {
            InstancesScreen(
                onConfigureInstance = { instanceId ->
                    navController.navigate(Screen.ConfigureInstance.createRoute(instanceId))
                },
                onNavigateToApkManager = { navController.navigate(Screen.ApkManager.route) }
            )
        }

        composable(
            route = Screen.ConfigureInstance.route,
            arguments = listOf(navArgument("instanceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val instanceId = backStackEntry.arguments?.getString("instanceId") ?: ""
            ConfigureInstanceScreen(
                instanceId = instanceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CloudPhone.route) {
            CloudPhoneScreen()
        }

        composable(Screen.ApkManager.route) {
            ApkManagerScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
