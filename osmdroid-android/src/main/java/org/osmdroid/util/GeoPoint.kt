// Created by plusminus on 21:28:12 - 25.09.2008
package org.osmdroid.util

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.constants.GeoConstants
import org.osmdroid.views.util.constants.MathConstants
import java.io.Serializable
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * @author Nicolas Gramlich
 * @author Theodore Hong
 */
open class GeoPoint : IGeoPoint, MathConstants, GeoConstants, Parcelable, Serializable, Cloneable {
    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    override var longitude: Double
    override var latitude: Double
    var altitude: Double = 0.0

    // ===========================================================
    // Constructors
    // ===========================================================
    @Deprecated("")
    constructor(aLatitudeE6: Int, aLongitudeE6: Int) {
        this.latitude = aLatitudeE6 / 1E6
        this.longitude = aLongitudeE6 / 1E6
    }

    @Deprecated("")
    constructor(aLatitudeE6: Int, aLongitudeE6: Int, aAltitude: Int) {
        this.latitude = aLatitudeE6 / 1E6
        this.longitude = aLongitudeE6 / 1E6
        this.altitude = aAltitude.toDouble()
    }

    constructor(aLatitude: Double, aLongitude: Double) {
        this.latitude = aLatitude
        this.longitude = aLongitude
    }

    constructor(aLatitude: Double, aLongitude: Double, aAltitude: Double) {
        this.latitude = aLatitude
        this.longitude = aLongitude
        this.altitude = aAltitude
    }

    constructor(aLocation: Location) : this(aLocation.getLatitude(), aLocation.getLongitude(), aLocation.getAltitude())

    constructor(aGeopoint: GeoPoint) {
        this.latitude = aGeopoint.latitude
        this.longitude = aGeopoint.longitude
        this.altitude = aGeopoint.altitude
    }

    /**
     * @since 6.0.3
     */
    constructor(pGeopoint: IGeoPoint) {
        this.latitude = pGeopoint.latitude
        this.longitude = pGeopoint.longitude
    }

