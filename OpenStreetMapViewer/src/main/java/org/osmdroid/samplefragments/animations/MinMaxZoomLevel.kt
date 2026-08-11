package org.osmdroid.samplefragments.animations

import android.widget.Toast
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import java.util.Locale

/**
 * Demonstrates interaction of double tab zoom with maximum zoom level
 * created on 10/18/2017.
 * https://github.com/osmdroid/osmdroid/issues/743
 *
 * @author Maradox
 * @since 6.0.0
 */
class MinMaxZoomLevel : BaseSampleFragment(), MapListener {
    override val sampleTitle: String
        get() = "Minimum and Maximum Zoom Level"

    override fun addOverlays() {
        super.addOverlays()
        mMapView!!.setMinZoomLevel(1.5)
        mMapView!!.setMaxZoomLevel(5.5)
        mMapView!!.setMapListener(this)
        mMapView!!.controller!!.zoomTo(2.5)
    }

    override fun onScroll(scrollEvent: ScrollEvent): Boolean {
        return false
    }

    override fun onZoom(zoomEvent: ZoomEvent): Boolean {
        val zoomLevel = String.format(Locale.getDefault(), "%.2f", zoomEvent.zoomLevel)
        Toast.makeText(getContext(), "Zoom to " + zoomLevel, Toast.LENGTH_SHORT).show()
        return false
    }
}
