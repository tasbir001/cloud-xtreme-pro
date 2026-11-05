package com.cloudxtreme.pro.ui.screens.settings

import android.widget.Toast
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
import com.cloudxtreme.pro.CloudXtremeApplication
import com.cloudxtreme.pro.core.utils.FileUtils
import com.cloudxtreme.pro.ui.components.CyberCard
import com.cloudxtreme.pro.ui.components.NeonButton
import com.cloudxtreme.pro.ui.theme.*
import com.cloudxtreme.pro.virtualization.workprofile.WorkProfileBridge

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = CloudXtremeApplication.instance

    val avfCapability = remember { app.virtualizationEngine.avfCapability }
    val workProfileStatus = remember { WorkProfileBridge.checkStatus(context) }
    val gmsReport = remember { app.gmsEngine.evaluateGmsCompatibility() }
    val currentUser by app.firebaseAuthManager.currentUser.collectAsState()

    var usedStorageBytes by remember { mutableStateOf(app.storageManager.getUsedVirtualStorageBytes()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "System Architecture & Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Virtualization layer diagnostics and cloud settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricBlue
                )
            }
        }

        // AVF Virtualization Engine Diagnostic Card
        item {
            CyberCard(
                borderColor = if (avfCapability.isSupported) StatusRunning.copy(alpha = 0.5f) else ElectricBlue.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = ElectricBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Virtualization Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        color = (if (avfCapability.isSupported) StatusRunning else ElectricBlue).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (avfCapability.isHardwareAccelerated) "HARDWARE pKVM" else "SANDBOX CONTAINER",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if (avfCapability.isHardwareAccelerated) StatusRunning else ElectricBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticRow("Hypervisor / Mode", avfCapability.hypervisorType)
                DiagnosticRow("Kernel /dev/kvm Node", if (avfCapability.kvmNodePresent) "Available" else "Virtual Process Sandbox")
                DiagnosticRow("AVF API Level 33+", if (avfCapability.apiSupported) "Supported" else "Android 8.0+ Compatibility Mode")
                DiagnosticRow("Architecture Tier", avfCapability.reason)
            }
        }

        // Android Work Profile Card
        item {
            CyberCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = LavenderGlow
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Android Work Profile Integration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticRow("Managed Users Feature", if (workProfileStatus.isSupportedOnDevice) "Supported" else "Disabled by OEM")
                DiagnosticRow("Work Profile Active", if (workProfileStatus.isManagedProfileActive) "YES" else "Single Host User Space")
                DiagnosticRow("Status", workProfileStatus.description)
            }
        }

        // Google Play Services Ecosystem Card
        item {
            CyberCard(
                borderColor = DeepViolet.copy(alpha = 0.4f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = DeepViolet
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Google Play Services Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticRow("Host Play Services", if (gmsReport.hostGmsAvailable) "Installed (v${gmsReport.hostGmsVersionCode})" else "Not Detected")
                DiagnosticRow("Google Play Store", if (gmsReport.isGooglePlayStoreInstalled) "Available" else "Unavailable")
                DiagnosticRow("MicroG Compatibility", if (gmsReport.isMicroGSupported) "Ready" else "Unsupported")
                DiagnosticRow("Recommended Strategy", gmsReport.recommendedMode.name)
            }
        }

        // Firebase Cloud Account & Sync
        item {
            CyberCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = HyperBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Firebase Cloud Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                currentUser?.let { user ->
                    DiagnosticRow("Authenticated User", user.email ?: user.displayName ?: user.uid)
                    DiagnosticRow("Cloud Sync", if (user.isCloudSyncEnabled) "Enabled (Firestore & Storage)" else "Disabled")
                    DiagnosticRow("User ID", user.uid)
                }
            }
        }

        // Storage & Cache Cleanup
        item {
            CyberCard {
                Text(
                    text = "Storage Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticRow("Total Virtual Sandboxes Size", FileUtils.formatFileSize(usedStorageBytes))
                DiagnosticRow("Instance Backups Size", FileUtils.formatFileSize(app.storageManager.getBackupsStorageBytes()))

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        app.storageManager.clearAllCaches()
                        usedStorageBytes = app.storageManager.getUsedVirtualStorageBytes()
                        Toast.makeText(context, "All instance caches purged", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoidSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Virtual Instance Caches", color = TextPrimary)
                }
            }
        }

        // Security & Ethics Compliance Card
        item {
            Surface(
                color = VoidSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = StatusRunning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security & Architectural Principles", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Complete isolated filesystems for every cloned instance.\n" +
                                "• Strict separation of SQLite databases, shared preferences & cache.\n" +
                                "• No unauthorized device identity spoofing, safety-net evasion, or anti-cheat manipulation.\n" +
                                "• Full user control over local data backup, export, and deletion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
