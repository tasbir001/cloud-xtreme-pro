package com.cloudxtreme.pro.core.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    /**
     * Calculates the total size of a file or directory recursively.
     */
    fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        if (directory.isFile) return directory.length()

        var totalSize = 0L
        val files = directory.listFiles() ?: return 0L
        for (file in files) {
            totalSize += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return totalSize
    }

    /**
     * Formats bytes into human-readable string (e.g. 1.25 GB, 350 MB).
     */
    fun formatFileSize(bytes: Long): String {
        val unit = 1024.0
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit)).toInt()
        val pre = ("KMGTPE")[exp - 1]
        return String.format("%.2f %sB", bytes / Math.pow(unit, exp.toDouble()), pre)
    }

    /**
     * Recursively deletes a directory or file.
     */
    fun deleteRecursively(fileOrDirectory: File): Boolean {
        if (fileOrDirectory.isDirectory) {
            val children = fileOrDirectory.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursively(child)
                }
            }
        }
        return fileOrDirectory.delete()
    }

    /**
     * Copies a directory recursively to a target directory.
     */
    @Throws(IOException::class)
    fun copyDirectory(sourceDir: File, destDir: File) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val items = sourceDir.listFiles() ?: return
        for (item in items) {
            val target = File(destDir, item.name)
            if (item.isDirectory) {
                copyDirectory(item, target)
            } else {
                copyFile(item, target)
            }
        }
    }

    /**
     * Copies a single file using streaming buffers.
     */
    @Throws(IOException::class)
    fun copyFile(source: File, destination: File) {
        source.inputStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Compresses a directory into a target ZIP / CXP archive.
     */
    @Throws(IOException::class)
    fun zipDirectory(sourceDir: File, outputZip: File, onProgress: ((Int) -> Unit)? = null) {
        outputZip.parentFile?.mkdirs()
        val totalFiles = countFiles(sourceDir).coerceAtLeast(1)
        var processedFiles = 0

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
            zipFolderRecursive(sourceDir, sourceDir, zos) {
                processedFiles++
                val pct = ((processedFiles.toDouble() / totalFiles) * 100).toInt()
                onProgress?.invoke(pct.coerceIn(0, 100))
            }
        }
    }

    private fun countFiles(file: File): Int {
        if (file.isFile) return 1
        var count = 0
        file.listFiles()?.forEach { count += countFiles(it) }
        return count
    }

    private fun zipFolderRecursive(
        root: File,
        current: File,
        zos: ZipOutputStream,
        onFileAdded: () -> Unit
    ) {
        val files = current.listFiles() ?: return
        val buffer = ByteArray(8192)

        for (file in files) {
            val relativePath = root.toURI().relativize(file.toURI()).path
            if (file.isDirectory) {
                zos.putNextEntry(ZipEntry(if (relativePath.endsWith("/")) relativePath else "$relativePath/"))
                zos.closeEntry()
                zipFolderRecursive(root, file, zos, onFileAdded)
            } else {
                val entry = ZipEntry(relativePath)
                zos.putNextEntry(entry)
                FileInputStream(file).use { fis ->
                    var count: Int
                    while (fis.read(buffer).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                    }
                }
                zos.closeEntry()
                onFileAdded()
            }
        }
    }

    /**
     * Unzips an archive into a target directory.
     */
    @Throws(IOException::class)
    fun unzipArchive(zipFile: File, targetDir: File, onProgress: ((Int) -> Unit)? = null) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(8192)

            while (entry != null) {
                val outFile = File(targetDir, entry.name)

                // Prevent zip slip vulnerability
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Zip entry is outside of target dir: ${entry.name}")
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
