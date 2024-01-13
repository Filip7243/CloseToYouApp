package com.example.closetoyou

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetPhoneNumberActivity : AppCompatActivity() {

    private lateinit var edtPhoneNumber: EditText
    private lateinit var textView: TextView
    private lateinit var okButton: Button
    private var enteredNumber = StringBuilder()
    private lateinit var functionalLayout: LinearLayout

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login)

        sharedPreferences = getSharedPreferences(getString(R.string.pin_preferences), MODE_PRIVATE)

        val textView = findViewById<TextView>(R.id.txt_enter)
        textView.text = "Set Phone Number"

        edtPhoneNumber = findViewById(R.id.edt_password)
        edtPhoneNumber.inputType = InputType.TYPE_CLASS_PHONE

        functionalLayout = findViewById(R.id.function_layout)

        okButton = Button(this)
        okButton.text = "Confirm"
        okButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        okButton.setOnClickListener { onOkButtonClick(); }

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
            button.setOnClickListener { onDigitButtonClick(button.text.toString()); }
        }

        val btnBack = findViewById<Button>(R.id.btn_back)
        btnBack.setOnClickListener { onBackButtonClick(); }
    }

    private fun onDigitButtonClick(digit: String) {
        if (enteredNumber.length < 9) {
            enteredNumber.append(digit)
            edtPhoneNumber.setText(enteredNumber.toString())
        }
    }

    private fun onBackButtonClick() {
        val length = enteredNumber.length
        if (length > 0) {
            enteredNumber.deleteCharAt(length - 1)
            edtPhoneNumber.setText(enteredNumber.toString())
        }
    }

    private fun onOkButtonClick() {
        textView = findViewById(R.id.txt_enter)
        edtPhoneNumber = findViewById(R.id.edt_password)

        if (enteredNumber.length == 9) {
            setPinAndFinish()
        } else {
            Toast.makeText(this, "Enter a 9-digit phone number!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setPinAndFinish() {
        sharedPreferences.edit().putString("UserPhoneNumber", "+48$enteredNumber").apply()

        Toast.makeText(this, "Phone Number set!", Toast.LENGTH_SHORT).show()
        Log.d("SET_PIN_SUCCESS", "Phone Number set successfully.")

        showLoginActivity()

        finish()
    }

    private fun showLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}