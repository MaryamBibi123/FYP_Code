package com.example.signuplogina.activities

import android.annotation.SuppressLint
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import java.io.InputStream

import com.example.signuplogina.R
import com.example.signuplogina.databinding.ActivitySelectLocationBinding
import android.widget.Button
import android.widget.Toast
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint

import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL

class SelectLocationActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var confirmBtn: Button
    private var selectedLat: Double? = null
    private var selectedLon: Double? = null
    private var selectedAddress: String = ""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_select_location)

        map = findViewById(R.id.map)
        confirmBtn = findViewById(R.id.btn_confirm)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        val startPoint = GeoPoint(24.8607, 67.0011) // Karachi
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)

        map.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val point = map.projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                selectedLat = point.latitude
                selectedLon = point.longitude
                showMarker(point)
                fetchAddress(point.latitude, point.longitude)
            }
            false
        }

        confirmBtn.setOnClickListener {
            if (selectedLat != null && selectedLon != null) {
                val resultIntent = intent
                resultIntent.putExtra("lat", selectedLat)
                resultIntent.putExtra("lon", selectedLon)
                resultIntent.putExtra("address", selectedAddress)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMarker(point: GeoPoint) {
        map.overlays.clear()
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun fetchAddress(lat: Double, lon: Double) {
        Thread {
            try {
//                val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon")
                val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&accept-language=en")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                selectedAddress = json.getString("display_name")
                runOnUiThread {
                    Toast.makeText(this, selectedAddress, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
