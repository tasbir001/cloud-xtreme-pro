package com.cloudxtreme.pro.gms

import android.content.Context
import android.content.pm.PackageManager
import com.cloudxtreme.pro.core.model.GmsMode
import com.cloudxtreme.pro.core.utils.AppLogger
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

data class GmsEnvironmentReport(
    val hostGmsAvailable: Boolean,
    val hostGmsVersionCode: Int,
    val isGooglePlayStoreInstalled: Boolean,
    val isMicroGSupported: Boolean,
    val isDeviceCertified: Boolean,
    val recommendedMode: GmsMode,
    val description: String
)

/**
 * Handles legitimate Google Play Services ecosystem evaluation and integration.
 * Complies strictly with licensing by using detected host APIs or open-source MicroG stubs
 * without bundling proprietary GMS binaries or impersonating certified devices.
 */
class GmsCompatibilityEngine(private val appContext: Context) {

    fun evaluateGmsCompatibility(): GmsEnvironmentReport {
        val googleApi = GoogleApiAvailability.getInstance()
        val resultCode = googleApi.isGooglePlayServicesAvailable(appContext)
        val isHostAvailable = resultCode == ConnectionResult.SUCCESS

        val pm = appContext.packageManager
        val hasPlayStore = isPackageInstalled("com.android.vending", pm)
        val hasGmsPackage = isPackageInstalled("com.google.android.gms", pm)
        val hasMicroG = isPackageInstalled("org.microg.gms.core", pm)

        var gmsVersion = 0
        if (hasGmsPackage) {
            try {
                val pi = pm.getPackageInfo("com.google.android.gms", 0)
                gmsVersion = pi.versionCode
            } catch (e: Exception) {
                // Ignore
            }
        }

        val recommended = when {
            isHostAvailable && hasPlayStore -> GmsMode.MICRO_G
            hasMicroG -> GmsMode.MICRO_G
            else -> GmsMode.OFF
        }

        val description = when {
            isHostAvailable -> "Host device features verified Google Play Services (v$gmsVersion). Virtual environments can bridge services via isolated microG architecture."
            hasMicroG -> "Open-source MicroG Core detected on system. Ready for sandbox authentication."
            else -> "Google Play Services not installed or not certified. Virtual instances will run in pure isolated AOSP mode."
        }

        AppLogger.i("GmsCompatibilityEngine", "Report: hostGms=$isHostAvailable, recommended=$recommended")

        return GmsEnvironmentReport(
            hostGmsAvailable = isHostAvailable,
            hostGmsVersionCode = gmsVersion,
            isGooglePlayStoreInstalled = hasPlayStore,
            isMicroGSupported = true,
            isDeviceCertified = isHostAvailable,
            recommendedMode = recommended,
            description = description
        )
    }

    private fun isPackageInstalled(packageName: String, pm: PackageManager): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
