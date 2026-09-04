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

#pragma once

#include "AxionFxParams.h"
#include "effects/AutoGainControl.h"
#include "effects/BassBoost.h"
#include "effects/Compressor.h"
#include "effects/Convolver.h"
#include "effects/Crossfeed.h"
#include "effects/DiffSurround.h"
#include "effects/Equalizer.h"
#include "effects/Limiter.h"
#include "effects/Reverb.h"
#include "effects/StereoWidener.h"
#include "effects/TubeSimulator.h"
#include "effects/MultibandCompressor.h"
#include "effects/Exciter.h"
#include "effects/FirEqualizer.h"
#include "effects/SteamSpatial.h"
#include <atomic>

namespace axionfx {

class AxionFxEngine {
public:
    void configure(float sampleRate);
    void process(float* in, float* out, int samples);

    void setParameter(int32_t paramId, int32_t value);
    int32_t getParameter(int32_t paramId) const;

    void setMasterEnabled(bool enabled);
    bool isMasterEnabled() const { return mMasterEnabled; }
    bool loadIrFromPath(const char* path);
    bool loadIrFromFd(int fd, int64_t offset, int64_t length);
    bool loadIrFromData(const uint8_t* data, size_t size);

private:
    std::atomic<uint64_t> mProcessCallCount{0};
    bool mMasterEnabled = false;
    float mSampleRate = 48000.0f;
    float mOutputGain = 1.0f;
    float mPanL = 1.0f;
    float mPanR = 1.0f;

    Equalizer mEqualizer;
    BassBoost mBassBoost;
    StereoWidener mStereoWidener;
    Reverb mReverb;
    Compressor mCompressor;
    TubeSimulator mTubeSimulator;
    AutoGainControl mAgc;
    Convolver mConvolver;
    Crossfeed mCrossfeed;
    DiffSurround mDiffSurround;
    Limiter mLimiter;
    MultibandCompressor mMultibandComp;
    Exciter mExciter;
    FirEqualizer mFirEq;
    SteamSpatial mSteamSpatial;
};

}  // namespace axionfx
