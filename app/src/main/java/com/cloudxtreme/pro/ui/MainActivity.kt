package com.cloudxtreme.pro.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cloudxtreme.pro.ui.components.HeaderBar
import com.cloudxtreme.pro.ui.components.NavigationBottomBar
import com.cloudxtreme.pro.ui.navigation.AppNavGraph
import com.cloudxtreme.pro.ui.theme.CloudXtremeTheme
import com.cloudxtreme.pro.ui.theme.VoidDark

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CloudXtremeTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = VoidDark,
                    topBar = {
                        HeaderBar(
                            title = "Cloud Xtreme Pro",
                            subtitle = "Android Virtualization & Cloud Phone"
                        )
                    },
                    bottomBar = {
                        NavigationBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(VoidDark)
                    ) {
                        AppNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
