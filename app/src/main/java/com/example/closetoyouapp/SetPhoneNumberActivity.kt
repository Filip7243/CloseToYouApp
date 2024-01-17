package com.example.closetoyouapp

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class SetPhoneNumberActivity : AppCompatActivity() {

    //Layout
    private lateinit var edtPhoneNumber: EditText
    private lateinit var txtEnter: TextView
    private var enteredNumber = StringBuilder()

    //User preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_set_phone_number)

        sharedPreferences = getSharedPreferences("USER_PREFERENCES", MODE_PRIVATE)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        edtPhoneNumber = findViewById(R.id.edt_password)
        txtEnter = findViewById(R.id.txt_enter)

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
        if (enteredNumber.length < 9) {
            enteredNumber.append(digit)
            edtPhoneNumber.setText(enteredNumber.toString())
        }
    }

    private fun vibratePhone() {
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(vibrationEffect)
    }

    private fun onBackButtonClick() {
        val length = enteredNumber.length
        if (length > 0) {
            enteredNumber.deleteCharAt(length - 1)
            edtPhoneNumber.setText(enteredNumber.toString())
        }
    }

    private fun onOkButtonClick() {
        edtPhoneNumber = findViewById(R.id.edt_password)

        if (enteredNumber.length == 9) {
            setPhoneNumberAndFinish()
        } else {
            Toast.makeText(this, "Enter a 9-digit phone number!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setPhoneNumberAndFinish() {
        sharedPreferences.edit().putString("USER_PHONE_NUMBER", "+48$enteredNumber").apply()

        Toast.makeText(this, "Phone Number set!", Toast.LENGTH_SHORT).show()
        Log.d("SET_PHONE_NUMBER_SUCCESS", "Phone Number set successfully.")

        showLoginActivity()

        finish()
    }

    private fun showLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}