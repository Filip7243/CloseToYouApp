package com.example.closetoyou

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.NOTIFICATION_SERVICE
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationManagerCompat
import com.example.closetoyou.HomeActivity.Companion.userLatitude
import com.example.closetoyou.HomeActivity.Companion.userLongitude
import com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_dark_normal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PostRequestCallback(
    val addGeoPoint: (localization: Localization) -> Unit,
    private val gson: Gson,
    private val context: Context
) : Callback {

    companion object {
        var notificationId: Int = 1
    }

    override fun onFailure(call: Call, e: IOException) {
        Log.d("REQUEST_ATTEMPT", "Failure ${e.message}")
    }

    override fun onResponse(call: Call, response: Response) {
        response.use {
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseStr: String? = response.body?.string()
            val typeToken = object : TypeToken<List<Localization>>() {}.type
            val friendsLocations = gson.fromJson<List<Localization>>(responseStr, typeToken)

            friendsLocations.forEach {
                addGeoPoint(it)

                val distance = countDistanceBasedOnCurrentLocalization(
                    userLatitude, userLongitude,
                    it.latitude, it.longitude
                )

                // TODO: zbierz wszystkie konakty ktore sa w okolicy i wyslij jednego pusha

                Log.d(
                    "POST_RESPONSE_DISTANCE_COUNT", "Distance for number = " +
                            "${it.phoneNumber}, distance = $distance, " +
                            "lat = ${it.latitude}, lon = ${it.longitude}"
                )
            }

            showNotification(friendsLocations)
        }
    }

    private fun countDistanceBasedOnCurrentLocalization(
        userLat: Double,
        userLon: Double,
        friendLat: Double,
        friendLon: Double
    ): Double {
        val R = 6371e3; // earth radius
        val fi1 = userLat * Math.PI / 180
        val fi2 = friendLat * Math.PI / 180
        val deltaFi1 = (friendLat - userLat) * Math.PI / 180
        val deltaLambda = (friendLon - userLon) * Math.PI / 180

        val a = sin(deltaFi1 / 2) * sin(deltaFi1 / 2) +
                cos(fi1) * cos(fi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val d = R * c

        return d // return in meters
    }

    private fun showNotification(friends: List<Localization>) {
        val channelId = "CloseToYou_Channel"
        createNotificationChannel(channelId)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("CloseToYou")
            .setContentText("You have ${friends.size} friends close to you!")
            .setSmallIcon(
                common_google_signin_btn_icon_dark_normal
            )
            .setPriority(PRIORITY_DEFAULT)
            .setVibrate(longArrayOf(1000, 1000))

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    POST_NOTIFICATIONS
                ) == PERMISSION_GRANTED
            ) {
                notify(notificationId++, notification.build())
            }
        }
    }

    private fun createNotificationChannel(channelId: String) {
        val name = "Close To You Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = "Channel of closest user's friends"
        }

        val notificationManager: NotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}