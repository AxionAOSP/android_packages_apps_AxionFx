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

package com.android.axion.axionfx.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import android.media.AudioManager
import android.media.AudioTrack
import com.android.axion.axionfx.AxionFxController
import com.android.axion.axionfx.data.EffectRepository
import com.android.axion.axionfx.domain.EffectDefaults
import com.android.axion.axionfx.domain.EffectInteractor
import com.android.axion.axionfx.domain.EffectKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AudioStats(
    val sampleRate: String,
    val bufferSize: String,
    val channels: String,
    val bitDepth: String,
    val sessionId: String,
    val latency: String,
)

class AxionFxViewModel(private val prefs: SharedPreferences) {

    val repo = EffectRepository(prefs)
    val interactor = EffectInteractor(repo)

    fun loadBoolean(key: String, default: Boolean): Boolean = repo.getBoolean(key, default)
    fun loadInt(key: String, default: Int): Int = repo.getInt(key, default)

    private val effectKeys = listOf(
        EffectKeys.BASS_ENABLED,
        EffectKeys.WIDENER_ENABLED,
        EffectKeys.REVERB_ENABLED,
        EffectKeys.COMPRESSOR_ENABLED,
        EffectKeys.TUBE_ENABLED,
        EffectKeys.AGC_ENABLED,
        EffectKeys.CROSSFEED_ENABLED,
        EffectKeys.SURROUND_ENABLED,
        EffectKeys.SPATIAL_ENABLED,
        EffectKeys.LIMITER_ENABLED,
        EffectKeys.EQ_ENABLED,
        EffectKeys.FIR_EQ_ENABLED,
        EffectKeys.EXCITER_ENABLED,
        EffectKeys.MCOMP_ENABLED,
    )

    private val _activeEffectCount = MutableStateFlow(computeActiveEffectCount())
    val activeEffectCount: StateFlow<Int> = _activeEffectCount

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in effectKeys) {
            _activeEffectCount.value = computeActiveEffectCount()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun computeActiveEffectCount(): Int =
        effectKeys.count { repo.getBoolean(it, false) }

    fun getAudioStats(): AudioStats {
        val nativeSr = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        val minBuf = AudioTrack.getMinBufferSize(nativeSr, 2, android.media.AudioFormat.ENCODING_PCM_FLOAT)
        val frameSizeBytes = 2 * 4
        val framesPerBuf = if (frameSizeBytes > 0) minBuf / frameSizeBytes else 0
        val latencyMs = if (nativeSr > 0) (framesPerBuf * 1000f / nativeSr) else 0f
        val sessionId = AxionFxController.getSessionId()

        return AudioStats(
            sampleRate = "${nativeSr / 1000f} kHz",
            bufferSize = "$framesPerBuf frames",
            channels = "Stereo",
            bitDepth = "32-bit float",
            sessionId = if (sessionId >= 0) "#$sessionId" else "—",
            latency = "%.1f ms".format(latencyMs),
        )
    }
}
