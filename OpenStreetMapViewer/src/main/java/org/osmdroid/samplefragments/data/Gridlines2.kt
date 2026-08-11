package org.osmdroid.samplefragments.data

import android.graphics.Color
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2

/**
 * created on 1/8/2017.
 *
 * @author Alex O'Ree
 */
class Gridlines2 : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Lat/Lon Gridlines (customized)"

    override fun addOverlays() {
        mMapView!!.getOverlayManager().getTilesOverlay()!!.setColorFilter(TilesOverlay.INVERT_COLORS)
        val grids = LatLonGridlineOverlay2()
        grids.setBackgroundColor(Color.BLACK)
        grids.setFontColor(Color.RED)
        grids.setLineColor(Color.RED)
        grids.setFontSizeDp(14.toShort())
        mMapView!!.getOverlayManager().add(grids)
    }
}
