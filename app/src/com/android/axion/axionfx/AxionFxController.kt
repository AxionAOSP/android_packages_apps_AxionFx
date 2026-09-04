/*
 * Copyright 2025-2026 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.axion.axionfx

import android.media.audiofx.AudioEffect
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

object AxionFxController {

    private const val TAG = "AxionFxController"

    val EFFECT_TYPE_UUID: UUID = UUID.fromString("5867be72-4060-4c55-a378-c1cdef3e1353")
    val EFFECT_IMPL_UUID: UUID = UUID.fromString("f35cb927-a887-4f3d-847f-770634486d53")

    private val sessions = mutableMapOf<Int, AudioEffect>()

    fun getSessionId(): Int = sessions.keys.firstOrNull() ?: -1

    fun attachSession(sessionId: Int): Boolean {
        if (sessions.containsKey(sessionId)) return true
        return try {
            val effect = AudioEffect(EFFECT_TYPE_UUID, EFFECT_IMPL_UUID, 0, sessionId)
            effect.enabled = true
            sessions[sessionId] = effect
            Log.d(TAG, "Attached to session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach session $sessionId", e)
            false
        }
    }

    fun detachSession(sessionId: Int) {
        sessions.remove(sessionId)?.let { effect ->
            try {
                effect.enabled = false
                effect.release()
                Log.d(TAG, "Detached session $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to detach session $sessionId", e)
            }
        }
    }

    fun setParameter(paramId: Int, value: Int) {
        val paramBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
            .putInt(paramId).array()
        val valueBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
            .putInt(value).array()

        sessions.values.forEach { effect ->
            try {
                if (!effect.enabled) effect.enabled = true
                val status = effect.setParameter(paramBytes, valueBytes)
                Log.d(TAG, "setParam 0x${paramId.toString(16)}=$value status=$status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set param 0x${paramId.toString(16)}=$value", e)
            }
        }
    }

    fun getParameter(paramId: Int): Int {
        val session = sessions.values.firstOrNull() ?: return 0
        return try {
            val paramBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
                .putInt(paramId).array()
            val valueBytes = ByteArray(4)
            session.getParameter(paramBytes, valueBytes)
            ByteBuffer.wrap(valueBytes).order(ByteOrder.nativeOrder()).getInt()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get param 0x${paramId.toString(16)}", e)
            0
        }
    }

    fun getHeartbeat() = getParameter(0x103);

    fun setMasterEnabled(enabled: Boolean) = setParameter(0x100, if (enabled) 1 else 0)
    fun setOutputGain(percent: Int) = setParameter(0x101, percent)

    fun setEqEnabled(enabled: Boolean) = setParameter(0x200, if (enabled) 1 else 0)
    fun setEqBandLevel(band: Int, levelCentibels: Int) =
        setParameter(0x201, (band shl 16) or (levelCentibels and 0xFFFF))
    fun setArbitraryEqBandLevel(band: Int, levelCentibels: Int) =
        setParameter(0x202, (band shl 16) or (levelCentibels and 0xFFFF))

    fun setBassEnabled(enabled: Boolean) = setParameter(0x300, if (enabled) 1 else 0)
    fun setBassMode(mode: Int) = setParameter(0x301, mode)
    fun setBassFrequency(hz: Int) = setParameter(0x302, hz)
    fun setBassGain(centibels: Int) = setParameter(0x303, centibels)

    fun setWidenerEnabled(enabled: Boolean) = setParameter(0x400, if (enabled) 1 else 0)
    fun setWidenerWidth(widthPercent: Int) = setParameter(0x401, widthPercent)

    fun setLimiterEnabled(enabled: Boolean) = setParameter(0x500, if (enabled) 1 else 0)

    fun setReverbEnabled(enabled: Boolean) = setParameter(0x600, if (enabled) 1 else 0)
    fun setReverbRoomSize(percent: Int) = setParameter(0x601, percent)
    fun setReverbDamping(percent: Int) = setParameter(0x602, percent)
    fun setReverbWet(percent: Int) = setParameter(0x603, percent)
    fun setReverbDry(percent: Int) = setParameter(0x604, percent)

    fun setCompressorEnabled(enabled: Boolean) = setParameter(0x700, if (enabled) 1 else 0)
    fun setCompressorThreshold(centibels: Int) = setParameter(0x701, centibels)
    fun setCompressorRatio(ratioTimes100: Int) = setParameter(0x702, ratioTimes100)

    fun setTubeEnabled(enabled: Boolean) = setParameter(0x800, if (enabled) 1 else 0)
    fun setTubeDrive(percent: Int) = setParameter(0x801, percent)
    fun setTubeMix(percent: Int) = setParameter(0x802, percent)

    fun setAgcEnabled(enabled: Boolean) = setParameter(0x900, if (enabled) 1 else 0)

    fun setCrossfeedEnabled(enabled: Boolean) = setParameter(0xA00, if (enabled) 1 else 0)
    fun setCrossfeedLevel(percent: Int) = setParameter(0xA01, percent)

    fun setSurroundEnabled(enabled: Boolean) = setParameter(0xB00, if (enabled) 1 else 0)
    fun setSurroundDelay(delayTimes100: Int) = setParameter(0xB01, delayTimes100)

    fun setConvolverEnabled(enabled: Boolean) = setParameter(0xC00, if (enabled) 1 else 0)

    fun loadConvolverIrData(wavBytes: ByteArray) {
        val paramBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(0xC03).array()
        sessions.values.forEach { effect ->
            try {
                effect.setParameter(paramBytes, wavBytes)
                Log.d(TAG, "loadConvolverIrData: ${wavBytes.size} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load IR data: ${wavBytes.size} bytes", e)
            }
        }
    }

    fun setMCompEnabled(enabled: Boolean) = setParameter(0xD00, if (enabled) 1 else 0)
    fun setMCompBandThreshold(band: Int, tenthsDb: Int) =
        setParameter(0xD01, (band shl 16) or (tenthsDb and 0xFFFF))
    fun setMCompBandRatio(band: Int, ratioTimes100: Int) =
        setParameter(0xD02, (band shl 16) or (ratioTimes100 and 0xFFFF))
    fun setMCompBandAttack(band: Int, tenthsMs: Int) =
        setParameter(0xD03, (band shl 16) or (tenthsMs and 0xFFFF))
    fun setMCompBandRelease(band: Int, tenthsMs: Int) =
        setParameter(0xD04, (band shl 16) or (tenthsMs and 0xFFFF))
    fun setMCompBandMakeup(band: Int, tenthsDb: Int) =
        setParameter(0xD05, (band shl 16) or (tenthsDb and 0xFFFF))
    fun setMCompCrossover(index: Int, hz: Int) =
        setParameter(0xD06, (index shl 16) or (hz and 0xFFFF))

    fun setExciterEnabled(enabled: Boolean) = setParameter(0xE00, if (enabled) 1 else 0)
    fun setExciterDrive(percent: Int) = setParameter(0xE01, percent)
    fun setExciterBlend(percent: Int) = setParameter(0xE02, percent)
    fun setExciterFreq(hz: Int) = setParameter(0xE03, hz)

    fun setFirEqEnabled(enabled: Boolean) = setParameter(0xF00, if (enabled) 1 else 0)
    fun setFirEqBandGain(band: Int, tenthsDb: Int) =
        setParameter(0xF01, (band shl 16) or (tenthsDb and 0xFFFF))

    fun setSpatialEnabled(enabled: Boolean) = setParameter(0x1000, if (enabled) 1 else 0)
    fun setSpatialWidth(percent: Int) = setParameter(0x1001, percent)
    fun setSpatialBlend(percent: Int) = setParameter(0x1003, percent)

    fun releaseAll() {
        sessions.keys.toList().forEach { detachSession(it) }
    }

    fun hasActiveSessions(): Boolean = sessions.isNotEmpty()
}
