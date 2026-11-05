package com.cloudxtreme.pro.cloud

import com.cloudxtreme.pro.core.model.CloudInstance
import com.cloudxtreme.pro.core.model.CloudServerRegion
import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.core.utils.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Manages low-latency screen streaming, network statistics, and telemetry
 * for the remote cloud Android instance.
 */
class RemoteStreamController {

    data class StreamMetrics(
        val isConnected: Boolean = false,
        val fps: Int = 60,
        val pingMs: Int = 38,
        val bitrateMbps: Double = 14.2,
        val resolution: String = "1080x2400 (FHD+)",
        val frameLossPct: Float = 0.02f,
        val serverRegion: CloudServerRegion = CloudServerRegion.US_EAST
    )

    private val _streamMetrics = MutableStateFlow(StreamMetrics())
    val streamMetrics: StateFlow<StreamMetrics> = _streamMetrics.asStateFlow()

    val cloudOs = VirtualCloudAndroidOS()

    private var telemetryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startStream(region: CloudServerRegion = CloudServerRegion.US_EAST) {
        AppLogger.i("RemoteStreamController", "Starting remote stream to cloud node in region: ${region.displayName}")

        telemetryJob?.cancel()
        _streamMetrics.value = _streamMetrics.value.copy(
            isConnected = true,
            serverRegion = region
        )

        telemetryJob = scope.launch {
            while (isActive) {
                delay(1000)
                cloudOs.updateTime()

                // Realistic jitter calculation based on region base latency
                val basePing = when (region) {
                    CloudServerRegion.US_EAST -> 36
                    CloudServerRegion.US_WEST -> 48
                    CloudServerRegion.EU_CENTRAL -> 68
                    CloudServerRegion.ASIA_EAST -> 85
                    CloudServerRegion.CUSTOM -> 20
                }

                val currentPing = basePing + Random.nextInt(-4, 5)
                val currentFps = Random.nextInt(59, 61)
                val currentBitrate = 12.0 + Random.nextDouble(0.5, 4.0)

                _streamMetrics.value = _streamMetrics.value.copy(
                    fps = currentFps,
                    pingMs = currentPing.coerceAtLeast(15),
                    bitrateMbps = (currentBitrate * 10).toInt() / 10.0
                )
            }
        }
    }

    fun stopStream() {
        telemetryJob?.cancel()
        _streamMetrics.value = _streamMetrics.value.copy(isConnected = false)
        AppLogger.i("RemoteStreamController", "Remote stream stopped")
    }

    fun setResolution(res: String) {
        _streamMetrics.value = _streamMetrics.value.copy(resolution = res)
    }

    fun dispatchTouchEvent(event: TouchInputProtocol.TouchEventPacket) {
        // Feed into cloud OS
        if (event.pointers.isNotEmpty()) {
            val primary = event.pointers[0]
            when (event.action) {
                TouchInputProtocol.TouchAction.DOWN, TouchInputProtocol.TouchAction.MOVE -> {
                    cloudOs.onTouchDown(primary.normalizedX, primary.normalizedY)
                }
                TouchInputProtocol.TouchAction.UP, TouchInputProtocol.TouchAction.CANCEL -> {
                    cloudOs.onTouchUp()
                }
            }
        }
    }

    fun dispatchSpecialKey(keyAction: TouchInputProtocol.KeyAction) {
        when (keyAction) {
            TouchInputProtocol.KeyAction.SPECIAL_HOME -> cloudOs.pressHome()
            TouchInputProtocol.KeyAction.SPECIAL_BACK -> cloudOs.pressBack()
            else -> {}
        }
    }
}
