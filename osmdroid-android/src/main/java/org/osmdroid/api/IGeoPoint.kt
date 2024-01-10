package org.osmdroid.api

/**
 * An interface that resembles the Google Maps API GeoPoint class.
 */
interface IGeoPoint {
    @get:Deprecated("")
    val latitudeE6: Int

    @get:Deprecated("")
    val longitudeE6: Int
    val latitude: Double
    val longitude: Double
}