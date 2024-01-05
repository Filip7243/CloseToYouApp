package com.example.closetoyou

import android.os.Build.MODEL
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.closetoyou.R.drawable.loc_pin1
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory.DEFAULT_TILE_SOURCE
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    private lateinit var contentFrame: FrameLayout

    // map
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val client = OkHttpClient()

    companion object {
        const val MAP_PERMISSION_CODE = 2

        //        const val API_URL = "http://192.168.43.29:8080/api/v1/localization"
        const val API_URL = "http://192.168.88.87:8080/api/v1/localization"

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
            PRIORITY_BALANCED_POWER_ACCURACY,
            15000
        ).build()

        locationCallback = LocationCallbackImpl(::checkPeopleInRadius, ::updateUserLocalization)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        contentFrame = findViewById(R.id.frame)
        val buttonMap = findViewById<Button>(R.id.buttonMap)
        val buttonContacts = findViewById<Button>(R.id.buttonContacts)

        initializeMapView()

        buttonMap.setOnClickListener {
            // Show map view
            showMapView()
        }

        buttonContacts.setOnClickListener {
            // Show contacts view
            showContactsView()
        }

        // Default view
        showMapView()
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

    private fun initializeMapView() {  //todo: mapa znika :/
        mapView = MapView(this)

        mapView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        mapView.setTileSource(DEFAULT_TILE_SOURCE)
        mapView.controller.setZoom(18.0)
        mapView.setMultiTouchControls(true)
        mapView.x = userLatitude.toFloat()
        mapView.y = userLongitude.toFloat()
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
        println("UPDATE USER LOCATION")
        val phoneNumber = "users"

        val currentLocalization = Localization(phoneNumber, latitude, longitude, true)

        val gson = Gson()
        val json = gson.toJson(currentLocalization)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .header("User-Agent", "CloseToYou app, $MODEL")
            .header("Accept", "application/json")
            .put(requestBody)
            .build()

        client.newCall(request).enqueue(PutRequestCallback())
    }

    private fun checkPeopleInRadius() {
        println("CHECK PEOPLE IN RADIUS!")
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
        val json = gson.toJson(numbers)
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
        marker.icon = ResourcesCompat.getDrawable(resources, loc_pin1, null)
        marker.title = "${localization.phoneNumber} is here!"

        mapView.overlays.add(marker)
    }

    private fun showMapView() {
        contentFrame.removeAllViews()
        mapView.onResume()
        contentFrame.addView(mapView)
    }

    private fun showContactsView() {
        contentFrame.removeAllViews()
    }
}