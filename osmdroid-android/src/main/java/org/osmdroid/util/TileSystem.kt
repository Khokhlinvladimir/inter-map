package org.osmdroid.util

import android.graphics.Point
import android.graphics.Rect
import org.osmdroid.util.constants.GeoConstants
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Proxy class for TileSystem. For coordinate conversions (tile to lat/lon and reverse) TileSystem
 * only accepts input parameters within certain ranges and crops any values outside of it. For
 * lat/lon the range is ~(-85,+85) / (-180,+180) and for tile coordinates (0,mapsize-1). Under
 * certain conditions osmdroid creates values outside of these ranges, for example when zooming out
 * and displaying the earth more that once side by side or when scrolling across the 180 degree
 * longitude (international date line). This class fixes this by wrapping input coordinates into a
 * valid range by adding/subtracting the valid span. Example: longitude +185 =&gt; -175
 *
 * @author Oliver Seiler
 */
abstract class TileSystem {
    @Deprecated("")
    fun LatLongToPixelXY(
        latitude: Double, longitude: Double, levelOfDetail: Int, reuse: Point?
    ): Point {
        val out = (if (reuse == null) Point() else reuse)
        val size: Int = MapSize(levelOfDetail)
        out.x = truncateToInt(getMercatorXFromLongitude(longitude, size.toDouble(), true))
        out.y = truncateToInt(getMercatorYFromLatitude(latitude, size.toDouble(), true))
        return out
    }

    /**
     * Use [TileSystem.getMercatorFromGeo] instead
     */
    @Deprecated("")
    fun LatLongToPixelXY(
        latitude: Double, longitude: Double, zoomLevel: Double, reuse: PointL?
    ): PointL {
        return LatLongToPixelXYMapSize(
            wrap(latitude, -90.0, 90.0, 180.0),
            wrap(longitude, -180.0, 180.0, 360.0),
            MapSize(zoomLevel), reuse
        )
    }

    /**
     * Use [TileSystem.getMercatorFromGeo] instead
     */
    @Deprecated("")
    fun LatLongToPixelXYMapSize(
        latitude: Double, longitude: Double,
        mapSize: Double, reuse: PointL?
    ): PointL {
        return getMercatorFromGeo(latitude, longitude, mapSize, reuse, true)
    }

    /**
     * Use [TileSystem.getGeoFromMercator] instead
     */
    @Deprecated("")
    fun PixelXYToLatLong(
        pixelX: Int, pixelY: Int, levelOfDetail: Int, reuse: GeoPoint?
    ): GeoPoint {
        return getGeoFromMercator(pixelX.toLong(), pixelY.toLong(), MapSize(levelOfDetail).toDouble(), reuse, true, true)
    }

    /**
     * Use [TileSystem.getGeoFromMercator] instead
     */
    @Deprecated("")
    fun PixelXYToLatLong(
        pixelX: Int, pixelY: Int, zoomLevel: Double, reuse: GeoPoint?
    ): GeoPoint {
        return getGeoFromMercator(pixelX.toLong(), pixelY.toLong(), MapSize(zoomLevel), reuse, true, true)
    }

    /**
     * Same as [PixelXYToLatLong][.PixelXYToLatLong] but without wrap
     */
    fun PixelXYToLatLongWithoutWrap(
        pixelX: Int, pixelY: Int, zoomLevel: Double, reuse: GeoPoint?
    ): GeoPoint {
        val mapSize: Double = MapSize(zoomLevel)
        return PixelXYToLatLongMapSizeWithoutWrap(
            pixelX,
            pixelY,
            mapSize, reuse
        )
    }

    /**
     * Converts a longitude to its "X01" value,
     * id est a double between 0 and 1 for the whole longitude range
     */
    fun getX01FromLongitude(longitude: Double, wrapEnabled: Boolean): Double {
        var longitude = longitude
        longitude = if (wrapEnabled) Clip(longitude, this.minLongitude, this.maxLongitude) else longitude
        val result = getX01FromLongitude(longitude)
        return if (wrapEnabled) Clip(result, 0.0, 1.0) else result
    }

    /**
     * Converts a latitude to its "Y01" value,
     * id est a double between 0 and 1 for the whole latitude range
     */
    fun getY01FromLatitude(latitude: Double, wrapEnabled: Boolean): Double {
        var latitude = latitude
        latitude = if (wrapEnabled) Clip(latitude, this.minLatitude, this.maxLatitude) else latitude
        val result = getY01FromLatitude(latitude)
        return if (wrapEnabled) Clip(result, 0.0, 1.0) else result
    }

