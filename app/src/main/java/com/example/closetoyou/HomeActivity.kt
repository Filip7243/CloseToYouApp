package com.example.closetoyou

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build.MODEL
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.closetoyou.fragment.ContactFragment
import com.example.closetoyou.fragment.MapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.osmdroid.util.GeoPoint
import java.io.IOException

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    private var friendsLocalizations: ArrayList<Localization> = arrayListOf()


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val client = OkHttpClient()

    private lateinit var progressBar: ProgressBar

    companion object {
        const val MAP_PERMISSION_CODE = 2

        //        const val API_URL = "http://192.168.43.29:8080/api/v1/localization"
        const val API_URL = "http://192.168.88.87:8080/api/v1/localization"

        var userLatitude: Double = 0.0
        var userLongitude: Double = 0.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        // GPS setup
        locationRequest = LocationRequest.Builder(
            PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)

                println("ESSUNIA WARIACE!!!!")
            }

            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val location = locationResult.lastLocation

                Toast.makeText(applicationContext, "CALLBACK!", Toast.LENGTH_SHORT).show()
                println("WIIITAM!")

                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude

                    updateUserLocalization(location.latitude, location.longitude)

                    checkPeopleInRadius()
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapBtn = findViewById<Button>(R.id.map_btn)
        val contactBtn = findViewById<Button>(R.id.contact_btn)
        progressBar = findViewById(R.id.progressBar)

        switchToMapFragment()

        mapBtn.setOnClickListener {
            switchToMapFragment()
        }

        contactBtn.setOnClickListener {
            Handler().postDelayed({
                switchToContactFragment()
            }, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdate()
        progressBar.visibility = View.VISIBLE
        Handler().postDelayed({
            switchToMapFragment()
        }, 1500)
        progressBar.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        println("WITAM W ON STOP!")
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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("PUT_REQUEST_FAILURE", "Failure ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val r: String? = response.body?.string()

                    Log.d("PUT_RESPONSE", "Response: $r")
                }
            }
        })
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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("POST_REQUEST_FAILURE", "Failure ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseStr: String? = response.body?.string()
                    val typeToken = object : TypeToken<List<Localization>>() {}.type
                    val friendsLocations = gson.fromJson<List<Localization>>(responseStr, typeToken)

                    friendsLocalizations = friendsLocations as ArrayList<Localization>

                    println("localizations = $friendsLocations")
                }
            }
        })
    }

    private fun addGeoPoint(localization: Localization) {
        val geoPoint = GeoPoint(localization.latitude, localization.longitude)

//        val marker = Marker(mapView)
//        marker.position = geoPoint
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//        marker.icon = ResourcesCompat.getDrawable(resources, loc_pin1, null)
//        marker.title = "${localization.phoneNumber} is here!"
//
//        mapView.overlays.add(marker)
    }

    private fun switchToMapFragment() {


        println("SWITCHING! TO MAP!")

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener {
            userLatitude = it.latitude
            userLongitude = it.longitude

            updateUserLocalization(userLatitude, userLongitude)
            checkPeopleInRadius()
        }

        Handler().postDelayed({
            var fragment: Fragment?
            fragment = MapFragment.newInstance(userLatitude, userLongitude, friendsLocalizations)
            val fragmentTransition: FragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransition.replace(R.id.frameLayout, fragment)
            fragmentTransition.addToBackStack(null)
            fragmentTransition.commit()
        }, 1500)
    }

    private fun switchToContactFragment() {
        var fragment: Fragment?

        fragment = ContactFragment.newInstance("DUPA", "KUPA")

        val fragmentTransition: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(R.id.frameLayout, fragment)
        fragmentTransition.addToBackStack(null)
        fragmentTransition.commit()
    }
}