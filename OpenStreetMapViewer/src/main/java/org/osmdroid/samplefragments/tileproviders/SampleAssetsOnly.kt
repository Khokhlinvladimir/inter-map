package org.osmdroid.samplefragments.tileproviders

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.MapTileAssetsProvider
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver

/**
 * test to force assets only loaded
 * https://github.com/osmdroid/osmdroid/issues/272
 * Created by alex on 2/21/16.
 */
class SampleAssetsOnly : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Assets Only"

    public override fun addOverlays() {
        this.mMapView!!.setUseDataConnection(false)
        val prov = MapTileAssetsProvider(SimpleRegisterReceiver(getContext()), getActivity()!!.getAssets())

        this.mMapView!!.setTileProvider(
            MapTileProviderArray(
                TileSourceFactory.MAPNIK,
                SimpleRegisterReceiver(getContext()),
                arrayOf<MapTileModuleProviderBase?>(prov)
            )
        )
    }
}
