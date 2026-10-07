package com.focusshield.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class ReelsDetectorAccessibilityService : AccessibilityService() {

    private val blockedPackages = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.twitter.android",
        "com.x.android"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("FocusShieldPrefs", Context.MODE_PRIVATE)
        val isBlockingActive = prefs.getBoolean("IS_BLOCKING_ACTIVE", false)

        if (!isBlockingActive) return

        val packageName = event.packageName?.toString() ?: return

        if (blockedPackages.contains(packageName)) {
            // Instantly send user back to Home Screen
            performGlobalAction(GLOBAL_ACTION_HOME)

            // Trigger Overlay Blocker Service
            val overlayIntent = Intent(this, OverlayBlockerService::class.java)
            startService(overlayIntent)
        }
    }

    override fun onInterrupt() {}
}
