package com.focusshield.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReelsDetectorAccessibilityService : AccessibilityService() {

    private var lastRedirectTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("FocusShieldPrefs", Context.MODE_PRIVATE)
        val isBlockingActive = prefs.getBoolean("IS_BLOCKING_ACTIVE", false)

        if (!isBlockingActive) return

        val packageName = event.packageName?.toString() ?: return

        // Throttle redirects to avoid loops
        if (System.currentTimeMillis() - lastRedirectTime < 1500) return

        val rootNode = rootInActiveWindow ?: return

        when (packageName) {
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> handleTikTokBlocking(rootNode)
            "com.instagram.android" -> handleInstagramBlocking(rootNode)
            "com.google.android.youtube" -> handleYouTubeShortsBlocking(rootNode)
        }
    }

    private fun handleTikTokBlocking(rootNode: AccessibilityNodeInfo) {
        // Detect "For You" or "Dla Ciebie" short-video tab
        val forYouNodes = rootNode.findAccessibilityNodeInfosByText("For You")
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Dla Ciebie") }

        if (forYouNodes.isNotEmpty()) {
            // Find "Inbox", "Skrzynka odbiorcza", or "Messages" tab node to auto-redirect
            val redirectTargets = rootNode.findAccessibilityNodeInfosByText("Inbox")
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Skrzynka odbiorcza") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Profile") }

            var clicked = false
            for (node in redirectTargets) {
                if (performClickOnNodeOrParent(node)) {
                    clicked = true
                    triggerOverlayNotice()
                    break
                }
            }

            if (!clicked) {
                // Fallback: Press system Back button to leave feed
                performGlobalAction(GLOBAL_ACTION_BACK)
                triggerOverlayNotice()
            }
            lastRedirectTime = System.currentTimeMillis()
        }
    }

    private fun handleInstagramBlocking(rootNode: AccessibilityNodeInfo) {
        // Detect Instagram Reels tab
        val reelsNodes = rootNode.findAccessibilityNodeInfosByText("Reels")
        
        if (reelsNodes.isNotEmpty()) {
            // Find Direct Messages tab or Home tab node to auto-redirect
            val redirectTargets = rootNode.findAccessibilityNodeInfosByText("Direct")
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Messages") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Chats") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Wiadomości") }

            var clicked = false
            for (node in redirectTargets) {
                if (performClickOnNodeOrParent(node)) {
                    clicked = true
                    triggerOverlayNotice()
                    break
                }
            }

            if (!clicked) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                triggerOverlayNotice()
            }
            lastRedirectTime = System.currentTimeMillis()
        }
    }

    private fun handleYouTubeShortsBlocking(rootNode: AccessibilityNodeInfo) {
        val shortsNodes = rootNode.findAccessibilityNodeInfosByText("Shorts")

        if (shortsNodes.isNotEmpty()) {
            val redirectTargets = rootNode.findAccessibilityNodeInfosByText("Subscriptions")
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Subskrypcje") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Library") }

            var clicked = false
            for (node in redirectTargets) {
                if (performClickOnNodeOrParent(node)) {
                    clicked = true
                    triggerOverlayNotice()
                    break
                }
            }

            if (!clicked) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                triggerOverlayNotice()
            }
            lastRedirectTime = System.currentTimeMillis()
        }
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    private fun triggerOverlayNotice() {
        val overlayIntent = Intent(this, OverlayBlockerService::class.java)
        startService(overlayIntent)
    }

    override fun onInterrupt() {}
}
