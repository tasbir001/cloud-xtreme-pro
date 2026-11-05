package com.cloudxtreme.pro.ui.screens.apkmanager

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudxtreme.pro.CloudXtremeApplication
import com.cloudxtreme.pro.core.model.ApkPackageInfo
import com.cloudxtreme.pro.core.model.EnvironmentConfig
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.result.OperationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ApkManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CloudXtremeApplication

    private val _deviceApps = MutableStateFlow<List<ApkPackageInfo>>(emptyList())
    val deviceApps: StateFlow<List<ApkPackageInfo>> = _deviceApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _installProgress = MutableStateFlow<Pair<Int, String>?>(null)
    val installProgress: StateFlow<Pair<Int, String>?> = _installProgress.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps(includeSystem: Boolean = false) {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = app.appsScanner.getInstalledApps(includeSystem)
            _deviceApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun cloneAppToNewInstance(apkInfo: ApkPackageInfo) {
        viewModelScope.launch {
            val sourceFile = File(apkInfo.apkPath)
            if (!sourceFile.exists()) {
                Toast.makeText(app, "Source APK not accessible", Toast.LENGTH_SHORT).show()
                return@launch
            }

            _installProgress.value = 10 to "Preparing sandbox..."
            val result = app.apkInstallerService.installApkAsNewInstance(
                sourceApkFile = sourceFile,
                customName = "${apkInfo.appName} – Instance 1",
                config = EnvironmentConfig(),
                onProgress = { pct, msg ->
                    _installProgress.value = pct to msg
                }
            )

            _installProgress.value = null
            when (result) {
                is OperationResult.Success -> {
                    Toast.makeText(app, "Created: ${result.data.name}", Toast.LENGTH_SHORT).show()
                }
                is OperationResult.Error -> {
                    Toast.makeText(app, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    fun installApkFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                _installProgress.value = 15 to "Copying APK from storage..."
                val tempFile = File(app.cacheDir, "imported_${System.currentTimeMillis()}.apk")
                app.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                _installProgress.value = 40 to "Parsing APK and manifest..."
                val result = app.apkInstallerService.installApkAsNewInstance(
                    sourceApkFile = tempFile,
                    onProgress = { pct, msg ->
                        _installProgress.value = pct to msg
                    }
                )

                _installProgress.value = null
                tempFile.delete()

                when (result) {
                    is OperationResult.Success -> {
                        Toast.makeText(app, "Imported: ${result.data.name}", Toast.LENGTH_SHORT).show()
                    }
                    is OperationResult.Error -> {
                        Toast.makeText(app, "Import failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _installProgress.value = null
                Toast.makeText(app, "Failed to read APK: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
