package org.osmdroid.samplefragments.tilesources

import android.graphics.Color
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.TilesOverlay

/**
 * This is another example of viewing multiple tile sources at the same time.
 * created on 12/13/2016.
 *
 * @author Alex O'Ree
 */
class SampleOpenSeaMap : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Open Sea Map"

    var mProvider: MapTileProviderBasic? = null

    public override fun addOverlays() {
        super.addOverlays()
        mProvider = MapTileProviderBasic(getContext())
        val seaMap = TilesOverlay(mProvider, getContext())
        seaMap.setLoadingLineColor(Color.TRANSPARENT)
        seaMap.setLoadingBackgroundColor(Color.TRANSPARENT)
        seaMap.setLoadingDrawable(null)
        mProvider!!.setTileSource(TileSourceFactory.OPEN_SEAMAP)
        mMapView!!.getOverlays()!!.add(seaMap)
        mMapView!!.postInvalidate()
        mMapView!!.controller!!.setCenter(GeoPoint(40.65716, -74.06507))
        mMapView!!.controller!!.setZoom(18)
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mMapView != null) mMapView!!.onDetach()
        mMapView = null
        if (mProvider != null) mProvider!!.detach()
        mProvider = null
    }
}
