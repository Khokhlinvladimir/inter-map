// Created by plusminus on 19:06:38 - 25.09.2008
package org.osmdroid.util

import android.graphics.PointF
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration.instance
import org.osmdroid.views.MapView.Companion.getTileSystem
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @author Nicolas Gramlich
 * @author Andreas Schildbach
 */
class BoundingBox : Parcelable, Serializable {
    // ===========================================================
    // Fields
    // ===========================================================
    var latNorth: Double = 0.0
    var latSouth: Double = 0.0
    var lonEast: Double = 0.0
    var lonWest: Double = 0.0

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * @param north
     * @param east
     * @param south
     * @param west
     */
    constructor(north: Double, east: Double, south: Double, west: Double) {
        set(north, east, south, west)
    }

    /**
     * @since 6.0.2
     * In order to avoid longitude and latitude checks that will crash
     * in TileSystem configurations with a bounding box that doesn't include [0,0]
     */
    constructor()

    /**
     * @since 6.0.0
     */
    fun set(north: Double, east: Double, south: Double, west: Double) {
        this.latNorth = north
        this.lonEast = east
        this.latSouth = south
        this.lonWest = west
        //validate the values
        if (instance!!.isEnforceTileSystemBounds) {
            val tileSystem = getTileSystem()
            require(tileSystem.isValidLatitude(north)) { "north must be in " + tileSystem.toStringLatitudeSpan() }
            require(tileSystem.isValidLatitude(south)) { "south must be in " + tileSystem.toStringLatitudeSpan() }
            require(tileSystem.isValidLongitude(west)) { "west must be in " + tileSystem.toStringLongitudeSpan() }
            require(tileSystem.isValidLongitude(east)) { "east must be in " + tileSystem.toStringLongitudeSpan() }
        }
    }

