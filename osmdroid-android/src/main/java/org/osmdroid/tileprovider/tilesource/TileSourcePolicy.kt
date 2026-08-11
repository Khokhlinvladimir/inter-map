package org.osmdroid.tileprovider.tilesource

import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.config.DefaultConfigurationProvider
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import java.net.HttpURLConnection

/**
 * Online Tile Source Usage Policy, including
 *  * the max number of concurrent downloads
 *  * if it accepts a meaningless user agent
 *  * if it accepts bulk downloads
 *  * if the user agent must be normalized
 *
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
open class TileSourcePolicy @JvmOverloads constructor(
    pMaxConcurrent: Int = 0,
    pFlags: Int = 0
) {
    /**
     * maximum number of concurrent downloads
     */
    val maxConcurrent: Int

    private val mFlags: Int

    init {
        this.maxConcurrent = pMaxConcurrent
        mFlags = pFlags
    }

    fun acceptsBulkDownload(): Boolean {
        return (mFlags and FLAG_NO_BULK) == 0
    }

    private fun acceptsMeaninglessUserAgent(): Boolean {
        return (mFlags and FLAG_USER_AGENT_MEANINGFUL) == 0
    }

    fun normalizesUserAgent(): Boolean {
        return (mFlags and FLAG_USER_AGENT_NORMALIZED) != 0
    }

    fun acceptsPreventive(): Boolean {
        return (mFlags and FLAG_NO_PREVENTIVE) == 0
    }

    fun acceptsUserAgent(pUserAgent: String?): Boolean {
        if (acceptsMeaninglessUserAgent()) {
            return true
        }
        return pUserAgent != null && pUserAgent.trim { it <= ' ' }.length > 0 && (pUserAgent != DefaultConfigurationProvider.Companion.DEFAULT_USER_AGENT)
    }

    /**
     * @return the Epoch timestamp corresponding to the http header (in milliseconds), or null
     * @since 6.1.7
     * Used to be in [TileDownloader]
     */
    fun getHttpExpiresTime(pHttpExpiresHeader: String?): Long? {
        if (pHttpExpiresHeader != null && pHttpExpiresHeader.length > 0) {
            try {
                val dateExpires = instance!!.httpHeaderDateTimeFormat!!.parse(pHttpExpiresHeader)
                return dateExpires!!.getTime()
            } catch (ex: Exception) {
                if (instance!!.isDebugMapTileDownloader) Log.d(
                    IMapView.LOGTAG,
                    "Unable to parse expiration tag for tile, server returned " + pHttpExpiresHeader,
                    ex
                )
            }
        }
        return null
    }

    /**
     * @return the max-age corresponding to the http header (in seconds), or null
     * @since 6.1.7
     * Used to be in [TileDownloader]
     */
    fun getHttpCacheControlDuration(pHttpCacheControlHeader: String?): Long? {
        if (pHttpCacheControlHeader != null && pHttpCacheControlHeader.length > 0) {
            try {
                val parts = pHttpCacheControlHeader.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val maxAge = "max-age="
                for (part in parts) {
                    val pos = part.indexOf(maxAge)
                    if (pos == 0) {
                        val durationString = part.substring(maxAge.length)
                        return durationString.toLong()
                    }
                }
            } catch (ex: Exception) {
                if (instance!!.isDebugMapTileDownloader) Log.d(
                    IMapView.LOGTAG,
                    "Unable to parse cache control tag for tile, server returned " + pHttpCacheControlHeader, ex
                )
            }
        }
        return null
    }

    /**
     * @return the expiration time (as Epoch timestamp in milliseconds)
     * @since 6.1.7
     * Used to be in [TileDownloader]
     */
    open fun computeExpirationTime(pHttpExpiresHeader: String?, pHttpCacheControlHeader: String?, pNow: Long): Long {
        val override = instance!!.expirationOverrideDuration
        if (override != null) {
            return pNow + override
        }

        val extension = instance!!.expirationExtendedDuration
        val cacheControlDuration = getHttpCacheControlDuration(pHttpCacheControlHeader)
        if (cacheControlDuration != null) {
            return pNow + cacheControlDuration * 1000 + extension
        }

        val httpExpiresTime = getHttpExpiresTime(pHttpExpiresHeader)
        if (httpExpiresTime != null) {
            return httpExpiresTime + extension
        }

        return pNow + OpenStreetMapTileProviderConstants.DEFAULT_MAXIMUM_CACHED_FILE_AGE + extension
    }

    /**
     * @return the expiration time (as Epoch timestamp in milliseconds)
     * @since 6.1.7
     */
    open fun computeExpirationTime(pHttpURLConnection: HttpURLConnection, pNow: Long): Long {
        val expires = pHttpURLConnection.getHeaderField(OpenStreetMapTileProviderConstants.HTTP_EXPIRES_HEADER)
        val cacheControl = pHttpURLConnection.getHeaderField(OpenStreetMapTileProviderConstants.HTTP_CACHE_CONTROL_HEADER)
        val result = computeExpirationTime(expires, cacheControl, pNow)
        if (instance!!.isDebugMapTileDownloader) {
            Log.d(IMapView.LOGTAG, "computeExpirationTime('" + expires + "','" + cacheControl + "'," + pNow + "=" + result)
        }
        return result
    }

    companion object {
        /**
         * No bulk downloads allowed
         */
        const val FLAG_NO_BULK: Int = 1

        /**
         * Don't try to preventively download tiles that aren't currently displayed
         */
        const val FLAG_NO_PREVENTIVE: Int = 2

        /**
         * Demands a user agent different from the default value
         */
        const val FLAG_USER_AGENT_MEANINGFUL: Int = 4

        /**
         * Uses the "normalized" user agent (package name + version)
         */
        const val FLAG_USER_AGENT_NORMALIZED: Int = 8
    }
}
