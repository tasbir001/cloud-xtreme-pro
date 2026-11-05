package com.cloudxtreme.pro.ui.screens.apkmanager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.cloudxtreme.pro.core.model.ApkPackageInfo
import com.cloudxtreme.pro.core.utils.FileUtils
import com.cloudxtreme.pro.ui.components.CyberCard
import com.cloudxtreme.pro.ui.components.NeonButton
import com.cloudxtreme.pro.ui.theme.*

@Composable
fun ApkManagerScreen(
    viewModel: ApkManagerViewModel = viewModel()
) {
    val deviceApps by viewModel.deviceApps.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var includeSystemApps by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.installApkFromUri(it) }
    }

    val filteredApps = remember(deviceApps, searchQuery) {
        if (searchQuery.isBlank()) deviceApps
        else deviceApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = VoidDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch("application/vnd.android.package-archive") },
                containerColor = HyperBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Import APK")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select APK File", fontWeight = FontWeight.Bold)
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "APK & App Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Detect installed applications or import APK from storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue
                    )
                }

                IconButton(onClick = { viewModel.loadInstalledApps(includeSystemApps) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = ElectricBlue)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search installed applications...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = GlassCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // System Apps Filter Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Found ${filteredApps.size} applications on host device",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Include System Apps", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = includeSystemApps,
                        onCheckedChange = {
                            includeSystemApps = it
                            viewModel.loadInstalledApps(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricBlue,
                            checkedTrackColor = HyperBlue.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ElectricBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Scanning installed applications...", color = TextSecondary)
                    }
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching applications found", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { appInfo ->
                        DeviceAppCard(
                            appInfo = appInfo,
                            onCloneToVirtual = { viewModel.cloneAppToNewInstance(appInfo) }
                        )
                    }
                }
            }
        }
    }

    // Installation Progress Dialog
    installProgress?.let { (pct, statusMsg) ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = VoidSurface,
            title = {
                Text(
                    text = "Installing to Virtual Sandbox...",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = statusMsg, color = ElectricBlue, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = pct / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ElectricBlue,
                        trackColor = VoidSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "$pct%", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.align(Alignment.End))
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun DeviceAppCard(
    appInfo: ApkPackageInfo,
    onCloneToVirtual: () -> Unit
) {
    CyberCard(
        backgroundColor = VoidSurface
    ) {
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
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(HyperBlue.copy(alpha = 0.35f), DeepViolet.copy(alpha = 0.35f))
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
                        text = appInfo.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = appInfo.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "v${appInfo.versionName} • ${FileUtils.formatFileSize(appInfo.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onCloneToVirtual,
                colors = ButtonDefaults.buttonColors(containerColor = HyperBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