    /**
     * Converts a longitude to its "X01" value,
     * Same as [getX01FromLongitude][.getX01FromLongitude] but without wrap
     */
    abstract fun getX01FromLongitude(longitude: Double): Double

    /**
     * Converts a latitude to its "Y01" value,
     * Same as [getY01FromLatitude][.getY01FromLatitude] but without wrap
     */
    abstract fun getY01FromLatitude(pLatitude: Double): Double

    /**
     * Use [TileSystem.getGeoFromMercator] instead
     */
    @Deprecated("")
    fun PixelXYToLatLongMapSize(
        pixelX: Int, pixelY: Int,
        mapSize: Double, reuse: GeoPoint?, horizontalWrapEnabled: Boolean,
        verticalWrapEnabled: Boolean
    ): GeoPoint {
        return getGeoFromMercator(pixelX.toLong(), pixelY.toLong(), mapSize, reuse, horizontalWrapEnabled, verticalWrapEnabled)
    }

    /**
     * Same as [PixelXYToLatLongMapSize][.PixelXYToLatLongMapSize]
     * but without wrap
     */
    fun PixelXYToLatLongMapSizeWithoutWrap(
        pixelX: Int, pixelY: Int,
        mapSize: Double, reuse: GeoPoint?
    ): GeoPoint {
        val out = (if (reuse == null) GeoPoint(0.0, 0.0) else reuse)
        val x = (pixelX / mapSize) - 0.5
        val y = 0.5 - (pixelY / mapSize)
        val latitude = 90 - 360 * atan(exp(-y * 2 * Math.PI)) / Math.PI
        val longitude = 360 * x
        out.latitude = latitude
        out.longitude = longitude
        return out
    }

    @Deprecated("")
    fun PixelXYToTileXY(pixelX: Int, pixelY: Int, reuse: Point?): Point {
        return PixelXYToTileXY(pixelX, pixelY, tileSize.toDouble(), reuse)
    }

    /**
     * Use [TileSystem.getTileFromMercator] instead
     */
    @Deprecated("")
    fun PixelXYToTileXY(pPixelX: Int, pPixelY: Int, pTileSize: Double, pReuse: Point?): Point {
        val out = (if (pReuse == null) Point() else pReuse)
        out.x = getTileFromMercator(pPixelX.toLong(), pTileSize)
        out.y = getTileFromMercator(pPixelY.toLong(), pTileSize)
        return out
    }

    /**
     * Use [TileSystem.getTileFromMercator] instead
     */
    @Deprecated("")
    fun PixelXYToTileXY(rect: Rect, pTileSize: Double, pReuse: Rect?): Rect {
        val out = (if (pReuse == null) Rect() else pReuse)
        out.left = getTileFromMercator(rect.left.toLong(), pTileSize)
        out.top = getTileFromMercator(rect.top.toLong(), pTileSize)
        out.right = getTileFromMercator(rect.right.toLong(), pTileSize)
        out.bottom = getTileFromMercator(rect.bottom.toLong(), pTileSize)
        return out
    }

    @Deprecated("")
    fun TileXYToPixelXY(tileX: Int, tileY: Int, reuse: Point?): Point {
        val out = (if (reuse == null) Point() else reuse)
        val size: Int = tileSize
        out.x = truncateToInt(getMercatorFromTile(tileX, size.toDouble()))
        out.y = truncateToInt(getMercatorFromTile(tileY, size.toDouble()))
        return out
    }

    /**
     * Use [TileSystem.getMercatorFromTile] instead
     */
    @Deprecated("")
    fun TileXYToPixelXY(pTileX: Int, pTileY: Int, pTileSize: Double, pReuse: PointL?): PointL {
        val out = (if (pReuse == null) PointL() else pReuse)
        out.x = getMercatorFromTile(pTileX, pTileSize)
        out.y = getMercatorFromTile(pTileY, pTileSize)
        return out
    }

