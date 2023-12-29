package com.example.closetoyou

import android.os.Build
import android.util.Log
import com.example.closetoyou.HomeActivity.Companion.userLatitude
import com.example.closetoyou.HomeActivity.Companion.userLongitude
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class LocationCallbackImpl(
    val checkPeopleInRadius: () -> Unit,
    val updateUserLocation: (lat: Double, lon: Double) -> Unit
) : LocationCallback() {

    override fun onLocationResult(locationResult: LocationResult) {
        super.onLocationResult(locationResult)

        var location = locationResult.lastLocation

        if (location != null) {
            userLatitude = location.latitude
            userLongitude = location.longitude

            updateUserLocation(location.latitude, location.longitude)

            checkPeopleInRadius()
        }
    }
}