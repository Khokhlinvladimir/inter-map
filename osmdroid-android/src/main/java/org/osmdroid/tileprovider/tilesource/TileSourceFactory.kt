package org.osmdroid.tileprovider.tilesource

import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource
import org.osmdroid.util.MapTileIndex

/**
 * looking for mapquest? it's moved because they stopped supporting anonymous access to tiles
 *
 * @see MapQuestTileSource
 *
 * @see BingMapTileSource
 *
 * @see CloudmadeTileSource
 *
 * @see HEREWeGoTileSource
 *
 * @see MapBoxTileSource
 *
 * @see TMSOnlineTileSourceBase
 */
object TileSourceFactory {
    /**
     * Get the tile source with the specified name. The tile source must be one of the registered sources
     * as defined in the static list mTileSources of this class.
     *
     * @param aName the tile source name
     * @return the tile source
     * @throws IllegalArgumentException if tile source not found
     */
    @Throws(IllegalArgumentException::class)
    fun getTileSource(aName: String?): ITileSource {
        for (tileSource in tileSources) {
            if (tileSource.name() == aName) {
                return tileSource
            }
        }
        throw IllegalArgumentException("No such tile source: " + aName)
    }

    fun containsTileSource(aName: String?): Boolean {
        for (tileSource in tileSources) {
            if (tileSource.name() == aName) {
                return true
            }
        }
        return false
    }

    /**
     * Get the tile source at the specified position.
     *
     * @param aOrdinal
     * @return the tile source
     * @throws IllegalArgumentException if tile source not found
     */
    @Deprecated("")
    @Throws(IllegalArgumentException::class)
    fun getTileSource(aOrdinal: Int): ITileSource {
        for (tileSource in tileSources) {
            if (tileSource.ordinal() == aOrdinal) {
                return tileSource
            }
        }
        throw IllegalArgumentException("No tile source at position: " + aOrdinal)
    }

    /**
     * adds a new tile source to the list
     *
     * @param mTileSource
     */
    fun addTileSource(mTileSource: ITileSource?) {
        tileSources.add(mTileSource!!)
    }

    /**
     * removes any tile sources whose name matches the regular expression
     *
     * @param aRegex regular expression
     * @return number of sources removed
     */
    fun removeTileSources(aRegex: String): Int {
        var n = 0
        for (i in tileSources.indices.reversed()) {
            if (tileSources.get(i).name()!!.matches(aRegex.toRegex())) {
                tileSources.removeAt(i)
                ++n
            }
        }
        return n
    }

    val MAPNIK: OnlineTileSourceBase = XYTileSource(
        "Mapnik",
        0, 19, 256, ".png", arrayOf<String>("https://tile.openstreetmap.org/"), "© OpenStreetMap contributors",
        TileSourcePolicy(
            2,
            (TileSourcePolicy.Companion.FLAG_NO_BULK
                    or TileSourcePolicy.Companion.FLAG_NO_PREVENTIVE
                    or TileSourcePolicy.Companion.FLAG_USER_AGENT_MEANINGFUL
                    or TileSourcePolicy.Companion.FLAG_USER_AGENT_NORMALIZED)
        )
    )

    // max concurrent thread number is 2 (cf. https://operations.osmfoundation.org/policies/tiles/)
    // let's be restrictive here
    // see https://foundation.wikimedia.org/wiki/Maps_Terms_of_Use
    val WIKIMEDIA: OnlineTileSourceBase = XYTileSource(
        "Wikimedia",
        1, 19, 256, ".png", arrayOf<String>("https://maps.wikimedia.org/osm-intl/"),
        "Wikimedia maps | Map data © OpenStreetMap contributors",
        TileSourcePolicy(
            1,
            (TileSourcePolicy.Companion.FLAG_NO_BULK
                    or TileSourcePolicy.Companion.FLAG_NO_PREVENTIVE
                    or TileSourcePolicy.Companion.FLAG_USER_AGENT_MEANINGFUL
                    or TileSourcePolicy.Companion.FLAG_USER_AGENT_NORMALIZED)
        )
    )

    //they do not have ssl setup as of oct 2019
    val PUBLIC_TRANSPORT: OnlineTileSourceBase = XYTileSource(
        "OSMPublicTransport", 0, 17, 256, ".png",
        arrayOf<String>("http://openptmap.org/tiles/"), "© OpenStreetMap contributors"
    )


    val DEFAULT_TILE_SOURCE: OnlineTileSourceBase = MAPNIK

    // CloudMade tile sources are not in mTileSource because they are not free
    // and therefore not provided by default.
    val CLOUDMADESTANDARDTILES: OnlineTileSourceBase = CloudmadeTileSource(
        "CloudMadeStandardTiles", 0, 18, 256, ".png",
        arrayOf<String>(
            "http://a.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s",
            "http://b.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s",
            "http://c.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s"
        )
    )

