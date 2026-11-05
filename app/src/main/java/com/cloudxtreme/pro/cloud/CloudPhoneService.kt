package com.cloudxtreme.pro.cloud

import com.cloudxtreme.pro.core.model.CloudInstance
import com.cloudxtreme.pro.core.model.CloudServerRegion
import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.core.result.OperationResult
import com.cloudxtreme.pro.core.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service managing Cloud-hosted Android instances and remote streaming sessions.
 */
class CloudPhoneService {

    val streamController = RemoteStreamController()
    val freeTierManager = CloudFreeTierManager()

    private val _cloudInstances = MutableStateFlow<List<CloudInstance>>(
        listOf(
            CloudInstance(
                id = "cloud_node_01",
                name = "Cloud Phone Pro (US East)",
                region = CloudServerRegion.US_EAST,
                status = InstanceStatus.STOPPED,
                ipAddress = "34.148.12.90",
                streamingPort = 8443,
                osVersion = "Android 14 (Cuttlefish/Redroid VM)",
                pingMs = 38
            ),
            CloudInstance(
                id = "cloud_node_02",
                name = "Cloud Phone Pro (EU Central)",
                region = CloudServerRegion.EU_CENTRAL,
                status = InstanceStatus.STOPPED,
                ipAddress = "35.198.77.104",
                streamingPort = 8443,
                osVersion = "Android 14 (Cuttlefish/Redroid VM)",
                pingMs = 68
            )
        )
    )
    val cloudInstances: StateFlow<List<CloudInstance>> = _cloudInstances.asStateFlow()

    private val _activeInstanceId = MutableStateFlow<String?>(null)
    val activeInstanceId: StateFlow<String?> = _activeInstanceId.asStateFlow()

    fun startCloudInstance(instanceId: String): OperationResult<Boolean> {
        val instance = _cloudInstances.value.firstOrNull { it.id == instanceId }
            ?: return OperationResult.Error("Cloud instance $instanceId not found")

        _cloudInstances.value = _cloudInstances.value.map {
            if (it.id == instanceId) it.copy(status = InstanceStatus.RUNNING) else it
        }
        _activeInstanceId.value = instanceId
        streamController.startStream(instance.region)

        AppLogger.i("CloudPhoneService", "Started cloud instance $instanceId")
        return OperationResult.Success(true)
    }

    fun stopCloudInstance(instanceId: String): OperationResult<Boolean> {
        _cloudInstances.value = _cloudInstances.value.map {
            if (it.id == instanceId) it.copy(status = InstanceStatus.STOPPED) else it
        }
        if (_activeInstanceId.value == instanceId) {
            _activeInstanceId.value = null
            streamController.stopStream()
        }

        AppLogger.i("CloudPhoneService", "Stopped cloud instance $instanceId")
        return OperationResult.Success(true)
    }

    fun restartCloudInstance(instanceId: String): OperationResult<Boolean> {
        stopCloudInstance(instanceId)
        return startCloudInstance(instanceId)
    }
}
