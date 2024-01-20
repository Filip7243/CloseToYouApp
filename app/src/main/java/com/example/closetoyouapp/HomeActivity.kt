package com.example.closetoyouapp

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_CONTACTS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.closetoyouapp.ActiveFragment.CONTACT
import com.example.closetoyouapp.ActiveFragment.MAP
import com.example.closetoyouapp.fragment.ContactFragment
import com.example.closetoyouapp.fragment.MapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
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
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class HomeActivity : AppCompatActivity() {

    // Default fragment
    private var activeFragment: ActiveFragment = MAP

    // Friends Info
    private var friendsLocalization: ArrayList<Localization> = arrayListOf()
    companion object {
        val localContactsMap = mutableMapOf<String, String>()
    }

    private val friendsDistance: MutableMap<Localization, Double> = mutableMapOf()
    private val contactPhotos: MutableMap<String, String> = mutableMapOf()
    private val filteredLocations = mutableListOf<Localization>()

    // User preferences
    private lateinit var sharedPreferences: SharedPreferences

    // Phone Localization (google API)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // Permissions
    private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        READ_CONTACTS,
        WRITE_CONTACTS,
        READ_EXTERNAL_STORAGE,
        WRITE_EXTERNAL_STORAGE,
        CAMERA,
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION
    )

    // User Localization info
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var radius: Double = 0.0

    // API
    private val API_URL = "http://192.168.88.87:8080/api/v1/localization"

    //    private val API_URL = "http://192.168.43.29:8080/api/v1/localization"
    private val client = OkHttpClient()


    // Notifications
    private val CHANNEL_ID = "CLOSE_TO_YOU_CHANNEL"
    private var notificationId = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timeInterval: Long = 60000 // 5 min
    private var filteredListSize = 0
    private val runnable = object : Runnable {
        override fun run() {
            val currentFilteredListSize = filteredLocations.size

            if (filteredLocations.isNotEmpty() && currentFilteredListSize != filteredListSize) {
                showNotification(filteredLocations)

                filteredListSize = currentFilteredListSize
            }
            handler.postDelayed(this, timeInterval)
        }
    }

    init {
        handler.postDelayed(runnable, timeInterval)
    }

    var firstTimeLoaded = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        val mapBtn = findViewById<Button>(R.id.map_btn)
        mapBtn.isSelected = true

        val contactBtn = findViewById<Button>(R.id.contact_btn)
        contactBtn.isSelected = false

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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
        }

        sharedPreferences = getSharedPreferences("USER_PREFERENCES", MODE_PRIVATE)

        locationRequest = LocationRequest()
        locationRequest.interval = 3000
        locationRequest.fastestInterval = 1200
        locationRequest.priority = PRIORITY_HIGH_ACCURACY


        Toast.makeText(applicationContext, "Loading data...", Toast.LENGTH_LONG).show()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val location = locationResult.lastLocation // todo; intenret policy

                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude

                    sendPutRequest(userLatitude, userLongitude)
                    sendPostRequest()

                    if (firstTimeLoaded == 0) {
                        firstTimeLoaded = 1;
                    }
                }
            }
        }

        updateGPS()
        loadAvatars()
    }

    fun updateContactPhotosMap(newMap: Map<String, String>) {
        contactPhotos.putAll(newMap)
    }

    private fun startLocationUpdates() {
        if (checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            updateGPS()
        }

    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateGPS() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    updateUserLocalization(it)
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Localization unavaliable....",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }
        }
    }

    private fun updateUserLocalization(location: Location) {
        userLatitude = location.latitude
        userLongitude = location.longitude

        println("UPDATED $userLatitude, $userLongitude")
    }

    private fun loadAvatars() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = MyApp.getDatabase(this@HomeActivity)
            val photoMap = database.contactPhotoDao().getAllPhotos()
                .associateBy({ it.phoneNumber }, { it.photoUri })
            println("photo map = $photoMap")
            contactPhotos.putAll(photoMap)
        }
    }

    private fun sendPutRequest(latitude: Double, longitude: Double) {
        println("UPDATE USER LOCATION")
        val phoneNumber = sharedPreferences.getString("USER_PHONE_NUMBER", "")

        val currentIsoDateTime = Instant.now().toString()
        val currentLocalization =
            Localization("NAME", phoneNumber, latitude, longitude, true, currentIsoDateTime)

        val gson = Gson()
        val json = gson.toJson(currentLocalization)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .header("User-Agent", "CloseToYou app, ${Build.MODEL}")
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

    private fun sendPostRequest() {
        val contacts = getContacts()

        val gson = Gson()
        val json = gson.toJson(contacts)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .header("User-Agent", "CloseToYou app, ${Build.MODEL}")
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

                    println("FRIENDS LOCATIONS = $friendsLocations")

                    if (friendsLocalization.isNotEmpty()) {
                        friendsLocalization.clear()
                    }

                    friendsLocations.forEach {
                        friendsLocalization.add(it)

                        val distance =
                            countDistanceBasedOnCurrentLocalization(
                                userLatitude,
                                userLongitude,
                                it.latitude,
                                it.longitude
                            )

                        friendsDistance[it] = distance
                    }

                    radius = sharedPreferences.getInt("RADIUS", 1000).toDouble() // default - 1 KM

                    filteredLocations.clear()
                    filteredLocations.addAll(
                        friendsDistance.filter { it.value <= radius }
                            .map { it.key }
                            .toList()
                    )
                }
            }
        })
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
                val parsedNumber = parseNumber(number)
                localContactsMap[parsedNumber] = name
                numberList.add(parsedNumber)
            }
        }

        return numberList
    }

    private fun parseNumber(number: String): String {
        var cleanedNumber = number.replace(" ", "")
            .replace("-", "")

        if (!cleanedNumber.startsWith("+48")) {
            cleanedNumber = "+48$cleanedNumber"
        }

        return cleanedNumber
    }

    private fun countDistanceBasedOnCurrentLocalization(
        userLat: Double,
        userLon: Double,
        friendLat: Double,
        friendLon: Double
    ): Double {
        val r = 6371e3 // earth radius
        val fi1 = userLat * Math.PI / 180
        val fi2 = friendLat * Math.PI / 180
        val deltaFi1 = (friendLat - userLat) * Math.PI / 180
        val deltaLambda = (friendLon - userLon) * Math.PI / 180

        val a = sin(deltaFi1 / 2) * sin(deltaFi1 / 2) +
                cos(fi1) * cos(fi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c // return in meters
    }

    private fun showNotification(friends: List<Localization>) {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("CloseToYou")
            .setContentText("You have ${friends.size} friends close to you!")
            .setSmallIcon(
                com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_dark_normal
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVibrate(longArrayOf(1000, 1000))

        with(NotificationManagerCompat.from(applicationContext)) {
            if (checkSelfPermission(
                    applicationContext,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId++, notification.build())
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Close To You Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = "Channel of closest user's friends"
        }

        val notificationManager: NotificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun switchToMapFragment() {
        if (checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkAndRequestPermissions()
            return
        }

        activeFragment = MAP

        if (firstTimeLoaded == 1) {
            Toast.makeText(applicationContext, "Loading data...", Toast.LENGTH_LONG).show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            var fragment: Fragment?
            fragment = MapFragment.newInstance(
                userLatitude,
                userLongitude,
                friendsLocalization,
                contactPhotos as HashMap<String, String>
            )
            val fragmentTransition: FragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransition.replace(R.id.frameLayout, fragment)
            fragmentTransition.addToBackStack(null)
            fragmentTransition.commit()
        }, 2000)
    }

    private fun switchToContactFragment() {
        checkAndRequestPermissions()

        activeFragment = CONTACT

        Handler().postDelayed({
            var fragment: Fragment?
            println("LOCS = $friendsLocalization")
            fragment = ContactFragment.newInstance(friendsLocalization)
            val fragmentTransition: FragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransition.replace(R.id.frameLayout, fragment)
            fragmentTransition.addToBackStack(null)
            fragmentTransition.commit()
        }, 700)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Request the permissions
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun stopCheckingNotifications() {
        handler.removeCallbacks(runnable)
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
        startLocationUpdates()

        when (activeFragment) {
            MAP -> switchToMapFragment()
            CONTACT -> switchToContactFragment()
        }

        handler.postDelayed(runnable, timeInterval)
    }

    override fun onStop() {
        super.onStop()

        stopLocationUpdates()
        stopCheckingNotifications()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateGPS()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)

        return true
    }
}

private enum class ActiveFragment {
    MAP, CONTACT
}