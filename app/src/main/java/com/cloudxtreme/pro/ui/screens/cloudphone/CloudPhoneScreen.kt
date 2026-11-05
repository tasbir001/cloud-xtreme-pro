package com.cloudxtreme.pro.ui.screens.cloudphone

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cloudxtreme.pro.cloud.TouchInputProtocol
import com.cloudxtreme.pro.core.model.CloudServerRegion
import com.cloudxtreme.pro.ui.components.CyberCard
import com.cloudxtreme.pro.ui.components.NeonButton
import com.cloudxtreme.pro.ui.theme.*

@Composable
fun CloudPhoneScreen(
    viewModel: CloudPhoneViewModel = viewModel()
) {
    val context = LocalContext.current
    val streamMetrics by viewModel.streamMetrics.collectAsState()
    val cloudOsState by viewModel.cloudOsState.collectAsState()
    val quota by viewModel.quota.collectAsState()
    var selectedRegion by remember { mutableStateOf(streamMetrics.serverRegion) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header & Region Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Cloud Phone Stream",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Remote Android VM execution via WebRTC & WebSockets",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue
                    )
                }

                // Reboot button
                IconButton(onClick = {
                    Toast.makeText(context, "Rebooting remote cloud VM node...", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Reboot",
                        tint = LavenderGlow
                    )
                }
            }
        }

        // Region Selector Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = CloudServerRegion.values().indexOf(selectedRegion),
                containerColor = VoidSurface,
                contentColor = ElectricBlue,
                edgePadding = 0.dp,
                divider = {}
            ) {
                CloudServerRegion.values().forEach { region ->
                    Tab(
                        selected = selectedRegion == region,
                        onClick = {
                            selectedRegion = region
                            viewModel.setServerRegion(region)
                        },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(region.displayName.substringBefore(" ("), fontSize = 12.sp)
                                Text(region.latencyIndicator, fontSize = 10.sp, color = StatusRunning)
                            }
                        }
                    )
                }
            }
        }

        // The Live Remote Interactive Screen Viewer
        item {
            CloudStreamViewer(
                streamMetrics = streamMetrics,
                cloudOsState = cloudOsState,
                onTouchDown = { x, y -> viewModel.onTouchDown(x, y) },
                onTouchUp = { viewModel.onTouchUp() },
                onSpecialKey = { key -> viewModel.onSpecialKey(key) },
                onOpenApp = { app -> viewModel.openOsApp(app) },
                onExecuteCommand = { cmd -> viewModel.executeAdbCommand(cmd) }
            )
        }

        // Controls Toolbar (Resolution, Quality, Fullscreen)
        item {
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stream Quality: ${streamMetrics.resolution}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.setResolution("720x1600 (HD)") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (streamMetrics.resolution.contains("720")) HyperBlue else VoidSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("720p", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.setResolution("1080x2400 (FHD+)") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (streamMetrics.resolution.contains("1080")) HyperBlue else VoidSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("1080p", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Google Cloud Free-Tier & Cost Transparency Banner
        item {
            CyberCard(
                borderColor = LavenderGlow.copy(alpha = 0.5f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = LavenderGlow
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google Cloud Free-Tier Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            color = StatusRunning.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = StatusRunning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Monthly Free Cloud VM Hours: ${quota.usedFreeHours} / ${quota.totalFreeHoursPerMonth} hrs (${(quota.usedFreeHours / quota.totalFreeHoursPerMonth * 100).toInt()}% used)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (quota.usedFreeHours / quota.totalFreeHoursPerMonth).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = HyperBlue,
                        trackColor = VoidSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Cloud VM Storage: ${quota.usedStorageGb} / ${quota.totalStorageGb} GB used",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (quota.usedStorageGb / quota.totalStorageGb).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = DeepViolet,
                        trackColor = VoidSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = VoidSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pricing transparency: Free tier includes 15 hrs/mo and 20GB storage. Dedicated GPU nodes scale at $0.075 / hr with zero hidden fees.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
