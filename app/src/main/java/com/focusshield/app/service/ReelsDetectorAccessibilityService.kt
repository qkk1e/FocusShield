package com.focusshield.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReelsDetectorAccessibilityService : AccessibilityService() {

    private val targetPackages = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !QuotaManager.isBlockingEnabled) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName !in targetPackages) return

        val rootNode = rootInActiveWindow ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                inspectAndEnforce(rootNode, packageName)
            }
        }
    }

    private fun inspectAndEnforce(rootNode: AccessibilityNodeInfo, packageName: String) {
        val isReelActive = when (packageName) {
            "com.instagram.android" -> isMatchingNode(rootNode, "reels_viewer", "clips_viewer")
            "com.google.android.youtube" -> isMatchingNode(rootNode, "shorts_player", "reel_recycler")
            "com.zhiliaoapp.musically" -> isMatchingNode(rootNode, "view_pager", "main_tab")
            else -> false
        }

        if (isReelActive) {
            if (QuotaManager.isDailyLimitExceeded()) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                OverlayBlockerService.showBlockerOverlay(this)
            } else {
                QuotaManager.startTimer()
            }
        } else {
            QuotaManager.stopTimer()
            OverlayBlockerService.hideBlockerOverlay(this)
        }
    }

    private fun isMatchingNode(node: AccessibilityNodeInfo, vararg keywords: String): Boolean {
        val viewId = node.viewIdResourceName ?: ""
        if (keywords.any { viewId.contains(it, ignoreCase = true) }) return true

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (isMatchingNode(child, *keywords)) return true
        }
        return false
    }

    override fun onInterrupt() {}
}
