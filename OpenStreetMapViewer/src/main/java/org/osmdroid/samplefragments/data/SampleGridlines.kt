package org.osmdroid.samplefragments.data

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2

/**
 * An example on how to use the lat/lon gridline overlay.
 *
 *
 * basically, listen for map motion/zoom events and remove the old overlay, then add the new one.
 * you can also override the color scheme and font sizes for the labels and lines
 */
open class SampleGridlines : BaseSampleFragment() {
    override val sampleTitle: String?
        get() = "Lat/Lon Gridlines"

    override fun addOverlays() {
        super.addOverlays()
        mMapView!!.controller!!.setCenter(GeoPoint(0.0, 0.0))
        mMapView!!.controller!!.setZoom(5)
        mMapView!!.setTilesScaledToDpi(true)

        mMapView!!.controller!!.setZoom(3)

        val grids = LatLonGridlineOverlay2()
        mMapView!!.getOverlayManager().add(grids)
    }
}
