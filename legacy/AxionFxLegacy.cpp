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

#define LOG_TAG "AxionFxLegacy"

#include <cstring>
#include <cstdlib>
#include <new>

#include <hardware/audio_effect.h>
#include <log/log.h>
#include <system/audio.h>

#include "AxionFxEngine.h"
#include "AxionFxParams.h"

static const effect_uuid_t kAxionFxTypeUuid = {
    0x5867be72, 0x4060, 0x4c55, 0xa378, {0xc1, 0xcd, 0xef, 0x3e, 0x13, 0x53}
};

static const effect_uuid_t kAxionFxImplUuid = {
    0xf35cb927, 0xa887, 0x4f3d, 0x847f, {0x77, 0x06, 0x34, 0x48, 0x6d, 0x53}
};

struct AxionFxContext {
    struct effect_interface_s *itfe;
    effect_config_t config;
    axionfx::AxionFxEngine engine;
    bool enabled;
    int32_t sessionId;
};

static int axfx_process(effect_handle_t self, audio_buffer_t *inBuffer,
                        audio_buffer_t *outBuffer) {
    auto *ctx = reinterpret_cast<AxionFxContext *>(self);
    if (!ctx || !inBuffer || !outBuffer) return -EINVAL;
    if (!ctx->enabled) {
        if (inBuffer->raw != outBuffer->raw) {
            memcpy(outBuffer->f32, inBuffer->f32,
                   inBuffer->frameCount * sizeof(float) *
                   audio_channel_count_from_out_mask(ctx->config.inputCfg.channels));
        }
        return 0;
    }
    int channels = audio_channel_count_from_out_mask(ctx->config.inputCfg.channels);
    ctx->engine.process(inBuffer->f32, outBuffer->f32,
                        static_cast<int>(inBuffer->frameCount) * channels);
    return 0;
}

static int axfx_command(effect_handle_t self, uint32_t cmdCode, uint32_t cmdSize,
                        void *pCmdData, uint32_t *replySize, void *pReplyData) {
    auto *ctx = reinterpret_cast<AxionFxContext *>(self);
    if (!ctx) return -EINVAL;

    switch (cmdCode) {
        case EFFECT_CMD_INIT:
            if (replySize && *replySize >= sizeof(int32_t) && pReplyData) {
                *static_cast<int32_t *>(pReplyData) = 0;
            }
            return 0;

        case EFFECT_CMD_SET_CONFIG: {
            if (cmdSize < sizeof(effect_config_t) || !pCmdData) return -EINVAL;
            memcpy(&ctx->config, pCmdData, sizeof(effect_config_t));
            float sr = static_cast<float>(ctx->config.inputCfg.samplingRate);
            if (sr > 0) ctx->engine.configure(sr);
            if (replySize && *replySize >= sizeof(int32_t) && pReplyData) {
                *static_cast<int32_t *>(pReplyData) = 0;
            }
            return 0;
        }

        case EFFECT_CMD_GET_CONFIG:
            if (replySize && *replySize >= sizeof(effect_config_t) && pReplyData) {
                memcpy(pReplyData, &ctx->config, sizeof(effect_config_t));
            }
            return 0;

        case EFFECT_CMD_RESET:
            ctx->engine.configure(
                static_cast<float>(ctx->config.inputCfg.samplingRate));
            return 0;

        case EFFECT_CMD_ENABLE:
            ctx->enabled = true;
            if (replySize && *replySize >= sizeof(int32_t) && pReplyData) {
                *static_cast<int32_t *>(pReplyData) = 0;
            }
            return 0;

        case EFFECT_CMD_DISABLE:
            ctx->enabled = false;
            ctx->engine.setMasterEnabled(false);
            if (replySize && *replySize >= sizeof(int32_t) && pReplyData) {
                *static_cast<int32_t *>(pReplyData) = 0;
            }
            return 0;

        case EFFECT_CMD_SET_PARAM: {
            if (cmdSize < sizeof(effect_param_t) + 8 || !pCmdData) return -EINVAL;
            auto *param = static_cast<effect_param_t *>(pCmdData);
            if (param->psize < 4 || param->vsize < 4) return -EINVAL;
            int32_t paramId = *reinterpret_cast<int32_t *>(param->data);
            uint32_t valueOffset = (param->psize + 3) & ~3;
            if (paramId == axionfx::PARAM_CONVOLVER_LOAD_IR ||
                paramId == axionfx::PARAM_CONVOLVER_LOAD_IR_DATA) {
                const uint8_t *wavData = reinterpret_cast<const uint8_t *>(
                    param->data + valueOffset);
                ctx->engine.loadIrFromData(wavData, param->vsize);
            } else {
                int32_t value = *reinterpret_cast<int32_t *>(
                    param->data + valueOffset);
                ctx->engine.setParameter(paramId, value);
            }
            if (replySize && *replySize >= sizeof(int32_t) && pReplyData) {
                *static_cast<int32_t *>(pReplyData) = 0;
            }
            return 0;
        }

        case EFFECT_CMD_GET_PARAM: {
            if (cmdSize < sizeof(effect_param_t) + 4 || !pCmdData) return -EINVAL;
            auto *param = static_cast<effect_param_t *>(pCmdData);
            if (param->psize < 4) return -EINVAL;
            int32_t paramId = *reinterpret_cast<int32_t *>(param->data);
            int32_t value = ctx->engine.getParameter(paramId);
            if (replySize && pReplyData) {
                auto *reply = static_cast<effect_param_t *>(pReplyData);
                reply->status = 0;
                reply->psize = 4;
                reply->vsize = 4;
                *reinterpret_cast<int32_t *>(reply->data) = paramId;
                *reinterpret_cast<int32_t *>(
                    reply->data + ((reply->psize + 3) & ~3)) = value;
                *replySize = sizeof(effect_param_t) + 8;
            }
            return 0;
        }

        default:
            return -EINVAL;
    }
}

