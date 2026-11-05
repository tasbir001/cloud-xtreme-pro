package com.cloudxtreme.pro.cloud

import com.cloudxtreme.pro.core.model.CloudQuota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages transparent resource tracking for Google Cloud free-tier allocations
 * and potential hosting costs.
 */
class CloudFreeTierManager {

    private val _quota = MutableStateFlow(
        CloudQuota(
            totalFreeHoursPerMonth = 15.0,
            usedFreeHours = 4.2,
            totalStorageGb = 20.0,
            usedStorageGb = 5.8,
            maxConcurrentInstances = 2,
            activeConcurrentInstances = 1,
            estimatedHourlyCostUsd = 0.075
        )
    )
    val quota: StateFlow<CloudQuota> = _quota.asStateFlow()

    fun recordActiveSession(minutes: Int) {
        val hours = minutes / 60.0
        val current = _quota.value
        val updatedUsed = (current.usedFreeHours + hours).coerceAtMost(current.totalFreeHoursPerMonth)
        _quota.value = current.copy(usedFreeHours = (updatedUsed * 10).toInt() / 10.0)
    }

    fun isQuotaExceeded(): Boolean {
        val q = _quota.value
        return q.usedFreeHours >= q.totalFreeHoursPerMonth || q.usedStorageGb >= q.totalStorageGb
    }

    fun calculateOverdueCost(additionalHours: Double): Double {
        return additionalHours * _quota.value.estimatedHourlyCostUsd
    }
}
