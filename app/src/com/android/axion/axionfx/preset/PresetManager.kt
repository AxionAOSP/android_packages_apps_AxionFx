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

package com.android.axion.axionfx.preset

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.axion.axionfx.domain.EffectKeys
import org.json.JSONObject
import java.io.File

object PresetManager {

    private const val TAG = "AxionFxPreset"

    private fun presetsDir(context: Context) = File(context.filesDir, "presets")

    private val PERSISTED_KEYS = listOf(
        EffectKeys.MASTER_ENABLED,
        EffectKeys.OUTPUT_GAIN,
        EffectKeys.OUTPUT_PAN,
        EffectKeys.MEDIA_ONLY,
        EffectKeys.EQ_ENABLED,
        EffectKeys.FIR_EQ_ENABLED,
        EffectKeys.BASS_ENABLED,
        EffectKeys.BASS_MODE,
        EffectKeys.BASS_GAIN,
        EffectKeys.WIDENER_ENABLED,
        EffectKeys.WIDENER_WIDTH,
        EffectKeys.CROSSFEED_ENABLED,
        EffectKeys.CROSSFEED_LEVEL,
        EffectKeys.SURROUND_ENABLED,
        EffectKeys.SURROUND_DELAY,
        EffectKeys.SURROUND_WIDTH,
        EffectKeys.SPATIAL_ENABLED,
        EffectKeys.SPATIAL_WIDTH,
        EffectKeys.SPATIAL_BLEND,
        EffectKeys.COMPRESSOR_ENABLED,
        EffectKeys.AGC_ENABLED,
        EffectKeys.LIMITER_ENABLED,
        EffectKeys.LIMITER_THRESHOLD,
        EffectKeys.REVERB_ENABLED,
        EffectKeys.REVERB_ROOM,
        EffectKeys.REVERB_WET,
        EffectKeys.TUBE_ENABLED,
        EffectKeys.TUBE_DRIVE,
        EffectKeys.TUBE_MIX,
        EffectKeys.EXCITER_ENABLED,
        EffectKeys.EXCITER_DRIVE,
        EffectKeys.EXCITER_BLEND,
        EffectKeys.EXCITER_FREQ,
        EffectKeys.MCOMP_ENABLED,
        EffectKeys.CONVOLVER_ENABLED,
        EffectKeys.CONVOLVER_MIX,
        EffectKeys.CONVOLVER_IR_PATH,
    )

    private val BAND_KEY_PREFIXES = listOf(
        EffectKeys.EQ_BAND_PREFIX to 10,
        EffectKeys.ARBITRARY_EQ_BAND_PREFIX to 32,
        EffectKeys.FIR_EQ_BAND_PREFIX to 128,
        EffectKeys.MCOMP_THRESH_PREFIX to 4,
        EffectKeys.MCOMP_RATIO_PREFIX to 4,
        EffectKeys.MCOMP_MAKEUP_PREFIX to 4,
    )

    fun savePreset(context: Context, name: String, prefs: SharedPreferences) {
        val dir = presetsDir(context)
        dir.mkdirs()
        val json = JSONObject()

        for (key in PERSISTED_KEYS) {
            val all = prefs.all
            if (all.containsKey(key)) {
                when (val value = all[key]) {
                    is Boolean -> json.put(key, value)
                    is Int -> json.put(key, value)
                    is Float -> json.put(key, value.toDouble())
                    is String -> json.put(key, value)
                }
            }
        }

        for ((prefix, count) in BAND_KEY_PREFIXES) {
            for (i in 0 until count) {
                val key = "$prefix$i"
                if (prefs.contains(key)) {
                    json.put(key, prefs.getInt(key, 0))
                }
            }
        }

        val file = File(dir, "${sanitizeName(name)}.json")
        file.writeText(json.toString(2))
        Log.d(TAG, "Saved preset: $name -> ${file.absolutePath}")
    }

    fun loadPreset(context: Context, name: String, prefs: SharedPreferences) {
        val file = File(presetsDir(context), "${sanitizeName(name)}.json")
        if (!file.exists()) return
        loadPresetFromJson(file.readText(), prefs)
        Log.d(TAG, "Loaded preset: $name")
    }

    fun loadPresetFromJson(jsonString: String, prefs: SharedPreferences) {
        val json = JSONObject(jsonString)
        val editor = prefs.edit()

        for (key in json.keys()) {
            when (val value = json.get(key)) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putInt(key, value.toInt())
                is Double -> editor.putFloat(key, value.toFloat())
                is String -> editor.putString(key, value)
            }
        }