static void axfx_fill_descriptor(effect_descriptor_t *pDescriptor);

static int axfx_get_descriptor_from_handle(effect_handle_t self,
                                           effect_descriptor_t *pDescriptor) {
    if (!pDescriptor) return -EINVAL;
    axfx_fill_descriptor(pDescriptor);
    return 0;
}

static struct effect_interface_s kAxionFxInterface = {
    axfx_process,
    axfx_command,
    axfx_get_descriptor_from_handle,
    nullptr
};

static int axfx_lib_create(const effect_uuid_t *uuid, int32_t sessionId,
                           int32_t /* ioId */, effect_handle_t *pHandle) {
    if (!uuid || !pHandle) return -EINVAL;
    if (memcmp(uuid, &kAxionFxImplUuid, sizeof(effect_uuid_t)) != 0) return -ENOENT;

    auto *ctx = new (std::nothrow) AxionFxContext();
    if (!ctx) return -ENOMEM;

    ctx->itfe = &kAxionFxInterface;
    memset(&ctx->config, 0, sizeof(effect_config_t));
    ctx->config.inputCfg.samplingRate = 48000;
    ctx->config.inputCfg.channels = AUDIO_CHANNEL_OUT_STEREO;
    ctx->config.inputCfg.format = AUDIO_FORMAT_PCM_FLOAT;
    ctx->config.outputCfg = ctx->config.inputCfg;
    ctx->enabled = false;
    ctx->sessionId = sessionId;
    ctx->engine.configure(48000.0f);

    *pHandle = reinterpret_cast<effect_handle_t>(&ctx->itfe);
    ALOGI("AxionFx: created effect instance for session %d", sessionId);
    return 0;
}

static int axfx_lib_release(effect_handle_t handle) {
    if (!handle) return -EINVAL;
    auto *ctx = reinterpret_cast<AxionFxContext *>(handle);
    delete ctx;
    ALOGI("AxionFx: released effect instance");
    return 0;
}

static void axfx_fill_descriptor(effect_descriptor_t *pDescriptor) {
    pDescriptor->type = kAxionFxTypeUuid;
    pDescriptor->uuid = kAxionFxImplUuid;
    pDescriptor->apiVersion = EFFECT_CONTROL_API_VERSION;
    pDescriptor->flags = EFFECT_FLAG_TYPE_INSERT | EFFECT_FLAG_INSERT_LAST;
    pDescriptor->cpuLoad = 10;
    pDescriptor->memoryUsage = 0;
    strncpy(pDescriptor->name, "AxionFx", sizeof(pDescriptor->name) - 1);
    strncpy(pDescriptor->implementor, "AxionOS", sizeof(pDescriptor->implementor) - 1);
}

static int axfx_lib_get_descriptor(const effect_uuid_t *uuid,
                                   effect_descriptor_t *pDescriptor) {
    if (!uuid || !pDescriptor) return -EINVAL;
    if (memcmp(uuid, &kAxionFxImplUuid, sizeof(effect_uuid_t)) != 0) return -ENOENT;
    axfx_fill_descriptor(pDescriptor);
    return 0;
}

extern "C" {

__attribute__((visibility("default")))
audio_effect_library_t AUDIO_EFFECT_LIBRARY_INFO_SYM = {
    .tag = AUDIO_EFFECT_LIBRARY_TAG,
    .version = EFFECT_LIBRARY_API_VERSION,
    .name = "AxionFx",
    .implementor = "AxionOS",
    .create_effect = axfx_lib_create,
    .release_effect = axfx_lib_release,
    .get_descriptor = axfx_lib_get_descriptor,
};

}
