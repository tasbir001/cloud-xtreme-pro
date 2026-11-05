package com.cloudxtreme.pro

import com.cloudxtreme.pro.cloud.TouchInputProtocol
import com.cloudxtreme.pro.core.model.EnvironmentConfig
import com.cloudxtreme.pro.core.model.GmsMode
import com.cloudxtreme.pro.core.model.VirtualInstance
import com.cloudxtreme.pro.core.utils.FileUtils
import com.cloudxtreme.pro.core.utils.SecurityUtils
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VirtualizationEngineArchitectureTest {

    @Test
    fun testUniqueInstanceIdGeneration() {
        val id1 = SecurityUtils.generateUniqueInstanceId()
        val id2 = SecurityUtils.generateUniqueInstanceId()
        assertNotNull(id1)
        assertNotNull(id2)
        assertNotEquals(id1, id2)
        assertTrue(id1.startsWith("inst_"))
    }

    @Test
    fun testIdentifierSanitization() {
        val unsafe = "My Cloned App! #1 / (Test)"
        val safe = SecurityUtils.sanitizeIdentifier(unsafe)
        assertFalse(safe.contains("/"))
        assertFalse(safe.contains("!"))
        assertFalse(safe.contains("#"))
    }

    @Test
    fun testFileUtilsSizeFormatting() {
        assertEquals("500 B", FileUtils.formatFileSize(500L))
        val mbStr = FileUtils.formatFileSize(1024 * 1024 * 15L)
        assertTrue(mbStr.contains("15.00 MB") || mbStr.contains("15.00"))
        val gbStr = FileUtils.formatFileSize(1024L * 1024L * 1024L * 3L)
        assertTrue(gbStr.contains("3.00 GB") || gbStr.contains("3.00"))
    }

    @Test
    fun testTouchInputProtocolSerialization() {
        val event = TouchInputProtocol.TouchEventPacket(
            action = TouchInputProtocol.TouchAction.DOWN,
            pointers = listOf(
                TouchInputProtocol.TouchPointer(
                    pointerId = 0,
                    normalizedX = 0.5f,
                    normalizedY = 0.8f,
                    pressure = 1.0f
                )
            )
        )
        val json = TouchInputProtocol.serializeTouchEvent(event)
        assertTrue(json.contains("DOWN"))
        assertTrue(json.contains("0.5"))
        assertTrue(json.contains("0.8"))
    }

    @Test
    fun testInstanceModelCloning() {
        val baseConfig = EnvironmentConfig(
            androidVersion = "Android 14 (API 34)",
            memoryLimitMb = 2048,
            gmsMode = GmsMode.MICRO_G
        )

        val instance1 = VirtualInstance(
            id = "inst_01",
            name = "Example App – Instance 1",
            targetPackageName = "com.example.app",
            appLabel = "Example App",
            versionName = "1.0",
            versionCode = 1,
            config = baseConfig,
            cloneIndex = 1
        )

        val instance2 = instance1.copy(
            id = "inst_02",
            name = "Example App – Instance 2",
            cloneIndex = 2
        )

        assertNotEquals(instance1.id, instance2.id)
        assertNotEquals(instance1.name, instance2.name)
        assertEquals(instance1.targetPackageName, instance2.targetPackageName)
        assertEquals(2, instance2.cloneIndex)
    }
}