    public fun clone(): BoundingBox {
        return BoundingBox(this.latNorth, this.lonEast, this.latSouth, this.lonWest)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as BoundingBox

        if (java.lang.Double.compare(this.latNorth, that.latNorth) != 0) return false
        if (java.lang.Double.compare(this.latSouth, that.latSouth) != 0) return false
        if (java.lang.Double.compare(this.lonEast, that.lonEast) != 0) return false
        return java.lang.Double.compare(this.lonWest, that.lonWest) == 0
    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long
        temp = java.lang.Double.doubleToLongBits(this.latNorth)
        result = (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(this.latSouth)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(this.lonEast)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(this.lonWest)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    /**
     * @return the BoundingBox enclosing this BoundingBox and bb2 BoundingBox
     */
    fun concat(bb2: BoundingBox): BoundingBox {
        return BoundingBox(
            max(this.latNorth, bb2.latNorth),
            max(this.lonEast, bb2.lonEast),
            min(this.latSouth, bb2.latSouth),
            min(this.lonWest, bb2.lonWest)
        )
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    @get:Deprecated("")
    val center: GeoPoint
        /**
         * Use [.getCenterWithDateLine] instead to take date line into consideration
         *
         * @return GeoPoint center of this BoundingBox
         */
        get() = GeoPoint(
            (this.latNorth + this.latSouth) / 2.0,
            (this.lonEast + this.lonWest) / 2.0
        )

    val centerWithDateLine: GeoPoint
        /**
         * This version takes into consideration the date line
         *
         * @since 6.0.0
         */
        get() = GeoPoint(this.centerLatitude, this.centerLongitude)

    val diagonalLengthInMeters: Double
        get() = GeoPoint(this.latNorth, this.lonWest).distanceToAsDouble(
            GeoPoint(
                this.latSouth, this.lonEast
            )
        )

    val centerLatitude: Double
        /**
         * @since 6.0.0
         */
        get() = (this.latNorth + this.latSouth) / 2.0

    val centerLongitude: Double
        /**
         * @since 6.0.0
         */
        get() = getCenterLongitude(this.lonWest, this.lonEast)

    val actualNorth: Double
        /**
         * @since 6.0.0
         */
        get() = max(this.latNorth, this.latSouth)

    val actualSouth: Double
        /**
         * @since 6.0.0
         */
        get() = min(this.latNorth, this.latSouth)

    val latitudeSpan: Double
        /**
         * Determines the height of the bounding box.
         *
         * @return latitude span in degrees
         */
        get() = abs(this.latNorth - this.latSouth)

    @get:Deprecated("use {@link #getLongitudeSpanWithDateLine()}")
    val longitudeSpan: Double
        get() = abs(this.lonEast - this.lonWest)

    val longitudeSpanWithDateLine: Double
        /**
         * Determines the width of the bounding box.
         *
         * @return longitude span in degrees
         */
        get() {
            if (this.lonEast > this.lonWest) return this.lonEast - this.lonWest
            else return this.lonEast - this.lonWest + 360
        }

    /**
     * @param aLatitude
     * @param aLongitude
     * @param reuse
     * @return relative position determined from the upper left corner.<br></br>
     * {0,0} would be the upper left corner. {1,1} would be the lower right corner. {1,0}
     * would be the lower left corner. {0,1} would be the upper right corner.
     */
    fun getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(
        aLatitude: Double, aLongitude: Double, reuse: PointF?
    ): PointF {
        val out = if (reuse != null) reuse else PointF()
        val y = ((this.latNorth - aLatitude) / this.latitudeSpan).toFloat()
        val x = 1 - ((this.lonEast - aLongitude) / this.longitudeSpan).toFloat()
        out.set(x, y)
        return out
    }

    fun getRelativePositionOfGeoPointInBoundingBoxWithExactGudermannInterpolation(
        aLatitude: Double, aLongitude: Double, reuse: PointF?
    ): PointF {
        val out = if (reuse != null) reuse else PointF()
        val y =
            ((MyMath.gudermannInverse(this.latNorth) - MyMath.gudermannInverse(aLatitude)) / (MyMath.gudermannInverse(this.latNorth) - MyMath.gudermannInverse(
                this.latSouth
            ))).toFloat()
        val x = 1 - ((this.lonEast - aLongitude) / this.longitudeSpan).toFloat()
        out.set(x, y)
        return out
    }

    fun getGeoPointOfRelativePositionWithLinearInterpolation(
        relX: Float,
        relY: Float
    ): GeoPoint {
        val tileSystem = getTileSystem()
        val lat = this.latNorth - (this.latitudeSpan * relY)
        val lon = this.lonWest + (this.longitudeSpan * relX)
        return GeoPoint(tileSystem.cleanLatitude(lat), tileSystem.cleanLongitude(lon))
    }

    fun getGeoPointOfRelativePositionWithExactGudermannInterpolation(
        relX: Float,
        relY: Float
    ): GeoPoint {
        val tileSystem = getTileSystem()
        val gudNorth = MyMath.gudermannInverse(this.latNorth)
        val gudSouth = MyMath.gudermannInverse(this.latSouth)
        val lat = MyMath.gudermann((gudSouth + (1 - relY) * (gudNorth - gudSouth)))
        val lon = this.lonWest + (this.longitudeSpan * relX)
        return GeoPoint(tileSystem.cleanLatitude(lat), tileSystem.cleanLongitude(lon))
    }

    /**
     * Scale this bounding box by a given factor.
     *
     * @param pBoundingboxPaddingRelativeScale scale factor
     * @return scaled bounding box
     */
    fun increaseByScale(pBoundingboxPaddingRelativeScale: Float): BoundingBox {
        require(!(pBoundingboxPaddingRelativeScale <= 0)) { "pBoundingboxPaddingRelativeScale must be positive" }
        val tileSystem = getTileSystem()
        // out-of-bounds latitude will be clipped
        val latCenter = this.centerLatitude
        val latSpanHalf = this.latitudeSpan / 2 * pBoundingboxPaddingRelativeScale
        val latNorth = tileSystem.cleanLatitude(latCenter + latSpanHalf)
        val latSouth = tileSystem.cleanLatitude(latCenter - latSpanHalf)
        // out-of-bounds longitude will be wrapped around
        val lonCenter = this.centerLongitude
        val lonSpanHalf = this.longitudeSpanWithDateLine / 2 * pBoundingboxPaddingRelativeScale
        val latEast = tileSystem.cleanLongitude(lonCenter + lonSpanHalf)
        val latWest = tileSystem.cleanLongitude(lonCenter - lonSpanHalf)
        return BoundingBox(latNorth, latEast, latSouth, latWest)
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun toString(): String {
        return StringBuffer().append("N:").append(this.latNorth).append("; E:")
            .append(this.lonEast).append("; S:").append(this.latSouth).append("; W:")
            .append(this.lonWest).toString()
    }

    // ===========================================================
    // Methods
    // ===========================================================
    fun bringToBoundingBox(aLatitude: Double, aLongitude: Double): GeoPoint {
        return GeoPoint(
            max(this.latSouth, min(this.latNorth, aLatitude)),
            max(this.lonWest, min(this.lonEast, aLongitude))
        )
    }

    fun contains(pGeoPoint: IGeoPoint): Boolean {
        return contains(pGeoPoint.latitude, pGeoPoint.longitude)
    }

    fun contains(aLatitude: Double, aLongitude: Double): Boolean {
        var latMatch = false
        var lonMatch = false
        //FIXME there's still issues when there's multiple wrap arounds
        if (this.latNorth < this.latSouth) {
            //either more than one world/wrapping or the bounding box is wrongish
            latMatch = true
        } else {
            //normal case
            latMatch = ((aLatitude < this.latNorth) && (aLatitude > this.latSouth))
        }


        if (this.lonEast < this.lonWest) {
            //check longitude bounds with consideration for date line with wrapping
            lonMatch = aLongitude <= this.lonEast && aLongitude >= this.lonWest

            //lonMatch = (aLongitude >= mLonEast || aLongitude <= mLonWest);
        } else {
            lonMatch = ((aLongitude < this.lonEast) && (aLongitude > this.lonWest))
        }

        return latMatch && lonMatch
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, arg1: Int) {
        out.writeDouble(this.latNorth)
        out.writeDouble(this.lonEast)
        out.writeDouble(this.latSouth)
        out.writeDouble(this.lonWest)
    }

    @get:Deprecated("")
    val latitudeSpanE6: Int
        get() = (this.latitudeSpan * 1E6).toInt()

    @get:Deprecated("")
    val longitudeSpanE6: Int
        get() = (this.longitudeSpan * 1E6).toInt()

    /**
     * returns true if there is any overlap from this to the input bounding box
     * edges includes of a match
     * sensitive to vertical and horiztonal map wrapping
     *
     * @param pBoundingBox
     * @return
     */
    fun overlaps(pBoundingBox: BoundingBox, pZoom: Double): Boolean {
        //FIXME this is a total hack but it works around a number of issues related to vertical map
        //replication and horiztonal replication that can cause polygons to completely disappear when
        //panning

        if (pZoom < 3) return true

        var latMatch = false
        var lonMatch = false

        //vertical wrapping detection
        if (pBoundingBox.latSouth <= this.latNorth &&
            pBoundingBox.latSouth >= this.latSouth
        ) latMatch = true


        //normal case, non overlapping
        if (this.lonWest >= pBoundingBox.lonWest && this.lonWest <= pBoundingBox.lonEast) lonMatch = true
        //normal case, non overlapping
        if (this.lonEast >= pBoundingBox.lonWest && this.lonWest <= pBoundingBox.lonEast) lonMatch = true

        //special case for when *this completely surrounds the pBoundbox
        if (this.lonWest <= pBoundingBox.lonWest && this.lonEast >= pBoundingBox.lonEast && this.latNorth >= pBoundingBox.latNorth && this.latSouth <= pBoundingBox.latSouth) return true

        //normal case, non overlapping
        if (this.latNorth >= pBoundingBox.latSouth && this.latNorth <= this.latSouth) latMatch = true
        //normal case, non overlapping
        if (this.latSouth >= pBoundingBox.latSouth && this.latSouth <= this.latSouth) latMatch = true

        if (this.lonWest > this.lonEast) {
            //the date line is included in the bounding box

            //we want to match lon from the dateline to the eastern bounds of the box
            //and the dateline to the western bounds of the box

            if (this.lonEast <= pBoundingBox.lonEast && pBoundingBox.lonWest >= this.lonWest) lonMatch = true


            if (this.lonWest >= pBoundingBox.lonEast &&
                this.lonEast <= pBoundingBox.lonEast
            ) {
                lonMatch = true
                if (pBoundingBox.lonEast < this.lonWest &&
                    pBoundingBox.lonWest < this.lonWest
                ) lonMatch = false

                if (pBoundingBox.lonEast > this.lonEast &&
                    pBoundingBox.lonWest > this.lonEast
                ) lonMatch = false
            }
            if (this.lonWest >= pBoundingBox.lonEast &&
                this.lonEast >= pBoundingBox.lonEast
            ) {
                lonMatch = true
            }
            /*
			//that is completely within this
			if (mLonWest>= pBoundingBox.mLonEast &&
				mLonEast<= pBoundingBox.mLonEast) {
				lonMatch = true;
				if (pBoundingBox.mLonEast < mLonWest &&
					pBoundingBox.mLonWest < mLonWest)
					lonMatch = false;

				if (pBoundingBox.mLonEast > mLonEast &&
					pBoundingBox.mLonWest > mLonEast )
					lonMatch = false;
			}
			if (mLonWest>= pBoundingBox.mLonEast &&
				mLonEast>= pBoundingBox.mLonEast) {
				lonMatch = true;

			}*/
        }

        return latMatch && lonMatch
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        const val serialVersionUID: Long = 2L

        /**
         * Compute the center of two longitudes
         * Taking into account the case when "west is on the right and east is on the left"
         *
         * @since 6.0.0
         */
        @JvmStatic
        fun getCenterLongitude(pWest: Double, pEast: Double): Double {
            var longitude = (pEast + pWest) / 2.0
            if (pEast < pWest) {
                // center is on the other side of earth
                longitude += 180.0
            }
            return getTileSystem().cleanLongitude(longitude)
        }

        @JvmStatic
        fun fromGeoPoints(partialPolyLine: MutableList<out IGeoPoint>): BoundingBox {
            var minLat = Double.Companion.MAX_VALUE
            var minLon = Double.Companion.MAX_VALUE
            var maxLat = -Double.Companion.MAX_VALUE
            var maxLon = -Double.Companion.MAX_VALUE
            for (gp in partialPolyLine) {
                val latitude = gp.latitude
                val longitude = gp.longitude

                minLat = min(minLat, latitude)
                minLon = min(minLon, longitude)
                maxLat = max(maxLat, latitude)
                maxLon = max(maxLon, longitude)
            }

            return BoundingBox(maxLat, maxLon, minLat, minLon)
        }

        // ===========================================================
        // Inner and Anonymous Classes
        // ===========================================================
        // ===========================================================
        // Parcelable
        // ===========================================================
        @JvmField
        val CREATOR: Creator<BoundingBox> = object : Creator<BoundingBox> {
            override fun createFromParcel(`in`: Parcel): BoundingBox {
                return readFromParcel(`in`)
            }

            override fun newArray(size: Int): Array<BoundingBox?> {
                return arrayOfNulls<BoundingBox>(size)
            }
        }

        private fun readFromParcel(`in`: Parcel): BoundingBox {
            val latNorth = `in`.readDouble()
            val lonEast = `in`.readDouble()
            val latSouth = `in`.readDouble()
            val lonWest = `in`.readDouble()
            return BoundingBox(latNorth, lonEast, latSouth, lonWest)
        }

        @JvmStatic
        fun fromGeoPointsSafe(points: MutableList<GeoPoint>): BoundingBox {
            try {
                return fromGeoPoints(points)
            } catch (e: IllegalArgumentException) {
                val tileSystem = getTileSystem()
                return BoundingBox(
                    tileSystem.maxLatitude,
                    tileSystem.maxLongitude,
                    tileSystem.minLatitude,
                    tileSystem.minLongitude
                )
            }
        }
    }
}
