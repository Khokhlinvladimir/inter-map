package org.osmdroid.samplefragments.events

import android.graphics.Point
import org.osmdroid.api.IMapView
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Overlay.Snappable

/**
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
class SampleSnappable : BaseSampleFragment() {
    // cf. https://en.wikipedia.org/wiki/Gare_de_Perpignan
    private val MAP_CENTER = GeoPoint(42.696111, 2.879444)

    override val sampleTitle: String
        get() = "Snappable"

    internal inner class MyOverlay : Overlay(), Snappable {
        override fun onSnapToItem(x: Int, y: Int, snapPoint: Point, mapView: IMapView?): Boolean {
            val projection = mapView!!.projection
            projection!!.toPixels(MAP_CENTER, snapPoint)
            return true
        }
    }

    override fun addOverlays() {
        super.addOverlays()

        mMapView!!.getOverlayManager().add(MyOverlay())
        mMapView!!.post(object : Runnable {
            override fun run() {
                mMapView!!.controller!!.setZoom(14.0)
                mMapView!!.setExpectedCenter(MAP_CENTER)
            }
        })
    }
}
