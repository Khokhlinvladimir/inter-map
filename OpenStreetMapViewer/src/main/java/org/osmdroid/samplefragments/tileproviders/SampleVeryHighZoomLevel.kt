package org.osmdroid.samplefragments.tileproviders

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.modules.MapTileAssetsProvider
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ScaleBarOverlay

/**
 * A lousy example of very high zoom levels.
 * A nicer example would require very high zoom level tiles.
 *
 * @author Fabrice Fontaine
 * @since 6.0.0
 */
class SampleVeryHighZoomLevel : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Offline abstract tiles for zoom levels 0 to 29"

    public override fun addOverlays() {
        mMapView!!.setUseDataConnection(false)

        val scaleBarOverlay = ScaleBarOverlay(mMapView!!)
        scaleBarOverlay.setCentred(true)
        scaleBarOverlay.setScaleBarOffset(200, 10)
        mMapView!!.getOverlays()!!.add(scaleBarOverlay)

        val tileSource: ITileSource = XYTileSource(
            "Abstract", 0, 29, 256, ".png", arrayOf<String>("http://localhost/"), "abstract data"
        )
        mMapView!!.setUseDataConnection(false)

        val assetsProvider = MapTileAssetsProvider(SimpleRegisterReceiver(getContext()), getActivity()!!.getAssets(), tileSource)

        val approximationProvider = MapTileApproximater()
        approximationProvider.addProvider(assetsProvider)

        val array = MapTileProviderArray(
            tileSource, SimpleRegisterReceiver(getContext()),
            arrayOf<MapTileModuleProviderBase?>(assetsProvider, approximationProvider)
        )

        mMapView!!.setTileProvider(array)

        mMapView!!.controller!!.setZoom(29.0)
        // cf. https://fr.wikipedia.org/wiki/Point_z%C3%A9ro_des_routes_de_France
        // In English: starting point of all French roads
        mMapView!!.setExpectedCenter(GeoPoint(48.85340215825712, 2.348784611094743))
        mMapView!!.invalidate()
    }
}
