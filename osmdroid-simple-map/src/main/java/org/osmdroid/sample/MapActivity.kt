package org.osmdroid.sample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.osmdroid.config.Configuration.instance
import org.osmdroid.simplemap.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.OneFingerZoomOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapActivity : AppCompatActivity() {
    private var mapView: MapView? = null

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        }
    }

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        instance!!.load(applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext))

        requestLocationPermission()

        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)

        instance!!.load(applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext))
        if (mapView != null) {
            mapView!!.setTilesScaledToDpi(true)
            mapView!!.setMultiTouchControls(true)
            mapView!!.setMapOrientation(0f, true)
        }

        val mRotationGestureOverlay = RotationGestureOverlay(mapView)
        mRotationGestureOverlay.isEnabled = true
        mapView!!.getOverlays()!!.add(mRotationGestureOverlay)

        //support for one finger zoom
        val mOneFingerZoomOverlay = OneFingerZoomOverlay()
        mapView!!.getOverlays()!!.add(mOneFingerZoomOverlay)

        val mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        mLocationOverlay.enableMyLocation()
        mapView!!.getOverlays()!!.add(mLocationOverlay)


        // Создание маркера
        val marker = Marker(mapView)
        marker.position = GeoPoint(55.75087199739751, 37.61757608743155) // Задайте широту и долготу
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.marker_default, null)
        marker.title = "Название места"
        marker.snippet = "Дополнительная информация"

        // Добавление маркера на карту
        mapView!!.getOverlays()!!.add(marker)

        // Добавление обьекта нарисованного на Canvas
        // mapView!!.getOverlays()!!.add(CustomOverlay(mapView = mapView!!))



    }

    public override fun onResume() {
        super.onResume()
        if (mapView != null) {
            mapView!!.onResume()
        }
    }

    public override fun onPause() {
        super.onPause()
        instance!!.save(applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext))
        if (mapView != null) {
            mapView!!.onPause()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение было предоставлено
            } else {
                // Разрешение не было предоставлено
            }
        }
    }


    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
    }

    class CustomOverlay(mapView: MapView) : Overlay() {
        private val projection = mapView.projection
        val bottomGeoPoint = GeoPoint(43.68242316567707, 40.26641550645863)
        val topGeoPoint = GeoPoint(43.66864227178646, 40.25869344400052)

        private val paint = Paint().apply {
            color = Color.RED // Цвет линии
            strokeWidth = 70f    // Толщина линии
            style = Paint.Style.STROKE
        }

        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
            super.draw(canvas, mapView, shadow)

            val screenCoordsBottom = projection.toPixels(bottomGeoPoint, null)

            projection
            canvas.drawLine(screenCoordsBottom!!.x.toFloat(), screenCoordsBottom.y.toFloat(),
                    300.toFloat(), 300.toFloat(), paint)
        }
    }

}