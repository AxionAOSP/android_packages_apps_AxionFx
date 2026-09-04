package com.android.axion.axionfx.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.axion.axionfx.AxionFxController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EffectHeartbeatMonitor(
    private val readHeartbeat: () -> Int,
    private val pollIntervalMs: Long = 1500L,
    private val gracePeriodMs: Long = 1000L,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastCount: Int? = null
    private var playbackStartedAt: Long = 0L

    private val _isHealthy = MutableStateFlow(true)
    val isHealthy: StateFlow<Boolean> = _isHealthy.asStateFlow()

    private val pollRunnable = object : Runnable {
        override fun run() {
            poll()
            handler.postDelayed(this, pollIntervalMs)
        }
    }

    fun onPlaybackStarted() {
        playbackStartedAt = System.currentTimeMillis()
        lastCount = null
        _isHealthy.value = true
        handler.removeCallbacks(pollRunnable)
        handler.postDelayed(pollRunnable, gracePeriodMs + pollIntervalMs)
    }

    fun onPlaybackStopped() {
        handler.removeCallbacks(pollRunnable)
        _isHealthy.value = true
    }

    private fun poll() {
        val elapsed = System.currentTimeMillis() - playbackStartedAt
        if (elapsed < gracePeriodMs) return

        val count = readHeartbeat()
        val previous = lastCount
        if (previous != null) {
            val advanced = (count - previous) != 0
            _isHealthy.value = advanced
            if (!advanced) {
                Log.w(TAG, "Heartbeat stalled: count=$count unchanged since last check")
            }
        }
        lastCount = count
    }

    fun stop() {
        handler.removeCallbacks(pollRunnable)
    }

    companion object {
        private const val TAG = "EffectHeartbeatMonitor"
    }
}
