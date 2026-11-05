package com.cloudxtreme.pro.apkmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.cloudxtreme.pro.core.model.ApkPackageInfo
import com.cloudxtreme.pro.core.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Scans installed applications on the host device so users can select
 * and clone any installed application into isolated virtual environments.
 */
class DeviceAppsScanner(private val appContext: Context) {

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<ApkPackageInfo> = withContext(Dispatchers.IO) {
        val pm = appContext.packageManager
        val resultList = mutableListOf<ApkPackageInfo>()

        try {
            val flags = PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(flags)
            }

            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue

                // Check system app filter
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystem && !includeSystemApps) {
                    continue
                }

                // Avoid scanning ourselves
                if (pkg.packageName == appContext.packageName) {
                    continue
                }

                val apkFile = File(appInfo.sourceDir)
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                val sizeBytes = if (apkFile.exists()) apkFile.length() else 0L

                val permissions = pkg.requestedPermissions?.toList() ?: emptyList()
                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 21

                resultList.add(
                    ApkPackageInfo(
                        packageName = pkg.packageName,
                        appName = appLabel,
                        versionName = pkg.versionName ?: "1.0",
                        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode else pkg.versionCode.toLong(),
                        apkPath = appInfo.sourceDir,
                        sizeBytes = sizeBytes,
                        minSdk = minSdk,
                        targetSdk = appInfo.targetSdkVersion,
                        permissions = permissions
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e("DeviceAppsScanner", "Failed to scan installed apps", e)
        }

        resultList.sortedBy { it.appName.lowercase() }
    }
}
