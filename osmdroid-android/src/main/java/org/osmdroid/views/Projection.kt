package org.osmdroid.views

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import org.osmdroid.api.IGeoPoint
import org.osmdroid.api.IProjection
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.GeometryMath
import org.osmdroid.util.PointL
import org.osmdroid.util.RectL
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView.Companion.getTileSystem
import kotlin.math.abs

/**
 * A Projection serves to translate between the coordinate system of x/y on-screen pixel coordinates
 * and that of latitude/longitude points on the surface of the earth. You obtain a Projection from
 * MapView.getProjection(). You should not hold on to this object for more than one draw, since the
 * projection of the map could change. <br></br>
 * <br></br>Uses the web mercator projection
 * **Note:** This class will "wrap" all pixel and lat/long values that overflow their bounds
 * (rather than clamping to their bounds).
 *
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Manuel Stahl
 * @author Fabrice Fontaine
 */
open class Projection(
    pZoomLevel: Double, pScreenRect: Rect,
    pCenter: GeoPoint?,
    pScrollX: Long, pScrollY: Long,
    pOrientation: Float,
    pHorizontalWrapEnabled: Boolean, pVerticalWrapEnabled: Boolean,
    pTileSystem: TileSystem,
    pMapCenterOffsetX: Int, pMapCenterOffsetY: Int
) : IProjection {
    var offsetX: Long
        private set
    var offsetY: Long
        private set
    private var mScrollX: Long
    private var mScrollY: Long

    /**
     * This will provide a Matrix that will revert the current map's scaling and rotation. This can
     * be useful when drawing to a fixed location on the screen.
     */
    val scaleRotateCanvasMatrix: Matrix = Matrix()

    /**
     * This will provide a Matrix that will revert the current map's scaling and rotation. This can
     * be useful when drawing to a fixed location on the screen.
     */
    val invertedScaleRotateCanvasMatrix: Matrix = Matrix()
    private val mRotateScalePoints = FloatArray(2)

    val boundingBox: BoundingBox = BoundingBox()
    val zoomLevel: Double
    val screenRect: Rect = Rect()
    val intrinsicScreenRect: Rect

    /**
     * @since 6.1.0
     */
    val isHorizontalWrapEnabled: Boolean

    /**
     * @since 6.1.0
     */
    val isVerticalWrapEnabled: Boolean

    /**
     * @since 6.2.0
     */
    val worldMapSize: Double
    private val mTileSize: Double

    /**
     * @since 6.1.0
     */
    val orientation: Float

    /**
     * @since 6.0.0
     */
    val currentCenter: GeoPoint = GeoPoint(0.0, 0.0)

    private val mTileSystem: TileSystem

    /**
     * @since 6.1.1
     */
    private val mMapCenterOffsetX: Int
    private val mMapCenterOffsetY: Int

    internal constructor(mapView: MapView) : this(
        mapView.zoomLevelDouble, mapView.getIntrinsicScreenRect(null),
        mapView.getExpectedCenter(),
        mapView.getMapScrollX(), mapView.getMapScrollY(),
        mapView.getMapOrientation(),
        mapView.isHorizontalMapRepetitionEnabled(), mapView.isVerticalMapRepetitionEnabled(),
        getTileSystem(),
        mapView.getMapCenterOffsetX(),
        mapView.getMapCenterOffsetY()
    )

    /**
     * @since 6.0.0
     */
    init {
        mMapCenterOffsetX = pMapCenterOffsetX
        mMapCenterOffsetY = pMapCenterOffsetY
        this.zoomLevel = pZoomLevel
        this.isHorizontalWrapEnabled = pHorizontalWrapEnabled
        this.isVerticalWrapEnabled = pVerticalWrapEnabled
        mTileSystem = pTileSystem
        this.worldMapSize = TileSystem.Companion.MapSize(this.zoomLevel)
        mTileSize = TileSystem.Companion.getTileSize(this.zoomLevel)
        this.intrinsicScreenRect = pScreenRect
        val center = if (pCenter != null) pCenter else GeoPoint(0.0, 0.0)
        mScrollX = pScrollX
        mScrollY = pScrollY
        this.offsetX = this.screenCenterX - mScrollX - mTileSystem.getMercatorXFromLongitude(
            center.longitude,
            this.worldMapSize, this.isHorizontalWrapEnabled
        )
        this.offsetY = this.screenCenterY - mScrollY - mTileSystem.getMercatorYFromLatitude(
            center.latitude,
            this.worldMapSize, this.isVerticalWrapEnabled
        )
        this.orientation = pOrientation
        scaleRotateCanvasMatrix.preRotate(this.orientation, this.screenCenterX.toFloat(), this.screenCenterY.toFloat())
        scaleRotateCanvasMatrix.invert(this.invertedScaleRotateCanvasMatrix)
        refresh()
    }

    /**
     * @since 6.1.0
     */
    constructor(
        pZoomLevel: Double, pWidth: Int, pHeight: Int,
        pCenter: GeoPoint?,
        pOrientation: Float,
        pHorizontalWrapEnabled: Boolean, pVerticalWrapEnabled: Boolean,
        pMapCenterOffsetX: Int, pMapCenterOffsetY: Int
    ) : this(
        pZoomLevel, Rect(0, 0, pWidth, pHeight),
        pCenter,
        0, 0,
        pOrientation,
        pHorizontalWrapEnabled, pVerticalWrapEnabled,
        getTileSystem(),
        pMapCenterOffsetX, pMapCenterOffsetY
    )

    /**
     * @since 6.0.0
     */
    fun getOffspring(pZoomLevel: Double, pScreenRect: Rect): Projection {
        return Projection(
            pZoomLevel, pScreenRect,
            this.currentCenter, 0, 0,
            this.orientation,
            this.isHorizontalWrapEnabled, this.isVerticalWrapEnabled,
            mTileSystem,
            0, 0
        ) // 0 looks like the most relevant value
    }

    override fun fromPixels(x: Int, y: Int): IGeoPoint {
        return fromPixels(x, y, null, false)
    }

    /**
     * * note: if [MapView.setHorizontalMapRepetitionEnabled] or
     * [MapView.setVerticalMapRepetitionEnabled] is false, then this
     * can return values that beyond the max extents of the world. This may or may not be
     * desired. [https://github.com/osmdroid/osmdroid/pull/722](https://github.com/osmdroid/osmdroid/pull/722)
     * for more information and the discussion associated with this.
     *
     * @param pPixelX
     * @param pPixelY
     * @param pReuse
     * @param forceWrap
     * @return
     */
    /**
     * note: if [MapView.setHorizontalMapRepetitionEnabled] or
     * [MapView.setVerticalMapRepetitionEnabled] is false, then this
     * can return values that beyond the max extents of the world. This may or may not be
     * desired. [https://github.com/osmdroid/osmdroid/pull/722](https://github.com/osmdroid/osmdroid/pull/722)
     * for more information and the discussion associated with this.
     *
     * @param pPixelX
     * @param pPixelY
     * @param pReuse
     * @return
     */
    @JvmOverloads
    fun fromPixels(pPixelX: Int, pPixelY: Int, pReuse: GeoPoint?, forceWrap: Boolean = false): IGeoPoint {
        //reverting https://github.com/osmdroid/osmdroid/issues/459
        //due to relapse of https://github.com/osmdroid/osmdroid/issues/507
        //reverted functionality is now on the method fromPixelsRotationSensitive
        return mTileSystem.getGeoFromMercator(
            getCleanMercator(getMercatorXFromPixel(pPixelX), this.isHorizontalWrapEnabled),
            getCleanMercator(getMercatorYFromPixel(pPixelY), this.isVerticalWrapEnabled), this.worldMapSize, pReuse,
            this.isHorizontalWrapEnabled || forceWrap, this.isVerticalWrapEnabled || forceWrap
        )
    }

    override fun toPixels(`in`: IGeoPoint?, reuse: Point?): Point {
        return toPixels(requireNotNull(`in`), reuse, false)
    }

    fun toPixels(`in`: IGeoPoint, reuse: Point?, forceWrap: Boolean): Point {
        val out = if (reuse != null) reuse else Point()
        out.x = TileSystem.Companion.truncateToInt(getLongPixelXFromLongitude(`in`.longitude, forceWrap))
        out.y = TileSystem.Companion.truncateToInt(getLongPixelYFromLatitude(`in`.latitude, forceWrap))
        return out
    }

    /**
     * @since 6.0.0
     * TODO refactor
     */
    fun getLongPixelXFromLongitude(pLongitude: Double, forceWrap: Boolean): Long {
        return getLongPixelXFromMercator(
            mTileSystem.getMercatorXFromLongitude(pLongitude, this.worldMapSize, this.isHorizontalWrapEnabled || forceWrap),
            this.isHorizontalWrapEnabled
        )
    }

    /**
     * @since 6.0.0
     * TODO refactor
     */
    fun getLongPixelXFromLongitude(pLongitude: Double): Long {
        return getLongPixelXFromMercator(mTileSystem.getMercatorXFromLongitude(pLongitude, this.worldMapSize, false), false)
    }

    /**
     * @since 6.0.0
     * TODO refactor
     */
    fun getLongPixelYFromLatitude(pLatitude: Double, forceWrap: Boolean): Long {
        return getLongPixelYFromMercator(
            mTileSystem.getMercatorYFromLatitude(pLatitude, this.worldMapSize, this.isVerticalWrapEnabled || forceWrap),
            this.isVerticalWrapEnabled
        )
    }

    /**
     * @since 6.0.0
     * TODO refactor
     */
    fun getLongPixelYFromLatitude(pLatitude: Double): Long {
        return getLongPixelYFromMercator(mTileSystem.getMercatorYFromLatitude(pLatitude, this.worldMapSize, false), false)
    }

    /**
     * A wrapper for [.toProjectedPixels]
     */
    fun toProjectedPixels(geoPoint: GeoPoint, reuse: PointL?): PointL? {
        return toProjectedPixels(geoPoint.latitude, geoPoint.longitude, reuse)
    }

    /**
     * Performs only the first computationally heavy part of the projection. Call
     * [.getLongPixelsFromProjected] to get the final position.
     *
     * @param latituteE6  the latitute of the point
     * @param longitudeE6 the longitude of the point
     * @param reuse       just pass null if you do not have a PointL to be 'recycled'.
     * @return intermediate value to be stored and passed to toMapPixelsTranslated.
     */
    @Deprecated("Use {@link #toProjectedPixels(double, double, PointL)} instead")
    fun toProjectedPixels(latituteE6: Long, longitudeE6: Long, reuse: PointL?): PointL? {
        return toProjectedPixels(latituteE6 * 1E-6, longitudeE6 * 1E-6, reuse)
    }

    /**
     * Performs only the first computationally heavy part of the projection. Call
     * [.getLongPixelsFromProjected] to get the final position.
     *
     * @param latitude  the latitute of the point
     * @param longitude the longitude of the point
     * @param reuse     just pass null if you do not have a PointL to be 'recycled'.
     * @return intermediate value to be stored and passed to toMapPixelsTranslated.
     */
    fun toProjectedPixels(latitude: Double, longitude: Double, reuse: PointL?): PointL? {
        return toProjectedPixels(latitude, longitude, true, reuse)
    }

    /**
     * @since 6.0.0
     */
    fun toProjectedPixels(latitude: Double, longitude: Double, pWrapEnabled: Boolean, reuse: PointL?): PointL? {
        return mTileSystem.getMercatorFromGeo(latitude, longitude, mProjectedMapSize, reuse, pWrapEnabled)
    }

    /**
     * Performs the second computationally light part of the projection.
     *
     * @param in    the PointL calculated by the [.toProjectedPixels]
     * @param reuse just pass null if you do not have a Point to be 'recycled'.
     * @return the Point containing the coordinates of the initial GeoPoint passed to the
     * [.toProjectedPixels].
     */
    @Deprecated("Use {@link #getLongPixelsFromProjected(PointL, double, boolean, PointL)} instead")
    fun toPixelsFromProjected(`in`: PointL, reuse: Point?): Point {
        val out = if (reuse != null) reuse else Point()
        val power = this.projectedPowerDifference
        val tmp = PointL()
        getLongPixelsFromProjected(`in`, power, true, tmp)
        out.x = TileSystem.Companion.truncateToInt(tmp.x)
        out.y = TileSystem.Companion.truncateToInt(tmp.y)
        return out
    }

    @Deprecated("Use {@link #getLongPixelsFromProjected(PointL, double, boolean, PointL)} instead")
    fun toPixelsFromMercator(pMercatorX: Long, pMercatorY: Long, reuse: Point?): Point {
        val out = if (reuse != null) reuse else Point()
        out.x = TileSystem.Companion.truncateToInt(getLongPixelXFromMercator(pMercatorX, true))
        out.y = TileSystem.Companion.truncateToInt(getLongPixelYFromMercator(pMercatorY, true))
        return out
    }

    fun toMercatorPixels(pPixelX: Int, pPixelY: Int, reuse: PointL?): PointL {
        val out = if (reuse != null) reuse else PointL()
        out.x = getCleanMercator(getMercatorXFromPixel(pPixelX), this.isHorizontalWrapEnabled)
        out.y = getCleanMercator(getMercatorYFromPixel(pPixelY), this.isVerticalWrapEnabled)
        return out
    }

    override fun metersToEquatorPixels(meters: Float): Float {
        return metersToPixels(meters, 0.0, this.zoomLevel)
    }

    /**
     * @since 6.0
     */
    /**
     * Converts a distance in meters to one in (horizontal) pixels at the current zoomlevel and at
     * the current latitude at the center of the screen.
     *
     * @param meters the distance in meters
     * @return The number of pixels corresponding to the distance, if measured at the center of the
     * screen, at the current zoom level. The return value may only be approximate.
     */
    @JvmOverloads
    fun metersToPixels(
        meters: Float,
        latitude: Double = this.boundingBox.centerWithDateLine.latitude,
        zoomLevel: Double = this.zoomLevel
    ): Float {
        return (meters / TileSystem.Companion.GroundResolution(latitude, zoomLevel)).toFloat()
    }

    override val northEast: IGeoPoint
        get() = fromPixels(intrinsicScreenRect.right, intrinsicScreenRect.top, null, true)

    override val southWest: IGeoPoint
        get() = fromPixels(intrinsicScreenRect.left, intrinsicScreenRect.bottom, null, true)

    /**
     * This will revert the current map's scaling and rotation for a point. This can be useful when
     * drawing to a fixed location on the screen.
     */
    fun unrotateAndScalePoint(x: Int, y: Int, reuse: Point?): Point {
        return applyMatrixToPoint(x, y, reuse, this.invertedScaleRotateCanvasMatrix, this.orientation != 0f)
    }

    /**
     * This will apply the current map's scaling and rotation for a point. This can be useful when
     * converting MotionEvents to a screen point.
     */
    fun rotateAndScalePoint(x: Int, y: Int, reuse: Point?): Point {
        return applyMatrixToPoint(x, y, reuse, this.scaleRotateCanvasMatrix, this.orientation != 0f)
    }

    /**
     * @since 6.0.0
     */
    private fun applyMatrixToPoint(pX: Int, pY: Int, reuse: Point?, pMatrix: Matrix, pCondition: Boolean): Point {
        val out = if (reuse != null) reuse else Point()
        if (pCondition) {
            mRotateScalePoints[0] = pX.toFloat()
            mRotateScalePoints[1] = pY.toFloat()
            pMatrix.mapPoints(mRotateScalePoints)
            out.x = mRotateScalePoints[0].toInt()
            out.y = mRotateScalePoints[1].toInt()
        } else {
            out.x = pX
            out.y = pY
        }
        return out
    }

    /**
     * @since 5.6
     */
    fun detach() {
    }

    /**
     * @since 6.0.0
     */
    fun getPixelFromTile(pTileX: Int, pTileY: Int, pReuse: Rect?): Rect {
        val out = if (pReuse != null) pReuse else Rect()
        out.left = TileSystem.Companion.truncateToInt(getLongPixelXFromMercator(getMercatorFromTile(pTileX), false))
        out.top = TileSystem.Companion.truncateToInt(getLongPixelYFromMercator(getMercatorFromTile(pTileY), false))
        out.right = TileSystem.Companion.truncateToInt(getLongPixelXFromMercator(getMercatorFromTile(pTileX + 1), false))
        out.bottom = TileSystem.Companion.truncateToInt(getLongPixelYFromMercator(getMercatorFromTile(pTileY + 1), false))
        return out
    }

    fun getMercatorFromTile(pTile: Int): Long {
        return TileSystem.Companion.getMercatorFromTile(pTile, mTileSize)
    }

    val projectedPowerDifference: Double
        get() = mProjectedMapSize / this.worldMapSize

    @Deprecated("Use {@link #getLongPixelsFromProjected(PointL, double, boolean, PointL)} instead")
    fun getPixelsFromProjected(`in`: PointL, powerDifference: Double, reuse: Point?): Point {
        val out = if (reuse != null) reuse else Point()
        val tmp = PointL()
        getLongPixelsFromProjected(`in`, powerDifference, true, tmp)
        out.x = TileSystem.Companion.truncateToInt(tmp.x)
        out.y = TileSystem.Companion.truncateToInt(tmp.y)
        return out
    }

    /**
     * @param in              Input point: a geo point projected to the map with the largest zoom level (aka "projected" map)
     * @param powerDifference Factor between the large "projected" map and the wanted projection zoom level
     * @param pCloser         "Should we move the resulting point - modulo the map size - so that it's
     * as close to the screen limits as possible?"
     */
    fun getLongPixelsFromProjected(`in`: PointL, powerDifference: Double, pCloser: Boolean, reuse: PointL?): PointL {
        val out = if (reuse != null) reuse else PointL()
        out.x = getLongPixelXFromMercator((`in`.x / powerDifference).toLong(), pCloser)
        out.y = getLongPixelYFromMercator((`in`.y / powerDifference).toLong(), pCloser)
        return out
    }

    /**
     * Correction of pixel value.
     * Pixel values are identical, modulo mapSize.
     * What we explicitly want is either:
     * * the visible pixel that is the closest to the left (first choice)
     * * the invisible pixel that is the closest to the screen center
     */
    private fun getCloserPixel(pPixel: Long, pScreenLimitFirst: Int, pScreenLimitLast: Int, pMapSize: Double): Long {
        var pPixel = pPixel
        val center = ((pScreenLimitFirst + pScreenLimitLast) / 2).toLong()
        var previous: Long = 0
        if (pPixel < pScreenLimitFirst) {
            while (pPixel < pScreenLimitFirst) {
                previous = pPixel
                pPixel = (pPixel + pMapSize).toLong()
            }
            if (pPixel < pScreenLimitLast) {
                return pPixel
            }
            if (abs(center - pPixel) < abs(center - previous)) {
                return pPixel
            }
            return previous
        }

        while (pPixel >= pScreenLimitFirst) {
            previous = pPixel
            pPixel = (pPixel - pMapSize).toLong()
        }
        if (previous < pScreenLimitLast) {
            return previous
        }
        if (abs(center - pPixel) < abs(center - previous)) {
            return pPixel
        }
        return previous
    }

    private fun getLongPixelXFromMercator(pMercatorX: Long, pCloser: Boolean): Long {
        return getLongPixelFromMercator(pMercatorX, pCloser, this.offsetX, intrinsicScreenRect.left, intrinsicScreenRect.right)
    }

    private fun getLongPixelYFromMercator(pMercatorY: Long, pCloser: Boolean): Long {
        return getLongPixelFromMercator(pMercatorY, pCloser, this.offsetY, intrinsicScreenRect.top, intrinsicScreenRect.bottom)
    }

    private fun getLongPixelFromMercator(pMercator: Long, pCloser: Boolean, pOffset: Long, pScreenLimitFirst: Int, pScreenLimitLast: Int): Long {
        var result = pMercator + pOffset
        if (pCloser) {
            result = getCloserPixel(result, pScreenLimitFirst, pScreenLimitLast, this.worldMapSize)
        }
        return result
    }


    fun getTileFromMercator(pMercator: Long): Int {
        return TileSystem.Companion.getTileFromMercator(pMercator, mTileSize)
    }

    fun getMercatorViewPort(pReuse: RectL?): RectL {
        val out = if (pReuse != null) pReuse else RectL()

        // in the standard case, that's all we need: the screen rect corners
        var left = intrinsicScreenRect.left.toFloat()
        var right = intrinsicScreenRect.right.toFloat()
        var top = intrinsicScreenRect.top.toFloat()
        var bottom = intrinsicScreenRect.bottom.toFloat()

        // sometimes we need to expand beyond in order to get all visible tiles
        if (this.orientation != 0f) {
            val scaleRotatePoints: FloatArray? = FloatArray(8)
            scaleRotatePoints!![0] = intrinsicScreenRect.left.toFloat()
            scaleRotatePoints[1] = intrinsicScreenRect.top.toFloat()
            scaleRotatePoints[2] = intrinsicScreenRect.right.toFloat()
            scaleRotatePoints[3] = intrinsicScreenRect.bottom.toFloat()
            scaleRotatePoints[4] = intrinsicScreenRect.left.toFloat()
            scaleRotatePoints[5] = intrinsicScreenRect.bottom.toFloat()
            scaleRotatePoints[6] = intrinsicScreenRect.right.toFloat()
            scaleRotatePoints[7] = intrinsicScreenRect.top.toFloat()
            invertedScaleRotateCanvasMatrix.mapPoints(scaleRotatePoints)

            var i = 0
            while (i < 8) {
                if (left > scaleRotatePoints[i]) {
                    left = scaleRotatePoints[i]
                }
                if (right < scaleRotatePoints[i]) {
                    right = scaleRotatePoints[i]
                }
                if (top > scaleRotatePoints[i + 1]) {
                    top = scaleRotatePoints[i + 1]
                }
                if (bottom < scaleRotatePoints[i + 1]) {
                    bottom = scaleRotatePoints[i + 1]
                }
                i += 2
            }
        }

        out.left = getMercatorXFromPixel(left.toInt())
        out.top = getMercatorYFromPixel(top.toInt())
        out.right = getMercatorXFromPixel(right.toInt())
        out.bottom = getMercatorYFromPixel(bottom.toInt())
        return out
    }


    val screenCenterX: Int
        get() = (intrinsicScreenRect.right + intrinsicScreenRect.left) / 2 + mMapCenterOffsetX

    val screenCenterY: Int
        get() = (intrinsicScreenRect.bottom + intrinsicScreenRect.top) / 2 + mMapCenterOffsetY

    /**
     * @since 6.0.0
     */
    fun getMercatorXFromPixel(pPixelX: Int): Long {
        return pPixelX - this.offsetX
    }

    /**
     * @since 6.0.0
     */
    fun getMercatorYFromPixel(pPixelY: Int): Long {
        return pPixelY - this.offsetY
    }

    /**
     * @since 6.0.0
     */
    fun getCleanMercator(pMercator: Long, wrapEnabled: Boolean): Long {
        return mTileSystem.getCleanMercator(pMercator, this.worldMapSize, wrapEnabled)
    }

    /**
     * @since 6.0.0
     */
    fun save(pCanvas: Canvas, pMapRotation: Boolean, pForce: Boolean) {
        if (this.orientation != 0f || pForce) {
            pCanvas.save()
            pCanvas.concat(if (pMapRotation) this.scaleRotateCanvasMatrix else this.invertedScaleRotateCanvasMatrix)
        }
    }

    /**
     * @since 6.0.0
     */
    fun restore(pCanvas: Canvas, pForce: Boolean) {
        if (this.orientation != 0f || pForce) {
            pCanvas.restore()
        }
    }

    /**
     * @since 6.0.0
     */
    private fun refresh() {
        // of course we could write mIntrinsicScreenRectProjection.centerX() and centerY()
        // but we should keep writing it that way (cf. ProjectionTest)
        fromPixels(this.screenCenterX, this.screenCenterY, this.currentCenter)

        if (this.orientation != 0f && this.orientation != 180f) {
            GeometryMath.getBoundingBoxForRotatatedRectangle(
                this.intrinsicScreenRect, this.screenCenterX, this.screenCenterY,
                this.orientation, this.screenRect
            )
        } else {
            // of course we could write mScreenRectProjection.set(mIntrinsicScreenRectProjection);
            // but we should keep writing it that way (cf. ProjectionTest)
            screenRect.left = intrinsicScreenRect.left
            screenRect.top = intrinsicScreenRect.top
            screenRect.right = intrinsicScreenRect.right
            screenRect.bottom = intrinsicScreenRect.bottom
        }

        var neGeoPoint = fromPixels(
            screenRect.right, screenRect.top, null, true
        )
        val tileSystem = getTileSystem()
        if (neGeoPoint.latitude > tileSystem.maxLatitude) {
            neGeoPoint = GeoPoint(tileSystem.maxLatitude, neGeoPoint.longitude)
        }
        if (neGeoPoint.latitude < tileSystem.minLatitude) {
            neGeoPoint = GeoPoint(tileSystem.minLatitude, neGeoPoint.longitude)
        }
        var swGeoPoint = fromPixels(
            screenRect.left, screenRect.bottom, null, true
        )
        if (swGeoPoint.latitude > tileSystem.maxLatitude) {
            swGeoPoint = GeoPoint(tileSystem.maxLatitude, swGeoPoint.longitude)
        }
        if (swGeoPoint.latitude < tileSystem.minLatitude) {
            swGeoPoint = GeoPoint(tileSystem.minLatitude, swGeoPoint.longitude)
        }

        boundingBox.set(
            neGeoPoint.latitude, neGeoPoint.longitude,
            swGeoPoint.latitude, swGeoPoint.longitude
        )
    }

    /**
     * Adjust the offsets so that this geo point projects into that pixel
     *
     * @since 6.0.0
     */
    fun adjustOffsets(pGeoPoint: IGeoPoint?, pPixel: PointF?) {
        if (pPixel == null) {
            return
        }
        if (pGeoPoint == null) {
            return
        }
        val unRotatedExpectedPixel = unrotateAndScalePoint(pPixel.x.toInt(), pPixel.y.toInt(), null)
        val unRotatedActualPixel = toPixels(pGeoPoint, null)
        val deltaX = (unRotatedExpectedPixel.x - unRotatedActualPixel.x).toLong()
        val deltaY = (unRotatedExpectedPixel.y - unRotatedActualPixel.y).toLong()
        adjustOffsets(deltaX, deltaY)
    }

    /**
     * Adjust the offsets so that
     * either this bounding box is bigger than the screen and contains it
     * or it is smaller and it is centered
     *
     * @since 6.0.0
     */
    @Deprecated("Use {@link #adjustOffsets(double, double, boolean, int)} instead")
    fun adjustOffsets(pBoundingBox: BoundingBox?) {
        if (pBoundingBox == null) {
            return
        }
        adjustOffsets(pBoundingBox.lonWest, pBoundingBox.lonEast, false, 0)
        adjustOffsets(pBoundingBox.actualNorth, pBoundingBox.actualSouth, true, 0)
    }

    /**
     * Adjust offsets so that north and south (if latitude, west and east if longitude)
     * actually "fit" into the screen, with a tolerance of extraSize pixels.
     * Used in order to ensure scroll limits.
     *
     * @since 6.0.0
     */
    fun adjustOffsets(
        pNorthOrWest: Double, pSouthOrEast: Double,
        isLatitude: Boolean, pExtraSize: Int
    ) {
        val min: Long
        val max: Long
        val deltaX: Long
        val deltaY: Long
        if (isLatitude) {
            min = getLongPixelYFromLatitude(pNorthOrWest)
            max = getLongPixelYFromLatitude(pSouthOrEast)
            deltaX = 0
            deltaY = getScrollableOffset(min, max, this.worldMapSize, intrinsicScreenRect.height(), pExtraSize)
        } else {
            min = getLongPixelXFromLongitude(pNorthOrWest)
            max = getLongPixelXFromLongitude(pSouthOrEast)
            deltaX = getScrollableOffset(min, max, this.worldMapSize, intrinsicScreenRect.width(), pExtraSize)
            deltaY = 0
        }
        adjustOffsets(deltaX, deltaY)
    }

    /**
     * @since 6.0.0
     */
    fun adjustOffsets(pDeltaX: Long, pDeltaY: Long) {
        if (pDeltaX == 0L && pDeltaY == 0L) {
            return
        }
        this.offsetX += pDeltaX
        this.offsetY += pDeltaY
        mScrollX -= pDeltaX
        mScrollY -= pDeltaY
        refresh()
    }

    /**
     * @since 6.0.0
     */
    fun setMapScroll(pMapView: MapView): Boolean {
        if (pMapView.getMapScrollX() == mScrollX && pMapView.getMapScrollY() == mScrollY) {
            return false
        }
        pMapView.setMapScroll(mScrollX, mScrollY)
        return true
    }

    val width: Int
        /**
         * @since 6.1.0
         */
        get() = intrinsicScreenRect.width()

    val height: Int
        /**
         * @since 6.1.0
         */
        get() = intrinsicScreenRect.height()

    companion object {
        /**
         * The size in pixels of a VERY large map, the "projected" map.
         * For optimization purpose, we may compute only once the projection of the GeoPoints
         * on this large map, and then just divide in order to get the projection on a corresponding
         * smaller map / smaller zoom
         */
        @JvmField
        val mProjectedMapSize: Double = (1L shl 60).toDouble()

        /**
         * @param pPixelMin   Pixel position of the limit (left)
         * @param pPixelMax   Pixel position of the limit (right)
         * @param pWorldSize  World map size - for modulo adjustments
         * @param pScreenSize Screen size
         * @param pExtraSize  Extra size to consider at each side of the screen
         * @return the offset to apply so that the limits are within the screen
         * @since 6.0.0
         */
        @JvmStatic
        fun getScrollableOffset(
            pPixelMin: Long, pPixelMax: Long,
            pWorldSize: Double,
            pScreenSize: Int, pExtraSize: Int
        ): Long {
            var pPixelMax = pPixelMax
            while (pPixelMax - pPixelMin < 0) { // date line + several worlds fix
                pPixelMax = (pPixelMax + pWorldSize).toLong()
            }

            var delta: Long
            if (pPixelMax - pPixelMin < pScreenSize - 2 * pExtraSize) {
                val half = (pPixelMax - pPixelMin) / 2
                if (((pScreenSize / 2 - half - pPixelMin).also { delta = it }) > 0) {
                    return delta
                }
                if (((pScreenSize / 2 + half - pPixelMax).also { delta = it }) < 0) {
                    return delta
                }
                return 0
            }
            if (((pExtraSize - pPixelMin).also { delta = it }) < 0) {
                return delta
            }
            if (((pScreenSize - pExtraSize - pPixelMax).also { delta = it }) > 0) {
                return delta
            }
            return 0
        }
    }
}
