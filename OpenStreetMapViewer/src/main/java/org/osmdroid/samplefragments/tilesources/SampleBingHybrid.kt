package org.osmdroid.samplefragments.tilesources

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource

/**
 * created on 1/8/2017.
 *
 * @author Alex O'Ree
 */
class SampleBingHybrid : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Bing Aerial with Labels"

    public override fun addOverlays() {
        super.addOverlays()
        //this gets the key from the manifest
        BingMapTileSource.retrieveBingKey(this.getContext())
        val source = BingMapTileSource(null)
        Thread(object : Runnable {
            override fun run() {
                source.initMetaData()
            }
        }).start()
        source.setStyle(BingMapTileSource.IMAGERYSET_AERIALWITHLABELS)
        mMapView!!.setTileSource(source)
    }
}
