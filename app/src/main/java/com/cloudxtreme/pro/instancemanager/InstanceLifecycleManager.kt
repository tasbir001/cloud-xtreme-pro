package com.cloudxtreme.pro.instancemanager

import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.result.OperationResult
import com.cloudxtreme.pro.virtualization.VirtualizationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coordinates instance lifecycle state transitions with the VirtualizationEngine
 * and persists status updates to the InstanceRepository.
 */
class InstanceLifecycleManager(
    private val repository: InstanceRepository,
    private val engine: VirtualizationEngine
) {

    suspend fun startInstance(instance: VirtualInstance): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        repository.updateStatus(instance.id, InstanceStatus.STARTING)
        val result = engine.launchInstance(instance)
        if (result is OperationResult.Success) {
            repository.updateStatus(instance.id, InstanceStatus.RUNNING)
        } else {
            repository.updateStatus(instance.id, InstanceStatus.STOPPED)
        }
        result
    }

    suspend fun stopInstance(instanceId: String): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        val result = engine.stopInstance(instanceId)
        if (result is OperationResult.Success) {
            repository.updateStatus(instanceId, InstanceStatus.STOPPED)
        }
        result
    }

    suspend fun pauseInstance(instanceId: String): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        val result = engine.pauseInstance(instanceId)
        if (result is OperationResult.Success) {
            repository.updateStatus(instanceId, InstanceStatus.PAUSED)
        }
        result
    }

    suspend fun resumeInstance(instanceId: String): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        val result = engine.resumeInstance(instanceId)
        if (result is OperationResult.Success) {
            repository.updateStatus(instanceId, InstanceStatus.RUNNING)
        }
        result
    }

    suspend fun restartInstance(instance: VirtualInstance): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        stopInstance(instance.id)
        kotlinx.coroutines.delay(400)
        startInstance(instance)
    }

    suspend fun deleteInstance(instanceId: String): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        engine.deleteInstance(instanceId)
        repository.deleteInstance(instanceId)
        OperationResult.Success(true)
    }
}
