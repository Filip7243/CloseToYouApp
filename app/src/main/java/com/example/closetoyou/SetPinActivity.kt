package com.example.closetoyou

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import com.example.closetoyou.R.id.edt_password
import com.example.closetoyou.R.id.txt_enter
import com.example.closetoyou.R.string.first_time_user
import com.example.closetoyou.R.string.pin_preferences
import com.example.closetoyou.R.string.user_pin

class SetPinActivity : AppCompatActivity() {

    private lateinit var edtPassword: EditText
    private lateinit var textView: TextView
    private lateinit var okButton: Button
    private var enteredPin = StringBuilder()
    private lateinit var functionalLayout: LinearLayout
    private lateinit var vibrator: Vibrator
    private var firstEnteredPin = ""
    private var secondEnteredPin = ""

    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login)

        sharedPreferences = getSharedPreferences(getString(pin_preferences), MODE_PRIVATE)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val textView = findViewById<TextView>(txt_enter)
        textView.text = "Set PIN"

        edtPassword = findViewById(edt_password)
        functionalLayout = findViewById(R.id.function_layout)

        okButton = Button(this)
        okButton.text = "OK!"
        okButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        okButton.setOnClickListener { onOkButtonClick(); vibratePhone() }

        functionalLayout.addView(okButton)

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
    }

    private fun onBackButtonClick() {
        val length = enteredPin.length
        if (length > 0) {
            enteredPin.deleteCharAt(length - 1)
            edtPassword.setText(enteredPin.toString())
        }
    }

    private fun onOkButtonClick() {
        textView = findViewById(txt_enter)
        edtPassword = findViewById(edt_password)

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
            Toast.makeText(this, "Enter a 4-digit PIN", LENGTH_SHORT).show()
        }
    }

    private fun restartPinEntry() {
        Toast.makeText(this, "Pins are not equal!", LENGTH_SHORT).show()
        Log.d("SET_PIN_FAILURE", "Pins are not equal.")

        firstEnteredPin = ""
        enteredPin.clear()
        edtPassword.text.clear()
        textView.text = "SET PIN AGAIN!"
    }

    private fun setPinAndFinish() {
        sharedPreferences.edit().putString(getString(user_pin), firstEnteredPin).apply()
        sharedPreferences.edit().putBoolean(getString(first_time_user), false).apply()

        Toast.makeText(this, "Pin set!", LENGTH_SHORT).show()
        Log.d("SET_PIN_SUCCESS", "PIN set successfully.")

        finish()
    }

    private fun setFirstEnteredPin() {
        firstEnteredPin = enteredPin.toString()
        enteredPin.clear()
        edtPassword.text.clear()
        textView.text = "Confirm PIN"
    }

    private fun vibratePhone() {
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(vibrationEffect)
    }
}