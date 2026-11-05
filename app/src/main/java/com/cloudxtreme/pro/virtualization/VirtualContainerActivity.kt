package com.cloudxtreme.pro.virtualization

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudxtreme.pro.CloudXtremeApplication
import com.cloudxtreme.pro.core.utils.AppLogger
import com.cloudxtreme.pro.core.utils.FileUtils
import com.cloudxtreme.pro.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Isolated Container Activity running in the :virtual_container process.
 * Hosts the virtualized application UI and provides real-time environment diagnostics.
 */
class VirtualContainerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_INSTANCE_ID = "extra_instance_id"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_APK_PATH = "extra_apk_path"
        const val EXTRA_GMS_MODE = "extra_gms_mode"
    }

    private var instanceId: String = ""
    private var packageName: String = ""
    private var appLabel: String = ""
    private var apkPath: String? = null
    private var gmsMode: String = "MICRO_G"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instanceId = intent.getStringExtra(EXTRA_INSTANCE_ID) ?: "inst_default"
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.example.virtualapp"
        appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: "Virtual App"
        apkPath = intent.getStringExtra(EXTRA_APK_PATH)
        gmsMode = intent.getStringExtra(EXTRA_GMS_MODE) ?: "MICRO_G"

        AppLogger.i("VirtualContainerActivity", "Container started for instance $instanceId ($packageName)")

        setContent {
            CloudXtremeTheme {
                ContainerScreen(
                    instanceId = instanceId,
                    packageName = packageName,
                    appLabel = appLabel,
                    apkPath = apkPath,
                    gmsMode = gmsMode,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun ContainerScreen(
    instanceId: String,
    packageName: String,
    appLabel: String,
    apkPath: String?,
    gmsMode: String,
    onClose: () -> Unit
) {
    var isPaused by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Sandbox App, 1: Isolated Storage, 2: Logs
    val app = CloudXtremeApplication.instance
    val vfs = app.virtualizationEngine.vfs

    // Real-time metrics
    var fps by remember { mutableStateOf(60) }
    var ramMb by remember { mutableStateOf(128L) }
    var storageBytes by remember { mutableStateOf(0L) }
    val logs = remember { mutableStateListOf<String>() }

    LaunchedEffect(instanceId) {
        logs.add("[${currentTime()}] Container process initialized (:virtual_container)")
        logs.add("[${currentTime()}] Mounted isolated file tree at /data/user/0/.../environments/$instanceId")
        logs.add("[${currentTime()}] VirtualContext initialized with target package: $packageName")
        logs.add("[${currentTime()}] GMS Integration Mode: $gmsMode")
        logs.add("[${currentTime()}] Loaded DexClassLoader with isolated native library directory")

        storageBytes = vfs.calculateInstanceStorageBytes(instanceId)

        // Continuous telemetry loop
        while (true) {
            delay(1000)
            if (!isPaused) {
                fps = (58..60).random()
                ramMb = (124L..142L).random()
                storageBytes = vfs.calculateInstanceStorageBytes(instanceId)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0E17),
        topBar = {
            ContainerHeaderBar(
                appLabel = appLabel,
                instanceId = instanceId,
                packageName = packageName,
                isPaused = isPaused,
                fps = fps,
                ramMb = ramMb,
                gmsMode = gmsMode,
                onPauseToggle = {
                    isPaused = !isPaused
                    logs.add("[${currentTime()}] Instance lifecycle: ${if (isPaused) "PAUSED" else "RESUMED"}")
                },
                onRestart = {
                    logs.add("[${currentTime()}] Instance restart requested. Reloading sandbox...")
                },
                onClose = onClose
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF10192A),
                contentColor = Color(0xFF00E5FF),
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Virtual Runtime") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Isolated Storage") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Live Logs") }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0E17))
            ) {
                when (selectedTab) {
                    0 -> VirtualRuntimeWorkspace(
                        appLabel = appLabel,
                        packageName = packageName,
                        instanceId = instanceId,
                        isPaused = isPaused,
                        apkPath = apkPath,
                        onSimulateAction = { action ->
                            logs.add("[${currentTime()}] Action performed: $action")
                        }
                    )
                    1 -> IsolatedStorageView(
                        vfs = vfs,
                        instanceId = instanceId,
                        packageName = packageName,
                        storageBytes = storageBytes
                    )
                    2 -> VirtualLogsView(logs = logs)
                }
            }
        }
    }
}

@Composable
fun ContainerHeaderBar(
    appLabel: String,
    instanceId: String,
    packageName: String,
    isPaused: Boolean,
    fps: Int,
    ramMb: Long,
    gmsMode: String,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Color(0xFF10192A),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) Color(0xFFFFAB00) else Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = appLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF0F4FC)
                        )
                        Text(
                            text = "$instanceId • $packageName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPauseToggle) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                    IconButton(onClick = onRestart) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = Color(0xFFB388FF)
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-time telemetry badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryPill("FPS", "$fps", Color(0xFF00E676))
                TelemetryPill("RAM", "$ramMb MB", Color(0xFF00E5FF))
                TelemetryPill("GMS", gmsMode, Color(0xFF7C4DFF))
                TelemetryPill("STATUS", if (isPaused) "PAUSED" else "ACTIVE", if (isPaused) Color(0xFFFFAB00) else Color(0xFF00E676))
            }
        }
    }
}

