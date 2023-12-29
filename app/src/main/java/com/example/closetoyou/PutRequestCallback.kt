package com.example.closetoyou

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class PutRequestCallback : Callback {

    override fun onFailure(call: Call, e: IOException) {
        Log.d("PUT_REQUEST_ATTEMPT_FAILURE", "Failure ${e.message}")
    }

    override fun onResponse(call: Call, response: Response) {
        response.use {
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val r: String? = response.body?.string()

            Log.d("PUT_RESPONSE", "Response: $r")
        }
    }
}