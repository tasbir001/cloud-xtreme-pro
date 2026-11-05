package com.cloudxtreme.pro.cloud

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Protocol for streaming touch gestures and key events to remote cloud Android VMs.
 */
object TouchInputProtocol {

    enum class TouchAction {
        DOWN,
        MOVE,
        UP,
        CANCEL
    }

    enum class KeyAction {
        KEY_DOWN,
        KEY_UP,
        SPECIAL_HOME,
        SPECIAL_BACK,
        SPECIAL_RECENTS,
        SPECIAL_POWER,
        SPECIAL_VOL_UP,
        SPECIAL_VOL_DOWN
    }

    @Serializable
    data class TouchPointer(
        val pointerId: Int,
        val normalizedX: Float, // 0.0f to 1.0f
        val normalizedY: Float, // 0.0f to 1.0f
        val pressure: Float = 1.0f
    )

    @Serializable
    data class TouchEventPacket(
        val action: TouchAction,
        val timestampMs: Long = System.currentTimeMillis(),
        val pointers: List<TouchPointer>
    )

    @Serializable
    data class KeyEventPacket(
        val keyAction: KeyAction,
        val keyCode: Int = 0,
        val unicodeChar: Char? = null,
        val timestampMs: Long = System.currentTimeMillis()
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun serializeTouchEvent(event: TouchEventPacket): String {
        return json.encodeToString(event)
    }

    fun serializeKeyEvent(event: KeyEventPacket): String {
        return json.encodeToString(event)
    }
}
