package com.example.closetoyou

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build.MODEL
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.closetoyou.fragment.ContactFragment
import com.example.closetoyou.fragment.MapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
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



    private lateinit var progressBar: ProgressBar

    private var localContactsMap = mutableMapOf<String, String>()

    companion object {
        const val MAP_PERMISSION_CODE = 2
        const val PERMISSION_CODE = 1
        private const val IMAGE_PICK_CODE = 1001
        private const val CAMERA_REQUEST_CODE = 1002

        //        const val API_URL = "http://192.168.43.29:8080/api/v1/localization"
        const val API_URL = "http://192.168.88.87:8080/api/v1/localization"

        var userLatitude: Double = 0.0
        var userLongitude: Double = 0.0
        val client = OkHttpClient()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        // GPS setup
        locationRequest = LocationRequest.Builder(
            PRIORITY_BALANCED_POWER_ACCURACY,
            5000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val location = locationResult.lastLocation

                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
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
        switchToMapFragment()
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
        println("UPDATE USER LOCATION")
        val phoneNumber = "users"

        val currentLocalization = Localization("NAME", phoneNumber, latitude, longitude, true)

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
        val numbers = getContacts()

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

        Toast.makeText(applicationContext, "Loading data...", Toast.LENGTH_SHORT).show()
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

        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA).forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(it), PERMISSION_CODE)
            }
        }

        fragment = ContactFragment.newInstance(friendsLocalizations)

        Handler().postDelayed({
            val fragmentTransition: FragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransition.replace(R.id.frameLayout, fragment)
            fragmentTransition.addToBackStack(null)
            fragmentTransition.commit()
        }, 1500)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getContacts(): List<String> {
        val contextResolver = contentResolver
        val cursor = contextResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        val numberList = mutableListOf<String>()
        cursor?.use {
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                localContactsMap[number] = name
                numberList.add(number)
            }
        }

        return numberList
    }
}