package com.cloudxtreme.pro.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cloudxtreme.pro.core.utils.AppLogger
import com.cloudxtreme.pro.core.utils.FileUtils
import com.cloudxtreme.pro.virtualization.VirtualFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

val Context.dataStore by preferencesDataStore(name = "cxp_settings")

/**
 * Storage management service overseeing virtual disk allocations and directory sizes.
 */
class VirtualStorageManager(
    private val appContext: Context,
    private val vfs: VirtualFileSystem
) {

    fun getUsedVirtualStorageBytes(): Long {
        val envDir = File(appContext.filesDir, "environments")
        return FileUtils.calculateDirectorySize(envDir)
    }

    fun getBackupsStorageBytes(): Long {
        val backupsDir = File(appContext.filesDir, "backups")
        return FileUtils.calculateDirectorySize(backupsDir)
    }

    fun clearAllCaches() {
        val envDir = File(appContext.filesDir, "environments")
        val instances = envDir.listFiles() ?: return
        for (inst in instances) {
            val cacheDir = File(inst, "data")
            cacheDir.walkBottomUp().filter { it.name == "cache" }.forEach {
                it.deleteRecursively()
            }
        }
        AppLogger.i("VirtualStorageManager", "All virtual instance caches cleared")
    }
}

/**
 * Persistent preferences for Cloud Xtreme Pro settings.
 */
class PreferencesDataStore(private val appContext: Context) {

    private object Keys {
        val DARK_THEME_ONLY = booleanPreferencesKey("dark_theme_only")
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
        val DEFAULT_GMS_MODE = stringPreferencesKey("default_gms_mode")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
    }

    val isDarkThemeOnly: Flow<Boolean> = appContext.dataStore.data.map {
        it[Keys.DARK_THEME_ONLY] ?: true
    }

    val isHardwareAcceleration: Flow<Boolean> = appContext.dataStore.data.map {
        it[Keys.HARDWARE_ACCELERATION] ?: true
    }

    suspend fun setHardwareAcceleration(enabled: Boolean) {
        appContext.dataStore.edit { it[Keys.HARDWARE_ACCELERATION] = enabled }
    }
}
