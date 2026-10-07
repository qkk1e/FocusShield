package com.focusshield.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var selectedMinutes = 30
    private var isInfinite = false
    private var isBlockingActive = false
    private var timer: CountDownTimer? = null

    private lateinit var timerDisplay: TextView
    private lateinit var durationText: TextView
    private lateinit var btnStartBlock: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerDisplay = findViewById(R.id.timerDisplay)
        durationText = findViewById(R.id.durationText)
        btnStartBlock = findViewById(R.id.btnStartBlock)

        val btnPlus = findViewById<TextView>(R.id.btnPlus)
        val btnMinus = findViewById<TextView>(R.id.btnMinus)
        val btnInfinite = findViewById<TextView>(R.id.btnInfinite)

        btnPlus.setOnClickListener {
            if (!isBlockingActive && !isInfinite) {
                selectedMinutes += 5
                updateControlsUI()
            }
        }

        btnMinus.setOnClickListener {
            if (!isBlockingActive && !isInfinite && selectedMinutes > 5) {
                selectedMinutes -= 5
                updateControlsUI()
            }
        }

        btnInfinite.setOnClickListener {
            if (!isBlockingActive) {
                isInfinite = !isInfinite
                updateControlsUI()
            }
        }

        btnStartBlock.setOnClickListener {
            if (!checkPermissions()) {
                requestPermissions()
                return@setOnClickListener
            }

            if (isBlockingActive) {
                stopBlockSession()
            } else {
                startBlockSession()
            }
        }

        updateControlsUI()
    }

    private fun updateControlsUI() {
        if (isInfinite) {
            durationText.text = "Infinite"
            timerDisplay.text = "∞"
            btnStartBlock.text = "🔒  Start Infinite block"
        } else {
            durationText.text = "$selectedMinutes min"
            timerDisplay.text = String.format("%02d:00", selectedMinutes)
            btnStartBlock.text = "🔒  Start $selectedMinutes min block"
        }
    }

    private fun startBlockSession() {
        isBlockingActive = true
        setBlockingStateInPrefs(true)

        btnStartBlock.text = "⏹  Stop Blocking"

        if (isInfinite) {
            timerDisplay.text = "∞"
            Toast.makeText(this, "Infinite app blocking activated", Toast.LENGTH_SHORT).show()
        } else {
            val totalMillis = selectedMinutes * 60 * 1000L
            timer = object : CountDownTimer(totalMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val minutes = (millisUntilFinished / 1000) / 60
                    val seconds = (millisUntilFinished / 1000) % 60
                    timerDisplay.text = String.format("%02d:%02d", minutes, seconds)
                }

                override fun onFinish() {
                    stopBlockSession()
                    Toast.makeText(this@MainActivity, "Block session completed!", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    private fun stopBlockSession() {
        isBlockingActive = false
        setBlockingStateInPrefs(false)
        timer?.cancel()
        btnStartBlock.text = if (isInfinite) "🔒  Start Infinite block" else "🔒  Start $selectedMinutes min block"
        updateControlsUI()
        Toast.makeText(this, "Blocking stopped", Toast.LENGTH_SHORT).show()
    }

    private fun setBlockingStateInPrefs(active: Boolean) {
        val prefs = getSharedPreferences("FocusShieldPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("IS_BLOCKING_ACTIVE", active).apply()
    }

    private fun checkPermissions(): Boolean {
        val overlayAllowed = Settings.canDrawOverlays(this)
        val accessibilityAllowed = isAccessibilityServiceEnabled()
        return overlayAllowed && accessibilityAllowed
    }

    private fun requestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please enable 'Display over other apps'", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable FocusShield in Accessibility", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return prefString.contains(packageName)
    }
}
