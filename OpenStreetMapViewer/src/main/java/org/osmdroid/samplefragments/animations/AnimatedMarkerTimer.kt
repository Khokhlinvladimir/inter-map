package org.osmdroid.samplefragments.animations

import android.app.Activity
import android.graphics.Color
import android.util.Log
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2
import java.util.Timer
import java.util.TimerTask

/**
 * Demonstrates a one way to move an icon in an animation.
 * It's dirty but it works
 * created on 7/29/2017.
 * https://github.com/osmdroid/osmdroid/issues/636
 *
 * @author Alex O'Ree
 */
class AnimatedMarkerTimer : BaseSampleFragment(), MapListener {
    var alive: Boolean = true
    var activeLatLonGrid: FolderOverlay? = null
    var marker: Marker? = null
    var t: Timer? = null
    var task: TimerTask? = null
    var added: Boolean = false

    override val sampleTitle: String
        get() = "Animated Marker"

    override fun addOverlays() {
        super.addOverlays()
        mMapView!!.controller!!.setCenter(GeoPoint(0.0, 0.0))
        mMapView!!.controller!!.setZoom(5)
        mMapView!!.setTilesScaledToDpi(true)
        mMapView!!.setMapListener(this)
        mMapView!!.controller!!.setZoom(3)

        marker = Marker(mMapView)
        marker!!.setPosition(GeoPoint(45.0, -74.0))

        val grids = LatLonGridlineOverlay2()

        grids.setBackgroundColor(Color.BLACK)
        grids.setFontColor(Color.GREEN)
        grids.setLineColor(Color.GREEN)
        mMapView!!.getOverlayManager().add(grids)
    }


    override fun onScroll(scrollEvent: ScrollEvent): Boolean {
        return false
    }

    override fun onZoom(zoomEvent: ZoomEvent): Boolean {
        return false
    }


    override fun onResume() {
        super.onResume()
        startTask()
    }


    private fun startTask() {
        task = object : TimerTask() {
            override fun run() {
                var current = marker!!.getPosition()
                if (current == null) current = GeoPoint(45.0, -74.0)
                val location = GeoPoint(current.latitude, current.longitude + 0.0003)
                if (location != null) {
                    val activity: Activity? = getActivity()
                    if (activity != null) try {
                        activity.runOnUiThread(object : Runnable {
                            override fun run() {
                                try {
                                    marker!!.setPosition(location)
                                    mMapView!!.controller!!.setCenter(location)

                                    if (marker!!.isInfoWindowShown()) {
                                        marker!!.closeInfoWindow()
                                        marker!!.showInfoWindow()
                                    }
                                    if (!added) {
                                        //only add it once
                                        mMapView!!.getOverlayManager().add(marker)
                                        added = true
                                    }
                                } catch (ex: Exception) {
                                    Log.e(TAG, "error updating marker", ex)
                                }
                            }
                        })
                    } catch (ex: Exception) {
                        Log.e(TAG, "error schedule task ", ex)
                    }
                }
            }
        }
        t = Timer()
        t!!.schedule(task, 1000, 1000)
    }

    override fun onPause() {
        super.onPause()
        alive = false
        if (t != null) t!!.cancel()
        t = null
    }

    override fun onDestroyView() {
        alive = false
        if (t != null) t!!.cancel()
        t = null
        marker!!.onDetach(mMapView)
        marker = null
        super.onDestroyView()
    }
}
