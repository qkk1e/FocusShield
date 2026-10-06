package com.focusshield.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.focusshield.app.R

class OverlayBlockerService : Service() {

    private var windowManager: WindowManager? = null
    private var blockerOverlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_SHOW" -> showOverlay()
            "ACTION_HIDE" -> hideOverlay()
        }
        return START_STICKY
    }

    private fun showOverlay() {
        if (blockerOverlayView != null) return

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        blockerOverlayView = inflater.inflate(R.layout.view_shield_overlay, null)

        windowManager?.addView(blockerOverlayView, params)
    }

    private fun hideOverlay() {
        blockerOverlayView?.let {
            windowManager?.removeView(it)
            blockerOverlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun showBlockerOverlay(context: Context) {
            val intent = Intent(context, OverlayBlockerService::class.java).apply {
                action = "ACTION_SHOW"
            }
            context.startService(intent)
        }

        fun hideBlockerOverlay(context: Context) {
            val intent = Intent(context, OverlayBlockerService::class.java).apply {
                action = "ACTION_HIDE"
            }
            context.startService(intent)
        }
    }
}
