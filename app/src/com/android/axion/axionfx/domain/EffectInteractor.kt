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

package com.android.axion.axionfx.domain

import com.android.axion.axionfx.AxionFxController
import com.android.axion.axionfx.data.EffectRepository

class EffectInteractor(private val repo: EffectRepository) {

    fun setMasterEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.MASTER_ENABLED, enabled)
        AxionFxController.setMasterEnabled(enabled)
    }

    fun setOutputGain(value: Int) {
        repo.putInt(EffectKeys.OUTPUT_GAIN, value)
        AxionFxController.setOutputGain(value)
    }

    fun setOutputPan(value: Int) {
        repo.putInt(EffectKeys.OUTPUT_PAN, value)
        AxionFxController.setParameter(0x102, value)
    }

    fun setEqEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.EQ_ENABLED, enabled)
        AxionFxController.setEqEnabled(enabled)
    }

    fun setEqBandLevel(band: Int, value: Int) {
        repo.putInt("${EffectKeys.EQ_BAND_PREFIX}$band", value)
        AxionFxController.setEqBandLevel(band, value)
    }

    fun setArbitraryEqBandLevel(band: Int, value: Int) {
        repo.putInt("${EffectKeys.ARBITRARY_EQ_BAND_PREFIX}$band", value)
        AxionFxController.setArbitraryEqBandLevel(band, value)
    }

    fun setFirEqEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.FIR_EQ_ENABLED, enabled)
        AxionFxController.setFirEqEnabled(enabled)
    }

    fun setFirEqBandGain(band: Int, value: Int) {
        repo.putInt("${EffectKeys.FIR_EQ_BAND_PREFIX}$band", value)
        AxionFxController.setFirEqBandGain(band, value)
    }

    fun setBassEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.BASS_ENABLED, enabled)
        AxionFxController.setBassEnabled(enabled)
    }

    fun setBassMode(mode: Int) {
        repo.putInt(EffectKeys.BASS_MODE, mode)
        AxionFxController.setBassMode(mode)
    }

    fun setBassGain(value: Int) {
        repo.putInt(EffectKeys.BASS_GAIN, value)
        AxionFxController.setBassGain(value)
    }

    fun setWidenerEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.WIDENER_ENABLED, enabled)
        AxionFxController.setWidenerEnabled(enabled)
    }

    fun setWidenerWidth(value: Int) {
        repo.putInt(EffectKeys.WIDENER_WIDTH, value)
        AxionFxController.setWidenerWidth(value)
    }

    fun setCrossfeedEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.CROSSFEED_ENABLED, enabled)
        AxionFxController.setCrossfeedEnabled(enabled)
    }

    fun setCrossfeedLevel(value: Int) {
        repo.putInt(EffectKeys.CROSSFEED_LEVEL, value)
        AxionFxController.setCrossfeedLevel(value)
    }

    fun setSurroundEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.SURROUND_ENABLED, enabled)
        AxionFxController.setSurroundEnabled(enabled)
    }

    fun setSurroundDelay(value: Int) {
        repo.putInt(EffectKeys.SURROUND_DELAY, value)
        AxionFxController.setParameter(0xB01, value)
    }

    fun setSurroundWidth(value: Int) {
        repo.putInt(EffectKeys.SURROUND_WIDTH, value)
        AxionFxController.setParameter(0xB02, value)
    }

    fun setSpatialEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.SPATIAL_ENABLED, enabled)
        AxionFxController.setSpatialEnabled(enabled)
    }

    fun setSpatialWidth(value: Int) {
        repo.putInt(EffectKeys.SPATIAL_WIDTH, value)
        AxionFxController.setSpatialWidth(value)
    }

    fun setSpatialBlend(value: Int) {
        repo.putInt(EffectKeys.SPATIAL_BLEND, value)
        AxionFxController.setSpatialBlend(value)
    }

    fun setCompressorEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.COMPRESSOR_ENABLED, enabled)
        AxionFxController.setCompressorEnabled(enabled)
    }

    fun setAgcEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.AGC_ENABLED, enabled)
        AxionFxController.setAgcEnabled(enabled)
    }

    fun setLimiterEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.LIMITER_ENABLED, enabled)
        AxionFxController.setLimiterEnabled(enabled)
    }

    fun setLimiterThreshold(value: Int) {
        repo.putInt(EffectKeys.LIMITER_THRESHOLD, value)
        AxionFxController.setParameter(0x501, value)
    }

    fun setReverbEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.REVERB_ENABLED, enabled)
        AxionFxController.setReverbEnabled(enabled)
    }

    fun setReverbRoomSize(value: Int) {
        repo.putInt(EffectKeys.REVERB_ROOM, value)
        AxionFxController.setReverbRoomSize(value)
    }

    fun setReverbWet(value: Int) {
        repo.putInt(EffectKeys.REVERB_WET, value)
        AxionFxController.setReverbWet(value)
    }

    fun setTubeEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.TUBE_ENABLED, enabled)
        AxionFxController.setTubeEnabled(enabled)
    }

    fun setTubeDrive(value: Int) {
        repo.putInt(EffectKeys.TUBE_DRIVE, value)
        AxionFxController.setTubeDrive(value)
    }

    fun setTubeMix(value: Int) {
        repo.putInt(EffectKeys.TUBE_MIX, value)
        AxionFxController.setTubeMix(value)
    }

    fun setExciterEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.EXCITER_ENABLED, enabled)
        AxionFxController.setExciterEnabled(enabled)
    }

    fun setExciterDrive(value: Int) {
        repo.putInt(EffectKeys.EXCITER_DRIVE, value)
        AxionFxController.setExciterDrive(value)
    }

    fun setExciterBlend(value: Int) {
        repo.putInt(EffectKeys.EXCITER_BLEND, value)
        AxionFxController.setExciterBlend(value)
    }

    fun setExciterFreq(value: Int) {
        repo.putInt(EffectKeys.EXCITER_FREQ, value)
        AxionFxController.setExciterFreq(value)
    }

    fun setMCompEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.MCOMP_ENABLED, enabled)
        AxionFxController.setMCompEnabled(enabled)
    }

    fun setMCompBandThreshold(band: Int, value: Int) {
        repo.putInt("${EffectKeys.MCOMP_THRESH_PREFIX}$band", value)
        AxionFxController.setMCompBandThreshold(band, value)
    }

    fun setMCompBandRatio(band: Int, value: Int) {
        repo.putInt("${EffectKeys.MCOMP_RATIO_PREFIX}$band", value)
        AxionFxController.setMCompBandRatio(band, value)
    }

    fun setMCompBandMakeup(band: Int, value: Int) {
        repo.putInt("${EffectKeys.MCOMP_MAKEUP_PREFIX}$band", value)
        AxionFxController.setMCompBandMakeup(band, value)
    }

    fun setConvolverEnabled(enabled: Boolean) {
        repo.putBoolean(EffectKeys.CONVOLVER_ENABLED, enabled)
        AxionFxController.setConvolverEnabled(enabled)
    }

    fun setConvolverMix(mix: Int) {
        repo.putInt(EffectKeys.CONVOLVER_MIX, mix)
        AxionFxController.setParameter(0xC01, mix)
    }

    fun loadConvolverIr(path: String) {
        repo.putString(EffectKeys.CONVOLVER_IR_PATH, path)
        try {
            val wavBytes = java.io.File(path).readBytes()
            AxionFxController.loadConvolverIrData(wavBytes)
        } catch (e: Exception) {
            android.util.Log.e("EffectInteractor", "Failed to read IR file: $path", e)
        }
    }

    fun resetAll() {
        repo.clear()
        
        // Master & Output
        setMasterEnabled(EffectDefaults.MASTER_ENABLED)
        setOutputGain(EffectDefaults.OUTPUT_GAIN)
        setOutputPan(EffectDefaults.OUTPUT_PAN)

        // Equalizers
        setEqEnabled(EffectDefaults.EQ_ENABLED)
        for (i in 0..9) {
            setEqBandLevel(i, 0)
        }
        for (i in 0..31) {
            setArbitraryEqBandLevel(i, 0)
        }
        setFirEqEnabled(EffectDefaults.FIR_EQ_ENABLED)
        for (i in 0..127) {
            setFirEqBandGain(i, 0)
        }

        // Bass & Widener
        setBassEnabled(EffectDefaults.BASS_ENABLED)
        setBassMode(EffectDefaults.BASS_MODE)
        setBassGain(EffectDefaults.BASS_GAIN)
        setWidenerEnabled(EffectDefaults.WIDENER_ENABLED)
        setWidenerWidth(EffectDefaults.WIDENER_WIDTH)

        // Reverb
        setReverbEnabled(EffectDefaults.REVERB_ENABLED)
        setReverbRoomSize(EffectDefaults.REVERB_ROOM)
        setReverbWet(EffectDefaults.REVERB_WET)

        // Dynamics
        setCompressorEnabled(EffectDefaults.COMPRESSOR_ENABLED)
        setAgcEnabled(EffectDefaults.AGC_ENABLED)
        setLimiterEnabled(EffectDefaults.LIMITER_ENABLED)
        setLimiterThreshold(EffectDefaults.LIMITER_THRESHOLD)

        // Saturation & Exciter
        setTubeEnabled(EffectDefaults.TUBE_ENABLED)
        setTubeDrive(EffectDefaults.TUBE_DRIVE)
        setTubeMix(EffectDefaults.TUBE_MIX)
        setExciterEnabled(EffectDefaults.EXCITER_ENABLED)
        setExciterDrive(EffectDefaults.EXCITER_DRIVE)
        setExciterBlend(EffectDefaults.EXCITER_BLEND)
        setExciterFreq(EffectDefaults.EXCITER_FREQ)

        // Spatial
        setCrossfeedEnabled(EffectDefaults.CROSSFEED_ENABLED)
        setCrossfeedLevel(EffectDefaults.CROSSFEED_LEVEL)
        setSurroundEnabled(EffectDefaults.SURROUND_ENABLED)
        setSurroundDelay(EffectDefaults.SURROUND_DELAY)
        setSurroundWidth(EffectDefaults.SURROUND_WIDTH)
        setSpatialEnabled(EffectDefaults.SPATIAL_ENABLED)
        setSpatialWidth(EffectDefaults.SPATIAL_WIDTH)
        setSpatialBlend(EffectDefaults.SPATIAL_BLEND)

        // Multiband & Convolver
        setMCompEnabled(EffectDefaults.MCOMP_ENABLED)
        for (i in 0..3) {
            setMCompBandThreshold(i, EffectDefaults.MCOMP_THRESHOLD)
            setMCompBandRatio(i, EffectDefaults.MCOMP_RATIO)
            setMCompBandMakeup(i, EffectDefaults.MCOMP_MAKEUP)
        }
        setConvolverEnabled(EffectDefaults.CONVOLVER_ENABLED)
        setConvolverMix(EffectDefaults.CONVOLVER_MIX)
        repo.putString(EffectKeys.CONVOLVER_IR_PATH, null)
        repo.putString(EffectKeys.CONVOLVER_IR_NAME, null)
    }
}
