package org.osmdroid.tileprovider.tilesource

import android.content.Context
import org.osmdroid.tileprovider.util.ManifestUtil
import org.osmdroid.util.MapTileIndex

/**
 * HERE We Go
 *
 * @since 5.3
 * Created by alex on 8/11/16.
 */
class HEREWeGoTileSource : OnlineTileSourceBase {
    var herewegoMapId: String? = "hybrid.day"
        private set
    var appId: String? = ""
    var appCode: String? = ""
    private var domainOverride = "aerial.maps.cit.api.here.com"

    /**
     * Creates a MapBox TileSource. You won't be able to use it until you set the access token and map id.
     */
    constructor() : super("herewego", 1, 20, 256, ".png", mapBoxBaseUrl, COPYRIGHT)

    /**
     * creates a new mapbox tile source, loading the access token and mapid from the manifest
     *
     * @param ctx
     * @since 5.1
     */
    constructor(ctx: Context) : super("herewego", 1, 20, 256, ".png", mapBoxBaseUrl, COPYRIGHT) {
        retrieveAppId(ctx)
        retrieveMapBoxMapId(ctx)
        retrieveAppCode(ctx)
        retrieveDomainOverride(ctx)
        //this line will ensure uniqueness in the tile cache
        mName = "herewego" + herewegoMapId
    }

    private fun retrieveDomainOverride(aContext: Context) {
        val temp = ManifestUtil.retrieveKey(aContext, HEREWEGO_DOMAIN_OVERRIDE)
        if (temp != null && temp.length > 0) domainOverride = temp
    }

    fun setDomainOverride(hostname: String) {
        domainOverride = hostname
    }

    /**
     * creates a new mapbox tile source, using the specified access token and mapbox id
     *
     * @since 5.1
     */
    constructor(herewegoMapId: String?, accesstoken: String?, appCode: String?) : super(
        "herewego" + herewegoMapId,
        1,
        20,
        256,
        ".png",
        mapBoxBaseUrl,
        COPYRIGHT
    ) {
        this.appId = accesstoken
        this.herewegoMapId = herewegoMapId
        this.appCode = appCode
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
        COPYRIGHT
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
        arrayOf<String?>(mapBoxVersionBaseUrl), "© 1987 - 2017 HERE. All rights reserved."
    )

    fun retrieveAppCode(aContext: Context) {
        // Retrieve the MapId from the Manifest
        appCode = ManifestUtil.retrieveKey(aContext, APPCODE)
    }

    /**
     * Reads the mapbox map id from the manifest.<br></br>
     */
    fun retrieveMapBoxMapId(aContext: Context) {
        // Retrieve the MapId from the Manifest
        herewegoMapId = ManifestUtil.retrieveKey(aContext, HEREWEGO_MAPID)
    }

    /**
     * Reads the access token from the manifest.
     */
    fun retrieveAppId(aContext: Context) {
        // Retrieve the MapId from the Manifest
        appId = ManifestUtil.retrieveKey(aContext, HEREWEGO_APPID)
    }

    fun setHereWeGoMapid(key: String?) {
        herewegoMapId = key
        mName = "herewego" + herewegoMapId
    }

    override fun getTileURLString(pMapTileIndex: Long): String {
        val url = StringBuilder(baseUrl.orEmpty().replace("{domain}", domainOverride))
        url.append(this.herewegoMapId)
        url.append("/")
        url.append(MapTileIndex.getZoom(pMapTileIndex))
        url.append("/")
        url.append(MapTileIndex.getX(pMapTileIndex))
        url.append("/")
        url.append(MapTileIndex.getY(pMapTileIndex))
        url.append("/").append(tileSizePixels).append("/png8?")
        url.append("app_id=").append(this.appId)
        url.append("&app_code=").append(this.appCode)
        url.append("&lg=pt-BR")
        val res = url.toString()

        //System.out.println(res);
        return res
    }

    companion object {
        /**
         * the meta data key in the manifest
         */
        //<meta-data android:name="HEREWEGO_MAPID" android:value="YOUR KEY" />
        private const val HEREWEGO_MAPID = "HEREWEGO_MAPID"

        //<meta-data android:name="HEREWEGO_ACCESS_TOKEN" android:value="YOUR TOKEN" />
        private const val HEREWEGO_APPID = "HEREWEGO_APPID"

        //<meta-data android:name="HEREWEGO_APPCODE" android:value="YOUR TOKEN" />
        private const val APPCODE = "HEREWEGO_APPCODE"

        //<meta-data android:name="HEREWEGO_DOMAIN_OVERRIDE" android:value="aerial.maps.cit.api.here.com" />
        private const val HEREWEGO_DOMAIN_OVERRIDE = "HEREWEGO_OVERRIDE"

        private const val COPYRIGHT = "© 1987 - 2019 HERE. All rights reserved."
        private val mapBoxBaseUrl: Array<String?> = arrayOf<String?>(
            "https://1.{domain}/maptile/2.1/maptile/newest/",
            "https://2.{domain}/maptile/2.1/maptile/newest/",
            "https://3.{domain}/maptile/2.1/maptile/newest/",
            "https://4.{domain}/maptile/2.1/maptile/newest/"
        )
    }
}
