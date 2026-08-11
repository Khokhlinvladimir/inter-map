package org.osmdroid.samplefragments.tilesources

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.HEREWeGoTileSource

/**
 * Created by alex on 8/11/16.
 */
class SampleHereWeGo : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "HERE WeGo map tiles (keys are expired)"

    public override fun addOverlays() {
        super.addOverlays()
        mMapView!!.setTileSource(HEREWeGoTileSource(getContext()))
    }
}
