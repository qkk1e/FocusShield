package com.focusshield.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.focusshield.app.service.QuotaManager
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val switchMasterToggle = findViewById<MaterialSwitch>(R.id.switchMasterToggle)
        val switchHardBlock = findViewById<MaterialSwitch>(R.id.switchHardBlock)
        val sliderDailyLimit = findViewById<Slider>(R.id.sliderDailyLimit)
        val textDailyLimit = findViewById<TextView>(R.id.textDailyLimit)
        val btnPermissions = findViewById<Button>(R.id.btnPermissions)

        switchMasterToggle.setOnCheckedChangeListener { _, isChecked ->
            QuotaManager.isBlockingEnabled = isChecked
        }

        switchHardBlock.setOnCheckedChangeListener { _, isChecked ->
            QuotaManager.isHardBlockActive = isChecked
        }

        sliderDailyLimit.addOnChangeListener { _, value, _ ->
            val mins = value.toInt()
            textDailyLimit.text = "Daily Limit: $mins minutes"
            QuotaManager.setDailyLimitMinutes(mins)
        }

        btnPermissions.setOnClickListener {
            requestRequiredPermissions()
        }
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        val accessibilityIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(accessibilityIntent)
    }
}
