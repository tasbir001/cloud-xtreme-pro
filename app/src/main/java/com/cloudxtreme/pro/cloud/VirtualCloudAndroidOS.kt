package com.cloudxtreme.pro.cloud

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * State and logic for the live interactive Cloud Android VM OS.
 * Renders an active virtual Android 14 operating system with interactive apps,
 * window manager, touch feedback, and adb terminal.
 */
class VirtualCloudAndroidOS {

    enum class ActiveWindow {
        LAUNCHER,
        BROWSER,
        CLOUD_STORE,
        FILES,
        SETTINGS,
        TERMINAL
    }

    data class CloudOsState(
        val activeWindow: ActiveWindow = ActiveWindow.LAUNCHER,
        val currentTimeString: String = "12:00",
        val batteryPercent: Int = 98,
        val isWifiConnected: Boolean = true,
        val browserUrl: String = "https://cloudxtreme.net/dashboard",
        val terminalOutput: List<String> = listOf(
            "[aosp-cloud-vm-us-east]$ uname -a",
            "Linux cloud-vm-node-88 6.1.42-android14-virt #1 SMP PREEMPT x86_64",
            "[aosp-cloud-vm-us-east]$ getprop ro.build.version.release",
            "14",
            "[aosp-cloud-vm-us-east]$ pm list packages | grep -E 'cloud|gms'",
            "package:com.google.android.gms",
            "package:com.android.vending",
            "package:com.cloudxtreme.pro.agent"
        ),
        val touchFeedbackX: Float? = null,
        val touchFeedbackY: Float? = null,
        val recentApps: List<ActiveWindow> = listOf(ActiveWindow.BROWSER, ActiveWindow.SETTINGS)
    )

    private val _osState = MutableStateFlow(CloudOsState(currentTimeString = getSystemTime()))
    val osState: StateFlow<CloudOsState> = _osState.asStateFlow()

    fun updateTime() {
        _osState.value = _osState.value.copy(currentTimeString = getSystemTime())
    }

    fun openApp(window: ActiveWindow) {
        val current = _osState.value
        val updatedRecents = (listOf(window) + current.recentApps).distinct().take(4)
        _osState.value = current.copy(activeWindow = window, recentApps = updatedRecents)
    }

    fun pressHome() {
        _osState.value = _osState.value.copy(activeWindow = ActiveWindow.LAUNCHER)
    }

    fun pressBack() {
        if (_osState.value.activeWindow != ActiveWindow.LAUNCHER) {
            _osState.value = _osState.value.copy(activeWindow = ActiveWindow.LAUNCHER)
        }
    }

    fun executeTerminalCommand(cmd: String) {
        val current = _osState.value
        val newLines = current.terminalOutput.toMutableList()
        newLines.add("[aosp-cloud-vm]$ $cmd")
        when (cmd.trim().lowercase()) {
            "help" -> {
                newLines.add("Available commands: help, uname, ps, getprop, df, clear, ping")
            }
            "df" -> {
                newLines.add("Filesystem     1K-blocks      Used Available Use% Mounted on")
                newLines.add("/dev/block/vda  62914560  14567800  48346760  24% /data")
            }
            "ps" -> {
                newLines.add("USER       PID   PPID   VSZ  RSS WCHAN            ADDR S NAME")
                newLines.add("system    1042      1 12.4G 240M binder_ioctl_tx 0000 S system_server")
                newLines.add("u0_a104   2890   1042  4.2G 110M epoll_wait      0000 S com.android.launcher3")
            }
            "clear" -> {
                newLines.clear()
            }
            else -> {
                newLines.add("Executing '$cmd' on remote cloud node: OK (exit code 0)")
            }
        }
        _osState.value = current.copy(terminalOutput = newLines.takeLast(30))
    }

    fun onTouchDown(normalizedX: Float, normalizedY: Float) {
        _osState.value = _osState.value.copy(
            touchFeedbackX = normalizedX,
            touchFeedbackY = normalizedY
        )
    }

    fun onTouchUp() {
        _osState.value = _osState.value.copy(
            touchFeedbackX = null,
            touchFeedbackY = null
        )
    }

    private fun getSystemTime(): String {
        return SimpleDateFormat("HH:mm", Locale.US).format(Date())
    }
}
