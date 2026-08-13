package org.osmdroid.samplefragments.tileproviders

import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint

/**
 * Demo checking if the zoom restriction for tiles is correctly applied
 * We actually download MAPNIK tiles but only for zoom levels 14 and 15
 *
 * @author Fabrice Fontaine
 * @since 6.1.3
 */
class SampleUnreachableOnlineTiles : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Zoom Restricted Online Tiles (" + ZOOM_MIN + "-" + ZOOM_MAX + ")"

    override fun addOverlays() {
        super.addOverlays()

        mMapView!!.setTileSource(MAPNIK_FOR_TESTS)
        mMapView!!.post(object : Runnable {
            override fun run() {
                mMapView!!.controller!!.setZoom((ZOOM_MIN * 1f).toDouble())
                mMapView!!.setExpectedCenter(GeoPoint(45.7597, 4.8422)) // Lyon, France
            }
        })
    }

    companion object {
        private const val ZOOM_MIN = 14
        private const val ZOOM_MAX = 15

        /**
         * cf. [TileSourceFactory.MAPNIK]
         */
        private val MAPNIK_FOR_TESTS: OnlineTileSourceBase = XYTileSource(
            "Mapnik",
            ZOOM_MIN, ZOOM_MAX, 256, ".png", arrayOf<String>(
                "https://a.tile.openstreetmap.org/",
                "https://b.tile.openstreetmap.org/",
                "https://c.tile.openstreetmap.org/"
            ), "© OpenStreetMap contributors",
            TileSourcePolicy(
                2,
                (TileSourcePolicy.FLAG_NO_BULK
                        or TileSourcePolicy.FLAG_NO_PREVENTIVE
                        or TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
                        or TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED)
            )
        )
    }
}