    /**
     * @return the maximum zoom level where a bounding box fits into a screen,
     * or Double.MIN_VALUE if bounding box is a single point
     */
    fun getBoundingBoxZoom(pBoundingBox: BoundingBox, pScreenWidth: Int, pScreenHeight: Int): Double {
        val longitudeZoom = getLongitudeZoom(pBoundingBox.lonEast, pBoundingBox.lonWest, pScreenWidth)
        val latitudeZoom = getLatitudeZoom(pBoundingBox.latNorth, pBoundingBox.latSouth, pScreenHeight)
        if (longitudeZoom == Double.Companion.MIN_VALUE) {
            return latitudeZoom
        }
        if (latitudeZoom == Double.Companion.MIN_VALUE) {
            return longitudeZoom
        }
        return min(latitudeZoom, longitudeZoom)
    }

    /**
     * @return the maximum zoom level where both longitudes fit into a screen,
     * or Double.MIN_VALUE if longitudes are equal
     */
    fun getLongitudeZoom(pEast: Double, pWest: Double, pScreenWidth: Int): Double {
        val x01West = getX01FromLongitude(pWest, true)
        val x01East = getX01FromLongitude(pEast, true)
        var span = x01East - x01West
        if (span < 0) {
            span += 1.0
        }
        if (span == 0.0) {
            return Double.Companion.MIN_VALUE
        }
        return ln(pScreenWidth / span / tileSize) / ln(2.0)
    }

    /**
     * @return the maximum zoom level where both latitudes fit into a screen,
     * or Double.MIN_VALUE if latitudes are equal or ill positioned
     */
    fun getLatitudeZoom(pNorth: Double, pSouth: Double, pScreenHeight: Int): Double {
        val y01North = getY01FromLatitude(pNorth, true)
        val y01South = getY01FromLatitude(pSouth, true)
        val span = y01South - y01North
        if (span <= 0) {
            return Double.Companion.MIN_VALUE
        }
        return ln(pScreenHeight / span / tileSize) / ln(2.0)
    }

    fun getMercatorYFromLatitude(pLatitude: Double, pMapSize: Double, wrapEnabled: Boolean): Long {
        return getMercatorFromXY01(getY01FromLatitude(pLatitude, wrapEnabled), pMapSize, wrapEnabled)
    }

    fun getMercatorXFromLongitude(pLongitude: Double, pMapSize: Double, wrapEnabled: Boolean): Long {
        return getMercatorFromXY01(getX01FromLongitude(pLongitude, wrapEnabled), pMapSize, wrapEnabled)
    }

    fun getMercatorFromXY01(pXY01: Double, pMapSize: Double, wrapEnabled: Boolean): Long {
        return ClipToLong(pXY01 * pMapSize, pMapSize, wrapEnabled)
    }

    /**
     * Converts a "Y01" value into latitude
     * "Y01" is a double between 0 and 1 for the whole latitude range
     * MaxLatitude:0 ... MinLatitude:1
     */
    fun getLatitudeFromY01(pY01: Double, wrapEnabled: Boolean): Double {
        val latitude = getLatitudeFromY01(if (wrapEnabled) Clip(pY01, 0.0, 1.0) else pY01)
        return if (wrapEnabled) Clip(latitude, this.minLatitude, this.maxLatitude) else latitude
    }

    abstract fun getLatitudeFromY01(pY01: Double): Double

    /**
     * Converts a "X01" value into longitude
     * "X01" is a double between 0 and 1 for the whole longitude range
     * MinLongitude:0 ... MaxLongitude:1
     */
    fun getLongitudeFromX01(pX01: Double, wrapEnabled: Boolean): Double {
        val longitude = getLongitudeFromX01(if (wrapEnabled) Clip(pX01, 0.0, 1.0) else pX01)
        return if (wrapEnabled) Clip(longitude, this.minLongitude, this.maxLongitude) else longitude
    }

    abstract fun getLongitudeFromX01(pX01: Double): Double

    fun getCleanMercator(pMercator: Long, pMercatorMapSize: Double, wrapEnabled: Boolean): Long {
        return ClipToLong(
            if (wrapEnabled) wrap(pMercator.toDouble(), 0.0, pMercatorMapSize, pMercatorMapSize) else pMercator.toDouble(),
            pMercatorMapSize,
            wrapEnabled
        )
    }

    fun getMercatorFromGeo(pLatitude: Double, pLongitude: Double, pMapSize: Double, pReuse: PointL?, wrapEnabled: Boolean): PointL {
        val out = (if (pReuse == null) PointL() else pReuse)
        out.x = getMercatorXFromLongitude(pLongitude, pMapSize, wrapEnabled)
        out.y = getMercatorYFromLatitude(pLatitude, pMapSize, wrapEnabled)
        return out
    }

