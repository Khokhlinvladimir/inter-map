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
        val tilesOverlay = this.mMapView!!.getOverlayManager().getTilesOverlay()!!
        tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        tilesOverlay.loadingBackgroundColor = R.color.black
        tilesOverlay.loadingLineColor = Color.argb(255, 0, 255, 0)
    }
}
