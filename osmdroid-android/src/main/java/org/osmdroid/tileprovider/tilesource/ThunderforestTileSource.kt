package org.osmdroid.tileprovider.tilesource

import android.content.Context
import org.osmdroid.tileprovider.util.ManifestUtil
import org.osmdroid.util.MapTileIndex

/**
 * Thunderforest Maps including OpenCycleMap
 */
class ThunderforestTileSource(ctx: Context, aMap: Int) :
    OnlineTileSourceBase(uiMap[aMap], 0, 17, 256, ".png", baseUrl, "Maps © Thunderforest, Data © OpenStreetMap contributors.") {
    private val mMap: Int
    private val mMapId: String

    /**
     * creates a new Thunderforest tile source, loading the access token and mapid from the manifest
     */
    init {
        mMap = aMap
        mMapId = retrieveMapId(ctx)
        //this line will ensure uniqueness in the tile cache
        //mName="thunderforest"+aMap+mMapId;
    }

    /**
     * Reads the map id from the manifest.<br></br>
     */
    fun retrieveMapId(aContext: Context): String {
        // Retrieve the MapId from the Manifest
        return ManifestUtil.retrieveKey(aContext, THUNDERFOREST_MAPID)
    }

    override fun getTileURLString(pMapTileIndex: Long): String {
        val url = StringBuilder(super.baseUrl.orEmpty().replace("{map}", urlMap[mMap]!!))
        url.append(MapTileIndex.getZoom(pMapTileIndex))
        url.append("/")
        url.append(MapTileIndex.getX(pMapTileIndex))
        url.append("/")
        url.append(MapTileIndex.getY(pMapTileIndex))
        url.append(".png?")
        url.append("apikey=").append(mMapId)
        val res = url.toString()

        //Log.d(IMapView.LOGTAG, res);
        return res
    }

    companion object {
        /**
         * the meta data key in the manifest
         */
        //<meta-data android:name="THUNDERFOREST_MAPID" android:value="YOUR KEY" />
        private const val THUNDERFOREST_MAPID = "THUNDERFOREST_MAPID"

        /**
         * the available map types
         */
        const val CYCLE: Int = 0
        const val TRANSPORT: Int = 1
        const val LANDSCAPE: Int = 2
        const val OUTDOORS: Int = 3
        const val TRANSPORT_DARK: Int = 4
        const val SPINAL_MAP: Int = 5
        const val PIONEER: Int = 6
        const val MOBILE_ATLAS: Int = 7
        const val NEIGHBOURHOOD: Int = 8


        /**
         * map names used in URLs
         */
        private val urlMap: Array<String?> = arrayOf<String?>(
            "cycle",
            "transport",
            "landscape",
            "outdoors",
            "transport-dark",
            "spinal-map",
            "pioneer",
            "mobile-atlas",
            "neighbourhood"
        )

        /**
         * map names used in UI (eg. menu)
         */
        private val uiMap = arrayOf<String>(
            "CycleMap",
            "Transport",
            "Landscape",
            "Outdoors",
            "TransportDark",
            "Spinal",
            "Pioneer",
            "MobileAtlas",
            "Neighbourhood"
        )

        private val baseUrl: Array<String?> = arrayOf<String?>(
            "https://a.tile.thunderforest.com/{map}/",
            "https://b.tile.thunderforest.com/{map}/",
            "https://c.tile.thunderforest.com/{map}/"
        )

        /**
         * return the name asociated with a map.
         */
        fun mapName(m: Int): String {
            if (m < 0 || m >= uiMap.size) return ""
            return uiMap[m]
        }

        /**
         * check if we have a key in the manifest for this provider.
         */
        fun haveMapId(aContext: Context): Boolean {
            return ManifestUtil.retrieveKey(aContext, THUNDERFOREST_MAPID) != ""
        }
    }
}
