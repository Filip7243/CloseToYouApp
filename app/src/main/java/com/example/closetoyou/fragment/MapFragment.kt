package com.example.closetoyou.fragment

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.closetoyou.Localization
import com.example.closetoyou.R
import com.google.android.gms.maps.model.MarkerOptions
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val USER_LAT_ARG = "userLat"
private const val USER_LON_ARG = "userLon"
private const val USER_FRIENDS_ARG = "userFriends"

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

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userLat = it.getDouble(USER_LAT_ARG)
            userLon = it.getDouble(USER_LON_ARG)
            userFriends = it.getParcelableArrayList(USER_FRIENDS_ARG)
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

        val marker = Marker(mapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.loc_pin1, null)
        mapView.overlays.add(marker)

        userFriends?.forEach { loc ->
            val point = GeoPoint(loc.latitude, loc.longitude)
            val marker = Marker(mapView)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.loc_pin1, null)
            mapView.overlays.add(marker)

            println("DUPA")
        }

        return rootView
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
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(userLat: Double, userLon: Double, userFriends: ArrayList<Localization>) =
            MapFragment().apply {
                arguments = Bundle().apply {
                    putDouble(USER_LAT_ARG, userLat)
                    putDouble(USER_LON_ARG, userLon)
                    putParcelableArrayList(USER_FRIENDS_ARG, userFriends)
                }
                println("JESTEM W MAPIE!!! newInstance!")
            }

    }

    private fun addMarkers() {
        userFriends?.forEach { location ->
            var point = GeoPoint(location.latitude, location.longitude)
            val marker = Marker(mapView)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.loc_pin1, null)
            mapView.overlays.add(marker)
        }
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
}