    fun getGeoFromMercator(
        pMercatorX: Long,
        pMercatorY: Long,
        pMapSize: Double,
        pReuse: GeoPoint?,
        horizontalWrapEnabled: Boolean,
        verticalWrapEnabled: Boolean
    ): GeoPoint {
        val out = if (pReuse == null) GeoPoint(0.0, 0.0) else pReuse
        out.latitude = getLatitudeFromY01(getXY01FromMercator(pMercatorY, pMapSize, verticalWrapEnabled), verticalWrapEnabled)
        out.longitude = getLongitudeFromX01(getXY01FromMercator(pMercatorX, pMapSize, horizontalWrapEnabled), horizontalWrapEnabled)
        return out
    }

    fun getXY01FromMercator(pMercator: Long, pMapSize: Double, wrapEnabled: Boolean): Double {
        return if (wrapEnabled) Clip(pMercator / pMapSize, 0.0, 1.0) else pMercator / pMapSize
    }

    /**
     * @param pRandom01 [0,1]
     */
    fun getRandomLongitude(pRandom01: Double): Double {
        return pRandom01 * (this.maxLongitude - this.minLongitude) + this.minLongitude
    }

    /**
     * @param pRandom01 [0,1]
     */
    fun getRandomLatitude(pRandom01: Double, pMinLatitude: Double): Double {
        return pRandom01 * (this.maxLatitude - pMinLatitude) + pMinLatitude
    }

    /**
     * @param pRandom01 [0,1]
     */
    fun getRandomLatitude(pRandom01: Double): Double {
        return getRandomLatitude(pRandom01, this.minLatitude)
    }

    abstract val minLatitude: Double

    abstract val maxLatitude: Double

    abstract val minLongitude: Double

    abstract val maxLongitude: Double

    fun cleanLongitude(pLongitude: Double): Double {
        var result = pLongitude

        while (result < -180) {
            result += 360.0
        }
        while (result > 180) {
            result -= 360.0
        }
        return Clip(result, this.minLongitude, this.maxLongitude)
    }

    fun cleanLatitude(pLatitude: Double): Double {
        return Clip(pLatitude, this.minLatitude, this.maxLatitude)
    }

    fun isValidLongitude(pLongitude: Double): Boolean {
        return pLongitude >= this.minLongitude && pLongitude <= this.maxLongitude
    }

    fun isValidLatitude(pLatitude: Double): Boolean {
        return pLatitude >= this.minLatitude && pLatitude <= this.maxLatitude
    }

    fun toStringLongitudeSpan(): String {
        return "[" + this.minLongitude + "," + this.maxLongitude + "]"
    }

    fun toStringLatitudeSpan(): String {
        return "[" + this.minLatitude + "," + this.maxLatitude + "]"
    }

    fun getTileXFromLongitude(pLongitude: Double, pZoom: Int): Int {
        return clipTile(floor(getX01FromLongitude(pLongitude) * (1 shl pZoom)).toInt(), pZoom)
    }

    fun getTileYFromLatitude(pLatitude: Double, pZoom: Int): Int {
        return clipTile(floor(getY01FromLatitude(pLatitude) * (1 shl pZoom)).toInt(), pZoom)
    }

    fun getLatitudeFromTileY(pY: Int, pZoom: Int): Double {
        return getLatitudeFromY01((clipTile(pY, pZoom).toDouble()) / (1 shl pZoom))
    }

    fun getLongitudeFromTileX(pX: Int, pZoom: Int): Double {
        return getLongitudeFromX01((clipTile(pX, pZoom).toDouble()) / (1 shl pZoom))
    }

    private fun clipTile(pTile: Int, pZoom: Int): Int {
        if (pTile < 0) {
            return 0
        }
        val max = 1 shl pZoom
        if (pTile >= max) {
            return max - 1
        }
        return pTile
    }

