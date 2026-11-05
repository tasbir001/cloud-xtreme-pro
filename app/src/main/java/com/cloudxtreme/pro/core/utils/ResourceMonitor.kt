package com.cloudxtreme.pro.core.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceResourceMetrics(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val usedRamBytes: Long,
    val ramUsagePercent: Float,
    val totalStorageBytes: Long,
    val availableStorageBytes: Long,
    val usedStorageBytes: Long,
    val storageUsagePercent: Float,
    val estimatedCpuUsagePercent: Float,
    val isLowMemory: Boolean
)

object ResourceMonitor {

    suspend fun getDeviceMetrics(context: Context): DeviceResourceMetrics = withContext(Dispatchers.IO) {
        // RAM Metrics
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem
        val availableRam = memoryInfo.availMem
        val usedRam = totalRam - availableRam
        val ramUsagePct = (usedRam.toFloat() / totalRam.toFloat()) * 100f

        // Internal Storage Metrics
        val dataPath = Environment.getDataDirectory()
        val statFs = StatFs(dataPath.path)
        val totalStorage = statFs.totalBytes
        val availableStorage = statFs.availableBytes
        val usedStorage = totalStorage - availableStorage
        val storageUsagePct = (usedStorage.toFloat() / totalStorage.toFloat()) * 100f

        // CPU estimation heuristic
        val cpuUsagePct = estimateCpuUsage()

        DeviceResourceMetrics(
            totalRamBytes = totalRam,
            availableRamBytes = availableRam,
            usedRamBytes = usedRam,
            ramUsagePercent = ramUsagePct.coerceIn(0f, 100f),
            totalStorageBytes = totalStorage,
            availableStorageBytes = availableStorage,
            usedStorageBytes = usedStorage,
            storageUsagePercent = storageUsagePct.coerceIn(0f, 100f),
            estimatedCpuUsagePercent = cpuUsagePct,
            isLowMemory = memoryInfo.lowMemory
        )
    }

    private fun estimateCpuUsage(): Float {
        return try {
            val cores = Runtime.getRuntime().availableProcessors()
            // Estimate based on active threads & load factor
            val activeThreads = Thread.activeCount()
            val load = (activeThreads.toFloat() / (cores * 8f)) * 50f + 15f
            load.coerceIn(5f, 95f)
        } catch (e: Exception) {
            25f
        }
    }

    /**
     * Checks if creating/launching another instance with requested RAM quota is safe.
     */
    fun canAllocateInstance(context: Context, requiredRamMb: Long): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val requiredBytes = requiredRamMb * 1024 * 1024
        // Keep a minimum reserve of 512 MB for host Android OS
        val reserveBytes = 512L * 1024 * 1024
        return (memoryInfo.availMem - requiredBytes) > reserveBytes
    }
}
