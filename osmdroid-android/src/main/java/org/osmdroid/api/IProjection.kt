package org.osmdroid.api

import android.graphics.Point

/**
 * An interface that resembles the Google Maps API Projection interface and is implemented by the
 * osmdroid [org.osmdroid.views.Projection] class.
 *
 * @author Neil Boyd
 */
interface IProjection {
    /**
     * Converts the given [IGeoPoint] to onscreen pixel coordinates, relative to the top-left
     * of the [org.osmdroid.views.MapView] that provided this Projection.
     *
     * @param in  The latitude/longitude pair to convert.
     * @param out A pre-existing object to use for the output; if null, a new Point will be
     * allocated and returned.
     */
    fun toPixels(`in`: IGeoPoint?, out: Point?): Point?

    /**
     * Create a new GeoPoint from pixel coordinates relative to the top-left of the MapView that
     * provided this PixelConverter.
     */
    fun fromPixels(x: Int, y: Int): IGeoPoint?

    /**
     * Converts a distance in meters (along the equator) to one in (horizontal) pixels at the
     * current zoomlevel. In the default Mercator projection, the actual number of pixels for a
     * given distance will get higher as you move away from the equator.
     *
     * @param meters the distance in meters
     * @return The number of pixels corresponding to the distance, if measured along the equator, at
     * the current zoom level. The return value may only be approximate.
     */
    fun metersToEquatorPixels(meters: Float): Float

    /**
     * Get the coordinates of the most north-easterly visible point of the map.
     */
    val northEast: IGeoPoint?

    /**
     * Get the coordinates of the most south-westerly visible point of the map.
     */
    val southWest: IGeoPoint?
}