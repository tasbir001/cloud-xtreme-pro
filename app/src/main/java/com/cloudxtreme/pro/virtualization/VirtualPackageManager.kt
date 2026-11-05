package com.cloudxtreme.pro.virtualization

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.os.Build
import com.cloudxtreme.pro.core.utils.AppLogger
import java.io.File

/**
 * Virtualized PackageManager that delivers application metadata and component definitions
 * from the isolated virtual instance's APK file.
 */
class VirtualPackageManager(
    private val hostContext: Context,
    private val instanceId: String,
    private val targetPackageName: String,
    private val apkFile: File
) {

    private val cachedPackageInfo: PackageInfo? by lazy {
        try {
            val flags = PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hostContext.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                hostContext.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            }
        } catch (e: Exception) {
            AppLogger.e("VirtualPackageManager", "Failed to parse archive info for $instanceId", e)
            null
        }
    }

    fun getPackageInfo(): PackageInfo? {
        val pi = cachedPackageInfo ?: return null
        pi.applicationInfo?.sourceDir = apkFile.absolutePath
        pi.applicationInfo?.publicSourceDir = apkFile.absolutePath
        return pi
    }

    fun getApplicationInfo(): ApplicationInfo? {
        return getPackageInfo()?.applicationInfo
    }

    fun getApplicationLabel(): CharSequence {
        val ai = getApplicationInfo() ?: return targetPackageName
        ai.sourceDir = apkFile.absolutePath
        ai.publicSourceDir = apkFile.absolutePath
        return try {
            hostContext.packageManager.getApplicationLabel(ai)
        } catch (e: Exception) {
            targetPackageName
        }
    }

    fun getMainActivityClassName(): String? {
        val pi = getPackageInfo() ?: return null
        val activities = pi.activities ?: return null
        // Return first activity or main launcher activity
        return activities.firstOrNull()?.name
    }

    fun getActivities(): List<ActivityInfo> {
        return getPackageInfo()?.activities?.toList() ?: emptyList()
    }

    fun getPermissions(): List<String> {
        return getPackageInfo()?.requestedPermissions?.toList() ?: emptyList()
    }
}
