package com.cloudxtreme.pro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cloudxtreme.pro.ui.navigation.Screen
import com.cloudxtreme.pro.ui.theme.*

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    NavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard),
    NavItem(Screen.Instances.route, "Instances", Icons.Default.Layers),
    NavItem(Screen.CloudPhone.route, "Cloud Phone", Icons.Default.Cloud),
    NavItem(Screen.ApkManager.route, "APK Manager", Icons.Default.InstallMobile),
    NavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
)

@Composable
fun NavigationBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = VoidSurface,
        tonalElevation = 8.dp
    ) {
        BottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) ElectricBlue else TextMuted
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ElectricBlue else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = HyperBlue.copy(alpha = 0.25f)
                )
            )
        }
    }
}
