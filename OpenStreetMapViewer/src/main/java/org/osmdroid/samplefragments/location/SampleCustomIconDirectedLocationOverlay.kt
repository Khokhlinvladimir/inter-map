package org.osmdroid.samplefragments.location

import android.app.Activity
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay
import java.util.Timer
import java.util.TimerTask

/**
 * https://github.com/osmdroid/osmdroid/issues/249
 *
 * @author alex
 */
class SampleCustomIconDirectedLocationOverlay : BaseSampleFragment(), LocationListener {
    private var hasFix = false
    private var overlay: DirectedLocationOverlay? = null

    override val sampleTitle: String
        get() = "Directed Location Overlay"

    public override fun onResume() {
        super.onResume()
        val lm = getActivity()!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            //on API15 AVDs,network provider fails. no idea why
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
        } catch (ex: Exception) {
        }
    }

    public override fun onPause() {
        super.onPause()
        val lm = getActivity()!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.removeUpdates(this)
    }

    override fun addOverlays() {
        super.addOverlays()
        overlay = DirectedLocationOverlay(getActivity())
        overlay!!.setShowAccuracy(true)
        Toast.makeText(getActivity(), "Requires location services turned on", Toast.LENGTH_LONG).show()
        mMapView!!.getOverlays()!!.add(overlay)
    }

    override fun onLocationChanged(location: Location) {
        //after the first fix, schedule the task to change the icon
        if (!hasFix) {
            Toast.makeText(getActivity(), "Location fixed, scheduling icon change", Toast.LENGTH_LONG).show()
            val changeIcon: TimerTask = object : TimerTask() {
                override fun run() {
                    val act: Activity? = getActivity()
                    if (act != null) act.runOnUiThread(object : Runnable {
                        override fun run() {
                            try {
                                val drawable = getResources().getDrawable(R.drawable.sfgpuci) as BitmapDrawable
                                overlay!!.setDirectionArrow(drawable.getBitmap())
                            } catch (t: Throwable) {
                                //insultates against crashing when the user rapidly switches fragments/activities
                            }
                        }
                    })
                }
            }
            val timer = Timer()
            timer.schedule(changeIcon, 5000)
        }
        hasFix = true
        overlay!!.setBearing(location.getBearing())
        overlay!!.setAccuracy(location.getAccuracy().toInt())
        overlay!!.setLocation(GeoPoint(location.getLatitude(), location.getLongitude()))
        mMapView!!.invalidate()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }
}
