package org.osmdroid.util.constants

import org.osmdroid.R

interface GeoConstants {

    enum class UnitOfMeasure(val conversionFactorToMeters: Double, val stringResId: Int) {
        Meter(1.0, R.string.format_distance_only_meter),
        Kilometer(1000.0, R.string.format_distance_only_kilometer),
        StatuteMile(METERS_PER_STATUTE_MILE, R.string.format_distance_only_mile),
        NauticalMile(METERS_PER_NAUTICAL_MILE, R.string.format_distance_only_nautical_mile),
        Foot(1 / FEET_PER_METER, R.string.format_distance_only_foot);

    }

    companion object {
        const val RADIUS_EARTH_METERS = 6378137 // http://en.wikipedia.org/wiki/Earth_radius#Equatorial_radius
        const val METERS_PER_STATUTE_MILE = 1609.344 // http://en.wikipedia.org/wiki/Mile
        const val METERS_PER_NAUTICAL_MILE = 1852.0 // http://en.wikipedia.org/wiki/Nautical_mile
        const val FEET_PER_METER = 3.2808399 // http://en.wikipedia.org/wiki/Feet_%28unit_of_length%29

        @Deprecated("")
        val EQUATORCIRCUMFENCE = (2 * Math.PI * RADIUS_EARTH_METERS).toInt()
    }
}