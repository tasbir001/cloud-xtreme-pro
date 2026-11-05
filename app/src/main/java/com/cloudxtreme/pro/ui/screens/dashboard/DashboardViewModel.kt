package com.cloudxtreme.pro.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudxtreme.pro.CloudXtremeApplication
import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.utils.DeviceResourceMetrics
import com.cloudxtreme.pro.core.utils.ResourceMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalInstances: Int = 0,
    val runningInstances: Int = 0,
    val stoppedInstances: Int = 0,
    val totalInstalledApps: Int = 0,
    val resourceMetrics: DeviceResourceMetrics? = null,
    val cloudUsedHours: Double = 4.2,
    val cloudTotalHours: Double = 15.0,
    val cloudUsedStorageGb: Double = 5.8,
    val cloudTotalStorageGb: Double = 20.0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CloudXtremeApplication
    val instances: StateFlow<List<VirtualInstance>> = app.instanceRepository.instances

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
        startPeriodicUpdate()
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            val metrics = ResourceMonitor.getDeviceMetrics(app)
            val currentInstances = instances.value
            val runningCount = currentInstances.count { it.status == InstanceStatus.RUNNING || it.status == InstanceStatus.STARTING }
            val stoppedCount = currentInstances.count { it.status == InstanceStatus.STOPPED || it.status == InstanceStatus.PAUSED }
            val quota = app.cloudPhoneService.freeTierManager.quota.value

            _uiState.value = DashboardUiState(
                totalInstances = currentInstances.size,
                runningInstances = runningCount,
                stoppedInstances = stoppedCount,
                totalInstalledApps = currentInstances.map { it.targetPackageName }.distinct().size,
                resourceMetrics = metrics,
                cloudUsedHours = quota.usedFreeHours,
                cloudTotalHours = quota.totalFreeHoursPerMonth,
                cloudUsedStorageGb = quota.usedStorageGb,
                cloudTotalStorageGb = quota.totalStorageGb
            )
        }
    }

    private fun startPeriodicUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(3000)
                loadMetrics()
            }
        }
    }

    fun launchInstance(instance: VirtualInstance) {
        viewModelScope.launch {
            app.lifecycleManager.startInstance(instance)
            loadMetrics()
        }
    }
}
