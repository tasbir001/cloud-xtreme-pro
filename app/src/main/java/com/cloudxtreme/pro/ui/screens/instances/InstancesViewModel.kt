package com.cloudxtreme.pro.ui.screens.instances

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudxtreme.pro.CloudXtremeApplication
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.result.OperationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InstancesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CloudXtremeApplication
    val instances: StateFlow<List<VirtualInstance>> = app.instanceRepository.instances

    private val _isOperationInProgress = MutableStateFlow(false)
    val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    fun launchInstance(instance: VirtualInstance) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val result = app.lifecycleManager.startInstance(instance)
            _isOperationInProgress.value = false
            if (result is OperationResult.Error) {
                Toast.makeText(app, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun stopInstance(instanceId: String) {
        viewModelScope.launch {
            app.lifecycleManager.stopInstance(instanceId)
        }
    }

    fun restartInstance(instance: VirtualInstance) {
        viewModelScope.launch {
            app.lifecycleManager.restartInstance(instance)
        }
    }

    fun cloneInstance(sourceId: String) {
        viewModelScope.launch {
            try {
                val cloned = app.instanceRepository.cloneInstance(sourceId)
                Toast.makeText(app, "Cloned: ${cloned.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(app, "Cloning failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun backupInstance(instanceId: String) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val result = app.backupEngine.createBackup(instanceId)
            _isOperationInProgress.value = false
            when (result) {
                is OperationResult.Success -> {
                    Toast.makeText(app, "Backup saved to: ${result.data.name}", Toast.LENGTH_LONG).show()
                }
                is OperationResult.Error -> {
                    Toast.makeText(app, "Backup error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    fun deleteInstance(instanceId: String) {
        viewModelScope.launch {
            app.lifecycleManager.deleteInstance(instanceId)
            Toast.makeText(app, "Instance deleted", Toast.LENGTH_SHORT).show()
        }
    }
}
