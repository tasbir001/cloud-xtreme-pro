package com.cloudxtreme.pro.virtualization

import android.content.Context
import android.content.Intent
import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.result.OperationResult
import com.cloudxtreme.pro.core.utils.AppLogger
import com.cloudxtreme.pro.core.utils.ResourceMonitor
import com.cloudxtreme.pro.virtualization.avf.AvfDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Core engine orchestrator for Cloud Xtreme Pro.
 * Coordinates virtual storage, isolated classloaders, process lifecycle,
 * and container launching.
 */
class VirtualizationEngine(private val appContext: Context) {

    val vfs = VirtualFileSystem(appContext)
    val processManager = VirtualProcessManager()
    val avfCapability = AvfDetector.detectCapability(appContext)

    /**
     * Prepares an instance sandbox and installs the APK into it.
     */
    suspend fun createInstance(
        instanceId: String,
        packageName: String,
        sourceApkFile: File?
    ): OperationResult<VirtualInstance> = withContext(Dispatchers.IO) {
        try {
            vfs.initializeInstanceStorage(instanceId, packageName)

            var installedApk: File? = null
            if (sourceApkFile != null && sourceApkFile.exists()) {
                installedApk = vfs.installApkToSandbox(instanceId, sourceApkFile)
            }

            val storageUsed = vfs.calculateInstanceStorageBytes(instanceId)

            AppLogger.i("VirtualizationEngine", "Created isolated instance $instanceId for $packageName")
            OperationResult.Success(
                VirtualInstance(
                    id = instanceId,
                    name = "$packageName (Instance 1)",
                    targetPackageName = packageName,
                    appLabel = packageName.substringAfterLast("."),
                    versionName = "1.0",
                    versionCode = 1L,
                    status = InstanceStatus.STOPPED,
                    storageUsedBytes = storageUsed,
                    apkFilePath = installedApk?.absolutePath
                )
            )
        } catch (e: Exception) {
            AppLogger.e("VirtualizationEngine", "Failed to create instance $instanceId", e)
            OperationResult.Error("Failed to initialize virtual sandbox: ${e.message}", e)
        }
    }

    /**
     * Launches a virtual instance inside the dedicated container activity.
     */
    suspend fun launchInstance(instance: VirtualInstance): OperationResult<Boolean> = withContext(Dispatchers.Main) {
        try {
            // Check resource limit
            val canAllocate = ResourceMonitor.canAllocateInstance(appContext, instance.config.memoryLimitMb)
            if (!canAllocate) {
                return@withContext OperationResult.Error(
                    "Insufficient RAM to start instance (${instance.config.memoryLimitMb} MB requested). Please close other instances or reduce memory allocation."
                )
            }

            processManager.markStarted(instance)

            val intent = Intent(appContext, VirtualContainerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                putExtra(VirtualContainerActivity.EXTRA_INSTANCE_ID, instance.id)
                putExtra(VirtualContainerActivity.EXTRA_PACKAGE_NAME, instance.targetPackageName)
                putExtra(VirtualContainerActivity.EXTRA_APP_LABEL, instance.appLabel)
                putExtra(VirtualContainerActivity.EXTRA_APK_PATH, instance.apkFilePath)
                putExtra(VirtualContainerActivity.EXTRA_GMS_MODE, instance.config.gmsMode.name)
            }

            appContext.startActivity(intent)
            OperationResult.Success(true)
        } catch (e: Exception) {
            processManager.markStopped(instance.id)
            AppLogger.e("VirtualizationEngine", "Failed to launch instance ${instance.id}", e)
            OperationResult.Error("Failed to launch virtual container: ${e.message}", e)
        }
    }

    /**
     * Stops a running instance.
     */
    fun stopInstance(instanceId: String): OperationResult<Boolean> {
        return try {
            processManager.markStopped(instanceId)
            OperationResult.Success(true)
        } catch (e: Exception) {
            OperationResult.Error("Failed to stop instance: ${e.message}", e)
        }
    }

    /**
     * Pauses an active instance.
     */
    fun pauseInstance(instanceId: String): OperationResult<Boolean> {
        return try {
            processManager.markPaused(instanceId)
            OperationResult.Success(true)
        } catch (e: Exception) {
            OperationResult.Error("Failed to pause instance: ${e.message}", e)
        }
    }

    /**
     * Resumes a paused instance.
     */
    fun resumeInstance(instanceId: String): OperationResult<Boolean> {
        return try {
            processManager.markResumed(instanceId)
            OperationResult.Success(true)
        } catch (e: Exception) {
            OperationResult.Error("Failed to resume instance: ${e.message}", e)
        }
    }

    /**
     * Completely deletes an instance's sandbox and files.
     */
    suspend fun deleteInstance(instanceId: String): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            processManager.markStopped(instanceId)
            val deleted = vfs.deleteInstanceStorage(instanceId)
            OperationResult.Success(deleted)
        } catch (e: Exception) {
            OperationResult.Error("Failed to delete instance storage: ${e.message}", e)
        }
    }
}
