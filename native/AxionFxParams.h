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

#include <cstdint>

namespace axionfx {

struct AxionFxParam {
    int32_t paramId;
    int32_t value;
};

enum ParamId : int32_t {
    PARAM_MASTER_ENABLE = 0x100,
    PARAM_OUTPUT_GAIN = 0x101,
    PARAM_OUTPUT_PAN = 0x102,
    PARAM_PROCESS_CALL_COUNT = 0x103,

    PARAM_EQ_ENABLE = 0x200,
    PARAM_EQ_BAND_LEVEL = 0x201,
    PARAM_EQ_ARBITRARY_BAND_LEVEL = 0x202,

    PARAM_BASS_ENABLE = 0x300,
    PARAM_BASS_MODE = 0x301,
    PARAM_BASS_FREQUENCY = 0x302,
    PARAM_BASS_GAIN = 0x303,

    PARAM_WIDENER_ENABLE = 0x400,
    PARAM_WIDENER_WIDTH = 0x401,

    PARAM_LIMITER_ENABLE = 0x500,
    PARAM_LIMITER_THRESHOLD = 0x501,
    PARAM_LIMITER_RELEASE = 0x502,

    PARAM_REVERB_ENABLE = 0x600,
    PARAM_REVERB_ROOM_SIZE = 0x601,
    PARAM_REVERB_DAMPING = 0x602,
    PARAM_REVERB_WET = 0x603,
    PARAM_REVERB_DRY = 0x604,
    PARAM_REVERB_WIDTH = 0x605,

    PARAM_COMPRESSOR_ENABLE = 0x700,
    PARAM_COMPRESSOR_THRESHOLD = 0x701,
    PARAM_COMPRESSOR_RATIO = 0x702,
    PARAM_COMPRESSOR_ATTACK = 0x703,
    PARAM_COMPRESSOR_RELEASE = 0x704,
    PARAM_COMPRESSOR_KNEE = 0x705,
    PARAM_COMPRESSOR_MAKEUP = 0x706,

    PARAM_TUBE_ENABLE = 0x800,
    PARAM_TUBE_DRIVE = 0x801,
    PARAM_TUBE_MIX = 0x802,

    PARAM_AGC_ENABLE = 0x900,
    PARAM_AGC_TARGET = 0x901,
    PARAM_AGC_MAX_GAIN = 0x902,
    PARAM_AGC_SPEED = 0x903,

    PARAM_CROSSFEED_ENABLE = 0xA00,
    PARAM_CROSSFEED_LEVEL = 0xA01,
    PARAM_CROSSFEED_CUTOFF = 0xA02,

    PARAM_SURROUND_ENABLE = 0xB00,
    PARAM_SURROUND_DELAY = 0xB01,
    PARAM_SURROUND_WIDTH = 0xB02,

    PARAM_CONVOLVER_ENABLE = 0xC00,
    PARAM_CONVOLVER_MIX = 0xC01,
    PARAM_CONVOLVER_LOAD_IR = 0xC02,
    PARAM_CONVOLVER_LOAD_IR_DATA = 0xC03,

    PARAM_MCOMP_ENABLE = 0xD00,
    PARAM_MCOMP_BAND_THRESHOLD = 0xD01,
    PARAM_MCOMP_BAND_RATIO = 0xD02,
    PARAM_MCOMP_BAND_ATTACK = 0xD03,
    PARAM_MCOMP_BAND_RELEASE = 0xD04,
    PARAM_MCOMP_BAND_MAKEUP = 0xD05,
    PARAM_MCOMP_CROSSOVER = 0xD06,

    PARAM_EXCITER_ENABLE = 0xE00,
    PARAM_EXCITER_DRIVE = 0xE01,
    PARAM_EXCITER_BLEND = 0xE02,
    PARAM_EXCITER_FREQ = 0xE03,

    PARAM_FIR_EQ_ENABLE = 0xF00,
    PARAM_FIR_EQ_BAND_GAIN = 0xF01,

    PARAM_SPATIAL_ENABLE = 0x1000,
    PARAM_SPATIAL_WIDTH = 0x1001,
    PARAM_SPATIAL_DIRECTION = 0x1002,
    PARAM_SPATIAL_BLEND = 0x1003,
    PARAM_SPATIAL_HRTF_PROFILE = 0x1004,
};

static constexpr int32_t PARAM_GROUP_MASK = 0xFF00;

static constexpr int32_t EQ_BAND_SHIFT = 16;
static constexpr int32_t EQ_LEVEL_MASK = 0xFFFF;

inline int32_t eqBandValue(int band, int levelCentibels) {
    return (band << EQ_BAND_SHIFT) | (levelCentibels & EQ_LEVEL_MASK);
}

inline int eqBandFromValue(int32_t value) {
    return (value >> EQ_BAND_SHIFT) & 0xFF;
}

inline int eqLevelFromValue(int32_t value) {
    int16_t level = static_cast<int16_t>(value & EQ_LEVEL_MASK);
    return level;
}

}  // namespace axionfx
