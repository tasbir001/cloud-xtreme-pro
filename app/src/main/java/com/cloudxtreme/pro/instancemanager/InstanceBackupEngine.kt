package com.cloudxtreme.pro.instancemanager

import android.content.Context
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.result.OperationResult
import com.cloudxtreme.pro.core.utils.AppLogger
import com.cloudxtreme.pro.core.utils.FileUtils
import com.cloudxtreme.pro.core.utils.SecurityUtils
import com.cloudxtreme.pro.virtualization.VirtualFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles exporting and importing full instance state backups (.cxp packages).
 */
class InstanceBackupEngine(
    private val appContext: Context,
    private val vfs: VirtualFileSystem,
    private val repository: InstanceRepository,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) {

    private val backupsDir: File
        get() = File(appContext.filesDir, "backups").apply {
            if (!exists()) mkdirs()
        }

    fun getAvailableBackups(): List<File> {
        return backupsDir.listFiles { _, name -> name.endsWith(".cxp") || name.endsWith(".zip") }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()
    }

    /**
     * Creates a compressed backup archive of an instance.
     */
    suspend fun createBackup(
        instanceId: String,
        onProgress: ((Int) -> Unit)? = null
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            val instance = repository.getInstance(instanceId)
                ?: return@withContext OperationResult.Error("Instance $instanceId not found")

            val instanceRoot = vfs.getInstanceRoot(instanceId)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeName = SecurityUtils.sanitizeIdentifier(instance.name)
            val backupFile = File(backupsDir, "${safeName}_$timestamp.cxp")

            // Write metadata file inside instance root before archiving
            val metadataFile = File(instanceRoot, "backup_manifest.json")
            metadataFile.writeText(json.encodeToString(instance))

            FileUtils.zipDirectory(instanceRoot, backupFile, onProgress)

            AppLogger.i("InstanceBackupEngine", "Created backup: ${backupFile.absolutePath} (${backupFile.length()} bytes)")
            OperationResult.Success(backupFile)
        } catch (e: Exception) {
            AppLogger.e("InstanceBackupEngine", "Failed to create backup", e)
            OperationResult.Error("Backup failed: ${e.message}", e)
        }
    }

    /**
     * Restores an instance from a .cxp backup archive.
     */
    suspend fun restoreBackup(
        backupFile: File,
        targetInstanceName: String? = null,
        onProgress: ((Int) -> Unit)? = null
    ): OperationResult<VirtualInstance> = withContext(Dispatchers.IO) {
        try {
            val tempExtractDir = File(appContext.cacheDir, "restore_temp_" + UUID.randomUUID())
            tempExtractDir.mkdirs()

            FileUtils.unzipArchive(backupFile, tempExtractDir, onProgress)

            val manifestFile = File(tempExtractDir, "backup_manifest.json")
            if (!manifestFile.exists()) {
                tempExtractDir.deleteRecursively()
                return@withContext OperationResult.Error("Invalid backup package: missing backup_manifest.json")
            }

            val originalInstance = json.decodeFromString<VirtualInstance>(manifestFile.readText())
            val newInstanceId = SecurityUtils.generateUniqueInstanceId()
            val newName = targetInstanceName ?: "${originalInstance.name} (Restored)"

            val newRoot = vfs.getInstanceRoot(newInstanceId)
            FileUtils.copyDirectory(tempExtractDir, newRoot)
            tempExtractDir.deleteRecursively()

            val restoredInstance = originalInstance.copy(
                id = newInstanceId,
                name = newName,
                createdAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            )

            repository.addInstance(restoredInstance)
            AppLogger.i("InstanceBackupEngine", "Restored instance ${restoredInstance.id} from ${backupFile.name}")
            OperationResult.Success(restoredInstance)
        } catch (e: Exception) {
            AppLogger.e("InstanceBackupEngine", "Failed to restore backup", e)
            OperationResult.Error("Restore failed: ${e.message}", e)
        }
    }
}
