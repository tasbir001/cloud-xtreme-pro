package com.cloudxtreme.pro.apkmanager

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.cloudxtreme.pro.core.model.ApkPackageInfo
import com.cloudxtreme.pro.core.result.OperationResult
import com.cloudxtreme.pro.core.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/**
 * Inspects and parses APK packages from storage.
 */
class PackageParserService(private val appContext: Context) {

    suspend fun parseApkFile(apkFile: File): OperationResult<ApkPackageInfo> = withContext(Dispatchers.IO) {
        if (!apkFile.exists() || !apkFile.canRead()) {
            return@withContext OperationResult.Error("Cannot read APK file: ${apkFile.absolutePath}")
        }

        try {
            val pm = appContext.packageManager
            val flags = PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_SERVICES

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            } ?: return@withContext OperationResult.Error("Invalid or corrupted APK: could not parse manifest")

            val appInfo = packageInfo.applicationInfo ?: return@withContext OperationResult.Error("Missing application info in APK")
            appInfo.sourceDir = apkFile.absolutePath
            appInfo.publicSourceDir = apkFile.absolutePath

            val appLabel = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageInfo.packageName
            }

            val mainActivity = packageInfo.activities?.firstOrNull()?.name
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val nativeAbis = detectNativeAbis(apkFile)

            val parsedInfo = ApkPackageInfo(
                packageName = packageInfo.packageName,
                appName = appLabel,
                versionName = packageInfo.versionName ?: "1.0",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
                apkPath = apkFile.absolutePath,
                sizeBytes = apkFile.length(),
                minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 21,
                targetSdk = appInfo.targetSdkVersion,
                mainActivity = mainActivity,
                permissions = permissions,
                nativeAbis = nativeAbis
            )

            OperationResult.Success(parsedInfo)
        } catch (e: Exception) {
            AppLogger.e("PackageParserService", "Failed to parse APK: ${apkFile.name}", e)
            OperationResult.Error("Failed to parse APK: ${e.message}", e)
        }
    }

    private fun detectNativeAbis(apkFile: File): List<String> {
        val abis = mutableSetOf<String>()
        try {
            val zip = ZipFile(apkFile)
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                if (name.startsWith("lib/") && name.count { it == '/' } >= 2) {
                    val abi = name.split("/")[1]
                    abis.add(abi)
                }
            }
            zip.close()
        } catch (e: Exception) {
            AppLogger.w("PackageParserService", "Could not read zip entries for ABIs: ${e.message}")
        }
        return abis.toList()
    }
}
