package com.focusshield.app.service

import android.os.SystemClock

object QuotaManager {
    var isBlockingEnabled: Boolean = true
    var isHardBlockActive: Boolean = false
    
    private var dailyLimitMillis: Long = 15 * 60 * 1000L
    private var accumulatedTimeTodayMillis: Long = 0L
    private var sessionStartTimeMillis: Long = 0L
    private var isTracking: Boolean = false

    fun startTimer() {
        if (!isTracking) {
            sessionStartTimeMillis = SystemClock.elapsedRealtime()
            isTracking = true
        } else {
            val elapsed = SystemClock.elapsedRealtime() - sessionStartTimeMillis
            accumulatedTimeTodayMillis += elapsed
            sessionStartTimeMillis = SystemClock.elapsedRealtime()
        }
    }

    fun stopTimer() {
        if (isTracking) {
            accumulatedTimeTodayMillis += SystemClock.elapsedRealtime() - sessionStartTimeMillis
            isTracking = false
        }
    }

    fun isDailyLimitExceeded(): Boolean {
        if (isHardBlockActive) return true
        return accumulatedTimeTodayMillis >= dailyLimitMillis
    }

    fun setDailyLimitMinutes(minutes: Int) {
        dailyLimitMillis = minutes * 60 * 1000L
    }
}
