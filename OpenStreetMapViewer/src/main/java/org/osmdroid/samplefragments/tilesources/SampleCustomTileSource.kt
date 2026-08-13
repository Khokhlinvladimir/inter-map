package org.osmdroid.samplefragments.tilesources

import org.osmdroid.samplefragments.BaseSampleFragment

/**
 * Simple how to for setting a custom tile source
 *
 * @author alex
 */
class SampleCustomTileSource : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Custom Tile Source"

    public override fun addOverlays() {
        mMapView!!.setTileSource(USGSTileSource())
    }
}
