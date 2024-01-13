package com.example.closetoyou

import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioButton1km: RadioButton
    private lateinit var radioButton3km: RadioButton
    private lateinit var radioButton5km: RadioButton
    private lateinit var radioButton10km: RadioButton
    private lateinit var radioButtonNotificationsOff: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        radioGroup = findViewById(R.id.radioGroup)
        radioButton1km = findViewById(R.id.radioButton1km)
        radioButton3km = findViewById(R.id.radioButton3km)
        radioButton5km = findViewById(R.id.radioButton5km)
        radioButton10km = findViewById(R.id.radioButton10km)
        radioButtonNotificationsOff = findViewById(R.id.radioButtonNotificationsOff)

        loadSettings()

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radius = when (checkedId) {
                R.id.radioButton1km -> 1
                R.id.radioButton3km -> 3
                R.id.radioButton5km -> 5
                R.id.radioButton10km -> 10
                else -> 0 // Notifications off
            }

            saveSettings(radius)
        }
    }

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        when (sharedPreferences.getInt("Radius", 1)) { // Default to 1km
            0 -> radioButtonNotificationsOff.isChecked = true
            1 -> radioButton1km.isChecked = true
            3 -> radioButton3km.isChecked = true
            5 -> radioButton5km.isChecked = true
            10 -> radioButton10km.isChecked = true
        }
    }

    private fun saveSettings(radius: Int) {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        sharedPreferences.edit {
            putInt("Radius", radius)
        }
    }
}