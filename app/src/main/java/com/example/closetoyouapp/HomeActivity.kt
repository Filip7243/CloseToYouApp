package com.example.closetoyouapp

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
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.closetoyouapp.ActiveFragment.MAP
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    //Default fragment
    private var activeFragment: ActiveFragment = MAP

    //Friends Info
    private var friendsLocalization: ArrayList<Localization> = arrayListOf()
    val localContactsMap = mutableMapOf<String, String>()
    private val friendsDistance: MutableMap<Localization, Double> = mutableMapOf()
    private val contactPhotos: MutableMap<String, String> = mutableMapOf()

    // User preferences
    private lateinit var sharedPreferences: SharedPreferences

    //Phone Localization (google API)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    //Permissions
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
    private val API_URL = "http://192.168.43.29:8080/api/v1/localization"
    private val client = OkHttpClient()

    //Notifications
    val CHANNEL_ID = "CLOSE_TO_YOU_CHANNEL"
    var notificationId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        sharedPreferences = getSharedPreferences("USER_PREFERENCES", MODE_PRIVATE)

        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 7000
        locationRequest.priority = PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val location = locationResult.lastLocation // todo; intenret policy
                println("WITAM Z LOOPBACK!")

                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude

                    sendPutRequest(userLatitude, userLongitude)
                    sendPostRequest()
                }
            }
        }

        updateGPS()
//        loadAvatars()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
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

        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                updateUserLocalization(it)
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

//    private fun loadAvatars() {
//        CoroutineScope(Dispatchers.IO).launch {
//            val database = MyApp.getDatabase(this@HomeActivity)
//            val photoMap = database.contactPhotoDao().getAllPhotos()
//                .associateBy({ it.phoneNumber }, { it.photoUri })
//            println("photo map = $photoMap")
//            contactPhotos.putAll(photoMap)
//        }
//    }

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

                    val filteredLocations: List<Localization> =
                        friendsDistance.filter { it.value <= radius }
                            .map { it.key }
                            .toList()

                    println("FILTERED TUTAJ YYEAH = $filteredLocations")

                    if (filteredLocations.isNotEmpty()) {
                        showNotification(filteredLocations)
                    }
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
            if (ActivityCompat.checkSelfPermission(
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

    override fun onResume() {
        super.onResume()

        checkAndRequestPermissions()
        startLocationUpdates()
    }

    override fun onStop() {
        super.onStop()

        stopLocationUpdates()
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
}

private enum class ActiveFragment {
    MAP, CONTACT
}