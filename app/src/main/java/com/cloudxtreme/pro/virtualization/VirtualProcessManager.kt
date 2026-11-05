package com.cloudxtreme.pro.virtualization

import android.os.SystemClock
import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks and controls execution state, memory, and lifecycle of virtual instances.
 */
class VirtualProcessManager {

    data class ActiveProcessInfo(
        val instanceId: String,
        val packageName: String,
        val startTimeMs: Long,
        val threadId: Long,
        var status: InstanceStatus,
        var memoryUsedMb: Long
    )

    private val activeProcesses = ConcurrentHashMap<String, ActiveProcessInfo>()
    private val _runningCountFlow = MutableStateFlow(0)
    val runningCountFlow: StateFlow<Int> = _runningCountFlow.asStateFlow()

    fun markStarted(instance: VirtualInstance) {
        val info = ActiveProcessInfo(
            instanceId = instance.id,
            packageName = instance.targetPackageName,
            startTimeMs = SystemClock.elapsedRealtime(),
            threadId = Thread.currentThread().id,
            status = InstanceStatus.RUNNING,
            memoryUsedMb = instance.config.memoryLimitMb.coerceAtMost(256L)
        )
        activeProcesses[instance.id] = info
        _runningCountFlow.value = activeProcesses.size
        AppLogger.i("VirtualProcessManager", "Instance ${instance.id} marked RUNNING. Total running: ${activeProcesses.size}")
    }

    fun markPaused(instanceId: String) {
        activeProcesses[instanceId]?.let {
            it.status = InstanceStatus.PAUSED
            AppLogger.i("VirtualProcessManager", "Instance $instanceId marked PAUSED")
        }
    }

    fun markResumed(instanceId: String) {
        activeProcesses[instanceId]?.let {
            it.status = InstanceStatus.RUNNING
            AppLogger.i("VirtualProcessManager", "Instance $instanceId marked RESUMED")
        }
    }

    fun markStopped(instanceId: String) {
        activeProcesses.remove(instanceId)
        _runningCountFlow.value = activeProcesses.size
        AppLogger.i("VirtualProcessManager", "Instance $instanceId marked STOPPED. Total running: ${activeProcesses.size}")
    }

    fun isRunning(instanceId: String): Boolean {
        val status = activeProcesses[instanceId]?.status
        return status == InstanceStatus.RUNNING || status == InstanceStatus.STARTING
    }

    fun getStatus(instanceId: String): InstanceStatus {
        return activeProcesses[instanceId]?.status ?: InstanceStatus.STOPPED
    }

    fun getMemoryUsedMb(instanceId: String): Long {
        return activeProcesses[instanceId]?.memoryUsedMb ?: 0L
    }

    fun getAllRunningInstances(): List<String> {
        return activeProcesses.keys().toList()
    }
}
