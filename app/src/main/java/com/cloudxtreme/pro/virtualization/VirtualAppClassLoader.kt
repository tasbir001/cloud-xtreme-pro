package com.cloudxtreme.pro.virtualization

import android.content.Context
import com.cloudxtreme.pro.core.utils.AppLogger
import dalvik.system.DexClassLoader
import java.io.File

/**
 * Isolated ClassLoader for virtual applications.
 * Dynamically loads classes from the instance's isolated APK and native libraries.
 */
class VirtualAppClassLoader(
    dexPath: String,
    optimizedDirectory: String,
    librarySearchPath: String,
    parent: ClassLoader
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent) {

    companion object {
        fun createForInstance(
            context: Context,
            vfs: VirtualFileSystem,
            instanceId: String,
            apkFile: File
        ): VirtualAppClassLoader {
            val dexCacheDir = vfs.getInstanceDexCacheDir(instanceId)
            val libDir = vfs.getInstanceLibDir(instanceId)

            if (!dexCacheDir.exists()) dexCacheDir.mkdirs()
            if (!libDir.exists()) libDir.mkdirs()

            AppLogger.i(
                "VirtualAppClassLoader",
                "Creating ClassLoader for instance $instanceId with dex=${apkFile.absolutePath}, lib=${libDir.absolutePath}"
            )

            return VirtualAppClassLoader(
                dexPath = apkFile.absolutePath,
                optimizedDirectory = dexCacheDir.absolutePath,
                librarySearchPath = libDir.absolutePath,
                parent = context.classLoader
            )
        }
    }
}
