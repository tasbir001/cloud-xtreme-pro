package com.cloudxtreme.pro.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Instances : Screen("instances")
    object ConfigureInstance : Screen("configure_instance/{instanceId}") {
        fun createRoute(instanceId: String) = "configure_instance/$instanceId"
    }
    object CloudPhone : Screen("cloud_phone")
    object ApkManager : Screen("apk_manager")
    object BackupRestore : Screen("backup_restore")
    object Settings : Screen("settings")
}
