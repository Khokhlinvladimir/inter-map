package org.osmdroid.config

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.net.Proxy
import java.text.SimpleDateFormat

/**
 * Singleton class to get/set a configuration provider for osmdroid
 * [Issue 481](https://github.com/osmdroid/osmdroid/issues/481)
 * Created on 11/29/2016.
 *
 * @author Alex O'Ree
 * @see org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
 *
 * @since 5.6
 */
interface IConfigurationProvider {
    /**
     * The time we wait after the last gps location before using a non-gps location.
     * was previously at org.osmdroid.util.constants.UtilConstants
     *
     * @return time in ms
     */
    /**
     * The time we wait after the last gps location before using a non-gps location.
     *
     * @param gpsWaitTime
     */
    var gpsWaitTime: Long

    /**
     * Typically used to enable additional debugging
     * from [org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants]
     *
     * @return
     */
    var isDebugMode: Boolean

    /**
     * Typically used to enable additional debugging
     *
     * @return
     */
    var isDebugMapView: Boolean

    /**
     * Typically used to enable additional debugging
     * from [org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants]
     *
     * @return
     */
    var isDebugTileProviders: Boolean
    var isDebugMapTileDownloader: Boolean
    /**
     * default is false
     *
     * @return
     */
    /**
     * must be set before the mapview is created or inflated from a layout.
     * If you're only using single point icons, then you can probably get away with setting this to true
     * otherwise (using polylines, paths, polygons) set it to false.
     *
     *
     * default is false
     *
     * @param mapViewHardwareAccelerated
     * @see org.osmdroid.views.overlay.Polygon
     *
     * @see org.osmdroid.views.overlay.Polyline
     */
    var isMapViewHardwareAccelerated: Boolean

    /**
     * Enables you to override the default "osmdroid" value for HTTP user agents. Used when downloading tiles
     *
     *
     *
     *
     * You MUST use this to set the user agent to some value specific to your application.
     * Typical usage: Context.getApplicationContext().getPackageName();
     * from [org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants]
     *
     * @param userAgentValue
     * @since 5.0
     */
    var userAgentValue: String?

    /**
     * Enables you to set and get additional HTTP request properties. Used when downloading tiles.
     * Mustn't be null, but will be empty in most cases.
     *
     *
     * A simple use case would be:
     * Configuration.getInstance().getAdditionalHttpRequestProperties().put("Origin", "http://www.example-social-network.com");
     *
     *
     * See https://github.com/osmdroid/osmdroid/issues/570
     *
     * @since 5.6.5
     */
    val additionalHttpRequestProperties: Map<String?, String?>?
    /**
     * Initial tile cache size (in memory). The size will be increased as required by calling
     * [MapTileCache.ensureCapacity] The tile cache will always be at least 3x3.
     * from [org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants]
     * used by MapTileCache
     *
     * @return
     * @see MapTileCache
     */
    /**
     * Initial tile cache size (in memory). The size will be increased as required by calling
     * [MapTileCache.ensureCapacity] The tile cache will always be at least 3x3.
     * from [org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants]
     * used by MapTileCache
     *
     * @param cacheMapTileCount
     * @see MapTileCache
     */
    var cacheMapTileCount: Short

    /**
     * number of tile download threads, conforming to OSM policy:
     * http://wiki.openstreetmap.org/wiki/Tile_usage_policy
     * default is 2
     */
    var tileDownloadThreads: Short
    /**
     * used for both file system cache and the sqlite cache
     *
     * @return
     */
    /**
     * used for both file system cache and the sqlite cache
     *
     * @param tileFileSystemThreads
     */
    var tileFileSystemThreads: Short
    var tileDownloadMaxQueueSize: Short
    var tileFileSystemMaxQueueSize: Short

    /**
     * default is 600 Mb
     */
    var tileFileSystemCacheMaxBytes: Long

    /**
     * When the cache size exceeds maxCacheSize, tiles will be automatically removed to reach this target. In bytes. Default is 500 Mb.
     *
     * @return
     */
    var tileFileSystemCacheTrimBytes: Long
    var httpHeaderDateTimeFormat: SimpleDateFormat?
    var httpProxy: Proxy?
    /**
     * Base path for osmdroid files. Zip/sqlite/mbtiles/etc files are in this folder.
     * Note: also used for offline tile sources
     *
     *
     * If no directory has been set before with [.setOsmdroidBasePath] it tries
     * to automatically detect one. On API>29 and for better results use
     * [.getOsmdroidBasePath]
     *
     * @return
     */
    /**
     * Base path for osmdroid files. Zip/sqlite/mbtiles/etc files are in this folder.
     * Note: also used for offline tile sources
     *
     *
     * Default is
     * StorageUtils.getStorage().getAbsolutePath(),"osmdroid", which usually maps to /sdcard/osmdroid
     *
     * @param osmdroidBasePath
     */
    var osmdroidBasePath: File?

