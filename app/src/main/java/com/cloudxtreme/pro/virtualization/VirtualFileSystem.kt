package com.cloudxtreme.pro.virtualization

import android.content.Context
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.utils.AppLogger
import com.cloudxtreme.pro.core.utils.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Manages the isolated file system for each virtual environment.
 * Ensures total separation of app data, databases, shared_prefs, and cache.
 */
class VirtualFileSystem(private val appContext: Context) {

    private val environmentsBaseDir: File
        get() = File(appContext.filesDir, "environments").apply {
            if (!exists()) mkdirs()
        }

    fun getInstanceRoot(instanceId: String): File {
        return File(environmentsBaseDir, instanceId)
    }

    fun getInstanceDataDir(instanceId: String, packageName: String): File {
        return File(getInstanceRoot(instanceId), "data/$packageName")
    }

    fun getInstanceFilesDir(instanceId: String, packageName: String): File {
        return File(getInstanceDataDir(instanceId, packageName), "files")
    }

    fun getInstanceCacheDir(instanceId: String, packageName: String): File {
        return File(getInstanceDataDir(instanceId, packageName), "cache")
    }

    fun getInstancePrefsDir(instanceId: String, packageName: String): File {
        return File(getInstanceDataDir(instanceId, packageName), "shared_prefs")
    }

    fun getInstanceDatabasesDir(instanceId: String, packageName: String): File {
        return File(getInstanceDataDir(instanceId, packageName), "databases")
    }

    fun getInstanceExternalDir(instanceId: String, packageName: String): File {
        return File(getInstanceRoot(instanceId), "external/$packageName")
    }

    fun getInstanceApkDir(instanceId: String): File {
        return File(getInstanceRoot(instanceId), "apk")
    }

    fun getInstanceLibDir(instanceId: String): File {
        return File(getInstanceRoot(instanceId), "lib")
    }

    fun getInstanceDexCacheDir(instanceId: String): File {
        return File(getInstanceRoot(instanceId), "dex_cache")
    }

    /**
     * Initializes all sandboxed directory structures for an instance.
     */
    fun initializeInstanceStorage(instanceId: String, packageName: String) {
        val root = getInstanceRoot(instanceId)
        val dirs = listOf(
            root,
            getInstanceDataDir(instanceId, packageName),
            getInstanceFilesDir(instanceId, packageName),
            getInstanceCacheDir(instanceId, packageName),
            getInstancePrefsDir(instanceId, packageName),
            getInstanceDatabasesDir(instanceId, packageName),
            getInstanceExternalDir(instanceId, packageName),
            getInstanceApkDir(instanceId),
            getInstanceLibDir(instanceId),
            getInstanceDexCacheDir(instanceId),
            File(root, "logs")
        )

        dirs.forEach { dir ->
            if (!dir.exists()) {
                val created = dir.mkdirs()
                AppLogger.d("VirtualFileSystem", "Initialized dir ${dir.absolutePath} (created=$created)")
            }
        }
    }

    /**
     * Copies and extracts an APK file into the instance sandbox.
     * Also extracts matching native .so libraries for the device ABI.
     */
    fun installApkToSandbox(instanceId: String, sourceApk: File): File {
        val targetApkDir = getInstanceApkDir(instanceId)
        if (!targetApkDir.exists()) targetApkDir.mkdirs()

        val destApk = File(targetApkDir, "base.apk")
        FileUtils.copyFile(sourceApk, destApk)

        // Extract native libraries (.so)
        extractNativeLibs(destApk, getInstanceLibDir(instanceId))

        return destApk
    }

    /**
     * Extracts native .so libraries from the APK archive to the instance's isolated lib directory.
     */
    private fun extractNativeLibs(apkFile: File, libDir: File) {
        if (!libDir.exists()) libDir.mkdirs()

        try {
            val zipFile = ZipFile(apkFile)
            val entries = zipFile.entries()
            val supportedAbis = android.os.Build.SUPPORTED_ABIS

            // Identify best matching ABI
            var selectedAbi: String? = null
            for (abi in supportedAbis) {
                val abiPrefix = "lib/$abi/"
                if (zipFile.getEntry(abiPrefix) != null || hasEntryWithPrefix(apkFile, abiPrefix)) {
                    selectedAbi = abi
                    break
                }
            }

            if (selectedAbi == null && supportedAbis.isNotEmpty()) {
                selectedAbi = supportedAbis[0]
            }

            val prefix = "lib/$selectedAbi/"
            AppLogger.i("VirtualFileSystem", "Extracting native libs for ABI: $selectedAbi from ${apkFile.name}")

            val zipEntries = zipFile.entries()
            while (zipEntries.hasMoreElements()) {
                val entry = zipEntries.nextElement()
                if (entry.name.startsWith(prefix) && !entry.isDirectory && entry.name.endsWith(".so")) {
                    val libName = entry.name.substringAfterLast("/")
                    val targetSo = File(libDir, libName)
                    zipFile.getInputStream(entry).use { input ->
                        FileOutputStream(targetSo).use { output ->
                            input.copyTo(output)
                        }
                    }
                    AppLogger.d("VirtualFileSystem", "Extracted native lib: $libName")
                }
            }
            zipFile.close()
        } catch (e: Exception) {
            AppLogger.w("VirtualFileSystem", "Native lib extraction completed with note: ${e.message}")
        }
    }

    private fun hasEntryWithPrefix(file: File, prefix: String): Boolean {
        return try {
            val zip = ZipFile(file)
            val exists = zip.entries().asSequence().any { it.name.startsWith(prefix) }
            zip.close()
            exists
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clones an existing instance data to a new instance (creating Instance 2, Instance 3, etc.)
     */
    fun cloneInstanceStorage(sourceInstanceId: String, targetInstanceId: String, packageName: String) {
        initializeInstanceStorage(targetInstanceId, packageName)

        val srcRoot = getInstanceRoot(sourceInstanceId)
        val dstRoot = getInstanceRoot(targetInstanceId)

        // Copy APK
        val srcApkDir = getInstanceApkDir(sourceInstanceId)
        val dstApkDir = getInstanceApkDir(targetInstanceId)
        if (srcApkDir.exists()) {
            FileUtils.copyDirectory(srcApkDir, dstApkDir)
        }

        // Copy native libs
        val srcLibDir = getInstanceLibDir(sourceInstanceId)
        val dstLibDir = getInstanceLibDir(targetInstanceId)
        if (srcLibDir.exists()) {
            FileUtils.copyDirectory(srcLibDir, dstLibDir)
        }

        // Note: We deliberately do NOT copy databases/shared_prefs unless user specifically
        // requests full state clone, to ensure completely clean independent sessions!
        AppLogger.i("VirtualFileSystem", "Cloned instance storage from $sourceInstanceId to $targetInstanceId")
    }

    /**
     * Calculates storage size used by this virtual instance.
     */
    fun calculateInstanceStorageBytes(instanceId: String): Long {
        val root = getInstanceRoot(instanceId)
        return FileUtils.calculateDirectorySize(root)
    }

    /**
     * Clears instance cache directory.
     */
    fun clearInstanceCache(instanceId: String, packageName: String) {
        val cacheDir = getInstanceCacheDir(instanceId, packageName)
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    /**
     * Deletes the entire virtual environment file system.
     */
    fun deleteInstanceStorage(instanceId: String): Boolean {
        val root = getInstanceRoot(instanceId)
        return FileUtils.deleteRecursively(root)
    }
}
