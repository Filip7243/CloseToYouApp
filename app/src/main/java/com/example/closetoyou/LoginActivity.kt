package com.example.closetoyou

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat.getMainExecutor
import com.example.closetoyou.R.string.pin_preferences
import com.example.closetoyou.R.string.user_pin
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var edtPassword: EditText
    private var enteredPin = StringBuilder()
    private lateinit var vibrator: Vibrator

    private lateinit var biometricManager: BiometricManager
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        sharedPreferences = getSharedPreferences(getString(pin_preferences), MODE_PRIVATE)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        if (isFirstTimeUser()) {
            showSetPinActivity()
        }

        biometricManager = BiometricManager.from(this)

        if (checkBiometricSupport()) {
            println("UDALO SIE!")

            showBiometricPrompt()
        } else {
            println("NIE UDALO SIE!")
        }

        edtPassword = findViewById(R.id.edt_password)

        setupButtonClickListeners()
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
        Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
        Log.d("ENTER_PIN_SUCCESS", "Correct PIN")

        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    private fun onBackButtonClick() {
        val length = enteredPin.length
        if (length > 0) {
            enteredPin.deleteCharAt(length - 1)
            edtPassword.setText(enteredPin.toString())
        }
    }

    private fun isFirstTimeUser(): Boolean {
        return sharedPreferences.getBoolean(getString(R.string.first_time_user), true)
    }

    private fun showSetPinActivity() {
        val intent = Intent(this, SetPinActivity::class.java)
        startActivity(intent)
    }

    private fun vibratePhone() {
        // Create a vibration effect of 100 milliseconds
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

        // Vibrate with the effect
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
            BIOMETRIC_SUCCESS ->
                true

            else -> false
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
                    showHome()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onAuthenticationFailed()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun onAuthenticationError() {
        Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show()
        Log.d("AUTH_ERR", "Authentication Error!")
    }

    private fun onAuthenticationFailed() {
        Log.d("AUTH_FAILED", "Authentication failed")
    }
}
