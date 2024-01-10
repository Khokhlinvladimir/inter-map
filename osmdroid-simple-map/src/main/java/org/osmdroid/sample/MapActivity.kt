package org.osmdroid.sample

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration.instance
import org.osmdroid.simplemap.R
import org.osmdroid.views.MapView

class MapActivity : AppCompatActivity() {
    private var mapView: MapView? = null
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        instance!!.load(applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext))

        //TODO check permissions
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
    }

    public override fun onResume() {
        super.onResume()
        instance!!.load(applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext))
        if (mapView != null) {
            mapView!!.onResume()
            mapView!!.setMultiTouchControls(true)
            mapView!!.setMapOrientation(0f, true)
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