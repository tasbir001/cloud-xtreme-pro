package com.cloudxtreme.pro.ui.screens.instances

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
import com.cloudxtreme.pro.core.model.*
import com.cloudxtreme.pro.ui.components.CyberCard
import com.cloudxtreme.pro.ui.components.NeonButton
import com.cloudxtreme.pro.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ConfigureInstanceScreen(
    instanceId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = CloudXtremeApplication.instance
    val coroutineScope = rememberCoroutineScope()

    val originalInstance = remember(instanceId) {
        app.instanceRepository.getInstance(instanceId)
    }

    if (originalInstance == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Instance not found", color = StatusStopped)
        }
        return
    }

    var instanceName by remember { mutableStateOf(originalInstance.name) }
    var selectedAndroidVersion by remember { mutableStateOf(originalInstance.config.androidVersion) }
    var selectedResolution by remember { mutableStateOf("${originalInstance.config.screenWidth}x${originalInstance.config.screenHeight}") }
    var selectedDensity by remember { mutableStateOf("${originalInstance.config.displayDensityDpi} DPI") }
    var selectedOrientation by remember { mutableStateOf(originalInstance.config.orientation) }
    var selectedLocale by remember { mutableStateOf(originalInstance.config.languageLocale) }
    var selectedTimeZone by remember { mutableStateOf(originalInstance.config.timeZoneId) }
    var selectedNetworkMode by remember { mutableStateOf(originalInstance.config.networkMode) }
    var memoryLimitMb by remember { mutableStateOf(originalInstance.config.memoryLimitMb) }
    var storageLimitMb by remember { mutableStateOf(originalInstance.config.storageLimitMb) }
    var selectedGmsMode by remember { mutableStateOf(originalInstance.config.gmsMode) }

    Scaffold(
        containerColor = VoidDark,
        topBar = {
            Surface(color = VoidSurface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Configure Environment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = originalInstance.targetPackageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricBlue
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Profile
            item {
                CyberCard {
                    Text(
                        text = "Instance Profile Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = instanceName,
                        onValueChange = { instanceName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }

            // Android Runtime Version
            item {
                CyberCard {
                    Text(
                        text = "Android System Runtime",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select virtualized framework compatibility target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val versions = listOf(
                        "Android 14 (API 34)",
                        "Android 13 (API 33)",
                        "Android 12 (API 32)",
                        "Android 11 (API 30)"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        versions.forEach { ver ->
                            FilterChip(
                                selected = selectedAndroidVersion == ver,
                                onClick = { selectedAndroidVersion = ver },
                                label = { Text(ver.take(10), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HyperBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Screen & Display Density
            item {
                CyberCard {
                    Text(
                        text = "Display & Virtual Resolution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val resolutions = listOf("720x1600 (HD+)", "1080x2400 (FHD+)", "1440x3200 (QHD+)")
                    resolutions.forEach { res ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedResolution.contains(res.take(9)),
                                onClick = { selectedResolution = res },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricBlue)
                            )
                            Text(text = res, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Display Density: $selectedDensity", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedDensity.filter { it.isDigit() }.toFloatOrNull() ?: 420f,
                        onValueChange = { selectedDensity = "${it.toInt()} DPI" },
                        valueRange = 280f..560f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                }
            }

            // Resource Quotas (RAM & Storage)
            item {
                CyberCard {
                    Text(
                        text = "Hardware Quotas (RAM & Storage)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "RAM Limit: $memoryLimitMb MB",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = memoryLimitMb.toFloat(),
                        onValueChange = { memoryLimitMb = it.toLong() },
                        valueRange = 1024f..6144f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = HyperBlue, activeTrackColor = HyperBlue)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Storage Quota: ${storageLimitMb / 1024} GB",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = storageLimitMb.toFloat(),
                        onValueChange = { storageLimitMb = it.toLong() },
                        valueRange = 2048f..32768f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = LavenderGlow, activeTrackColor = LavenderGlow)
                    )
                }
            }

            // GMS & Google Ecosystem
            item {
                CyberCard {
                    Text(
                        text = "Google Play Services Integration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Legitimate MicroG or isolated AOSP compatibility layer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GmsMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedGmsMode == mode,
                                onClick = { selectedGmsMode = mode },
                                colors = RadioButtonDefaults.colors(selectedColor = DeepViolet)
                            )
                            Text(
                                text = when (mode) {
                                    GmsMode.MICRO_G -> "MicroG Core (Open-source compatibility)"
                                    GmsMode.OFF -> "Pure AOSP (Non-GMS Isolated)"
                                    GmsMode.OFFICIAL_GMS -> "Official Certified GMS (Cloud Host Only)"
                                    GmsMode.SANDBOX_STUB -> "Sandbox API Stub"
                                },
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Legitimate Security Notice
            item {
                Surface(
                    color = VoidSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder)
                ) {
                    Row(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = ElectricBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cloud Xtreme Pro guarantees accurate environment reporting. No fake hardware identities, device attestation tampering, or anti-cheat bypasses are used.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Save Changes Button
            item {
                NeonButton(
                    text = "Apply Configuration",
                    onClick = {
                        coroutineScope.launch {
                            val updatedConfig = originalInstance.config.copy(
                                androidVersion = selectedAndroidVersion,
                                screenWidth = selectedResolution.substringBefore("x").toIntOrNull() ?: 1080,
                                screenHeight = selectedResolution.substringAfter("x").substringBefore(" ").toIntOrNull() ?: 2400,
                                displayDensityDpi = selectedDensity.filter { it.isDigit() }.toIntOrNull() ?: 420,
                                memoryLimitMb = memoryLimitMb,
                                storageLimitMb = storageLimitMb,
                                gmsMode = selectedGmsMode
                            )
                            val updatedInstance = originalInstance.copy(
                                name = instanceName,
                                config = updatedConfig
                            )
                            app.instanceRepository.updateInstance(updatedInstance)
                            Toast.makeText(context, "Configuration saved successfully", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = HyperBlue
                )
            }
        }
    }
}