    /**
     * Base path for osmdroid files. Zip/sqlite/mbtiles/etc files are in this folder.
     * Note: also used for offline tile sources
     *
     *
     * If no directory has been set before with [.setOsmdroidBasePath] it tries
     * to automatically detect one. Passing a context gives better results than
     * [.getOsmdroidBasePath] and is required to find any location on API29.
     *
     * @return
     */
    fun getOsmdroidBasePath(context: Context?): File?
    /**
     * by default, maps to getOsmdroidBasePath() + "/tiles"
     * By default, it is defined in SD card, osmdroid directory.
     * Sets the location where the tile cache is stored. Changes are only in effect when the [{]
     * is created. Changes made after it's creation (either pogrammatic or via layout inflator) have
     * no effect until the map is restarted or the [org.osmdroid.views.MapView.setTileProvider]
     * is changed or recreated.
     *
     *
     * Note: basePath and tileCache directories can be changed independently
     * This has no effect on offline archives and can be changed independently
     *
     *
     * If no directory has been set before with [.setOsmdroidTileCache] it tries
     * to automatically detect one. On API>29 and for better results use
     * [.getOsmdroidTileCache]
     *
     * @return
     */
    /**
     * by default, maps to getOsmdroidBasePath() + "/tiles"
     * Sets the location where the tile cache is stored. Changes are only in effect when the @{link [org.osmdroid.views.MapView]}
     * is created. Changes made after it's creation (either pogrammatic or via layout inflator) have
     * no effect until the map is restarted or the [org.osmdroid.views.MapView.setTileProvider]
     * is changed or recreated.
     *
     *
     * This has no effect on offline archives and can be changed independently
     *
     * @param osmdroidTileCache
     */
    var osmdroidTileCache: File?

    /**
     * by default, maps to getOsmdroidBasePath() + "/tiles"
     * By default, it is defined in SD card, osmdroid directory.
     * Sets the location where the tile cache is stored. Changes are only in effect when the [org.osmdroid.views.MapView]}
     * is created. Changes made after it's creation (either pogrammatic or via layout inflator) have
     * no effect until the map is restarted or the [org.osmdroid.views.MapView.setTileProvider]
     * is changed or recreated.
     *
     *
     * Note: basePath and tileCache directories can be changed independently
     * This has no effect on offline archives and can be changed independently
     *
     *
     * If no directory has been set before with [.setOsmdroidTileCache] it tries
     * to automatically detect one. Passing a context gives better results than
     * [.getOsmdroidTileCache] and is required to find any location on API29.
     *
     * @return
     */
    fun getOsmdroidTileCache(context: Context?): File?
    /**
     * "User-Agent" is the default value and standard used throughout all http servers, unlikely to change
     * When calling @link [.load], it is set to
     * [Context.getPackageName] which is defined your manifest file
     *
     *
     * made adjustable just in case
     * from [org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants]
     *
     * @return
     */
    /**
     * "User-Agent" is the default value and standard used throughout all http servers, unlikely to change
     * When calling @link [.load], it is set to
     * [Context.getPackageName] which is defined your manifest file
     *
     *
     * made adjustable just in case
     * from [org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants]
     */
    var userAgentHttpHeader: String?

    /**
     * loads the configuration from shared preferences, if the preferences defined in this file are not already
     * set, them they will be populated with defaults. This also initializes the tile storage cache to
     * the largested writable storage partition available.
     *
     * @param ctx
     * @param preferences
     */
    fun load(ctx: Context?, preferences: SharedPreferences?)

