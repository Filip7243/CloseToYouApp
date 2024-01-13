package com.example.closetoyou.fragment

import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.closetoyou.HomeActivity
import com.example.closetoyou.Localization
import com.example.closetoyou.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private const val USER_LAT_ARG = "userLat"
private const val USER_LON_ARG = "userLon"
private const val USER_FRIENDS_ARG = "userFriends"
private const val CONTACT_PHOTOS_ARG = "contactPhotos"

/**
 * A simple [Fragment] subclass.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var userLat: Double? = null
    private var userLon: Double? = null
    private var userFriends: ArrayList<Localization>? = null
    private var contactPhotos: Map<String, String>? = null

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userLat = it.getDouble(USER_LAT_ARG)
            userLon = it.getDouble(USER_LON_ARG)
            userFriends = it.getParcelableArrayList(USER_FRIENDS_ARG)
            contactPhotos = it.getSerializable(CONTACT_PHOTOS_ARG) as HashMap<String, String>?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        val rootView = inflater.inflate(R.layout.fragment_map, container, false)

        mapView = rootView.findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setZoom(16.0)
        mapView.setMultiTouchControls(true)
        val point = userLat?.let { userLon?.let { it1 -> GeoPoint(it, it1) } }
        mapView.controller.setCenter(point)

        val userMarker = Marker(mapView)
        userMarker.position = point
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userMarker.icon = ResourcesCompat.getDrawable(resources, R.drawable.loc_pin1, null)
        mapView.overlays.add(userMarker)

        val sharedPreferences = requireActivity().getSharedPreferences("AppSettings", MODE_PRIVATE)
        val radius = sharedPreferences.getInt("Radius", 1000).toDouble() // default - 1 KM

        println("MAP RADIUS = ${HomeActivity.radius}")

        addCircle(mapView, point!!, radius)

        userFriends?.forEach { loc ->
            println(loc)
            val point = GeoPoint(loc.latitude, loc.longitude)
            val marker = Marker(mapView)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            val photoPath = contactPhotos!![loc.phoneNumber]
            println("sciezka do awatara: "+ photoPath + " nazwa: " + contactPhotos!![loc.name])
            Log.d("MapFragment", "Photo path for ${loc.phoneNumber}: $photoPath")
            if (photoPath != null) {
                val originalBitmap = BitmapFactory.decodeFile(photoPath)
                if (originalBitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 120, 120, true)
                    val roundedBitmap = getRoundedCornerBitmap(scaledBitmap, 60)
                    marker.icon = BitmapDrawable(resources, roundedBitmap)

                    println("IKONA = ${marker.icon}")
                }
            } else {
                val defaultBitmap = BitmapFactory.decodeResource(resources, R.drawable.background_main)
                val scaledBitmap = Bitmap.createScaledBitmap(defaultBitmap, 120, 120, true)
                val roundedBitmap = getRoundedCornerBitmap(scaledBitmap, 60)
                marker.icon = BitmapDrawable(resources, roundedBitmap)
            }
            marker.setOnMarkerClickListener { marker, _ ->
                Toast.makeText(context, marker.title, Toast.LENGTH_SHORT).show()
                true
            }
            marker.title = loc.name
            mapView.overlays.add(marker)
        }

        return rootView
    }

    private fun addCircle(mapView: MapView, center: GeoPoint, radius: Double) {
        val circle = Polygon()
        val points = ArrayList<GeoPoint>()

        val numPoints = 100
        for (i in 0 until numPoints) {
            val angle = (i.toDouble() / numPoints.toDouble()) * 2.0 * Math.PI
            val lat = center.latitude + radius / 111111.0 * Math.sin(angle)
            val lon = center.longitude + radius / (111111.0 * Math.cos(center.latitude * Math.PI / 180.0)) * Math.cos(angle)
            points.add(GeoPoint(lat, lon))
        }

        circle.points = points
        circle.strokeColor = Color.BLUE
        circle.strokeWidth = 2F
        circle.fillColor = Color.argb(75, 0, 0, 255)

        mapView.overlayManager.add(circle)
        mapView.postInvalidate()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val color = 0xff424242.toInt()
        val paint = Paint().apply {
            isAntiAlias = true
            this.color = color
        }
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = pixels.toFloat()

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BlankFragment.
         */
        @JvmStatic
        fun newInstance(userLat: Double, userLon: Double, userFriends: ArrayList<Localization>, contactPhotos: HashMap<String, String>) =
            MapFragment().apply {
                arguments = Bundle().apply {
                    putDouble(USER_LAT_ARG, userLat)
                    putDouble(USER_LON_ARG, userLon)
                    putParcelableArrayList(USER_FRIENDS_ARG, userFriends)
                    putSerializable(CONTACT_PHOTOS_ARG, contactPhotos)
                }
            }
    }
}