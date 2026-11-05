package com.cloudxtreme.pro.core.utils

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID

object SecurityUtils {

    fun generateUniqueInstanceId(): String {
        return "inst_" + UUID.randomUUID().toString().replace("-", "").take(12)
    }

    fun generateSessionToken(): String {
        return "cxp_" + UUID.randomUUID().toString().replace("-", "")
    }

    /**
     * Sanitizes instance name and directory strings.
     */
    fun sanitizeIdentifier(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(64)
    }

    /**
     * Computes SHA-256 hash of a file for integrity verification.
     */
    fun calculateFileSha256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hash = digest.digest()
        return hash.joinToString("") { "%02x".format(it) }
    }
}
