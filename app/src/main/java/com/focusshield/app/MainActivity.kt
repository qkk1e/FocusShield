package com.focusshield.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
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

    private lateinit var tabSleep: View
    private lateinit var tabBlocking: View
    private lateinit var tabHome: View
    private lateinit var tabWebApps: View
    private lateinit var tabProfile: View

    private lateinit var textSleep: TextView
    private lateinit var textBlocking: TextView
    private lateinit var textHome: TextView
    private lateinit var textWebApps: TextView
    private lateinit var textProfile: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Tab Layout Views
        tabSleep = findViewById(R.id.tabSleep)
        tabBlocking = findViewById(R.id.tabBlocking)
        tabHome = findViewById(R.id.tabHome)
        tabWebApps = findViewById(R.id.tabWebApps)
        tabProfile = findViewById(R.id.tabProfile)

        // Initialize Nav Text Labels
        textSleep = findViewById(R.id.textSleep)
        textBlocking = findViewById(R.id.textBlocking)
        textHome = findViewById(R.id.textHome)
        textWebApps = findViewById(R.id.textWebApps)
        textProfile = findViewById(R.id.textProfile)

        // Initialize Timer UI Controls
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

        setupBottomNavigation()
        updateControlsUI()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navSleep).setOnClickListener { switchTab(0) }
        findViewById<LinearLayout>(R.id.navBlocking).setOnClickListener { switchTab(1) }
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener { switchTab(2) }
        findViewById<LinearLayout>(R.id.navWebApps).setOnClickListener { switchTab(3) }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener { switchTab(4) }
    }

    private fun switchTab(tabIndex: Int) {
        tabSleep.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        tabBlocking.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        tabHome.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        tabWebApps.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE
        tabProfile.visibility = if (tabIndex == 4) View.VISIBLE else View.GONE

        resetNavText(textSleep)
        resetNavText(textBlocking)
        resetNavText(textHome)
        resetNavText(textWebApps)
        resetNavText(textProfile)

        when (tabIndex) {
            0 -> highlightNavText(textSleep)
            1 -> highlightNavText(textBlocking)
            2 -> highlightNavText(textHome)
            3 -> highlightNavText(textWebApps)
            4 -> highlightNavText(textProfile)
        }
    }

    private fun resetNavText(tv: TextView) {
        tv.setTextColor(android.graphics.Color.parseColor("#71717A"))
        tv.setTypeface(null, Typeface.NORMAL)
    }

    private fun highlightNavText(tv: TextView) {
        tv.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        tv.setTypeface(null, Typeface.BOLD)
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
            Toast.makeText(this, "Targeted Feed Shield Activated", Toast.LENGTH_SHORT).show()
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
