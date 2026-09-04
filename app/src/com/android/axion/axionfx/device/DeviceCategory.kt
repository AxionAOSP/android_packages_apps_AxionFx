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

package com.android.axion.axionfx.device

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCodecConfig
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

enum class DeviceCategory(val prefKey: String) {
    SPEAKER("device_profile_speaker"),
    WIRED("device_profile_wired"),
    BLUETOOTH("device_profile_bluetooth"),
    USB("device_profile_usb"),
    OTHER("device_profile_other");

    companion object {
        private val PRIORITY = listOf(USB, WIRED, BLUETOOTH, SPEAKER, OTHER)

        fun fromDeviceType(type: Int): DeviceCategory = when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> SPEAKER

            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_AUX_LINE -> WIRED

            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST,
            AudioDeviceInfo.TYPE_HEARING_AID -> BLUETOOTH

            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> USB

            else -> OTHER
        }

        fun getActiveA2dpCodecName(context: Context, onResult: (String?) -> Unit) {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                onResult(null)
                return
            }

            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        val a2dp = proxy as BluetoothA2dp
                        val device = a2dp.connectedDevices.firstOrNull()
                        val codecStatus = device?.let { a2dp.getCodecStatus(it) }

                        val codecName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            codecStatus?.codecConfig?.extendedCodecType?.codecName
                        } else {
                            codecStatus?.codecConfig?.let { config ->
                                @Suppress("DEPRECATION")
                                when (config.codecType) {
                                    BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC -> "SBC"
                                    BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC -> "AAC"
                                    BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX -> "aptX"
                                    BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD -> "aptX HD"
                                    BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC -> "LDAC"
                                    BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS -> "Opus"
                                    else -> "Unknown"
                                }
                            }
                        }

                        adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        onResult(codecName)
                    }
                }
                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
        }

        fun activeOutputCategory(context: Context): DeviceCategory =
            routedOutput(context).category

        fun routedOutput(context: Context): RoutedOutput {
            val am = context.getSystemService(AudioManager::class.java)
                ?: return RoutedOutput(SPEAKER, null)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val routed = runCatching { am.getAudioDevicesForAttributes(attrs) }
                .getOrNull()
                ?.firstOrNull { it.isSink }
            if (routed != null) {
                return RoutedOutput(fromDeviceType(routed.type), cleanName(routed))
            }
            val attached = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { it.isSink }
            val cats = attached.map { fromDeviceType(it.type) }.toSet()
            val category = PRIORITY.firstOrNull { it in cats } ?: SPEAKER
            val pick = attached.firstOrNull { fromDeviceType(it.type) == category }
            return RoutedOutput(category, pick?.let { cleanName(it) })
        }

        private fun cleanName(device: AudioDeviceInfo): String? {
            val category = fromDeviceType(device.type)
            if (category == SPEAKER) return null
            val raw = device.productName?.toString()?.trim().orEmpty()
            if (raw.isEmpty()) return null
            if (raw.equals(Build.MODEL, ignoreCase = true)) return null
            return raw
        }
    }
}

data class RoutedOutput(val category: DeviceCategory, val deviceName: String?)
