package com.cloudxtreme.pro

import android.app.Application
import com.cloudxtreme.pro.apkmanager.ApkInstallerService
import com.cloudxtreme.pro.apkmanager.DeviceAppsScanner
import com.cloudxtreme.pro.apkmanager.PackageParserService
import com.cloudxtreme.pro.cloud.CloudPhoneService
import com.cloudxtreme.pro.cloud.firebase.FirebaseAuthManager
import com.cloudxtreme.pro.cloud.firebase.FirebaseStorageService
import com.cloudxtreme.pro.cloud.firebase.FirestoreSyncService
import com.cloudxtreme.pro.core.utils.AppLogger
import com.cloudxtreme.pro.gms.GmsCompatibilityEngine
import com.cloudxtreme.pro.instancemanager.InstanceBackupEngine
import com.cloudxtreme.pro.instancemanager.InstanceLifecycleManager
import com.cloudxtreme.pro.instancemanager.InstanceRepository
import com.cloudxtreme.pro.storage.PreferencesDataStore
import com.cloudxtreme.pro.storage.VirtualStorageManager
import com.cloudxtreme.pro.virtualization.VirtualProcessService
import com.cloudxtreme.pro.virtualization.VirtualizationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Main Application class for Cloud Xtreme Pro.
 * Initializes and wires the virtualization, storage, cloud, and APK management engines.
 */
class CloudXtremeApplication : Application() {

    companion object {
        lateinit var instance: CloudXtremeApplication
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Virtualization & Lifecycle
    lateinit var virtualizationEngine: VirtualizationEngine
        private set
    lateinit var instanceRepository: InstanceRepository
        private set
    lateinit var lifecycleManager: InstanceLifecycleManager
        private set
    lateinit var backupEngine: InstanceBackupEngine
        private set

    // APK Management
    lateinit var packageParserService: PackageParserService
        private set
    lateinit var apkInstallerService: ApkInstallerService
        private set
    lateinit var appsScanner: DeviceAppsScanner
        private set

    // Cloud Phone & Firebase
    lateinit var cloudPhoneService: CloudPhoneService
        private set
    lateinit var firebaseAuthManager: FirebaseAuthManager
        private set
    lateinit var firestoreSyncService: FirestoreSyncService
        private set
    lateinit var firebaseStorageService: FirebaseStorageService
        private set

    // Storage & GMS
    lateinit var storageManager: VirtualStorageManager
        private set
    lateinit var gmsEngine: GmsCompatibilityEngine
        private set
    lateinit var preferencesDataStore: PreferencesDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        AppLogger.i("CloudXtremeApplication", "Initializing Cloud Xtreme Pro architecture...")

        // Virtualization Core
        virtualizationEngine = VirtualizationEngine(this)
        instanceRepository = InstanceRepository(this)
        lifecycleManager = InstanceLifecycleManager(instanceRepository, virtualizationEngine)
        backupEngine = InstanceBackupEngine(this, virtualizationEngine.vfs, instanceRepository)

        // APK Core
        packageParserService = PackageParserService(this)
        apkInstallerService = ApkInstallerService(this, virtualizationEngine.vfs, instanceRepository, packageParserService)
        appsScanner = DeviceAppsScanner(this)

        // Cloud & Firebase Core
        cloudPhoneService = CloudPhoneService()
        firebaseAuthManager = FirebaseAuthManager(this)
        firestoreSyncService = FirestoreSyncService(this)
        firebaseStorageService = FirebaseStorageService(this)

        // Storage & GMS Core
        storageManager = VirtualStorageManager(this, virtualizationEngine.vfs)
        gmsEngine = GmsCompatibilityEngine(this)
        preferencesDataStore = PreferencesDataStore(this)

        // Asynchronously load instances manifest
        applicationScope.launch(Dispatchers.IO) {
            instanceRepository.initialize()
            AppLogger.i("CloudXtremeApplication", "Instance manifest loaded successfully")
        }

        // Start background monitoring service
        VirtualProcessService.startService(this)
    }
}
