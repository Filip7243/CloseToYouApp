package com.example.closetoyouapp

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_CONTACTS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission

class LoginActivity : AppCompatActivity() {

    //Layout
    private lateinit var edtPassword: EditText
    private var enteredPin = StringBuilder()

    // User preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var vibrator: Vibrator

    //Biometrics
    private lateinit var biometricManager: BiometricManager

    //Permissions
    private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        READ_CONTACTS,
        WRITE_CONTACTS,
        READ_EXTERNAL_STORAGE,
        WRITE_EXTERNAL_STORAGE,
        CAMERA,
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences("USER_PREFERENCES", MODE_PRIVATE)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        biometricManager = BiometricManager.from(this)

        if (isFirstTimeUser()) {
            showSetPinActivity()
        }

        if (checkBiometricSupport() && !isFirstTimeUser()) {
            showBiometricPrompt()
        }

        edtPassword = findViewById(R.id.edt_password)
        setupButtonClickListeners()
    }

    private fun isFirstTimeUser(): Boolean {
        return sharedPreferences.getBoolean("IS_FIRST_TIME", true)
    }

    private fun checkBiometricSupport(): Boolean {
        return when (biometricManager.canAuthenticate()) {
            BIOMETRIC_SUCCESS -> {
                Log.d("BIOMETRIC_HARDWARE_ATTEMPT", "Hardware detected!")
                true
            }

            else -> {
                Log.d("BIOMETRIC_HARDWARE_ATTEMPT", "Hardware not detected!")
                false
            }
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("Biometric Authentication")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Enter PIN").build()

        val biometricPrompt = BiometricPrompt(this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    showOnAuthenticationErrorPrompt()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    checkAndRequestPermissions()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showOnAuthenticationErrorPrompt() {
        Toast.makeText(this, "Enter PIN...", LENGTH_SHORT).show()
        Log.d("AUTH_ERR", "Authentication Error!")
    }

    private fun showSetPinActivity() {
        val intent = Intent(this, SetPinActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupButtonClickListeners() {
        val buttons = listOf<Button>(
            findViewById(R.id.btn_one),
            findViewById(R.id.btn_two),
            findViewById(R.id.btn_three),
            findViewById(R.id.btn_four),
            findViewById(R.id.btn_five),
            findViewById(R.id.btn_six),
            findViewById(R.id.btn_seven),
            findViewById(R.id.btn_eight),
            findViewById(R.id.btn_nine),
            findViewById(R.id.btn_zero),
        )

        for (button in buttons) {
            button.setOnClickListener { onDigitButtonClick(button.text.toString()); vibratePhone() }
        }

        val btnBack = findViewById<Button>(R.id.btn_back)
        btnBack.setOnClickListener { onBackButtonClick(); vibratePhone() }
    }

    private fun onBackButtonClick() {
        val length = enteredPin.length
        if (length > 0) {
            enteredPin.deleteCharAt(length - 1)
            edtPassword.setText(enteredPin.toString())
        }
    }

    private fun vibratePhone() {
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(vibrationEffect)
    }

    private fun onDigitButtonClick(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin.append(digit)
            edtPassword.setText(enteredPin.toString())
        }

        if (enteredPin.length == 4) {
            val savedPin = sharedPreferences.getString("USER_PIN", "1111")
            if (enteredPin.toString() == savedPin) {
                checkAndRequestPermissions()
            } else {
                restartPinEntry()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        for (permission in REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Request the permissions
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            showHomeActivity()
        }
    }

    private fun restartPinEntry() {
        vibratePhone()
        Toast.makeText(this, "Wrong PIN!", LENGTH_SHORT).show()
        Log.d("ENTER_PIN_FAILURE", "Wrong PIN")

        enteredPin.clear()
        edtPassword.text.clear()
    }

    private fun showHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allPermissionsGranted) {
                showHomeActivity()
            } else {
                checkAndRequestPermissions()
            }
        }
    }
}
