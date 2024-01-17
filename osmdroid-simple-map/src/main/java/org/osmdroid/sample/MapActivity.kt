package org.osmdroid.sample

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration.instance
import org.osmdroid.simplemap.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.gestures.OneFingerZoomOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay


class MapActivity : AppCompatActivity() {
    private var mapView: MapView? = null
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        instance!!.load(applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext))

        //TODO check permissions
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)

        instance!!.load(applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext))
        if (mapView != null) {
            mapView!!.setTilesScaledToDpi(true)
            // mapView!!.onResume()
            mapView!!.setMultiTouchControls(true)
            mapView!!.setMapOrientation(0f, true)
        }

        val mRotationGestureOverlay = RotationGestureOverlay(mapView)
        mRotationGestureOverlay.isEnabled = true
        mapView!!.getOverlays()!!.add(mRotationGestureOverlay)

        //support for one finger zoom

        //support for one finger zoom
        val mOneFingerZoomOverlay = OneFingerZoomOverlay()
        mapView!!.getOverlays()!!.add(mOneFingerZoomOverlay)
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
}