    // FYI - This tile source has a tileSize of "6"
    val CLOUDMADESMALLTILES: OnlineTileSourceBase = CloudmadeTileSource(
        "CloudMadeSmallTiles", 0, 21, 64, ".png",
        arrayOf<String>(
            "http://a.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s",
            "http://b.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s",
            "http://c.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s"
        )
    )

    // The following tile sources are overlays, not standalone map views.
    // They are therefore not in mTileSources.
    val FIETS_OVERLAY_NL: OnlineTileSourceBase = XYTileSource(
        "Fiets",
        3, 18, 256, ".png",
        arrayOf<String>("https://overlay.openstreetmap.nl/openfietskaart-overlay/"), "© OpenStreetMap contributors"
    )

    val BASE_OVERLAY_NL: OnlineTileSourceBase = XYTileSource(
        "BaseNL",
        0, 18, 256, ".png",
        arrayOf<String>("https://overlay.openstreetmap.nl/basemap/")
    )

    val ROADS_OVERLAY_NL: OnlineTileSourceBase = XYTileSource(
        "RoadsNL",
        0, 18, 256, ".png",
        arrayOf<String>("https://overlay.openstreetmap.nl/roads/"), "© OpenStreetMap contributors"
    )

    /**
     * 2020.03.12 there is also a "http://(a|b|c).tiles.wmflabs.org/hikebike/" version
     */
    val HIKEBIKEMAP: OnlineTileSourceBase = XYTileSource(
        "HikeBikeMap",
        0, 18, 256, ".png",
        arrayOf<String>("https://tiles.wmflabs.org/hikebike/")
    )

    /**
     * This is actually another tile overlay
     *
     * @sunce 5.6.2
     */
    val OPEN_SEAMAP: OnlineTileSourceBase = XYTileSource(
        "OpenSeaMap",
        3, 18, 256, ".png", arrayOf<String>("https://tiles.openseamap.org/seamark/"), "OpenSeaMap"
    )


    val USGS_TOPO: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "USGS National Map Topo", 0, 15, 256, "",
        arrayOf<String>("https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/tile/"), "USGS"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
        }
    }
    val USGS_SAT: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "USGS National Map Sat", 0, 15, 256, "",
        arrayOf<String>("https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryTopo/MapServer/tile/"), "USGS"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
        }
    }


    /**
     * Chart Bundle US Aeronautical Charts
     *
     * @since 5.6.2
     */
    val ChartbundleWAC: OnlineTileSourceBase = XYTileSource(
        "ChartbundleWAC", 4, 12, 256, ".png?type=google",
        arrayOf<String>("https://wms.chartbundle.com/tms/v1.0/wac/"), "chartbundle.com"
    )

    /**
     * Chart Bundle US Aeronautical Charts Enroute High
     *
     * @since 5.6.2
     */
    val ChartbundleENRH: OnlineTileSourceBase = XYTileSource(
        "ChartbundleENRH", 4, 12, 256, ".png?type=google",
        arrayOf<String>("https://wms.chartbundle.com/tms/v1.0/enrh/", "chartbundle.com")
    )

    /**
     * Chart Bundle US Aeronautical Charts Enroute Low
     *
     * @since 5.6.2
     */
    val ChartbundleENRL: OnlineTileSourceBase = XYTileSource(
        "ChartbundleENRL", 4, 12, 256, ".png?type=google",
        arrayOf<String>("https://wms.chartbundle.com/tms/v1.0/enrl/", "chartbundle.com")
    )

    /**
     * Open Topo Maps https://opentopomap.org
     *
     * @since 5.6.2
     */
    val OpenTopo: OnlineTileSourceBase = XYTileSource(
        "OpenTopoMap", 0, 17, 256, ".png",
        arrayOf<String>(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        ),
        "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Kartendarstellung: © OpenTopoMap (CC-BY-SA)"
    )

    /**
     * returns all predefined tiles sources that are generally free to use. be sure to check the usage
     * agreements yourself.
     *
     * @return
     */
    val tileSources: MutableList<ITileSource>

    init {
        tileSources = ArrayList<ITileSource>()
        tileSources.add(MAPNIK)
        tileSources.add(WIKIMEDIA)
        tileSources.add(PUBLIC_TRANSPORT)
        tileSources.add(HIKEBIKEMAP)
        tileSources.add(USGS_TOPO)
        tileSources.add(USGS_SAT)
        tileSources.add(ChartbundleWAC)
        tileSources.add(ChartbundleENRH)
        tileSources.add(ChartbundleENRL)
        tileSources.add(OpenTopo)
    }
}
