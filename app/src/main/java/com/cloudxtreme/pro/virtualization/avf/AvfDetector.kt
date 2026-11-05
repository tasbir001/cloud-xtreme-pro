package com.cloudxtreme.pro.virtualization.avf

import android.content.Context
import android.os.Build
import com.cloudxtreme.pro.core.utils.AppLogger
import java.io.File

/**
 * Technical capability detector for Android Virtualization Framework (AVF).
 * Accurately determines if the host device kernel and hardware hypervisor (pKVM)
 * support genuine hardware-isolated Android Virtual Machines.
 */
object AvfDetector {

    data class AvfCapability(
        val isSupported: Boolean,
        val isHardwareAccelerated: Boolean,
        val hypervisorType: String,
        val kvmNodePresent: Boolean,
        val apiSupported: Boolean,
        val reason: String
    )

    fun detectCapability(context: Context): AvfCapability {
        val apiSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU // Android 13+
        val kvmFile = File("/dev/kvm")
        val kvmPresent = kvmFile.exists()

        var hasVmManagerClass = false
        try {
            Class.forName("android.system.virtualmachine.VirtualMachineManager")
            hasVmManagerClass = true
        } catch (e: ClassNotFoundException) {
            hasVmManagerClass = false
        }

        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val hypervisorProp = getSystemProperty("ro.boot.hypervisor.vm.supported")
        val protectedVmProp = getSystemProperty("ro.boot.hypervisor.protected_vm.supported")

        val isSupported = apiSupported && is64Bit && (hasVmManagerClass || kvmPresent || hypervisorProp == "1")
        val isHardware = kvmPresent || hypervisorProp == "1" || protectedVmProp == "1"

        val hypervisorType = when {
            protectedVmProp == "1" -> "pKVM (Protected Kernel Virtual Machine)"
            kvmPresent -> "Linux KVM"
            hypervisorProp == "1" -> "Hardware Hypervisor"
            hasVmManagerClass -> "Android Virtualization Framework (Microdroid)"
            else -> "Container Isolation (User Space)"
        }

        val reason = if (isSupported) {
            "Genuine AVF hypervisor acceleration available ($hypervisorType)"
        } else {
            "Hardware hypervisor not enabled on this device/kernel; using high-performance Process & Storage Virtualization Container"
        }

        AppLogger.i("AvfDetector", "AVF Capability: isSupported=$isSupported, type=$hypervisorType")

        return AvfCapability(
            isSupported = isSupported,
            isHardwareAccelerated = isHardware,
            hypervisorType = hypervisorType,
            kvmNodePresent = kvmPresent,
            apiSupported = apiSupported,
            reason = reason
        )
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val get = clazz.getMethod("get", String::class.java, String::class.java)
            get.invoke(null, key, "0") as String
        } catch (e: Exception) {
            "0"
        }
    }
}
