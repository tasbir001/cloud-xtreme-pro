package com.cloudxtreme.pro.ui.screens.cloudphone

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudxtreme.pro.cloud.RemoteStreamController
import com.cloudxtreme.pro.cloud.TouchInputProtocol
import com.cloudxtreme.pro.cloud.VirtualCloudAndroidOS
import com.cloudxtreme.pro.ui.theme.*

@Composable
fun CloudStreamViewer(
    streamMetrics: RemoteStreamController.StreamMetrics,
    cloudOsState: VirtualCloudAndroidOS.CloudOsState,
    onTouchDown: (Float, Float) -> Unit,
    onTouchUp: () -> Unit,
    onSpecialKey: (TouchInputProtocol.KeyAction) -> Unit,
    onOpenApp: (VirtualCloudAndroidOS.ActiveWindow) -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(520.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = androidx.compose.foundation.BorderStroke(2.dp, ElectricBlue.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val normX = (offset.x / size.width).coerceIn(0f, 1f)
                            val normY = (offset.y / size.height).coerceIn(0f, 1f)
                            onTouchDown(normX, normY)
                            tryAwaitRelease()
                            onTouchUp()
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Remote Android Status Bar
                RemoteStatusBar(
                    timeString = cloudOsState.currentTimeString,
                    batteryPct = cloudOsState.batteryPercent
                )

                // Remote Stream OS Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (cloudOsState.activeWindow) {
                        VirtualCloudAndroidOS.ActiveWindow.LAUNCHER -> {
                            CloudLauncherContent(onOpenApp = onOpenApp)
                        }
                        VirtualCloudAndroidOS.ActiveWindow.BROWSER -> {
                            CloudBrowserContent(url = cloudOsState.browserUrl)
                        }
                        VirtualCloudAndroidOS.ActiveWindow.TERMINAL -> {
                            CloudTerminalContent(
                                lines = cloudOsState.terminalOutput,
                                onExecute = onExecuteCommand
                            )
                        }
                        VirtualCloudAndroidOS.ActiveWindow.SETTINGS -> {
                            CloudSettingsContent()
                        }
                        VirtualCloudAndroidOS.ActiveWindow.FILES -> {
                            CloudFilesContent()
                        }
                        VirtualCloudAndroidOS.ActiveWindow.CLOUD_STORE -> {
                            CloudStoreContent()
                        }
                    }

                    // Touch Feedback Ripple
                    cloudOsState.touchFeedbackX?.let { tx ->
                        cloudOsState.touchFeedbackY?.let { ty ->
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (tx * 320).dp,
                                        y = (ty * 400).dp
                                    )
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }

                // Remote Android 3-Button Navigation Bar
                RemoteNavigationBar(
                    onBack = { onSpecialKey(TouchInputProtocol.KeyAction.SPECIAL_BACK) },
                    onHome = { onSpecialKey(TouchInputProtocol.KeyAction.SPECIAL_HOME) },
                    onRecents = { onSpecialKey(TouchInputProtocol.KeyAction.SPECIAL_RECENTS) }
                )
            }

            // Stream Telemetry HUD Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 28.dp, end = 12.dp),
                color = Color(0xCC10192A),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusRunning)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${streamMetrics.fps} FPS • ${streamMetrics.pingMs}ms • ${streamMetrics.bitrateMbps} Mbps",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RemoteStatusBar(timeString: String, batteryPct: Int) {
    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$batteryPct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = StatusRunning, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun RemoteNavigationBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRecents: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onHome) {
                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRecents) {
                Icon(Icons.Default.Square, contentDescription = "Recents", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun CloudLauncherContent(
    onOpenApp: (VirtualCloudAndroidOS.ActiveWindow) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF0A0E17))
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Widget Clock
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Cloud Android 14",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "AOSP VM • Redroid & Cuttlefish Host",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricBlue
                )
            }

            // App Grid (2 rows x 3 columns)
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CloudAppIcon("Browser", Icons.Default.Language, Color(0xFF2979FF)) {
                        onOpenApp(VirtualCloudAndroidOS.ActiveWindow.BROWSER)
                    }
                    CloudAppIcon("Cloud Store", Icons.Default.Shop, Color(0xFF00E676)) {
                        onOpenApp(VirtualCloudAndroidOS.ActiveWindow.CLOUD_STORE)
                    }
                    CloudAppIcon("Files", Icons.Default.Folder, Color(0xFFFFAB00)) {
                        onOpenApp(VirtualCloudAndroidOS.ActiveWindow.FILES)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CloudAppIcon("Terminal", Icons.Default.Terminal, Color(0xFF7C4DFF)) {
                        onOpenApp(VirtualCloudAndroidOS.ActiveWindow.TERMINAL)
                    }
                    CloudAppIcon("Settings", Icons.Default.Settings, Color(0xFF90A4AE)) {
                        onOpenApp(VirtualCloudAndroidOS.ActiveWindow.SETTINGS)
                    }
                    CloudAppIcon("App Cloner", Icons.Default.Layers, Color(0xFF00E5FF)) {
                        onOpenApp(VirtualCloudAndroidOS.ActiveWindow.LAUNCHER)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun CloudAppIcon(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.25f))
                .border(1.5.dp, accent, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = accent, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CloudTerminalContent(
    lines: List<String>,
    onExecute: (String) -> Unit
) {
    var cmdText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050811))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Cloud ADB Shell", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("root@cloud-node-88", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(lines) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (line.startsWith("[")) Color(0xFF00E5FF) else Color(0xFFE2E8F0),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$ ", color = ElectricBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = cmdText,
                onValueChange = { cmdText = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text("type adb command (e.g. uname, df, ps)...", fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = GlassCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            IconButton(onClick = {
                if (cmdText.isNotBlank()) {
                    onExecute(cmdText)
                    cmdText = ""
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Run", tint = ElectricBlue)
            }
        }
    }
}

@Composable
fun CloudBrowserContent(url: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
    ) {
        Surface(color = Color(0xFF0F172A), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = StatusRunning, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(url, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cloud Xtreme Pro Web Gateway", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Isolated browser execution active.", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CloudSettingsContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Cloud VM Settings", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        item {
            SettingsItem("Virtual Android Version", "Android 14 (UpsideDownCake)")
        }
        item {
            SettingsItem("Kernel Version", "6.1.42-android14-virt")
        }
        item {
            SettingsItem("Hypervisor Backend", "QEMU/KVM + Cuttlefish Device Model")
        }
        item {
            SettingsItem("Memory Quota", "4096 MB vRAM allocated")
        }
        item {
            SettingsItem("Virtual Storage", "64 GB SSD virtual disk")
        }
    }
}

@Composable
fun CloudFilesContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Text("Cloud VM File System", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        SettingsItem("/data/app", "3.2 GB • 18 packages installed")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem("/data/data", "4.1 GB • isolated user data")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem("/sdcard/Download", "1.1 GB • downloaded APKs")
    }
}

@Composable
fun CloudStoreContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = StatusRunning, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Cloud Application Store", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Direct 1-click cloud app installer", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun SettingsItem(label: String, value: String) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
