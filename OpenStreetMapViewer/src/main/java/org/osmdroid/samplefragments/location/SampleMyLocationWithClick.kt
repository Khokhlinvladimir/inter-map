package org.osmdroid.samplefragments.location

import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * created on 1/13/2017.
 *
 * @author Alex O'Ree
 */
class SampleMyLocationWithClick : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "MyLocationNewOverlay with Click"

    public override fun addOverlays() {
        super.addOverlays()

        val overlay = MyLocationOverlayWithClick(mMapView!!)
        overlay.enableFollowLocation()
        overlay.enableMyLocation()
        overlay.runOnFirstFix(object : Runnable {
            override fun run() {
                Log.i(TAG, "I was ran on the first fix")
                val activity = this@SampleMyLocationWithClick.getActivity()
                if (activity != null) activity.runOnUiThread(object : Runnable {
                    override fun run() {
                        val myLocation = overlay.myLocation
                        if (myLocation != null) Toast.makeText(
                            this@SampleMyLocationWithClick.getContext(),
                            "GPS fix acquired at " + myLocation.toDoubleString(),
                            Toast.LENGTH_LONG
                        ).show()
                        else Toast.makeText(this@SampleMyLocationWithClick.getContext(), "GPS fix acquired (null)", Toast.LENGTH_LONG).show()
                    }
                })
            }
        })
        mMapView!!.getOverlayManager().add(overlay)
    }

    class MyLocationOverlayWithClick(mapView: MapView) : MyLocationNewOverlay(mapView) {
        override fun onSingleTapConfirmed(e: MotionEvent, map: MapView?): Boolean {
            val fix = lastFix
            if (fix != null) Toast.makeText(
                map!!.getContext(),
                "Tap! I am at " + fix.getLatitude() + "," + fix.getLongitude(),
                Toast.LENGTH_LONG
            ).show()
            return true
        }
    }
}
