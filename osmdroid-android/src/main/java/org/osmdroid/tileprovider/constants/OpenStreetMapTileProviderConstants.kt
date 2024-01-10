package org.osmdroid.tileprovider.constants

/**
 * This class contains constants used by the tile provider.
 *
 * @author Neil Boyd
 */
object OpenStreetMapTileProviderConstants {
    /**
     * Minimum Zoom Level
     */
    const val MINIMUM_ZOOM_LEVEL = 0

    /**
     * add an extension to files on sdcard so that gallery doesn't index them
     */
    const val TILE_PATH_EXTENSION = ".tile"
    const val ONE_SECOND: Long = 1000
    const val ONE_MINUTE = ONE_SECOND * 60
    const val ONE_HOUR = ONE_MINUTE * 60
    const val ONE_DAY = ONE_HOUR * 24
    const val ONE_WEEK = ONE_DAY * 7
    const val ONE_YEAR = ONE_DAY * 365
    const val DEFAULT_MAXIMUM_CACHED_FILE_AGE = ONE_WEEK

    /**
     * default tile expiration time, only used if the server doesn't specify
     * 30 days
     */
    const val TILE_EXPIRY_TIME_MILLISECONDS = 1000L * 60 * 60 * 24 * 30

    /**
     * this is the expected http header to expect from a tile server
     *
     * @since 5.1
     */
    const val HTTP_EXPIRES_HEADER = "Expires"

    /**
     * @since 6.0.3
     */
    const val HTTP_CACHE_CONTROL_HEADER = "Cache-Control"

    /**
     * this is the default and expected http header for Expires, date time format that is used
     * for more http servers. Can be overridden via Configuration
     *
     * @since 5.1
     */
    const val HTTP_EXPIRES_HEADER_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"
}