    fun setCoords(aLatitude: Double, aLongitude: Double) {
        this.latitude = aLatitude
        this.longitude = aLongitude
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    public override fun clone(): GeoPoint {
        return GeoPoint(this.latitude, this.longitude, this.altitude)
    }

    fun toIntString(): String {
        return StringBuilder().append((this.latitude * 1E6).toInt())
            .append(",")
            .append((this.longitude * 1E6).toInt())
            .append(",")
            .append(this.altitude.toInt())
            .toString()
    }

    override fun toString(): String {
        return StringBuilder().append(this.latitude).append(",").append(this.longitude).append(",").append(this.altitude)
            .toString()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (obj === this) {
            return true
        }
        if (obj.javaClass != javaClass) {
            return false
        }
        val rhs = obj as GeoPoint
        return rhs.latitude == this.latitude && rhs.longitude == this.longitude && rhs.altitude == this.altitude
    }

    override fun hashCode(): Int {
        return 37 * (17 * (this.latitude * 1E-6).toInt() + (this.longitude * 1E-6).toInt()) + altitude.toInt()
    }

    // ===========================================================
    // Parcelable
    // ===========================================================
    private constructor(`in`: Parcel) {
        this.latitude = `in`.readDouble()
        this.longitude = `in`.readDouble()
        this.altitude = `in`.readDouble()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeDouble(this.latitude)
        out.writeDouble(this.longitude)
        out.writeDouble(this.altitude)
    }

    // ===========================================================
    // Methods
    // ===========================================================
    /**
     * @return distance in meters
     * @see [Haversine formula](https://en.wikipedia.org/wiki/Haversine_formula)
     *
     * @see [GIS FAQ](http://www.movable-type.co.uk/scripts/gis-faq-5.1.html)
     *
     * @since 6.0.0
     */
    fun distanceToAsDouble(other: IGeoPoint): Double {
        val lat1 = MathConstants.Companion.DEG2RAD * latitude
        val lat2 = MathConstants.Companion.DEG2RAD * other.latitude
        val lon1 = MathConstants.Companion.DEG2RAD * longitude
        val lon2 = MathConstants.Companion.DEG2RAD * other.longitude
        return GeoConstants.Companion.RADIUS_EARTH_METERS * 2 * asin(
            min(
                1.0, sqrt(
                    sin((lat2 - lat1) / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2).pow(2.0)
                )
            )
        )
    }

    /**
     * @return bearing in degrees
     * @see [discussion](http://groups.google.com/group/osmdroid/browse_thread/thread/d22c4efeb9188fe9/bc7f9b3111158dd)
     */
    fun bearingTo(other: IGeoPoint): Double {
        val lat1 = Math.toRadians(this.latitude)
        val long1 = Math.toRadians(this.longitude)
        val lat2 = Math.toRadians(other.latitude)
        val long2 = Math.toRadians(other.longitude)
        val delta_long = long2 - long1
        val a = sin(delta_long) * cos(lat2)
        val b = cos(lat1) * sin(lat2) -
                sin(lat1) * cos(lat2) * cos(delta_long)
        val bearing = Math.toDegrees(atan2(a, b))
        val bearing_normalized = (bearing + 360) % 360
        return bearing_normalized
    }

    /**
     * Calculate a point that is the specified distance and bearing away from this point.
     *
     * @see [latlong.html](http://www.movable-type.co.uk/scripts/latlong.html)
     *
     * @see [latlon.js](http://www.movable-type.co.uk/scripts/latlon.js)
     */
    fun destinationPoint(aDistanceInMeters: Double, aBearingInDegrees: Double): GeoPoint {
        // convert distance to angular distance

        val dist = aDistanceInMeters / GeoConstants.Companion.RADIUS_EARTH_METERS

        // convert bearing to radians
        val brng = MathConstants.Companion.DEG2RAD * aBearingInDegrees

        // get current location in radians
        val lat1 = MathConstants.Companion.DEG2RAD * latitude
        val lon1 = MathConstants.Companion.DEG2RAD * longitude

        val lat2 = asin(sin(lat1) * cos(dist) + (cos(lat1) * sin(dist) * cos(brng)))
        val lon2 = (lon1
                + atan2(sin(brng) * sin(dist) * cos(lat1), cos(dist) - sin(lat1) * sin(lat2)))

        val lat2deg = lat2 / MathConstants.Companion.DEG2RAD
        val lon2deg = lon2 / MathConstants.Companion.DEG2RAD

        return GeoPoint(lat2deg, lon2deg)
    }

    fun toDoubleString(): String {
        return StringBuilder().append(this.latitude).append(",")
            .append(this.longitude).append(",").append(this.altitude).toString()
    }

    fun toInvertedDoubleString(): String {
        return StringBuilder().append(this.longitude).append(",")
            .append(this.latitude).append(",").append(this.altitude).toString()
    }

    @get:Deprecated("")
    override val latitudeE6: Int
        // ===========================================================
        get() = (this.latitude * 1E6).toInt()

    @get:Deprecated("")
    override val longitudeE6: Int
        get() = (this.longitude * 1E6).toInt()

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        const val serialVersionUID: Long = 1L

        @JvmStatic
        fun fromDoubleString(s: String, spacer: Char): GeoPoint {
            val spacerPos1 = s.indexOf(spacer)
            val spacerPos2 = s.indexOf(spacer, spacerPos1 + 1)

            if (spacerPos2 == -1) {
                return GeoPoint(
                    (s.substring(0, spacerPos1).toDouble()),
                    (s.substring(spacerPos1 + 1, s.length).toDouble())
                )
            } else {
                return GeoPoint(
                    (s.substring(0, spacerPos1).toDouble()),
                    (s.substring(spacerPos1 + 1, spacerPos2).toDouble()),
                    s.substring(spacerPos2 + 1, s.length).toDouble()
                )
            }
        }

        @JvmStatic
        fun fromInvertedDoubleString(s: String, spacer: Char): GeoPoint {
            val spacerPos1 = s.indexOf(spacer)
            val spacerPos2 = s.indexOf(spacer, spacerPos1 + 1)

            if (spacerPos2 == -1) {
                return GeoPoint(
                    s.substring(spacerPos1 + 1, s.length).toDouble(),
                    s.substring(0, spacerPos1).toDouble()
                )
            } else {
                return GeoPoint(
                    s.substring(spacerPos1 + 1, spacerPos2).toDouble(),
                    s.substring(0, spacerPos1).toDouble(),
                    s.substring(spacerPos2 + 1, s.length).toDouble()
                )
            }
        }

        @Deprecated("")
        @JvmStatic
        fun fromIntString(s: String): GeoPoint {
            val commaPos1 = s.indexOf(',')
            val commaPos2 = s.indexOf(',', commaPos1 + 1)

            if (commaPos2 == -1) {
                return GeoPoint(
                    s.substring(0, commaPos1).toInt(),
                    s.substring(commaPos1 + 1, s.length).toInt()
                )
            } else {
                return GeoPoint(
                    s.substring(0, commaPos1).toInt(),
                    s.substring(commaPos1 + 1, commaPos2).toInt(),
                    s.substring(commaPos2 + 1, s.length).toInt()
                )
            }
        }

        @JvmField
        val CREATOR: Creator<GeoPoint> = object : Creator<GeoPoint> {
            override fun createFromParcel(`in`: Parcel): GeoPoint {
                return GeoPoint(`in`)
            }

            override fun newArray(size: Int): Array<GeoPoint?> {
                return arrayOfNulls<GeoPoint>(size)
            }
        }

        @JvmStatic
        fun fromCenterBetween(geoPointA: GeoPoint, geoPointB: GeoPoint): GeoPoint {
            return GeoPoint(
                (geoPointA.latitude + geoPointB.latitude) / 2,
                (geoPointA.longitude + geoPointB.longitude) / 2
            )
        }
    }
}
