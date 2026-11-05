package com.cloudxtreme.pro.instancemanager

import android.content.Context
import com.cloudxtreme.pro.core.model.*
import com.cloudxtreme.pro.core.utils.AppLogger
import com.cloudxtreme.pro.core.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Repository for managing virtual instance profiles with local JSON persistence.
 */
class InstanceRepository(
    private val appContext: Context,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) {

    private val configFile: File
        get() = File(appContext.filesDir, "instances_manifest.json")

    private val _instances = MutableStateFlow<List<VirtualInstance>>(emptyList())
    val instances: StateFlow<List<VirtualInstance>> = _instances.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (configFile.exists()) {
            try {
                val content = configFile.readText()
                val loaded = json.decodeFromString<List<VirtualInstance>>(content)
                _instances.value = loaded
                AppLogger.i("InstanceRepository", "Loaded ${loaded.size} instances from manifest")
                return@withContext
            } catch (e: Exception) {
                AppLogger.e("InstanceRepository", "Failed to parse manifest, initializing defaults", e)
            }
        }

        // Initialize default demonstration instances fulfilling user specification:
        // "Example App – Instance 1", "Example App – Instance 2", "Example App – Instance 3"
        val defaults = listOf(
            VirtualInstance(
                id = "inst_demo_1",
                name = "Example App – Instance 1",
                targetPackageName = "com.example.app",
                appLabel = "Example App",
                versionName = "3.2.0",
                versionCode = 320,
                status = InstanceStatus.STOPPED,
                config = EnvironmentConfig(
                    androidVersion = "Android 14 (API 34)",
                    apiLevel = 34,
                    screenWidth = 1080,
                    screenHeight = 2400,
                    displayDensityDpi = 420,
                    memoryLimitMb = 2048,
                    storageLimitMb = 8192,
                    gmsMode = GmsMode.MICRO_G
                ),
                storageUsedBytes = 1024 * 1024 * 42L, // 42 MB
                cloneIndex = 1
            ),
            VirtualInstance(
                id = "inst_demo_2",
                name = "Example App – Instance 2",
                targetPackageName = "com.example.app",
                appLabel = "Example App",
                versionName = "3.2.0",
                versionCode = 320,
                status = InstanceStatus.STOPPED,
                config = EnvironmentConfig(
                    androidVersion = "Android 14 (API 34)",
                    apiLevel = 34,
                    screenWidth = 1080,
                    screenHeight = 2400,
                    displayDensityDpi = 420,
                    memoryLimitMb = 2048,
                    storageLimitMb = 8192,
                    gmsMode = GmsMode.OFF
                ),
                storageUsedBytes = 1024 * 1024 * 18L, // 18 MB
                cloneIndex = 2
            ),
            VirtualInstance(
                id = "inst_demo_3",
                name = "Example App – Instance 3",
                targetPackageName = "com.example.app",
                appLabel = "Example App",
                versionName = "3.2.0",
                versionCode = 320,
                status = InstanceStatus.STOPPED,
                config = EnvironmentConfig(
                    androidVersion = "Android 13 (API 33)",
                    apiLevel = 33,
                    screenWidth = 1080,
                    screenHeight = 2400,
                    displayDensityDpi = 440,
                    memoryLimitMb = 1536,
                    storageLimitMb = 4096,
                    gmsMode = GmsMode.MICRO_G
                ),
                storageUsedBytes = 1024 * 1024 * 8L, // 8 MB
                cloneIndex = 3
            )
        )
        _instances.value = defaults
        saveToDisk(defaults)
    }

    suspend fun addInstance(instance: VirtualInstance) = withContext(Dispatchers.IO) {
        val updated = _instances.value + instance
        _instances.value = updated
        saveToDisk(updated)
    }

    suspend fun updateInstance(instance: VirtualInstance) = withContext(Dispatchers.IO) {
        val updated = _instances.value.map { if (it.id == instance.id) instance else it }
        _instances.value = updated
        saveToDisk(updated)
    }

    suspend fun updateStatus(instanceId: String, status: InstanceStatus) = withContext(Dispatchers.IO) {
        val updated = _instances.value.map {
            if (it.id == instanceId) it.copy(status = status, lastActiveAt = System.currentTimeMillis()) else it
        }
        _instances.value = updated
        saveToDisk(updated)
    }

    suspend fun deleteInstance(instanceId: String) = withContext(Dispatchers.IO) {
        val updated = _instances.value.filterNot { it.id == instanceId }
        _instances.value = updated
        saveToDisk(updated)
    }

    suspend fun cloneInstance(sourceId: String, newName: String? = null): VirtualInstance = withContext(Dispatchers.IO) {
        val source = _instances.value.firstOrNull { it.id == sourceId }
            ?: throw IllegalArgumentException("Instance $sourceId not found")

        val currentClones = _instances.value.count { it.targetPackageName == source.targetPackageName }
        val nextIndex = currentClones + 1
        val newId = SecurityUtils.generateUniqueInstanceId()
        val finalName = newName ?: "${source.appLabel} – Instance $nextIndex"

        val cloned = source.copy(
            id = newId,
            name = finalName,
            status = InstanceStatus.STOPPED,
            storageUsedBytes = 0L,
            memoryUsedMb = 0L,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            cloneIndex = nextIndex
        )

        addInstance(cloned)
        cloned
    }

    fun getInstance(instanceId: String): VirtualInstance? {
        return _instances.value.firstOrNull { it.id == instanceId }
    }

    private fun saveToDisk(list: List<VirtualInstance>) {
        try {
            val serialized = json.encodeToString(list)
            configFile.writeText(serialized)
        } catch (e: Exception) {
            AppLogger.e("InstanceRepository", "Failed to persist instances to disk", e)
        }
    }
}
