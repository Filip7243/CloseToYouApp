package com.example.closetoyouapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.closetoyouapp.R.id.edt_password
import com.example.closetoyouapp.R.id.txt_enter
import com.example.closetoyouapp.R.layout.activity_set_pin

class SetPinActivity : AppCompatActivity() {

    //Layout
    private lateinit var edtPassword: EditText
    private lateinit var txtEnter: TextView
    private var enteredPin = StringBuilder()

    // User preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var vibrator: Vibrator

    private var firstEnteredPin: String = ""
    private var secondEnteredPin: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activity_set_pin)

        sharedPreferences = getSharedPreferences("USER_PREFERENCES", MODE_PRIVATE)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        edtPassword = findViewById(edt_password)
        txtEnter = findViewById(txt_enter)

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

        val btnOk = findViewById<Button>(R.id.btn_ok)
        btnOk.setOnClickListener { onOkButtonClick(); vibratePhone() }
    }

    private fun onDigitButtonClick(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin.append(digit)
            edtPassword.setText(enteredPin.toString())
        }
    }

    private fun vibratePhone() {
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(vibrationEffect)
    }

    private fun onBackButtonClick() {
        val length = enteredPin.length
        if (length > 0) {
            enteredPin.deleteCharAt(length - 1)
            edtPassword.setText(enteredPin.toString())
        }
    }

    private fun onOkButtonClick() {
        if (enteredPin.length == 4) {
            if (firstEnteredPin.isEmpty()) {
                setFirstEnteredPin()
            } else {
                secondEnteredPin = enteredPin.toString()

                if (firstEnteredPin == secondEnteredPin) {
                    setPinAndFinish()
                } else {
                    restartPinEntry()
                }
            }
        } else {
            Toast.makeText(this, "Enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setFirstEnteredPin() {
        firstEnteredPin = enteredPin.toString()
        enteredPin.clear()
        edtPassword.text.clear()
        txtEnter.text = "Confirm PIN"
    }

    private fun setPinAndFinish() {
        sharedPreferences.edit().putString("USER_PIN", firstEnteredPin).apply()
        sharedPreferences.edit().putBoolean("IS_FIRST_TIME", false).apply()

        Toast.makeText(this, "Pin set!", Toast.LENGTH_SHORT).show()
        Log.d("SET_PIN_SUCCESS", "PIN set successfully.")

        showSetPhoneNumberActivity()

        finish()
    }

    private fun restartPinEntry() {
        Toast.makeText(this, "Pins are not equal!", Toast.LENGTH_SHORT).show()
        Log.d("SET_PIN_FAILURE", "Pins are not equal.")

        firstEnteredPin = ""
        enteredPin.clear()
        edtPassword.text.clear()
        txtEnter.text = "SET PIN AGAIN!"
    }

    private fun showSetPhoneNumberActivity() {
        val intent = Intent(this, SetPhoneNumberActivity::class.java)
        startActivity(intent)
    }
}