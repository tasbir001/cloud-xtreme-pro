package com.cloudxtreme.pro.apkmanager

import android.content.Context
import com.cloudxtreme.pro.core.model.EnvironmentConfig
import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.result.OperationResult
import com.cloudxtreme.pro.core.utils.AppLogger
import com.cloudxtreme.pro.core.utils.SecurityUtils
import com.cloudxtreme.pro.instancemanager.InstanceRepository
import com.cloudxtreme.pro.virtualization.VirtualFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles installing imported or cloned APKs into virtual instances.
 */
class ApkInstallerService(
    private val appContext: Context,
    private val vfs: VirtualFileSystem,
    private val repository: InstanceRepository,
    private val parserService: PackageParserService
) {

    /**
     * Installs an APK from file into a brand new virtual instance.
     */
    suspend fun installApkAsNewInstance(
        sourceApkFile: File,
        customName: String? = null,
        config: EnvironmentConfig = EnvironmentConfig(),
        onProgress: ((Int, String) -> Unit)? = null
    ): OperationResult<VirtualInstance> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(10, "Parsing APK headers and manifest...")
            val parseResult = parserService.parseApkFile(sourceApkFile)
            if (parseResult is OperationResult.Error) {
                return@withContext parseResult
            }
            val apkInfo = (parseResult as OperationResult.Success).data

            val instanceId = SecurityUtils.generateUniqueInstanceId()
            val existingCount = repository.instances.value.count { it.targetPackageName == apkInfo.packageName }
            val cloneIndex = existingCount + 1
            val instanceName = customName ?: "${apkInfo.appName} – Instance $cloneIndex"

            onProgress?.invoke(30, "Initializing isolated virtual sandbox...")
            vfs.initializeInstanceStorage(instanceId, apkInfo.packageName)

            onProgress?.invoke(60, "Copying APK and extracting native ABIs...")
            val sandboxedApk = vfs.installApkToSandbox(instanceId, sourceApkFile)

            onProgress?.invoke(90, "Registering instance profile...")
            val storageUsed = vfs.calculateInstanceStorageBytes(instanceId)

            val newInstance = VirtualInstance(
                id = instanceId,
                name = instanceName,
                targetPackageName = apkInfo.packageName,
                appLabel = apkInfo.appName,
                versionName = apkInfo.versionName,
                versionCode = apkInfo.versionCode,
                status = InstanceStatus.STOPPED,
                config = config,
                storageUsedBytes = storageUsed,
                cloneIndex = cloneIndex,
                apkFilePath = sandboxedApk.absolutePath,
                nativeLibDir = vfs.getInstanceLibDir(instanceId).absolutePath
            )

            repository.addInstance(newInstance)
            onProgress?.invoke(100, "Installation complete!")

            AppLogger.i("ApkInstallerService", "Successfully installed ${apkInfo.packageName} into instance $instanceId")
            OperationResult.Success(newInstance)
        } catch (e: Exception) {
            AppLogger.e("ApkInstallerService", "Failed to install APK", e)
            OperationResult.Error("Installation failed: ${e.message}", e)
        }
    }
}
