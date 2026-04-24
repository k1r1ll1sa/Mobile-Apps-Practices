package ru.Gavrilyuk.Ex1

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class RestaurantMapActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var backButton: ImageButton

    private val hardcodedRestaurants = listOf(
        MapRestaurant(id = 1, name = "Rostic's", lat = 55.9979, lon = 92.7954, imageRes = R.drawable.rostics),
        MapRestaurant(id = 2, name = "Вкусно — и точка", lat = 56.0117, lon = 92.8066, imageRes = R.drawable.vkusno_i_tochka),
        MapRestaurant(id = 3, name = "Burger King", lat = 56.0227, lon = 92.7980, imageRes = R.drawable.burger_king)
    )

    data class MapRestaurant(val id: Int, val name: String, val lat: Double, val lon: Double, val imageRes: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().apply {
            load(this@RestaurantMapActivity, PreferenceManager.getDefaultSharedPreferences(this@RestaurantMapActivity))
            setUserAgentValue("${packageName}/1.0")
        }

        setContentView(R.layout.restaurant_map)

        mapView = findViewById(R.id.mapView)
        backButton = findViewById(R.id.backButton)

        setupMap()
        backButton.setOnClickListener { finish() }
    }

    private fun setupMap() {
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(56.0103, 92.8705))
        }

        hardcodedRestaurants.forEach { restaurant ->
            addMarker(restaurant)
        }
    }

    private fun addMarker(restaurant: MapRestaurant) {
        val marker = Marker(mapView)
        marker.position = GeoPoint(restaurant.lat, restaurant.lon)
        marker.title = restaurant.name
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        marker.setOnMarkerClickListener { _, _ ->
            showRestaurantPopup(restaurant)
            true
        }

        mapView.overlays.add(marker)
    }

    private fun showRestaurantPopup(restaurant: MapRestaurant) {
        val rootLayout = findViewById<ViewGroup>(R.id.rootMapLayout)
        val popupView = layoutInflater.inflate(R.layout.popup_restaurant_info, rootLayout, false)

        popupView.findViewById<ImageView>(R.id.popupImage).setImageResource(restaurant.imageRes)
        popupView.findViewById<TextView>(R.id.popupName).text = restaurant.name

        popupView.findViewById<Button>(R.id.popupGoButton).setOnClickListener {
            startActivity(Intent(this, RestMenuActivity::class.java).apply {
                putExtra("restaurant_id", restaurant.id)
                putExtra("restaurant_name", restaurant.name)
            })
        }

        popupView.setOnClickListener {
            rootLayout.removeView(popupView)
        }

        popupView.findViewById<View>(R.id.popupContent).setOnClickListener { /* consume click */ }

        rootLayout.addView(popupView)
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDetach() }
}