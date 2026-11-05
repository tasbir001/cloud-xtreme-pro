package com.cloudxtreme.pro.ui.screens.cloudphone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudxtreme.pro.CloudXtremeApplication
import com.cloudxtreme.pro.cloud.RemoteStreamController
import com.cloudxtreme.pro.cloud.TouchInputProtocol
import com.cloudxtreme.pro.cloud.VirtualCloudAndroidOS
import com.cloudxtreme.pro.core.model.CloudInstance
import com.cloudxtreme.pro.core.model.CloudQuota
import com.cloudxtreme.pro.core.model.CloudServerRegion
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CloudPhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CloudXtremeApplication
    private val cloudService = app.cloudPhoneService

    val cloudInstances: StateFlow<List<CloudInstance>> = cloudService.cloudInstances
    val activeInstanceId: StateFlow<String?> = cloudService.activeInstanceId
    val streamMetrics: StateFlow<RemoteStreamController.StreamMetrics> = cloudService.streamController.streamMetrics
    val cloudOsState: StateFlow<VirtualCloudAndroidOS.CloudOsState> = cloudService.streamController.cloudOs.osState
    val quota: StateFlow<CloudQuota> = cloudService.freeTierManager.quota

    init {
        // Start stream by default for immediate preview
        cloudService.streamController.startStream(CloudServerRegion.US_EAST)
    }

    fun startCloudInstance(instanceId: String) {
        cloudService.startCloudInstance(instanceId)
    }

    fun stopCloudInstance(instanceId: String) {
        cloudService.stopCloudInstance(instanceId)
    }

    fun restartCloudInstance(instanceId: String) {
        cloudService.restartCloudInstance(instanceId)
    }

    fun setServerRegion(region: CloudServerRegion) {
        cloudService.streamController.startStream(region)
    }

    fun setResolution(res: String) {
        cloudService.streamController.setResolution(res)
    }

    fun onTouchDown(normalizedX: Float, normalizedY: Float) {
        cloudService.streamController.dispatchTouchEvent(
            TouchInputProtocol.TouchEventPacket(
                action = TouchInputProtocol.TouchAction.DOWN,
                pointers = listOf(
                    TouchInputProtocol.TouchPointer(0, normalizedX, normalizedY)
                )
            )
        )
    }

    fun onTouchUp() {
        cloudService.streamController.dispatchTouchEvent(
            TouchInputProtocol.TouchEventPacket(
                action = TouchInputProtocol.TouchAction.UP,
                pointers = emptyList()
            )
        )
    }

    fun onSpecialKey(action: TouchInputProtocol.KeyAction) {
        cloudService.streamController.dispatchSpecialKey(action)
    }

    fun openOsApp(window: VirtualCloudAndroidOS.ActiveWindow) {
        cloudService.streamController.cloudOs.openApp(window)
    }

    fun executeAdbCommand(cmd: String) {
        cloudService.streamController.cloudOs.executeTerminalCommand(cmd)
    }
}