    /**
     * saves the current configuration to the shared preference location
     *
     * @param ctx
     * @param preferences
     */
    fun save(ctx: Context?, preferences: SharedPreferences?)
    /**
     * Returns the amount of time in ms added to server specified tile expiration time
     * Added as part of issue https://github.com/osmdroid/osmdroid/issues/490
     *
     * @return time in ms
     * @since 5.6.1
     */
    /**
     * Optionally extends the amount of time that downloaded tiles remain in the cache beyond either the
     * server specified expiration time stamp or the default expiration time {[org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.DEFAULT_MAXIMUM_CACHED_FILE_AGE]}
     *
     *
     * Note: this setting only controls tiles as they are downloaded. tiles already in the cache are
     * not effected by this setting
     * Added as part of issue https://github.com/osmdroid/osmdroid/issues/490
     *
     * @param period time in ms, if 0, no additional time to the 'server provided expiration' or the
     * 'default expiration time' is added. If the value is less than 0, 0 will be used
     * @since 5.6.1
     */
    var expirationExtendedDuration: Long
    /**
     * Optional period of time in ms that will override any downloaded tile's expiration timestamp
     *
     * @return period if null, this setting is unset, server value + getExpirationExtendedDuration apply
     * if not null, this this value is used
     * @since 5.6.1
     */
    /**
     * Optional period of time in ms that will override any downloaded tile's expiration timestamp
     *
     * @param period if null, this setting is unset, server value + getExpirationExtendedDuration apply
     * if not null, this this value is used
     * @since 5.6.1
     */
    var expirationOverrideDuration: Long?
    /**
     * Used during zoom animations
     * https://github.com/osmdroid/osmdroid/issues/650
     *
     * @return
     * @since 6.0.0
     */
    /**
     * Used during zoom animations
     * https://github.com/osmdroid/osmdroid/issues/650
     *
     * @param durationsMilliseconds
     * @since 6.0.0
     */
    var animationSpeedDefault: Int
    /**
     * Used during zoom animations
     * https://github.com/osmdroid/osmdroid/issues/650
     *
     * @return
     * @since 6.0.0
     */
    /**
     * Used during zoom animations
     * https://github.com/osmdroid/osmdroid/issues/650
     *
     * @param durationsMilliseconds
     * @since 6.0.0
     */
    var animationSpeedShort: Int
    /**
     * If true, the map view will set .setHasTransientState(true) for API 16+ devices.
     * This is now the default setting. Set to false if this is causing you issues
     *
     * @return
     * @since 6.0.0
     */
    /**
     * If true, the map view will set .setHasTransientState(true) for API 16+ devices.
     * This is now the default setting. Set to false if this is causing you issues
     *
     * @return
     * @since 6.0.0
     */
    var isMapViewRecyclerFriendly: Boolean
    /**
     * In memory tile count, used by the tiles overlay
     *
     * @return
     * @since 6.0.0
     */
    /**
     * In memory tile count, used by the tiles overlay
     *
     * @param value
     * @see org.osmdroid.views.overlay.TilesOverlay
     *
     * @since 6.0.0
     */
    var cacheMapTileOvershoot: Short
    /**
     * Delay between tile garbage collection calls
     *
     * @since 6.0.2
     */
    /**
     * @since 6.0.2
     */
    var tileGCFrequencyInMillis: Long
    /**
     * Tile garbage collection bulk size
     *
     * @since 6.0.2
     */
    /**
     * @since 6.0.2
     */
    var tileGCBulkSize: Int
    /**
     * Pause during tile garbage collection bulk deletions
     *
     * @since 6.0.2
     */
    /**
     * @since 6.0.2
     */
    var tileGCBulkPauseInMillis: Long

    /**
     * enables/disables tile downloading following redirects. default is true
     *
     * @param value
     * @since 6.0.2
     */
    var isMapTileDownloaderFollowRedirects: Boolean

    /**
     * @since 6.1.0
     */
    val normalizedUserAgent: String?
    /**
     * Default is false for the DefaultConfigurationProvider<br></br><br></br>
     * If true and a bounding box is beyond that of the [org.osmdroid.util.TileSystem],
     * then an exception is thrown by checks in [org.osmdroid.util.BoundingBox]
     * <br></br><br></br>
     * If false, then no exception is thrown.<br></br><br></br>
     * Historical note. Prior to late Feb 2018, which could have been around v6.0.2,
     * the behavior was to NOT throw an exception, Starting with 6.0.2, it starting throwing.
     * This caused a number of issues when importing content from other sources.<br></br>
     * July 2022, this method was added to help reduce the pain associated with this with
     * the default set to false (do not throw).<br></br><br></br>
     *
     * Keep in mind, that coordinates beyond that of the tile system may render inaccurately or
     * have strange behavior.
     *
     * @since 6.1.14
     * @return true = throw an exception when the bounding box is beyond the tile system, false = do not throw
     */
    /**
     * See [.isEnforceTileSystemBounds].
     * @since 6.1.14
     * @param mValue
     */
    var isEnforceTileSystemBounds: Boolean
}