package org.osmdroid.samplefragments.tilesources

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.MapQuestTileSource

/**
 * Created by alex on 8/11/16.
 */
class SampleMapQuest : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "MapQuest tile source"

    public override fun addOverlays() {
        super.addOverlays()
        mMapView!!.setTileSource(MapQuestTileSource(getContext()))
    }
}
