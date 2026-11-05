package com.cloudxtreme.pro.virtualization.workprofile

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import com.cloudxtreme.pro.core.utils.AppLogger

/**
 * Technical bridge for Android Work Profiles and multi-user environments.
 */
object WorkProfileBridge {

    data class WorkProfileStatus(
        val isSupportedOnDevice: Boolean,
        val isManagedProfileActive: Boolean,
        val canProvisionWorkProfile: Boolean,
        val description: String
    )

    fun checkStatus(context: Context): WorkProfileStatus {
        val pm = context.packageManager
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val um = context.getSystemService(Context.USER_SERVICE) as? UserManager

        val hasManagedUsersFeature = pm.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)
        var isManagedProfile = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && um != null) {
            isManagedProfile = um.isManagedProfile
        }

        val canProvision = hasManagedUsersFeature && dpm != null

        val description = when {
            isManagedProfile -> "Currently executing inside an isolated Work Profile user container."
            hasManagedUsersFeature -> "Device supports Android Enterprise isolated Work Profiles."
            else -> "Standard single-user system. Using Cloud Xtreme Pro container virtualization."
        }

        AppLogger.i("WorkProfileBridge", "Status: $description")

        return WorkProfileStatus(
            isSupportedOnDevice = hasManagedUsersFeature,
            isManagedProfileActive = isManagedProfile,
            canProvisionWorkProfile = canProvision,
            description = description
        )
    }
}