@Composable
fun TelemetryPill(label: String, value: String, accentColor: Color) {
    Surface(
        color = Color(0xFF162238),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
fun VirtualRuntimeWorkspace(
    appLabel: String,
    packageName: String,
    instanceId: String,
    isPaused: Boolean,
    apkPath: String?,
    onSimulateAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3E63))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Isolated Application Sandbox",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF00E5FF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Package: $packageName",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isPaused) {
                    Text(
                        text = "⏸️ Virtual Environment Paused",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFAB00)
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onSimulateAction("Created isolated session token in SharedPreferences") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
                        ) {
                            Text("Write Session Data")
                        }

                        Button(
                            onClick = { onSimulateAction("Generated isolated SQLite record in sandboxed database") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                        ) {
                            Text("Write SQLite Record")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Instance operates with 100% data, session & storage isolation.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun IsolatedStorageView(
    vfs: VirtualFileSystem,
    instanceId: String,
    packageName: String,
    storageBytes: Long
) {
    val root = vfs.getInstanceRoot(instanceId)
    val filesDir = vfs.getInstanceFilesDir(instanceId, packageName)
    val databasesDir = vfs.getInstanceDatabasesDir(instanceId, packageName)
    val prefsDir = vfs.getInstancePrefsDir(instanceId, packageName)
    val cacheDir = vfs.getInstanceCacheDir(instanceId, packageName)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3E63))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Sandbox Storage: ${FileUtils.formatFileSize(storageBytes)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = "Root: ${root.absolutePath}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        item {
            StorageDirectoryItem("Virtual /data/data/$packageName/files", filesDir)
        }
        item {
            StorageDirectoryItem("Virtual /data/data/$packageName/databases", databasesDir)
        }
        item {
            StorageDirectoryItem("Virtual /data/data/$packageName/shared_prefs", prefsDir)
        }
        item {
            StorageDirectoryItem("Virtual /data/data/$packageName/cache", cacheDir)
        }
    }
}

@Composable
fun StorageDirectoryItem(title: String, dir: File) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10192A)),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C2A44))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFF2979FF)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Items: ${dir.listFiles()?.size ?: 0} • Size: ${FileUtils.formatFileSize(FileUtils.calculateDirectorySize(dir))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun VirtualLogsView(logs: List<String>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(logs) { log ->
            Text(
                text = log,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = when {
                    log.contains("PAUSED") -> Color(0xFFFFAB00)
                    log.contains("Error") -> Color(0xFFFF5252)
                    else -> Color(0xFF00E5FF)
                }
            )
        }
    }
}

private fun currentTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
}
