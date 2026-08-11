package org.osmdroid.tileprovider.tilesource

import android.content.Context
import org.osmdroid.tileprovider.util.ManifestUtil
import org.osmdroid.util.MapTileIndex

/**
 * MapBox tile source, revised in 5.1 to not use static map ids or tokens
 *
 * @author Brad Leege bleege AT gmail.com
 * Created on 10/15/13 at 7:57 PM
 */
class MapBoxTileSource : OnlineTileSourceBase {
    var mapBoxMapId: String? = ""
        private set
    var accessToken: String? = null
    private var highDPI = ""

    /**
     * Creates a MapBox TileSource. You won't be able to use it until you set the access token and map id.
     */
    constructor() : super("mapbox", 1, 19, 256, ".png", mapBoxBaseUrl)

    /**
     * creates a new mapbox tile source, loading the access token and mapid from the manifest
     *
     * @param ctx
     * @since 5.1
     */
    constructor(ctx: Context) : super("mapbox", 1, 19, 256, ".png", mapBoxBaseUrl) {
        retrieveAccessToken(ctx)
        retrieveMapBoxMapId(ctx)
        //this line will ensure uniqueness in the tile cache
        mName = "mapbox" + mapBoxMapId
    }

    /**
     * creates a new mapbox tile source, using the specified access token and mapbox id
     *
     * @param mapboxid
     * @param accesstoken
     * @since 5.1
     */
    constructor(mapboxid: String?, accesstoken: String?) : super("mapbox", 1, 19, 256, ".png", mapBoxBaseUrl) {
        this.accessToken = accesstoken
        this.mapBoxMapId = mapboxid
        //this line will ensure uniqueness in the tile cache
        mName = "mapbox" + mapBoxMapId
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
        mapBoxBaseUrl
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
        name, zoomMinLevel, zoomMaxLevel, tileSizePixels, imageFilenameEnding,
        arrayOf<String?>(mapBoxVersionBaseUrl)
    )

    /**
     * Reads the mapbox map id from the manifest.<br></br>
     */
    fun retrieveMapBoxMapId(aContext: Context) {
        // Retrieve the MapId from the Manifest
        mapBoxMapId = ManifestUtil.retrieveKey(aContext, MAPBOX_MAPID)
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
        mName = "mapbox" + mapBoxMapId
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
        url.append(highDPI) //for high-DPI
        url.append(mImageFilenameEnding)
        url.append("?access_token=").append(this.accessToken)

        return url.toString()
    }

    fun enableHighDPI(isHighDPI: Boolean) {
        if (isHighDPI) {
            highDPI = "@2x"
        } else {
            highDPI = ""
        }
    }

    companion object {
        /**
         * the meta data key in the manifest
         */
        //<meta-data android:name="MAPBOX_MAPID" android:value="YOUR KEY" />
        private const val MAPBOX_MAPID = "MAPBOX_MAPID"

        //NOTE change as of 5.3, it was ACCESS_TOKEN in the manifest, it is now MAPBOX_ACCESS_TOKEN
        //<meta-data android:name="MAPBOX_ACCESS_TOKEN" android:value="YOUR TOKEN" />
        private const val ACCESS_TOKEN = "MAPBOX_ACCESS_TOKEN"

        private val mapBoxBaseUrl: Array<String?> = arrayOf<String?>("https://api.mapbox.com/v4/")
    }
}
