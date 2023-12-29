package com.example.closetoyou

import com.example.closetoyou.HomeActivity.Companion.userLatitude
import com.example.closetoyou.HomeActivity.Companion.userLongitude
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

class LocationCallbackImpl(
    val checkPeopleInRadius: () -> Unit,
    val updateUserLocation: (lat: Double, lon: Double) -> Unit
) : LocationCallback() {

    override fun onLocationResult(locationResult: LocationResult) {
        super.onLocationResult(locationResult)

        val location = locationResult.lastLocation

        if (location != null) {
            userLatitude = location.latitude
            userLongitude = location.longitude

            updateUserLocation(location.latitude, location.longitude)

            checkPeopleInRadius()
        }
    }
}