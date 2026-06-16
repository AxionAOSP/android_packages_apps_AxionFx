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

#include "../dsp/Biquad.h"

namespace axionfx {

class Equalizer {
public:
    static constexpr int NUM_BANDS = 32;
    static constexpr int LEGACY_NUM_BANDS = 10;
    static constexpr float CENTER_FREQS[NUM_BANDS] = {
        20.0f, 31.5f, 40.0f, 50.0f, 63.0f, 78.0f, 100.0f, 125.0f,
        160.0f, 200.0f, 250.0f, 315.0f, 406.0f, 500.0f, 630.0f, 800.0f,
        1000.0f, 1216.0f, 1600.0f, 2000.0f, 2500.0f, 3150.0f, 3640.0f, 4000.0f,
        5000.0f, 6299.0f, 8000.0f, 10000.0f, 12500.0f, 16000.0f, 18851.0f, 20000.0f
    };
    static constexpr int LEGACY_BAND_TO_RESPONSE[LEGACY_NUM_BANDS] = {
        1, 4, 7, 10, 13, 16, 19, 23, 26, 29
    };
    static constexpr float DEFAULT_Q = 3.0f;
    static constexpr float MIN_GAIN_DB = -12.0f;
    static constexpr float MAX_GAIN_DB = 12.0f;

    void configure(float sampleRate);
    void setBandLevel(int band, float dB);
    void setLegacyBandLevel(int band, float dB);
    float getBandLevel(int band) const;
    void process(float* buffer, int frames);
    void setEnabled(bool enabled);
    bool isEnabled() const { return mEnabled; }
    void reset();

private:
    void updateBand(int band);

    bool mEnabled = false;
    float mSampleRate = 48000.0f;
    float mBandLevels[NUM_BANDS] = {};
    Biquad mFiltersL[NUM_BANDS];
    Biquad mFiltersR[NUM_BANDS];
};

}  // namespace axionfx
