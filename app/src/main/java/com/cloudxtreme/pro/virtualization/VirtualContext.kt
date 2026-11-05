package com.cloudxtreme.pro.virtualization

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import com.cloudxtreme.pro.core.model.EnvironmentConfig
import com.cloudxtreme.pro.core.utils.AppLogger
import java.io.File
import java.lang.reflect.Method
import java.util.Locale

/**
 * Isolated ContextWrapper for the virtualized application instance.
 * Redirects all data directories, SQLite databases, and SharedPreferences
 * to the instance's isolated sandbox storage.
 */
class VirtualContext(
    baseContext: Context,
    val instanceId: String,
    val targetPackageName: String,
    val vfs: VirtualFileSystem,
    val config: EnvironmentConfig,
    val virtualClassLoader: ClassLoader,
    val apkFile: File
) : ContextWrapper(baseContext) {

    private val vpm by lazy {
        VirtualPackageManager(baseContext, instanceId, targetPackageName, apkFile)
    }

    private var virtualResources: Resources? = null
    private var virtualAssetManager: AssetManager? = null

    init {
        loadVirtualResources()
    }

    private fun loadVirtualResources() {
        try {
            val assetManagerClass = AssetManager::class.java
            val assetManager = assetManagerClass.newInstance()
            val addAssetPathMethod: Method = assetManagerClass.getMethod("addAssetPath", String::class.java)
            addAssetPathMethod.invoke(assetManager, apkFile.absolutePath)

            val superRes = baseContext.resources
            val displayMetrics = superRes.displayMetrics.apply {
                densityDpi = config.displayDensityDpi
            }

            val configuration = Configuration(superRes.configuration).apply {
                if (config.languageLocale.isNotEmpty()) {
                    val locale = Locale.forLanguageTag(config.languageLocale)
                    setLocale(locale)
                }
            }

            virtualAssetManager = assetManager
            virtualResources = Resources(assetManager, displayMetrics, configuration)
        } catch (e: Exception) {
            AppLogger.w("VirtualContext", "Failed to load custom virtual resources: ${e.message}")
        }
    }

    override fun getPackageName(): String {
        return targetPackageName
    }

    override fun getDataDir(): File {
        val dir = vfs.getInstanceDataDir(instanceId, targetPackageName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getFilesDir(): File {
        val dir = vfs.getInstanceFilesDir(instanceId, targetPackageName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getNoBackupFilesDir(): File {
        val dir = File(getDataDir(), "no_backup")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getCacheDir(): File {
        val dir = vfs.getInstanceCacheDir(instanceId, targetPackageName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getCodeCacheDir(): File {
        val dir = File(getDataDir(), "code_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getExternalFilesDir(type: String?): File? {
        val base = vfs.getInstanceExternalDir(instanceId, targetPackageName)
        val dir = if (type != null) File(base, type) else base
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getExternalCacheDir(): File? {
        val dir = File(vfs.getInstanceExternalDir(instanceId, targetPackageName), "cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getDatabasePath(name: String): File {
        val dbDir = vfs.getInstanceDatabasesDir(instanceId, targetPackageName)
        if (!dbDir.exists()) dbDir.mkdirs()
        return File(dbDir, name)
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        // Enforce per-instance isolated SharedPreferences
        val prefsDir = vfs.getInstancePrefsDir(instanceId, targetPackageName)
        if (!prefsDir.exists()) prefsDir.mkdirs()
        val isolatedKey = "inst_${instanceId}_$name"
        return baseContext.getSharedPreferences(isolatedKey, mode)
    }

    override fun getClassLoader(): ClassLoader {
        return virtualClassLoader
    }

    override fun getResources(): Resources {
        return virtualResources ?: super.getResources()
    }

    override fun getAssets(): AssetManager {
        return virtualAssetManager ?: super.getAssets()
    }

    override fun getApplicationInfo(): ApplicationInfo {
        val ai = vpm.getApplicationInfo() ?: super.getApplicationInfo()
        ai.dataDir = dataDir.absolutePath
        ai.sourceDir = apkFile.absolutePath
        ai.publicSourceDir = apkFile.absolutePath
        ai.nativeLibraryDir = vfs.getInstanceLibDir(instanceId).absolutePath
        return ai
    }

    override fun getPackageManager(): PackageManager {
        return super.getPackageManager()
    }
}
