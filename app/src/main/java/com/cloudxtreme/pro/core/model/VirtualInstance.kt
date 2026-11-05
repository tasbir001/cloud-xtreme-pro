package com.cloudxtreme.pro.core.model

import kotlinx.serialization.Serializable

/**
 * Represents the lifecycle status of a virtual environment instance.
 */
enum class InstanceStatus {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED,
    ERROR
}

/**
 * Supported Google Play Services modes inside the virtual environment.
 */
enum class GmsMode {
    OFF,              // Pure isolated AOSP environment
    MICRO_G,          // MicroG open-source Google services compatibility layer
    OFFICIAL_GMS,     // Certified GMS (available on compatible cloud instances or licensed images)
    SANDBOX_STUB      // Isolated stub provider to satisfy basic API calls without data leakage
}

/**
 * Orientation setting for virtual environment.
 */
enum class DisplayOrientation {
    PORTRAIT,
    LANDSCAPE,
    AUTO
}

/**
 * Network configuration mode for virtual environment.
 */
enum class NetworkMode {
    HOST_BRIDGED,     // Direct network access via host network stack
    ISOLATED_SANDBOX, // Strictly isolated (offline or loopback only)
    VIRTUAL_PROXY     // Route through custom proxy/VPN tunnel
}

/**
 * Per-instance device & environment configuration.
 * All settings reflect genuine virtualization parameters.
 */
@Serializable
data class EnvironmentConfig(
    val androidVersion: String = "Android 14 (API 34)",
    val apiLevel: Int = 34,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val displayDensityDpi: Int = 420,
    val orientation: DisplayOrientation = DisplayOrientation.PORTRAIT,
    val languageLocale: String = "en-US",
    val timeZoneId: String = "UTC",
    val networkMode: NetworkMode = NetworkMode.HOST_BRIDGED,
    val memoryLimitMb: Long = 2048L,
    val storageLimitMb: Long = 8192L,
    val gmsMode: GmsMode = GmsMode.MICRO_G,
    val avfAccelerationEnabled: Boolean = false
)

/**
 * Represents an isolated virtual application instance.
 */
@Serializable
data class VirtualInstance(
    val id: String,
    val name: String,
    val targetPackageName: String,
    val appLabel: String,
    val versionName: String,
    val versionCode: Long,
    val status: InstanceStatus = InstanceStatus.STOPPED,
    val config: EnvironmentConfig = EnvironmentConfig(),
    val storageUsedBytes: Long = 0L,
    val memoryUsedMb: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val isCloud: Boolean = false,
    val cloudInstanceId: String? = null,
    val cloneIndex: Int = 1,
    val apkFilePath: String? = null,
    val nativeLibDir: String? = null,
    val iconCachePath: String? = null
)

/**
 * Metadata for imported or device-installed APK files.
 */
@Serializable
data class ApkPackageInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkPath: String,
    val sizeBytes: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val isSplitApk: Boolean = false,
    val permissions: List<String> = emptyList(),
    val mainActivity: String? = null,
    val nativeAbis: List<String> = emptyList(),
    val iconUri: String? = null
)

/**
 * Cloud instance regions available for cloud phone virtualization.
 */
enum class CloudServerRegion(val displayName: String, val latencyIndicator: String) {
    US_EAST("US East (N. Virginia)", "38 ms"),
    US_WEST("US West (Oregon)", "52 ms"),
    EU_CENTRAL("Europe Central (Frankfurt)", "74 ms"),
    ASIA_EAST("Asia East (Tokyo)", "89 ms"),
    CUSTOM("Custom Private VM (GCP/AWS)", "Direct")
}

/**
 * Cloud-hosted Android VM instance representation.
 */
@Serializable
data class CloudInstance(
    val id: String,
    val name: String,
    val region: CloudServerRegion = CloudServerRegion.US_EAST,
    val status: InstanceStatus = InstanceStatus.STOPPED,
    val ipAddress: String = "10.142.0.24",
    val streamingPort: Int = 8443,
    val osVersion: String = "AOSP 14 Cloud VM",
    val pingMs: Int = 42,
    val fps: Int = 60,
    val resolution: String = "1080x2400",
    val bandwidthMbps: Double = 12.4,
    val totalUptimeSeconds: Long = 3600L,
    val cloudStorageBytes: Long = 3221225472L // 3GB
)

/**
 * Resource quota tracking for Cloud and Free-Tier users.
 */
@Serializable
data class CloudQuota(
    val totalFreeHoursPerMonth: Double = 10.0,
    val usedFreeHours: Double = 3.5,
    val totalStorageGb: Double = 15.0,
    val usedStorageGb: Double = 4.2,
    val maxConcurrentInstances: Int = 2,
    val activeConcurrentInstances: Int = 1,
    val estimatedHourlyCostUsd: Double = 0.08
)
