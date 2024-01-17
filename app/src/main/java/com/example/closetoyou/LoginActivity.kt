package com.example.closetoyou

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_CONTACTS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.getMainExecutor
import com.example.closetoyou.R.string.pin_preferences
import com.example.closetoyou.R.string.user_pin

class LoginActivity : AppCompatActivity() {

    private lateinit var edtPassword: EditText
    private var enteredPin = StringBuilder()
    private lateinit var vibrator: Vibrator

    private lateinit var biometricManager: BiometricManager
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
        val REQUIRED_PERMISSIONS = arrayOf(
            READ_CONTACTS,
            WRITE_CONTACTS,
            READ_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE,
            CAMERA,
            ACCESS_COARSE_LOCATION,
            ACCESS_BACKGROUND_LOCATION
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        sharedPreferences = getSharedPreferences(getString(pin_preferences), MODE_PRIVATE)
        val a = sharedPreferences.getString(getString(user_pin), "")
        val b = sharedPreferences.getString("UserPhoneNumber", "")  //TODO: delte test code
        println("PIN = $a")
        println("P NUMBER = $b")

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        if (isFirstTimeUser() || !hasPhoneNumber()) {
            showSetPinActivity()

            finish()
        }

        biometricManager = BiometricManager.from(this)

        if (checkBiometricSupport() && (!isFirstTimeUser() || hasPhoneNumber())) {
            showBiometricPrompt()
        }

        edtPassword = findViewById(R.id.edt_password)

        setupButtonClickListeners()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                for (i in grantResults.indices) {
                    if (grantResults[i] != PERMISSION_GRANTED) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", packageName, null)
                            data = uri
                        }
                        startActivity(intent)
                        Toast.makeText(this, "All permissions are required!", Toast.LENGTH_SHORT).show()
                        println("PERMISSION IS NOT GRATED")
                        break
                    }
                }
            }
        }
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

    private fun onDigitButtonClick(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin.append(digit)
            edtPassword.setText(enteredPin.toString())
        }

        if (enteredPin.length == 4) {
            val savedPin = sharedPreferences.getString(getString(user_pin), "")
            if (enteredPin.toString() == savedPin) {
                showHome()
            } else {
                restartPinEntry()
            }
        }
    }

    private fun showHome() {
        val hasPermissions = hasPermissions()
        println("hasPermission = $hasPermissions")

        if (!hasPermissions) {
            requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        } else {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)

            Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
            Log.d("ENTER_PIN_SUCCESS", "Correct PIN")

            finish()
        }
    }

    private fun isFirstTimeUser(): Boolean {
        println("FIRST TIME = ${sharedPreferences.getBoolean(getString(R.string.first_time_user), true)}")
        return sharedPreferences.getBoolean(getString(R.string.first_time_user), true)
    }

    private fun hasPhoneNumber(): Boolean {
        val number: String? = sharedPreferences.getString("UserPhoneNumber", "")

        if (number != null) {
            return number.isNotBlank()
        }

        return false
    }
    private fun showSetPinActivity() {
        val intent = Intent(this, SetPinActivity::class.java)
        startActivity(intent)
    }

    private fun vibratePhone() {
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(vibrationEffect)
    }

    private fun restartPinEntry() {
        vibratePhone()
        Toast.makeText(this, "Wrong PIN!", Toast.LENGTH_SHORT).show()
        Log.d("ENTER_PIN_FAILURE", "Wrong PIN")

        enteredPin.clear()
        edtPassword.text.clear()
    }

    private fun checkBiometricSupport(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
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
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Enter PIN")
            .build()

        val biometricPrompt = BiometricPrompt(this,
            getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onAuthenticationError()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    println("WITAM Z SUCCEDE")
                    showHome()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onAuthFailed()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun onAuthenticationError() {
        Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()
        Log.d("AUTH_ERR", "Authentication Error!")
    }

    private fun onAuthFailed() {
        Log.d("AUTH_FAILED", "Authentication failed")
    }

    private fun hasPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                println("ESSUNIA YUTA FIUTA $permission")

                return false
            }
        }
        return true
    }

    private fun onBackButtonClick() {
        val length = enteredPin.length
        if (length > 0) {
            enteredPin.deleteCharAt(length - 1)
            edtPassword.setText(enteredPin.toString())
        }
    }
}
