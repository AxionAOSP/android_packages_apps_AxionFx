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

#define LOG_TAG "AxionFxEngine"
#include <log/log.h>

#include "AxionFxEngine.h"

#include <algorithm>
#include <cstdint>
#include <cstring>
#include "dsp/WavLoader.h"

namespace axionfx {

static inline void enableFlushToZero() {
#if defined(__aarch64__)
    uint64_t fpcr;
    __asm__ __volatile__("mrs %0, fpcr" : "=r"(fpcr));
    fpcr |= (1 << 24);
    __asm__ __volatile__("msr fpcr, %0" : : "r"(fpcr));
#elif defined(__arm__)
    uint32_t fpscr;
    __asm__ __volatile__("vmrs %0, fpscr" : "=r"(fpscr));
    fpscr |= (1 << 24);
    __asm__ __volatile__("vmsr fpscr, %0" : : "r"(fpscr));
#endif
}

void AxionFxEngine::configure(float sampleRate) {
    mSampleRate = sampleRate;
    mEqualizer.configure(sampleRate);
    mBassBoost.configure(sampleRate);
    mStereoWidener.configure(sampleRate);
    mReverb.configure(sampleRate);
    mCompressor.configure(sampleRate);
    mTubeSimulator.configure(sampleRate);
    mAgc.configure(sampleRate);
    mConvolver.configure(sampleRate);
    mCrossfeed.configure(sampleRate);
    mDiffSurround.configure(sampleRate);
    mLimiter.configure(sampleRate);
    mMultibandComp.configure(sampleRate);
    mExciter.configure(sampleRate);
    mFirEq.configure(sampleRate);
    mSteamSpatial.configure(sampleRate);
}

void AxionFxEngine::process(float* in, float* out, int samples) {
    mProcessCallCount.fetch_add(1, std::memory_order_relaxed);
    if (!mMasterEnabled) {
        if (in != out) {
            std::memcpy(out, in, samples * sizeof(float));
        }
        return;
    }

    if (in != out) {
        std::memcpy(out, in, samples * sizeof(float));
    }

    enableFlushToZero();

    int frames = samples / 2;

    mFirEq.process(out, frames);
    mEqualizer.process(out, frames);
    mBassBoost.process(out, frames);
    mExciter.process(out, frames);
    mTubeSimulator.process(out, frames);
    mCompressor.process(out, frames);
    mMultibandComp.process(out, frames);
    mConvolver.process(out, frames);
    mReverb.process(out, frames);
    mStereoWidener.process(out, frames);
    mDiffSurround.process(out, frames);
    mCrossfeed.process(out, frames);
    mSteamSpatial.process(out, frames);
    mAgc.process(out, frames);

    if (mOutputGain != 1.0f || mPanL != 1.0f || mPanR != 1.0f) {
        float gainL = mOutputGain * mPanL;
        float gainR = mOutputGain * mPanR;
        for (int f = 0; f < frames; ++f) {
            float l = out[f * 2] * gainL;
            float r = out[f * 2 + 1] * gainR;
            if (l > 1.0f) { float x = l - 1.0f; l = 1.0f - x / (x + 1.0f); }
            else if (l < -1.0f) { float x = -l - 1.0f; l = -(1.0f - x / (x + 1.0f)); }
            if (r > 1.0f) { float x = r - 1.0f; r = 1.0f - x / (x + 1.0f); }
            else if (r < -1.0f) { float x = -r - 1.0f; r = -(1.0f - x / (x + 1.0f)); }
            out[f * 2] = l;
            out[f * 2 + 1] = r;
        }
    }

    mLimiter.process(out, frames);
}

void AxionFxEngine::setMasterEnabled(bool enabled) {
    mMasterEnabled = enabled;
}

void AxionFxEngine::setParameter(int32_t paramId, int32_t value) {
    switch (paramId) {
        case PARAM_MASTER_ENABLE:
            mMasterEnabled = (value != 0);
            break;
        case PARAM_OUTPUT_GAIN:
            mOutputGain = static_cast<float>(value) / 100.0f;
            break;
        case PARAM_OUTPUT_PAN: {
            float pan = static_cast<float>(value) / 100.0f;
            pan = std::clamp(pan, -1.0f, 1.0f);
            mPanL = std::min(1.0f, 1.0f - pan);
            mPanR = std::min(1.0f, 1.0f + pan);
        }
            break;

        case PARAM_EQ_ENABLE:
            mEqualizer.setEnabled(value != 0);
            break;
        case PARAM_EQ_BAND_LEVEL: {
            int band = eqBandFromValue(value);
            int levelCb = eqLevelFromValue(value);
            mEqualizer.setLegacyBandLevel(band, static_cast<float>(levelCb) / 100.0f);
            break;
        }
        case PARAM_EQ_ARBITRARY_BAND_LEVEL: {
            int band = eqBandFromValue(value);
            int levelCb = eqLevelFromValue(value);
            mEqualizer.setBandLevel(band, static_cast<float>(levelCb) / 100.0f);
            break;
        }

        case PARAM_BASS_ENABLE:
            mBassBoost.setEnabled(value != 0);
            break;
        case PARAM_BASS_MODE:
            mBassBoost.setMode(static_cast<BassMode>(value));
            break;
        case PARAM_BASS_FREQUENCY:
            mBassBoost.setFrequency(static_cast<float>(value));
            break;
        case PARAM_BASS_GAIN:
            mBassBoost.setGain(static_cast<float>(value) / 100.0f);
            break;

        case PARAM_WIDENER_ENABLE:
            mStereoWidener.setEnabled(value != 0);
            break;
        case PARAM_WIDENER_WIDTH:
            mStereoWidener.setWidth(static_cast<float>(value) / 100.0f);
            break;

        case PARAM_LIMITER_ENABLE:
            mLimiter.setEnabled(value != 0);
            break;
        case PARAM_LIMITER_THRESHOLD:
            mLimiter.setThreshold(static_cast<float>(value));
            break;
        case PARAM_LIMITER_RELEASE:
            mLimiter.setRelease(static_cast<float>(value));
            break;

        case PARAM_REVERB_ENABLE:
            mReverb.setEnabled(value != 0);
            break;
        case PARAM_REVERB_ROOM_SIZE:
            mReverb.setRoomSize(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_REVERB_DAMPING:
            mReverb.setDamping(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_REVERB_WET:
            mReverb.setWetLevel(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_REVERB_DRY:
            mReverb.setDryLevel(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_REVERB_WIDTH:
            mReverb.setWidth(static_cast<float>(value) / 100.0f);
            break;

        case PARAM_COMPRESSOR_ENABLE:
            mCompressor.setEnabled(value != 0);
            break;
        case PARAM_COMPRESSOR_THRESHOLD:
            mCompressor.setThreshold(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_COMPRESSOR_RATIO:
            mCompressor.setRatio(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_COMPRESSOR_ATTACK:
            mCompressor.setAttack(static_cast<float>(value) / 10.0f);
            break;
        case PARAM_COMPRESSOR_RELEASE:
            mCompressor.setRelease(static_cast<float>(value));
            break;
        case PARAM_COMPRESSOR_KNEE:
            mCompressor.setKnee(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_COMPRESSOR_MAKEUP:
            mCompressor.setMakeupGain(static_cast<float>(value) / 100.0f);
            break;

        case PARAM_TUBE_ENABLE:
            mTubeSimulator.setEnabled(value != 0);
            break;
        case PARAM_TUBE_DRIVE:
            mTubeSimulator.setDrive(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_TUBE_MIX:
            mTubeSimulator.setMix(static_cast<float>(value) / 100.0f);
            break;

        case PARAM_AGC_ENABLE:
            mAgc.setEnabled(value != 0);
            break;
        case PARAM_AGC_TARGET:
            mAgc.setTargetLevel(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_AGC_MAX_GAIN:
            mAgc.setMaxGain(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_AGC_SPEED:
            mAgc.setSpeed(static_cast<float>(value));
            break;

        case PARAM_CROSSFEED_ENABLE:
            mCrossfeed.setEnabled(value != 0);
            break;
        case PARAM_CROSSFEED_LEVEL:
            mCrossfeed.setLevel(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_CROSSFEED_CUTOFF:
            mCrossfeed.setCutoff(static_cast<float>(value));
            break;

        case PARAM_SURROUND_ENABLE:
            mDiffSurround.setEnabled(value != 0);
            break;
        case PARAM_SURROUND_DELAY:
            mDiffSurround.setDelay(static_cast<float>(value) / 100.0f);
            break;
        case PARAM_SURROUND_WIDTH:
            mDiffSurround.setWidth(static_cast<float>(value) / 100.0f);
            break;

        case PARAM_CONVOLVER_ENABLE:
            mConvolver.setEnabled(value != 0);
            break;
        case PARAM_CONVOLVER_MIX:
            mConvolver.setMix(static_cast<float>(value) / 100.0f);
            break;

        case PARAM_MCOMP_ENABLE:
            mMultibandComp.setEnabled(value != 0);
            break;
        case PARAM_MCOMP_BAND_THRESHOLD: {
            int band = (value >> 16) & 0xFF;
            float dB = static_cast<float>(static_cast<int16_t>(value & 0xFFFF)) / 10.0f;
            mMultibandComp.setBandThreshold(band, dB);
            break;
        }
        case PARAM_MCOMP_BAND_RATIO: {
            int band = (value >> 16) & 0xFF;
            float ratio = static_cast<float>(value & 0xFFFF) / 100.0f;
            mMultibandComp.setBandRatio(band, ratio);
            break;
        }
        case PARAM_MCOMP_BAND_ATTACK: {
            int band = (value >> 16) & 0xFF;
            float ms = static_cast<float>(value & 0xFFFF) / 10.0f;
            mMultibandComp.setBandAttack(band, ms);
            break;
        }
        case PARAM_MCOMP_BAND_RELEASE: {
            int band = (value >> 16) & 0xFF;
            float ms = static_cast<float>(value & 0xFFFF) / 10.0f;
            mMultibandComp.setBandRelease(band, ms);
            break;
        }
        case PARAM_MCOMP_BAND_MAKEUP: {
            int band = (value >> 16) & 0xFF;
            float dB = static_cast<float>(static_cast<int16_t>(value & 0xFFFF)) / 10.0f;
            mMultibandComp.setBandMakeup(band, dB);
            break;
        }
        case PARAM_MCOMP_CROSSOVER: {
            int index = (value >> 16) & 0xFF;
            float hz = static_cast<float>(value & 0xFFFF);
            mMultibandComp.setCrossoverFreq(index, hz);
            break;
        }

        case PARAM_EXCITER_ENABLE:
            mExciter.setEnabled(value != 0);
            break;
        case PARAM_EXCITER_DRIVE:
            mExciter.setDrive(static_cast<float>(value));
            break;
        case PARAM_EXCITER_BLEND:
            mExciter.setBlend(static_cast<float>(value));
            break;
        case PARAM_EXCITER_FREQ:
            mExciter.setHarmonicFreq(static_cast<float>(value));
            break;

        case PARAM_FIR_EQ_ENABLE:
            mFirEq.setEnabled(value != 0);
            break;
        case PARAM_FIR_EQ_BAND_GAIN: {
            int band = (value >> 16) & 0xFF;
            float dB = static_cast<float>(static_cast<int16_t>(value & 0xFFFF)) / 10.0f;
            mFirEq.setBandGain(band, dB);
            break;
        }

        case PARAM_SPATIAL_ENABLE:
            mSteamSpatial.setEnabled(value != 0);
            break;
        case PARAM_SPATIAL_WIDTH:
            mSteamSpatial.setWidth(value);
            break;
        case PARAM_SPATIAL_BLEND:
            mSteamSpatial.setBlend(value);
            break;
        case PARAM_SPATIAL_HRTF_PROFILE:
            mSteamSpatial.setHrtfProfile(value);
            break;

        default:
            break;
    }
}

bool AxionFxEngine::loadIrFromPath(const char* path) {
    auto wav = loadWavFile(path);
    if (!wav.valid || wav.samples.empty()) return false;
    mConvolver.loadImpulseResponse(wav.samples.data(), wav.numFrames);
    return true;
}

bool AxionFxEngine::loadIrFromData(const uint8_t* data, size_t size) {
    ALOGI("loadIrFromData: %zu bytes", size);
    auto wav = loadWavFromMemory(data, size);
    if (!wav.valid || wav.samples.empty()) {
        ALOGE("loadIrFromData: WAV parse failed valid=%d samples=%zu", wav.valid, wav.samples.size());
        return false;
    }
    ALOGI("loadIrFromData: parsed %d frames, %d channels, %dHz", wav.numFrames, wav.channels, wav.sampleRate);
    mConvolver.loadImpulseResponse(wav.samples.data(), wav.numFrames);
    return true;
}

bool AxionFxEngine::loadIrFromFd(int fd, int64_t offset, int64_t length) {
    auto wav = loadWavFromFd(fd, offset, length);
    if (!wav.valid || wav.samples.empty()) return false;
    mConvolver.loadImpulseResponse(wav.samples.data(), wav.numFrames);
    return true;
}


int32_t AxionFxEngine::getParameter(int32_t paramId) const {
    switch (paramId) {
        case PARAM_MASTER_ENABLE:
            return mMasterEnabled ? 1 : 0;
        case PARAM_OUTPUT_GAIN:
            return static_cast<int32_t>(mOutputGain * 100.0f);
        case PARAM_EQ_ENABLE:
            return mEqualizer.isEnabled() ? 1 : 0;
        case PARAM_BASS_ENABLE:
            return mBassBoost.isEnabled() ? 1 : 0;
        case PARAM_WIDENER_ENABLE:
            return mStereoWidener.isEnabled() ? 1 : 0;
        case PARAM_LIMITER_ENABLE:
            return mLimiter.isEnabled() ? 1 : 0;
        case PARAM_REVERB_ENABLE:
            return mReverb.isEnabled() ? 1 : 0;
        case PARAM_COMPRESSOR_ENABLE:
            return mCompressor.isEnabled() ? 1 : 0;
        case PARAM_TUBE_ENABLE:
            return mTubeSimulator.isEnabled() ? 1 : 0;
        case PARAM_AGC_ENABLE:
            return mAgc.isEnabled() ? 1 : 0;
        case PARAM_CROSSFEED_ENABLE:
            return mCrossfeed.isEnabled() ? 1 : 0;
        case PARAM_SURROUND_ENABLE:
            return mDiffSurround.isEnabled() ? 1 : 0;
        case PARAM_CONVOLVER_ENABLE:
            return mConvolver.isEnabled() ? 1 : 0;
        case PARAM_MCOMP_ENABLE:
            return mMultibandComp.isEnabled() ? 1 : 0;
        case PARAM_EXCITER_ENABLE:
            return mExciter.isEnabled() ? 1 : 0;
        case PARAM_FIR_EQ_ENABLE:
            return mFirEq.isEnabled() ? 1 : 0;
        case PARAM_SPATIAL_ENABLE:
            return mSteamSpatial.isEnabled() ? 1 : 0;
        case PARAM_PROCESS_CALL_COUNT:
            return static_cast<int32_t>(mProcessCallCount.load(std::memory_order_relaxed));
        default:
            return 0;
    }
}

}  // namespace axionfx
