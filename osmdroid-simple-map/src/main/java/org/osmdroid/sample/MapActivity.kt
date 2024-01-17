package org.osmdroid.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration.instance
import org.osmdroid.simplemap.R
import org.osmdroid.views.MapView
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

                //My Location



            } else {
                // Разрешение не было предоставлено
            }
        }
    }


    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
    }
}