        editor.apply()
    }

    private val BUILTIN_PRESETS = mapOf(
        "Clarity" to mapOf(
            EffectKeys.EQ_ENABLED to true,
            "${EffectKeys.EQ_BAND_PREFIX}6" to 150, "${EffectKeys.EQ_BAND_PREFIX}7" to 200, 
            "${EffectKeys.EQ_BAND_PREFIX}8" to 250, "${EffectKeys.EQ_BAND_PREFIX}9" to 150,
            EffectKeys.EXCITER_ENABLED to true,
            EffectKeys.EXCITER_DRIVE to 40, EffectKeys.EXCITER_BLEND to 25, EffectKeys.EXCITER_FREQ to 4000,
            EffectKeys.COMPRESSOR_ENABLED to true,
            EffectKeys.LIMITER_ENABLED to true,
            EffectKeys.OUTPUT_GAIN to 100,
        ),
        "Speaker Boost" to mapOf(
            EffectKeys.BASS_ENABLED to true, EffectKeys.BASS_MODE to 1, EffectKeys.BASS_GAIN to 400,
            EffectKeys.EQ_ENABLED to true,
            "${EffectKeys.EQ_BAND_PREFIX}0" to 300, "${EffectKeys.EQ_BAND_PREFIX}1" to 200, 
            "${EffectKeys.EQ_BAND_PREFIX}8" to 150, "${EffectKeys.EQ_BAND_PREFIX}9" to 100,
            EffectKeys.COMPRESSOR_ENABLED to true,
            EffectKeys.AGC_ENABLED to true,
            EffectKeys.LIMITER_ENABLED to true,
            EffectKeys.OUTPUT_GAIN to 130,
        ),
        "Headphone" to mapOf(
            EffectKeys.CROSSFEED_ENABLED to true, EffectKeys.CROSSFEED_LEVEL to 25,
            EffectKeys.SPATIAL_ENABLED to true, EffectKeys.SPATIAL_BLEND to 50,
            EffectKeys.WIDENER_ENABLED to true, EffectKeys.WIDENER_WIDTH to 130,
            EffectKeys.LIMITER_ENABLED to true,
            EffectKeys.OUTPUT_GAIN to 100,
        ),
        "Bass Heavy" to mapOf(
            EffectKeys.BASS_ENABLED to true, EffectKeys.BASS_MODE to 2, EffectKeys.BASS_GAIN to 800,
            EffectKeys.EQ_ENABLED to true,
            "${EffectKeys.EQ_BAND_PREFIX}0" to 400, "${EffectKeys.EQ_BAND_PREFIX}1" to 350, 
            "${EffectKeys.EQ_BAND_PREFIX}2" to 250, "${EffectKeys.EQ_BAND_PREFIX}3" to 100,
            EffectKeys.TUBE_ENABLED to true, EffectKeys.TUBE_DRIVE to 150, EffectKeys.TUBE_MIX to 30,
            EffectKeys.LIMITER_ENABLED to true,
            EffectKeys.OUTPUT_GAIN to 110,
        ),
        "Vocal" to mapOf(
            EffectKeys.EQ_ENABLED to true,
            "${EffectKeys.EQ_BAND_PREFIX}0" to -200, "${EffectKeys.EQ_BAND_PREFIX}1" to -100,
            "${EffectKeys.EQ_BAND_PREFIX}4" to 250, "${EffectKeys.EQ_BAND_PREFIX}5" to 300, 
            "${EffectKeys.EQ_BAND_PREFIX}6" to 200,
            "${EffectKeys.EQ_BAND_PREFIX}8" to -100, "${EffectKeys.EQ_BAND_PREFIX}9" to -200,
            EffectKeys.EXCITER_ENABLED to true,
            EffectKeys.EXCITER_DRIVE to 30, EffectKeys.EXCITER_BLEND to 20, EffectKeys.EXCITER_FREQ to 3000,
            EffectKeys.COMPRESSOR_ENABLED to true,
            EffectKeys.LIMITER_ENABLED to true,
            EffectKeys.OUTPUT_GAIN to 100,
        ),
    )

    fun listBuiltinPresets(): List<String> = BUILTIN_PRESETS.keys.toList()

    fun loadBuiltinPreset(name: String, prefs: SharedPreferences) {
        val preset = BUILTIN_PRESETS[name] ?: return
        val editor = prefs.edit()
        editor.clear()
        for ((key, value) in preset) {
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is String -> editor.putString(key, value)
            }
        }
        editor.putBoolean(EffectKeys.MASTER_ENABLED, true)
        editor.apply()
        Log.d(TAG, "Loaded builtin preset: $name")
    }

    fun deletePreset(context: Context, name: String) {
        val file = getPresetFile(context, name)
        if (file.exists()) file.delete()
    }

    fun renamePreset(context: Context, oldName: String, newName: String) {
        val oldFile = getPresetFile(context, oldName)
        val newFile = getPresetFile(context, newName)
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
        }
    }

    fun listPresets(context: Context): List<String> {
        val dir = presetsDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    fun exportPreset(context: Context, name: String): String? {
        val file = getPresetFile(context, name)
        return if (file.exists()) file.readText() else null
    }

    fun getPresetFile(context: Context, name: String): File {
        return File(presetsDir(context), "${sanitizeName(name)}.json")
    }

    fun importPreset(context: Context, name: String, json: String) {
        val dir = presetsDir(context)
        dir.mkdirs()
        File(dir, "${sanitizeName(name)}.json").writeText(json)
    }

    private fun sanitizeName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
}
