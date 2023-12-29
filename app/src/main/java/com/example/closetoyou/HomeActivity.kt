package com.example.closetoyou

import android.location.LocationRequest.QUALITY_BALANCED_POWER_ACCURACY
import android.os.Build.MODEL
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.closetoyou.R.drawable.loc_pin1
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    // map
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val client = OkHttpClient()

    companion object {
        const val MAP_PERMISSION_CODE = 2
        const val API_URL = "http://192.168.43.29:8080/api/v1/localization"
        //        const val API_URL = "http://192.168.88.87:8080/api/v1/localization"

        var userLatitude: Double = 0.0
        var userLongitude: Double = 0.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance()
            .load(
                applicationContext,
                getDefaultSharedPreferences(applicationContext)
            )

        setContentView(R.layout.activity_home)

        // GPS setup
        locationRequest = LocationRequest.Builder(
            QUALITY_BALANCED_POWER_ACCURACY,
            15000
        ).build()

        locationCallback = LocationCallbackImpl(::checkPeopleInRadius, ::updateUserLocalization)

        val buttonMap = findViewById<Button>(R.id.buttonMap)
        val buttonContacts = findViewById<Button>(R.id.buttonContacts)
        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)

        buttonMap.setOnClickListener {
            // Show map view
            showMapView(contentFrame)
        }

        buttonContacts.setOnClickListener {
            // Show contacts view
            showContactsView(contentFrame)
        }

        // Default view
        showMapView(contentFrame)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        startLocationUpdate()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun startLocationUpdate() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            println("Security Exception: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateUserLocalization(latitude: Double, longitude: Double) {
        var phoneNumber = "ten co da uzytkonwik"

        val currentLocalization = Localization(phoneNumber, latitude, longitude, true)

        val gson = Gson()
        val json = gson.toJson(currentLocalization)
        var requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .header("User-Agent", "CloseToYou app, $MODEL")
            .header("Accept", "application/json")
            .put(requestBody)
            .build()

        client.newCall(request).enqueue(PutRequestCallback())
    }

    private fun checkPeopleInRadius() {
        // TODO: get numbers from db
        val numbers = listOf(
            "+48 662-291-021",
            "+48 661-291-021",
            "+48 664-291-021",
            "+48 665-291-021",
            "+48 666-291-021",
            "+48 667-291-021",
            "+48 668-291-021",
            "+48 669-291-021",
            "+48 764-291-021",
            "+48 765-291-021",
            "+48 962-291-021",
            ""
        )

        val gson = Gson()
        val json = gson.toJson(numbers);
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .header("User-Agent", "CloseToYou app, $MODEL")
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(PostRequestCallback(::addGeoPoint, gson, this))
    }

    private fun addGeoPoint(localization: Localization) {
        val geoPoint = GeoPoint(localization.latitude, localization.longitude)

        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = resources.getDrawable(loc_pin1);
        marker.title = "${localization.phoneNumber} is here!";

        mapView.overlays.add(marker)
    }

    private fun showMapView(frameLayout: FrameLayout) {
        frameLayout.removeAllViews()
    }

    private fun showContactsView(frameLayout: FrameLayout) {
        frameLayout.removeAllViews()
    }
}