package com.cloudxtreme.pro.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cloudxtreme.pro.R
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.utils.FileUtils
import com.cloudxtreme.pro.ui.components.*
import com.cloudxtreme.pro.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToInstances: () -> Unit,
    onNavigateToCloudPhone: () -> Unit,
    onNavigateToApkManager: () -> Unit,
    onConfigureInstance: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val instances by viewModel.instances.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Branding Card
        item {
            CyberCard(
                borderColor = ElectricBlue.copy(alpha = 0.35f),
                backgroundColor = VoidSurface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, ElectricBlue, RoundedCornerShape(16.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(HyperBlue.copy(alpha = 0.5f), VoidDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Icon",
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CLOUD XTREME PRO",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Hardware Hypervisor & Sandboxed Containers",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Multi-instance isolation with independent data & login sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Quick Stats Row (2x2 Grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Envs",
                    value = "${uiState.totalInstances}",
                    subText = "Isolated Sandboxes",
                    icon = Icons.Default.Layers,
                    accentColor = ElectricBlue
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Running",
                    value = "${uiState.runningInstances}",
                    subText = "Active Instances",
                    icon = Icons.Default.PlayCircle,
                    accentColor = StatusRunning
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Stopped",
                    value = "${uiState.stoppedInstances}",
                    subText = "Idle In Storage",
                    icon = Icons.Default.PauseCircle,
                    accentColor = StatusPaused
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Cloned Apps",
                    value = "${uiState.totalInstalledApps}",
                    subText = "Distinct Packages",
                    icon = Icons.Default.Apps,
                    accentColor = DeepViolet
                )
            }
        }

        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeonButton(
                    text = "Manage Instances",
                    onClick = onNavigateToInstances,
                    modifier = Modifier.weight(1f),
                    containerColor = HyperBlue,
                    icon = {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = Color.White)
                    }
                )
                NeonButton(
                    text = "Import APK",
                    onClick = onNavigateToApkManager,
                    modifier = Modifier.weight(1f),
                    containerColor = DeepViolet,
                    icon = {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                )
            }
        }

        // Device Resource Usage Card
        item {
            CyberCard(
                borderColor = GlassCardBorder
            ) {
                Text(
                    text = "Device Resource Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                uiState.resourceMetrics?.let { metrics ->
                    ResourceMeterBar(
                        label = "RAM Utilization (${FileUtils.formatFileSize(metrics.usedRamBytes)} / ${FileUtils.formatFileSize(metrics.totalRamBytes)})",
                        currentValueStr = "${metrics.ramUsagePercent.toInt()}%",
                        percentage = metrics.ramUsagePercent,
                        barColor = if (metrics.ramUsagePercent > 80f) StatusStopped else ElectricBlue
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ResourceMeterBar(
                        label = "Internal Storage (${FileUtils.formatFileSize(metrics.usedStorageBytes)} / ${FileUtils.formatFileSize(metrics.totalStorageBytes)})",
                        currentValueStr = "${metrics.storageUsagePercent.toInt()}%",
                        percentage = metrics.storageUsagePercent,
                        barColor = HyperBlue
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ResourceMeterBar(
                        label = "Host CPU Load",
                        currentValueStr = "${metrics.estimatedCpuUsagePercent.toInt()}%",
                        percentage = metrics.estimatedCpuUsagePercent,
                        barColor = LavenderGlow
                    )
                } ?: CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        // Cloud Phone & Storage Tier Card
        item {
            CyberCard(
                borderColor = LavenderGlow.copy(alpha = 0.4f),
                onClick = onNavigateToCloudPhone
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = LavenderGlow
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cloud Phone Free Tier",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Google Cloud Android VM: ${uiState.cloudUsedHours} / ${uiState.cloudTotalHours} hrs used",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Cloud Storage: ${uiState.cloudUsedStorageGb} / ${uiState.cloudTotalStorageGb} GB used",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Cloud Phone",
                        tint = ElectricBlue
                    )
                }
            }
        }

        // Active Instances Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Virtual Environments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = onNavigateToInstances) {
                    Text("View All (${instances.size})", color = ElectricBlue)
                }
            }
        }

        // Recent Instances List
        items(instances.take(4)) { inst ->
            InstanceSummaryCard(
                instance = inst,
                onLaunch = { viewModel.launchInstance(inst) },
                onConfigure = { onConfigureInstance(inst.id) }
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VoidSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun InstanceSummaryCard(
    instance: VirtualInstance,
    onLaunch: () -> Unit,
    onConfigure: () -> Unit
) {
    CyberCard(
        onClick = onLaunch,
        backgroundColor = VoidSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(HyperBlue.copy(alpha = 0.4f), DeepViolet.copy(alpha = 0.4f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = instance.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${instance.config.androidVersion} • ${FileUtils.formatFileSize(instance.storageUsedBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = instance.status)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onConfigure) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}
