package org.osmdroid.tileprovider.tilesource

import android.content.Context
import org.osmdroid.tileprovider.util.ManifestUtil
import org.osmdroid.util.MapTileIndex

/**
 * MapQuest tile source, revised as 2016 July to meet the new way to access tiles, via api key
 *
 * @author alex
 * @since 5.3
 */
class MapQuestTileSource : OnlineTileSourceBase {
    var mapBoxMapId: String? = "mapquest.streets-mb"
        private set
    var accessToken: String? = null

    /**
     * creates a new mapbox tile source, loading the access token and mapid from the manifest
     *
     * @param ctx
     * @since 5.1
     */
    constructor(ctx: Context) : super("MapQuest", 1, 19, 256, ".png", mapBoxBaseUrl, "MapQuest") {
        retrieveAccessToken(ctx)
        retrieveMapBoxMapId(ctx)
        mName = "MapQuest" + mapBoxMapId
    }

    /**
     * creates a new mapbox tile source, using the specified access token and mapbox id
     *
     * @param mapboxid
     * @param accesstoken
     * @since 5.1
     */
    constructor(mapboxid: String?, accesstoken: String?) : super("MapQuest" + mapboxid, 1, 19, 256, ".png", mapBoxBaseUrl, "MapQuest") {
        this.accessToken = accesstoken
        this.mapBoxMapId = mapboxid
    }

    /**
     * TileSource allowing majority of options (sans url) to be user selected.
     * <br></br> **Warning, the static method [.retrieveMapBoxMapId] should have been invoked once before constructor invocation**
     *
     * @param name                Name
     * @param zoomMinLevel        Minimum Zoom Level
     * @param zoomMaxLevel        Maximum Zoom Level
     * @param tileSizePixels      Size of Tile Pixels
     * @param imageFilenameEnding Image File Extension
     */
    constructor(name: String?, zoomMinLevel: Int, zoomMaxLevel: Int, tileSizePixels: Int, imageFilenameEnding: String?) : super(
        name,
        zoomMinLevel,
        zoomMaxLevel,
        tileSizePixels,
        imageFilenameEnding,
        mapBoxBaseUrl,
        "MapQuest"
    )

    /**
     * TileSource allowing all options to be user selected.
     * <br></br> **Warning, the static method [.retrieveMapBoxMapId] should have been invoked once before constructor invocation**
     *
     * @param name                 Name
     * @param zoomMinLevel         Minimum Zoom Level
     * @param zoomMaxLevel         Maximum Zoom Level
     * @param tileSizePixels       Size of Tile Pixels
     * @param imageFilenameEnding  Image File Extension
     * @param mapBoxVersionBaseUrl MapBox Version Base Url @see https://www.mapbox.com/developers/api/#Versions
     */
    constructor(
        name: String?,
        zoomMinLevel: Int,
        zoomMaxLevel: Int,
        tileSizePixels: Int,
        imageFilenameEnding: String?,
        mapBoxMapId: String?,
        mapBoxVersionBaseUrl: String?
    ) : super(
        name + mapBoxMapId, zoomMinLevel, zoomMaxLevel, tileSizePixels, imageFilenameEnding,
        arrayOf<String?>(mapBoxVersionBaseUrl), "MapQuest"
    ) {
        this.mapBoxMapId = mapBoxMapId
    }

    /**
     * Reads the mapbox map id from the manifest.<br></br>
     * It will use the default value of mapquest if not defined
     */
    fun retrieveMapBoxMapId(aContext: Context) {
        // Retrieve the MapId from the Manifest
        val temp = ManifestUtil.retrieveKey(aContext, MAPBOX_MAPID)
        if (temp != null && temp.length > 0) mapBoxMapId = temp
    }

    /**
     * Reads the access token from the manifest.
     */
    fun retrieveAccessToken(aContext: Context) {
        // Retrieve the MapId from the Manifest
        accessToken = ManifestUtil.retrieveKey(aContext, ACCESS_TOKEN)
    }

    fun setMapboxMapid(key: String?) {
        mapBoxMapId = key
    }

    override fun getTileURLString(pMapTileIndex: Long): String {
        val url = StringBuilder(baseUrl)
        url.append(this.mapBoxMapId)
        url.append("/")
        url.append(MapTileIndex.getZoom(pMapTileIndex))
        url.append("/")
        url.append(MapTileIndex.getX(pMapTileIndex))
        url.append("/")
        url.append(MapTileIndex.getY(pMapTileIndex))
        url.append(".png")
        url.append("?access_token=").append(this.accessToken)
        val res = url.toString()

        return res
    }

    companion object {
        /**
         * the meta data key in the manifest
         */
        //<meta-data android:name="MAPQUEST_MAPID" android:value="YOUR KEY" />
        private const val MAPBOX_MAPID = "MAPQUEST_MAPID"

        //<meta-data android:name="MAPQUEST_ACCESS_TOKEN" android:value="YOUR TOKEN" />
        private const val ACCESS_TOKEN = "MAPQUEST_ACCESS_TOKEN"

        private val mapBoxBaseUrl: Array<String?> = arrayOf<String?>("http://api.tiles.mapbox.com/v4/")
    }
}