    companion object {
        @Deprecated("")
        @JvmField
        val EarthRadius: Double = GeoConstants.RADIUS_EARTH_METERS.toDouble()

        /**
         * Use [TileSystem.getMinLatitude] instead
         */
        @Deprecated("")
        @JvmField
        val MinLatitude: Double = -85.05112877980659

        /**
         * Use [TileSystem.getMaxLatitude] instead
         */
        @Deprecated("")
        const val MaxLatitude: Double = 85.05112877980659

        /**
         * Use [TileSystem.getMinLongitude] instead
         */
        @Deprecated("")
        @JvmField
        val MinLongitude: Double = -180.0

        /**
         * Use [TileSystem.getMaxLongitude] instead
         */
        @Deprecated("")
        const val MaxLongitude: Double = 180.0

        /**
         * Used to be in the `TileSystem` class of another package
         */
        private var mTileSize = 256

        /**
         * The maximum possible zoom for primary key of SQLite table is 29,
         * because it gives enough space for y(29bits), x(29bits) and zoom(5bits in order to code 29),
         * total: 63 bits used, just small enough for a `long` variable of 4 bytes
         *
         * Used to be in the `TileSystem` class of another package
         */
        const val primaryKeyMaxZoomLevel: Int = 29

        /**
         * Used to be in the `TileSystem` class of another package
         */
        @Deprecated("Just don't use it anymore")
        @JvmField
        val projectionZoomLevel: Int = primaryKeyMaxZoomLevel + 1

        /**
         * Used to be in the `TileSystem` class of another package
         */
        /**
         * Maximum Zoom Level - we use Integers to store zoom levels so overflow happens at 2^32 - 1,
         * but we also have a tile size that is typically 2^8, so (32-1)-8-1 = 22
         *
         * Used to be in the `TileSystem` class of another package
         */
        @get:JvmStatic
        var maximumZoomLevel: Int = primaryKeyMaxZoomLevel
            private set

        @get:JvmStatic
        @set:JvmStatic
        var tileSize: Int
            get() = mTileSize
            set(tileSize) {
                val pow2 = (0.5 + ln(tileSize.toDouble()) / ln(2.0)).toInt()
                maximumZoomLevel = min(primaryKeyMaxZoomLevel, (64 - 1) - pow2 - 1)

                mTileSize = tileSize
            }

        @JvmStatic
        fun getTileSize(pZoomLevel: Double): Double {
            return MapSize(pZoomLevel - getInputTileZoomLevel(pZoomLevel))
        }

        @JvmStatic
        fun getInputTileZoomLevel(pZoomLevel: Double): Int {
            return MyMath.floorToInt(pZoomLevel)
        }

        @Deprecated("")
        @JvmStatic
        fun MapSize(levelOfDetail: Int): Int {
            return Math.round(MapSize(levelOfDetail.toDouble())).toInt()
        }

        @JvmStatic
        fun MapSize(pZoomLevel: Double): Double {
            return tileSize * getFactor(pZoomLevel)
        }

        @JvmStatic
        fun getFactor(pZoomLevel: Double): Double {
            return 2.0.pow(pZoomLevel)
        }

        @JvmStatic
        fun GroundResolution(latitude: Double, levelOfDetail: Int): Double {
            return GroundResolution(latitude, levelOfDetail.toDouble())
        }

        @JvmStatic
        fun GroundResolution(latitude: Double, zoomLevel: Double): Double {
            return GroundResolutionMapSize(wrap(latitude, -90.0, 90.0, 180.0), MapSize(zoomLevel))
        }

        /**
         * Most likely meters/pixel at the given latitude
         */
        @JvmStatic
        fun GroundResolutionMapSize(latitude: Double, mapSize: Double): Double {
            var latitude = latitude
            latitude = Clip(latitude, -90.0, 90.0)
            return (cos(latitude * Math.PI / 180) * 2 * Math.PI * GeoConstants.RADIUS_EARTH_METERS
                    / mapSize)
        }

        @JvmStatic
        fun MapScale(latitude: Double, levelOfDetail: Int, screenDpi: Int): Double {
            return GroundResolution(latitude, levelOfDetail) * screenDpi / 0.0254
        }

        @JvmStatic
        fun Clip(n: Double, minValue: Double, maxValue: Double): Double {
            return min(max(n, minValue), maxValue)
        }

        /**
         * Use [MapTileIndex.getTileIndex] instead
         * Quadkey principles can be found at https://msdn.microsoft.com/en-us/library/bb259689.aspx
         * Works only for zoom level >= 1
         */
        @JvmStatic
        fun TileXYToQuadKey(tileX: Int, tileY: Int, levelOfDetail: Int): String {
            val quadKey = CharArray(levelOfDetail)
            for (i in 0 until levelOfDetail) {
                var digit = '0'
                val mask = 1 shl i
                if ((tileX and mask) != 0) {
                    digit++
                }
                if ((tileY and mask) != 0) {
                    digit++
                    digit++
                }
                quadKey[levelOfDetail - i - 1] = digit
            }
            return String(quadKey)
        }

        /**
         * Use [MapTileIndex.getX] and [MapTileIndex.getY] instead
         * Quadkey principles can be found at https://msdn.microsoft.com/en-us/library/bb259689.aspx
         */
        @JvmStatic
        fun QuadKeyToTileXY(quadKey: String, reuse: Point?): Point {
            val out = if (reuse == null) Point() else reuse
            require(!(quadKey == null || quadKey.length == 0)) { "Invalid QuadKey: " + quadKey }
            var tileX = 0
            var tileY = 0
            val zoom = quadKey.length
            for (i in 0 until zoom) {
                val value = 1 shl i
                when (quadKey.get(zoom - i - 1)) {
                    '0' -> {}
                    '1' -> tileX += value
                    '2' -> tileY += value
                    '3' -> {
                        tileX += value
                        tileY += value
                    }

                    else -> throw IllegalArgumentException("Invalid QuadKey: " + quadKey)
                }
            }
            out.x = tileX
            out.y = tileY
            return out
        }

        /**
         * Returns a value that lies within `minValue` and `maxValue` by
         * subtracting/adding `interval`.
         *
         * @param n        the input number
         * @param minValue the minimum value
         * @param maxValue the maximum value
         * @param interval the interval length
         * @return a value that lies within `minValue` and `maxValue` by
         * subtracting/adding `interval`
         */
        private fun wrap(n: Double, minValue: Double, maxValue: Double, interval: Double): Double {
            var n = n
            require(!(minValue > maxValue)) {
                ("minValue must be smaller than maxValue: "
                        + minValue + ">" + maxValue)
            }
            require(!(interval > maxValue - minValue + 1)) {
                ("interval must be equal or smaller than maxValue-minValue: " + "min: "
                        + minValue + " max:" + maxValue + " int:" + interval)
            }
            while (n < minValue) {
                n += interval
            }
            while (n > maxValue) {
                n -= interval
            }
            return n
        }

        @JvmStatic
        fun ClipToLong(pValue: Double, pMax: Double, pWrapEnabled: Boolean): Long {
            val longValue = MyMath.floorToLong(pValue)
            if (!pWrapEnabled) {
                return longValue
            }
            if (longValue <= 0) {
                return 0
            }
            val longMax = MyMath.floorToLong(pMax - 1)
            return if (longValue >= pMax) longMax else longValue
        }

        @Deprecated("")
        @JvmStatic
        fun Clip(n: Long, minValue: Long, maxValue: Long): Long {
            return min(max(n, minValue), maxValue)
        }

        /**
         * Casts a long type value into an int with no harm.
         * The typical use case is to compute pixel coordinates with high zoom
         * (which won't fit into int but will fit into long)
         * and to truncate them into int in order to display them on the screen (which requires int)
         * The meaning of a pixel coordinate of MIN/MAX_VALUE is just:
         * it's far far away and it doesn't crash the app
         */
        @JvmStatic
        fun truncateToInt(value: Long): Int {
            return max(min(value, Int.Companion.MAX_VALUE.toLong()), Int.Companion.MIN_VALUE.toLong()).toInt()
        }

        @JvmStatic
        fun getTileFromMercator(pMercator: Long, pTileSize: Double): Int {
            return MyMath.floorToInt(pMercator / pTileSize)
        }

        @JvmStatic
        fun getTileFromMercator(pMercatorRect: RectL, pTileSize: Double, pReuse: Rect?): Rect {
            val out = (if (pReuse == null) Rect() else pReuse)
            out.left = getTileFromMercator(pMercatorRect.left, pTileSize)
            out.top = getTileFromMercator(pMercatorRect.top, pTileSize)
            out.right = getTileFromMercator(pMercatorRect.right, pTileSize)
            out.bottom = getTileFromMercator(pMercatorRect.bottom, pTileSize)
            return out
        }

        @JvmStatic
        fun getMercatorFromTile(pTile: Int, pTileSize: Double): Long {
            return Math.round(pTile * pTileSize)
        }
    }
}
