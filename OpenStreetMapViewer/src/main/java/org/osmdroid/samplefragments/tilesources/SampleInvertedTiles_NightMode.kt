package org.osmdroid.samplefragments.tilesources

import android.R
import android.graphics.Color
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.overlay.TilesOverlay

/**
 * sample fragment to show invert tiles, aka night mode and to set the tile loading background colors
 *
 * @author alex
 */
class SampleInvertedTiles_NightMode : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Inverted Tiles"

    public override fun addOverlays() {
        this.mMapView!!.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS)
        this.mMapView!!.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(R.color.black)
        this.mMapView!!.getOverlayManager().getTilesOverlay().setLoadingLineColor(Color.argb(255, 0, 255, 0))
    }
}
