package com.example.closetoyou

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.MODEL
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.closetoyou.ActiveFragment.CONTACT
import com.example.closetoyou.ActiveFragment.MAP
import com.example.closetoyou.fragment.ContactFragment
import com.example.closetoyou.fragment.MapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class HomeActivity : AppCompatActivity() {

    // Default fragment (on startup)
    private var activeFragment: ActiveFragment = MAP

    private var friendsLocalizations: ArrayList<Localization> = arrayListOf()

    // Localization
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var localContactsMap = mutableMapOf<String, String>()
    private var contactPhotos: MutableMap<String, String> = mutableMapOf()

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

        val mapBtn = findViewById<Button>(R.id.map_btn)
        mapBtn.isSelected = true

        val contactBtn = findViewById<Button>(R.id.contact_btn)
        contactBtn.isSelected = false

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val radiusSettings = sharedPreferences.getInt("Radius", 1); // default - 1 KM

        mapBtn.setOnClickListener {
            if (!mapBtn.isSelected) {
                mapBtn.isSelected = true
                contactBtn.isSelected = false

                switchToMapFragment()
            }
        }

        contactBtn.setOnClickListener {
            if (!contactBtn.isSelected) {
                mapBtn.isSelected = false
                contactBtn.isSelected = true

                switchToContactFragment()
            }
            //TODO: Handler(Looper.getMainLooper()).postDelayed({}, 1000)
        }

        // GPS setup
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000
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

        loadAvatars()
    }

    override fun onResume() {
        super.onResume()

        when (activeFragment) {
            MAP -> switchToMapFragment()
            CONTACT -> switchToContactFragment()
        }
        startLocationUpdate()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)

                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    fun updateContactPhotosMap(newMap: Map<String, String>) {
        contactPhotos.putAll(newMap)
    }

    private fun loadAvatars() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = MyApp.getDatabase(this@HomeActivity)
            val photoMap = database.contactPhotoDao().getAllPhotos()
                .associateBy({ it.phoneNumber }, { it.photoUri })

            contactPhotos.putAll(photoMap);
        }
    }

    private fun startLocationUpdate() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.d("START_LOCATION_EXC","Security Exception: ${e.message}")
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

    private fun switchToMapFragment() {
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
            if (it != null) {
                userLatitude = it.latitude
                userLongitude = it.longitude

                updateUserLocalization(userLatitude, userLongitude)
                checkPeopleInRadius()
            } else {
                Toast.makeText(applicationContext, "Localization unaveliable....", Toast.LENGTH_SHORT).show()
            }

        }

        activeFragment = MAP

        Toast.makeText(applicationContext, "Loading data...", Toast.LENGTH_SHORT).show()
        Handler().postDelayed({
            var fragment: Fragment?
            fragment = MapFragment.newInstance(userLatitude, userLongitude, friendsLocalizations)
            fragment.apply {
                arguments = Bundle().apply {
                    putSerializable("contactPhotos", HashMap(contactPhotos))
                }
            }
            val fragmentTransition: FragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransition.replace(R.id.frameLayout, fragment)
            fragmentTransition.addToBackStack(null)
            fragmentTransition.commit()
        }, 1500)
    }

    private fun switchToContactFragment() {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA).forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(it), PERMISSION_CODE)
            }
        }

        activeFragment = CONTACT

        // todo: switch it from hanlder
        Handler().postDelayed({
            var fragment: Fragment?
            fragment = ContactFragment.newInstance(friendsLocalizations)
            val fragmentTransition: FragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransition.replace(R.id.frameLayout, fragment)
            fragmentTransition.addToBackStack(null)
            fragmentTransition.commit()
        }, 1500)
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
            val nameIndex =
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
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

private enum class ActiveFragment {
    MAP, CONTACT
}