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

package com.android.axion.axionfx.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.android.axion.axionfx.AxionFxActivity
import com.android.axion.axionfx.R
import com.android.axion.axionfx.AxionFxController
import com.android.axion.axionfx.device.DeviceCategory
import com.android.axion.axionfx.device.DeviceProfile
import com.android.axion.axionfx.device.DeviceProfileManager
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AxionFxService : Service() {

    private lateinit var prefs: SharedPreferences

    private val routingThread = HandlerThread("AxionFxRouting").apply { start() }
    private val routingHandler = Handler(routingThread.looper)
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private var lastAppliedCategory: DeviceCategory? = null

    private val evalRunnable = Runnable { evaluateRoutingChange() }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) {
            scheduleRoutingEval()
        }

        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) {
            scheduleRoutingEval()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getPrefs(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        AxionFxController.attachSession(0)
        restoreSettings()
        _autoSwitchEnabled.value = prefs.getBoolean(KEY_AUTO_SWITCH, true)
        audioManager?.registerAudioDeviceCallback(deviceCallback, routingHandler)
        scheduleRoutingEval()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                AxionFxController.setMasterEnabled(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        val masterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        if (!masterEnabled) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        routingHandler.removeCallbacks(evalRunnable)
        routingThread.quitSafely()
        AxionFxController.releaseAll()
        super.onDestroy()
    }

    fun setAutoSwitchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SWITCH, enabled).apply()
        _autoSwitchEnabled.value = enabled
        if (enabled) scheduleRoutingEval()
    }

    private fun scheduleRoutingEval() {
        routingHandler.removeCallbacks(evalRunnable)
        routingHandler.postDelayed(evalRunnable, ROUTING_DEBOUNCE_MS)
    }

    private fun evaluateRoutingChange() {
        val routed = DeviceCategory.routedOutput(this)
        _currentDeviceCategory.value = routed.category
        _currentDeviceName.value = routed.deviceName
        if (!_autoSwitchEnabled.value) return
        if (routed.category == lastAppliedCategory) return
        val profile = DeviceProfile.Fixed(routed.category)
        val token = DeviceProfileManager.getBinding(prefs, profile)
        if (token.isNullOrEmpty()) {
            lastAppliedCategory = routed.category
            return
        }
        val applied = DeviceProfileManager.applyBinding(this, prefs, profile)
        if (applied) {
            restoreSettings()
            lastAppliedCategory = routed.category
            _appliedPresetName.value = DeviceProfileManager.displayName(token)
            Log.d(TAG, "Auto-switched profile for ${routed.category}")
        }
    }

    fun applyProfile(profile: DeviceProfile): Boolean {
        val token = DeviceProfileManager.getBinding(prefs, profile) ?: return false
        val ok = DeviceProfileManager.applyBinding(this, prefs, profile)
        if (ok) {
            restoreSettings()
            _appliedPresetName.value = DeviceProfileManager.displayName(token)
            if (profile is DeviceProfile.Fixed) lastAppliedCategory = profile.category
        }
        return ok
    }

    internal fun restoreSettings() {
        AxionFxController.setOutputGain(prefs.getInt(KEY_OUTPUT_GAIN, 100))
        AxionFxController.setParameter(0x102, prefs.getInt("output_pan", 0))

        for (i in 0..31) {
            AxionFxController.setArbitraryEqBandLevel(i, prefs.getInt("${KEY_ARBITRARY_EQ_BAND_PREFIX}$i", 0))
        }
        for (i in 0..9) {
            AxionFxController.setEqBandLevel(i, prefs.getInt("${KEY_EQ_BAND_PREFIX}$i", 0))
        }
        AxionFxController.setEqEnabled(prefs.getBoolean(KEY_EQ_ENABLED, false))

        AxionFxController.setBassMode(prefs.getInt(KEY_BASS_MODE, 0))
        AxionFxController.setBassGain(prefs.getInt(KEY_BASS_GAIN, 0))
        AxionFxController.setBassEnabled(prefs.getBoolean(KEY_BASS_ENABLED, false))

        AxionFxController.setWidenerWidth(prefs.getInt(KEY_WIDENER_WIDTH, 100))
        AxionFxController.setWidenerEnabled(prefs.getBoolean(KEY_WIDENER_ENABLED, false))

        AxionFxController.setParameter(0x501, prefs.getInt("limiter_threshold", -10))
        AxionFxController.setLimiterEnabled(prefs.getBoolean(KEY_LIMITER_ENABLED, true))

        AxionFxController.setReverbRoomSize(prefs.getInt(KEY_REVERB_ROOM, 50))
        AxionFxController.setReverbWet(prefs.getInt(KEY_REVERB_WET, 30))
        AxionFxController.setReverbEnabled(prefs.getBoolean(KEY_REVERB_ENABLED, false))

        AxionFxController.setCompressorEnabled(prefs.getBoolean(KEY_COMPRESSOR_ENABLED, false))

        AxionFxController.setTubeDrive(prefs.getInt(KEY_TUBE_DRIVE, 100))
        AxionFxController.setTubeMix(prefs.getInt(KEY_TUBE_MIX, 50))
        AxionFxController.setTubeEnabled(prefs.getBoolean(KEY_TUBE_ENABLED, false))

        AxionFxController.setAgcEnabled(prefs.getBoolean(KEY_AGC_ENABLED, false))

        AxionFxController.setCrossfeedLevel(prefs.getInt(KEY_CROSSFEED_LEVEL, 30))
        AxionFxController.setCrossfeedEnabled(prefs.getBoolean(KEY_CROSSFEED_ENABLED, false))

        AxionFxController.setParameter(0xB01, prefs.getInt("surround_delay", 1200))
        AxionFxController.setParameter(0xB02, prefs.getInt("surround_width", 60))
        AxionFxController.setSurroundEnabled(prefs.getBoolean(KEY_SURROUND_ENABLED, false))

        AxionFxController.setExciterDrive(prefs.getInt("exciter_drive", 50))
        AxionFxController.setExciterBlend(prefs.getInt("exciter_blend", 30))
        AxionFxController.setExciterFreq(prefs.getInt("exciter_freq", 3000))
        AxionFxController.setExciterEnabled(prefs.getBoolean("exciter_enabled", false))

        for (i in 0..3) {
            AxionFxController.setMCompBandThreshold(i, prefs.getInt("mcomp_thresh_$i", -200))
            AxionFxController.setMCompBandRatio(i, prefs.getInt("mcomp_ratio_$i", 400))
            AxionFxController.setMCompBandMakeup(i, prefs.getInt("mcomp_makeup_$i", 0))
        }
        AxionFxController.setMCompEnabled(prefs.getBoolean("mcomp_enabled", false))

        for (i in 0..14) {
            AxionFxController.setFirEqBandGain(i, prefs.getInt("fir_eq_band_$i", 0))
        }
        AxionFxController.setFirEqEnabled(prefs.getBoolean("fir_eq_enabled", false))

        AxionFxController.setParameter(0xC01, prefs.getInt("convolver_mix", 100))
        prefs.getString("convolver_ir_path", null)?.let { path ->
            try {
                val wavBytes = java.io.File(path).readBytes()
                AxionFxController.loadConvolverIrData(wavBytes)
            } catch (_: Exception) {}
        }
        AxionFxController.setConvolverEnabled(prefs.getBoolean("convolver_enabled", false))

        AxionFxController.setSpatialBlend(prefs.getInt("spatial_blend", 70))
        AxionFxController.setParameter(0x1004, prefs.getInt("spatial_hrtf_profile", 0))
        AxionFxController.setSpatialEnabled(prefs.getBoolean("spatial_enabled", false))

        val masterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        _masterEnabled.value = masterEnabled
        AxionFxController.setMasterEnabled(masterEnabled)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(active: Boolean = true): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, AxionFxActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = if (active) getString(R.string.notification_title)
            else getString(R.string.notification_title_idle)
        val text = if (active) getString(R.string.notification_text)
            else getString(R.string.notification_text_idle)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tapIntent)
            .setOngoing(active)
            .build()
    }

    private fun updateNotification(active: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(active))
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "axionfx_processing"
        const val PREFS_NAME = "axionfx_settings"

        const val KEY_MASTER_ENABLED = "master_enabled"
        const val KEY_EQ_ENABLED = "eq_enabled"
        const val KEY_EQ_BAND_PREFIX = "eq_band_"
        const val KEY_ARBITRARY_EQ_BAND_PREFIX = "arbitrary_eq_band_"
        const val KEY_BASS_ENABLED = "bass_enabled"
        const val KEY_BASS_MODE = "bass_mode"
        const val KEY_BASS_GAIN = "bass_gain"
        const val KEY_WIDENER_ENABLED = "widener_enabled"
        const val KEY_WIDENER_WIDTH = "widener_width"
        const val KEY_LIMITER_ENABLED = "limiter_enabled"
        const val KEY_REVERB_ENABLED = "reverb_enabled"
        const val KEY_REVERB_ROOM = "reverb_room"
        const val KEY_REVERB_WET = "reverb_wet"
        const val KEY_COMPRESSOR_ENABLED = "compressor_enabled"
        const val KEY_TUBE_ENABLED = "tube_enabled"
        const val KEY_TUBE_DRIVE = "tube_drive"
        const val KEY_TUBE_MIX = "tube_mix"
        const val KEY_AGC_ENABLED = "agc_enabled"
        const val KEY_CROSSFEED_ENABLED = "crossfeed_enabled"
        const val KEY_CROSSFEED_LEVEL = "crossfeed_level"
        const val KEY_SURROUND_ENABLED = "surround_enabled"
        const val KEY_OUTPUT_GAIN = "output_gain"
        const val KEY_MEDIA_ONLY = "media_only_mode"
        const val KEY_AUTO_SWITCH = "device_profile_auto_switch"
        private const val TAG = "AxionFxService"
        private const val ROUTING_DEBOUNCE_MS = 250L

        private const val ACTION_STOP = "com.android.axion.axionfx.STOP"
        internal var instance: AxionFxService? = null
        private val _mediaPlaying = MutableStateFlow(false)
        val mediaPlayingFlow: StateFlow<Boolean> = _mediaPlaying.asStateFlow()
        private val _currentDeviceCategory = MutableStateFlow(DeviceCategory.SPEAKER)
        val currentDeviceCategoryFlow: StateFlow<DeviceCategory> = _currentDeviceCategory.asStateFlow()
        private val _currentDeviceName = MutableStateFlow<String?>(null)
        val currentDeviceNameFlow: StateFlow<String?> = _currentDeviceName.asStateFlow()
        private val _appliedPresetName = MutableStateFlow<String?>(null)
        val appliedPresetNameFlow: StateFlow<String?> = _appliedPresetName.asStateFlow()
        private val _autoSwitchEnabled = MutableStateFlow(true)
        val autoSwitchEnabledFlow: StateFlow<Boolean> = _autoSwitchEnabled.asStateFlow()
        private val _masterEnabled = MutableStateFlow(true)
        val masterEnabledFlow: StateFlow<Boolean> = _masterEnabled.asStateFlow()

        internal fun updateMasterEnabledFlow(enabled: Boolean) {
            _masterEnabled.value = enabled
        }

        fun primeFromContext(context: Context) {
            val routed = DeviceCategory.routedOutput(context)
            _currentDeviceCategory.value = routed.category
            _currentDeviceName.value = routed.deviceName
            val prefs = getPrefs(context)
            _autoSwitchEnabled.value = prefs.getBoolean(KEY_AUTO_SWITCH, true)
            _masterEnabled.value = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        }

        fun start(context: Context) {
            val prefs = getPrefs(context)
            val masterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true)
            if (!masterEnabled) return
            context.startForegroundService(Intent(context, AxionFxService::class.java))
        }

        fun stop(context: Context) {
            val intent = Intent(context, AxionFxService::class.java)
            intent.action = ACTION_STOP
            context.startService(intent)
        }

        fun getPrefs(context: Context): SharedPreferences =
            context.createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
