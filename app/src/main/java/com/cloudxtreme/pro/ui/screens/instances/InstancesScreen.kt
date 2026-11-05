package com.cloudxtreme.pro.ui.screens.instances

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.utils.FileUtils
import com.cloudxtreme.pro.ui.components.*
import com.cloudxtreme.pro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InstancesScreen(
    viewModel: InstancesViewModel = viewModel(),
    onConfigureInstance: (String) -> Unit,
    onNavigateToApkManager: () -> Unit
) {
    val instances by viewModel.instances.collectAsState()
    val isBusy by viewModel.isOperationInProgress.collectAsState()
    var instanceToDelete by remember { mutableStateOf<VirtualInstance?>(null) }

    Scaffold(
        containerColor = VoidDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToApkManager,
                containerColor = HyperBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Instance")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Instance", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Instance Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${instances.size} isolated environment profiles active",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = ElectricBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (instances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LayersClear,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Virtual Instances Created",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToApkManager,
                            colors = ButtonDefaults.buttonColors(containerColor = HyperBlue)
                        ) {
                            Text("Import APK to Create Instance")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(instances, key = { it.id }) { inst ->
                        InstanceManagementCard(
                            instance = inst,
                            onLaunch = { viewModel.launchInstance(inst) },
                            onStop = { viewModel.stopInstance(inst.id) },
                            onRestart = { viewModel.restartInstance(inst) },
                            onConfigure = { onConfigureInstance(inst.id) },
                            onClone = { viewModel.cloneInstance(inst.id) },
                            onBackup = { viewModel.backupInstance(inst.id) },
                            onDelete = { instanceToDelete = inst }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    instanceToDelete?.let { inst ->
        AlertDialog(
            onDismissRequest = { instanceToDelete = null },
            containerColor = VoidSurface,
            title = {
                Text(
                    text = "Delete Instance Sandbox?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently remove the virtual environment '${inst.name}' and all its isolated databases, files, and login sessions.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInstance(inst.id)
                        instanceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusStopped)
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { instanceToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun InstanceManagementCard(
    instance: VirtualInstance,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onConfigure: () -> Unit,
    onClone: () -> Unit,
    onBackup: () -> Unit,
    onDelete: () -> Unit
) {
    val isRunning = instance.status == InstanceStatus.RUNNING
    val lastActiveStr = remember(instance.lastActiveAt) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(instance.lastActiveAt))
    }

    CyberCard(
        borderColor = if (isRunning) ElectricBlue.copy(alpha = 0.5f) else GlassCardBorder,
        backgroundColor = VoidSurface
    ) {
        Column {
            // Card Header: Icon, App Name, Instance Name, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(HyperBlue.copy(alpha = 0.5f), DeepViolet.copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(28.dp)
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
                            text = "${instance.appLabel} (${instance.targetPackageName})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                StatusBadge(status = instance.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Specs Row: Android Version, Storage Usage, Last Active Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpecItem(label = "Android", value = instance.config.androidVersion.substringBefore(" ("))
                SpecItem(label = "Storage", value = FileUtils.formatFileSize(instance.storageUsedBytes))
                SpecItem(label = "GMS Mode", value = instance.config.gmsMode.name)
                SpecItem(label = "Last Active", value = lastActiveStr)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = VoidSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row: Launch/Stop, Restart, Configure, Clone, Backup, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Primary Action Button (Launch or Stop)
                if (isRunning) {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusStopped.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = onLaunch,
                        colors = ButtonDefaults.buttonColors(containerColor = HyperBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Launch", fontSize = 13.sp)
                    }
                }

                // Secondary Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRestart) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = TextSecondary)
                    }
                    IconButton(onClick = onConfigure) {
                        Icon(Icons.Default.Tune, contentDescription = "Configure", tint = ElectricBlue)
                    }
                    IconButton(onClick = onClone) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Clone Instance", tint = LavenderGlow)
                    }
                    IconButton(onClick = onBackup) {
                        Icon(Icons.Default.Archive, contentDescription = "Backup .cxp", tint = StatusPaused)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusStopped)
                    }
                }
            }
        }
    }
}

@Composable
fun SpecItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            fontSize = 12.sp
        )
    }
}
