package com.example.closetoyouapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
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

        var radius = 0;

        loadSettings()

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButton1km -> {
                    radius = 1 * 1000
                    radioGroup.check(radioButton1km.id)
                }
                R.id.radioButton3km -> {
                    radius = 3 * 1000
                    radioGroup.check(radioButton3km.id)
                }
                R.id.radioButton5km -> {
                    radius = 5 * 1000
                    radioGroup.check(radioButton5km.id)
                }
                R.id.radioButton10km -> {
                    radius = 10 * 1000
                    radioGroup.check(radioButton10km.id)
                }
                else -> {
                    radius = 0 // Notifications off
                    radioGroup.check(radioButtonNotificationsOff.id)
                }
            }
            saveSettings(radius)
        }
    }

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("USER_PREFERENCES", MODE_PRIVATE)

        when (sharedPreferences.getInt("RADIUS", 1000)) { // Default to 1km
            0 -> radioButtonNotificationsOff.isChecked = true
            1 * 1000 -> radioButton1km.isChecked = true
            3 * 1000 -> radioButton3km.isChecked = true
            5 * 1000 -> radioButton5km.isChecked = true
            10 * 1000 -> radioButton10km.isChecked = true
        }
    }

    private fun saveSettings(radius: Int) {
        val sharedPreferences = getSharedPreferences("USER_PREFERENCES", MODE_PRIVATE)

        sharedPreferences.edit {
            putInt("RADIUS", radius)
        }
    }
}