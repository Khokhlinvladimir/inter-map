package org.osmdroid.views.overlay

import android.graphics.Path
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.Distance
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.IntegerAccepter
import org.osmdroid.util.LineBuilder
import org.osmdroid.util.ListPointAccepter
import org.osmdroid.util.ListPointL
import org.osmdroid.util.PathBuilder
import org.osmdroid.util.PointAccepter
import org.osmdroid.util.PointL
import org.osmdroid.util.SegmentClipper
import org.osmdroid.util.SideOptimizationPointAccepter
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.Projection
import org.osmdroid.views.util.constants.MathConstants
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Class holding one ring: the polygon outline, or a hole inside the polygon
 * Used to be an inner class of [Polygon] and [Polyline]
 *
 * @author Fabrice Fontaine
 * @since 6.0.0
 */
class LinearRing {
    /**
     * We build a virtual area [mClipMin, mClipMin, mClipMax, mClipMax]
     * used to clip our Path in order to cope with
     * - very high pixel values (that go beyond the int values, for instance on zoom 29)
     * - some kind of Android bug related to hardware acceleration
     * "One size fits all" clip area values cannot be determined
     * as there's not explicit value given by Android to avoid Path drawing issues.
     * If the size is too big (magnitude around Integer.MAX_VALUE), the Path won't show properly.
     * If it's small (just above the size of the screen), the approximations of the clipped Path
     * may look gross, particularly if you zoom out in animation.
     * The smaller it is, the better it is for performances because the clip
     * will then often approximate consecutive Path segments as identical, and we only add
     * distinct points to the Path as an optimization.
     * The best idea so far is to compute the clip area border values
     * from the current MapView's characteristics (width, height, scale, orientation)
     */
    private val mOriginalPoints = java.util.ArrayList<GeoPoint>()
    private var mDistances: DoubleArray? = null
    private var mProjectedPoints: LongArray? = null
    private val mProjectedCenter = PointL()
    private val mSegmentClipper = SegmentClipper()
    private val mPath: Path?
    private val mBoundingBox = BoundingBox()
    private var mProjectedPrecomputed = false
    private var mDistancesPrecomputed = false
    private var isHorizontalRepeating = true
    private var isVerticalRepeating = true

    /**
     * @since 6.0.0
     */
    private val mPointsForMilestones: ListPointL = ListPointL()
    private val mPointAccepter: PointAccepter
    private val mIntegerAccepter: IntegerAccepter?
    private var mGeodesic: Boolean = false

    /**
     * @since 6.1.0
     */
    private val mClosed: Boolean

    /**
     * @since 6.2.0
     */
    private var mDowngradePointList: FloatArray? = null
    private var mDowngradePixelSize = 0
    private var mProjectedWidth: Long = 0
    private var mProjectedHeight: Long = 0

    /**
     * Dedicated to lines
     *
     * @since 6.2.0
     */
    /**
     * Dedicated to lines
     *
     * @since 6.0.0
     */
    @JvmOverloads
    constructor(pLineBuilder: LineBuilder, pClosePath: Boolean = false) {
        mPath = null
        mPointAccepter = pLineBuilder
        if (pLineBuilder is LineDrawer) {
            mIntegerAccepter = IntegerAccepter(pLineBuilder.lines.size / 2)
            pLineBuilder.setIntegerAccepter(mIntegerAccepter)
        } else {
            mIntegerAccepter = null
        }
        mClosed = pClosePath
    }

    /**
     * @since 6.1.0
     */
    /**
     * Dedicated to `Path`
     */
    @JvmOverloads
    constructor(pPath: Path, pClosed: Boolean = true) {
        mPath = pPath
        mPointAccepter = SideOptimizationPointAccepter(PathBuilder(pPath))
        mIntegerAccepter = null
        mClosed = pClosed
    }

    fun clearPath() {
        mOriginalPoints.clear()
        mProjectedPoints = null
        mDistances = null
        resetPrecomputations()
        mPointAccepter.init()
    }

    protected fun addGreatCircle(startPoint: GeoPoint, endPoint: GeoPoint, numberOfPoints: Int) {
        //	adapted from page http://compastic.blogspot.co.uk/2011/07/how-to-draw-great-circle-on-map-in.html
        //	which was adapted from page http://maps.forum.nu/gm_flight_path.html

        // convert to radians

        val lat1 = startPoint.latitude * MathConstants.DEG2RAD
        val lon1 = startPoint.longitude * MathConstants.DEG2RAD
        val lat2 = endPoint.latitude * MathConstants.DEG2RAD
        val lon2 = endPoint.longitude * MathConstants.DEG2RAD

        val d = 2 * asin(sqrt(sin((lat1 - lat2) / 2).pow(2.0) + (cos(lat1) * cos(lat2) * sin((lon1 - lon2) / 2).pow(2.0))))

        /*
		double bearing = Math.atan2(Math.sin(lon1 - lon2) * Math.cos(lat2),
				Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2))
				/ -MathConstants.DEG2RAD;
		bearing = bearing < 0 ? 360 + bearing : bearing;
		*/
        for (i in 1..numberOfPoints) {
            val f = 1.0 * i / (numberOfPoints + 1)
            val A = sin((1 - f) * d) / sin(d)
            val B = sin(f * d) / sin(d)
            val x = A * cos(lat1) * cos(lon1) + B * cos(lat2) * cos(lon2)
            val y = A * cos(lat1) * sin(lon1) + B * cos(lat2) * sin(lon2)
            val z = A * sin(lat1) + B * sin(lat2)

            val latN = atan2(z, sqrt(x.pow(2.0) + y.pow(2.0)))
            val lonN = atan2(y, x)
            val p = GeoPoint(latN * MathConstants.RAD2DEG, lonN * MathConstants.RAD2DEG)
            mOriginalPoints.add(p)
        }
    }

    fun addPoint(p: GeoPoint) {
        if (mGeodesic && mOriginalPoints.size > 0) {
            //add potential intermediate points:
            val prev = mOriginalPoints.get(mOriginalPoints.size - 1)
            val greatCircleLength = prev.distanceToAsDouble(p).toInt()
            //add one point for every 100kms of the great circle path
            val numberOfPoints = greatCircleLength / 100000
            addGreatCircle(prev, p, numberOfPoints)
        }
        mOriginalPoints.add(p)
        resetPrecomputations()
    }

    /**
     * @since 6.2.0
     */
    private fun resetPrecomputations() {
        mProjectedPrecomputed = false
        mDistancesPrecomputed = false
        mDowngradePixelSize = 0
        mDowngradePointList = null
    }

    fun getPoints(): ArrayList<GeoPoint> = mOriginalPoints

    fun setPoints(points: List<GeoPoint>) {
        clearPath()
        for (point in points) {
            addPoint(point)
        }
    }

    fun getDistances(): DoubleArray {
        computeDistances()
        return mDistances!!
    }

    fun getDistance(): Double {
        var result = 0.0
        for (distance in getDistances()) {
            result += distance
        }
        return result
    }

    fun setGeodesic(geodesic: Boolean) {
        mGeodesic = geodesic
    }

    fun isGeodesic(): Boolean = mGeodesic

    fun getPointsForMilestones(): ListPointL = mPointsForMilestones

    /**
     * Feed the path with the segments corresponding to the GeoPoint pairs
     * projected using pProjection and clipped into a "reasonable" clip area
     * In most cases (Polygon without holes, Polyline) the offset parameter will be null.
     * In the case of a Polygon with holes, the first path will use a null offset.
     * Then this method will return the pixel offset computed for this path so that
     * the path is in the best possible place on the map:
     * the center of all pixels is as close to the screen center as possible
     * Then, this computed offset must be injected into the buildPathPortion for each hole,
     * in order to have the main polygon and its holes at the same place on the map.
     *
     * @return the initial offset if not null, or the computed offset
     */
    fun buildPathPortion(
        pProjection: Projection,
        pOffset: PointL?,
        pStorePoints: Boolean
    ): PointL? {
        val size = mOriginalPoints.size
        if (size < 2) { // nothing to paint
            return pOffset
        }
        computeProjected()
        computeDistances()
        val offset: PointL?
        if (pOffset != null) {
            offset = pOffset
        } else {
            offset = PointL()
            getBestOffset(pProjection, offset)
        }
        mSegmentClipper.init()
        clipAndStore(pProjection, offset, mClosed, pStorePoints, mSegmentClipper)
        mSegmentClipper.end()
        if (mClosed) {
            mPath!!.close()
        }
        return offset
    }

    /**
     * Dedicated to Polyline, as they can run much faster with drawLine than through a Path
     *
     * @since 6.0.0
     */
    fun buildLinePortion(
        pProjection: Projection,
        pStorePoints: Boolean
    ) {
        val size = mOriginalPoints.size
        if (size < 2) { // nothing to paint
            return
        }
        computeProjected()
        computeDistances()
        val offset = PointL()
        getBestOffset(pProjection, offset)
        mSegmentClipper.init()
        clipAndStore(pProjection, offset, mClosed, pStorePoints, mSegmentClipper)
        mSegmentClipper.end()
    }

    /**
     * Compute the pixel offset so that a list of pixel segments display in the best possible way:
     * the center of all pixels is as close to the screen center as possible
     * This notion of pixel offset only has a meaning on very low zoom level,
     * when a GeoPoint can be projected on different places on the screen.
     */
    private fun getBestOffset(pProjection: Projection, pOffset: PointL) {
        val powerDifference = pProjection.projectedPowerDifference
        val center = pProjection.getLongPixelsFromProjected(
            mProjectedCenter, powerDifference, false, null
        )
        getBestOffset(pProjection, pOffset, center!!)
    }

    /**
     * @since 6.2.0
     */
    fun getBestOffset(pProjection: Projection, pOffset: PointL, pPixel: PointL) {
        val screenRect = pProjection.intrinsicScreenRect
        val screenCenterX = (screenRect.left + screenRect.right) / 2.0
        val screenCenterY = (screenRect.top + screenRect.bottom) / 2.0
        val worldSize = pProjection.worldMapSize
        getBestOffset(pPixel.x.toDouble(), pPixel.y.toDouble(), screenCenterX, screenCenterY, worldSize, pOffset)
    }

    /**
     * @since 6.0.0
     */
    private fun getBestOffset(
        pPolyCenterX: Double, pPolyCenterY: Double,
        pScreenCenterX: Double, pScreenCenterY: Double,
        pWorldSize: Double, pOffset: PointL
    ) {
        val worldSize = Math.round(pWorldSize)
        var deltaPositive: Int
        var deltaNegative: Int
        if (!isVerticalRepeating) {
            deltaPositive = 0
            deltaNegative = 0
        } else {
            deltaPositive = getBestOffset(
                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, 0, worldSize
            )
            deltaNegative = getBestOffset(
                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, 0, -worldSize
            )
        }
        pOffset.y = worldSize * (if (deltaPositive > deltaNegative) deltaPositive else -deltaNegative)
        if (!isHorizontalRepeating) {
            deltaPositive = 0
            deltaNegative = 0
        } else {
            deltaPositive = getBestOffset(
                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, worldSize, 0
            )
            deltaNegative = getBestOffset(
                pPolyCenterX, pPolyCenterY, pScreenCenterX, pScreenCenterY, -worldSize, 0
            )
        }
        pOffset.x = worldSize * (if (deltaPositive > deltaNegative) deltaPositive else -deltaNegative)
    }

    /**
     * @since 6.0.0
     */
    private fun getBestOffset(
        pPolyCenterX: Double, pPolyCenterY: Double,
        pScreenCenterX: Double, pScreenCenterY: Double,
        pDeltaX: Long, pDeltaY: Long
    ): Int {
        var squaredDistance = 0.0
        var i = 0
        while (true) {
            val tmpSquaredDistance = Distance.getSquaredDistanceToPoint(
                pPolyCenterX + i * pDeltaX, pPolyCenterY + i * pDeltaY,
                pScreenCenterX, pScreenCenterY
            )
            if (i == 0 || squaredDistance > tmpSquaredDistance) {
                squaredDistance = tmpSquaredDistance
                i++
            } else {
                break
            }
        }
        return i - 1
    }

    /**
     * @since 6.0.0
     */
    private fun clipAndStore(
        pProjection: Projection, pOffset: PointL,
        pClosePath: Boolean, pStorePoints: Boolean,
        pSegmentClipper: SegmentClipper?
    ) {
        mPointsForMilestones.clear()
        val powerDifference = pProjection.projectedPowerDifference
        val projected = PointL()
        val point = PointL()
        val first = PointL()
        var i = 0
        while (i < mProjectedPoints!!.size) {
            projected.set(mProjectedPoints!![i], mProjectedPoints!![i + 1])
            pProjection.getLongPixelsFromProjected(projected, powerDifference, false, point)
            val x = point.x + pOffset.x
            val y = point.y + pOffset.y
            if (pStorePoints) {
                mPointsForMilestones.add(x, y)
            }
            if (pSegmentClipper != null) {
                pSegmentClipper.add(x, y)
            }
            if (i == 0) {
                first.set(x, y)
            }
            i += 2
        }
        if (pClosePath) {
            if (pSegmentClipper != null) {
                pSegmentClipper.add(first.x, first.y)
            }
            if (pStorePoints) {
                mPointsForMilestones.add(first.x, first.y)
            }
        }
    }

    /**
     * We want consecutive projected points to be as close as possible,
     * and not a world away (typically when dealing with very low zoom levels)
     */
    private fun setCloserPoint(
        pPrevious: PointL, pNext: PointL,
        pWorldSize: Double
    ) {
        if (isHorizontalRepeating) {
            pNext.x = Math.round(getCloserValue(pPrevious.x.toDouble(), pNext.x.toDouble(), pWorldSize))
        }
        if (isVerticalRepeating) {
            pNext.y = Math.round(getCloserValue(pPrevious.y.toDouble(), pNext.y.toDouble(), pWorldSize))
        }
    }

    /**
     * Detection is done in screen coordinates.
     *
     * @param tolerance in pixels
     * @return true if the Polyline is close enough to the point.
     */
    fun isCloseTo(
        pPoint: GeoPoint?, tolerance: Double,
        pProjection: Projection, pClosePath: Boolean
    ): Boolean {
        return getCloseTo(pPoint, tolerance, pProjection, pClosePath) != null
    }

    /**
     * @param tolerance in pixels
     * @return the first GeoPoint of the Polyline close enough to the point
     * @since 6.0.3
     * Detection is done in screen coordinates.
     */
    fun getCloseTo(
        pPoint: GeoPoint?, tolerance: Double,
        pProjection: Projection, pClosePath: Boolean
    ): GeoPoint? {
        computeProjected()
        val pixel = pProjection.toPixels(pPoint, null)
        val offset = PointL()
        getBestOffset(pProjection, offset)
        clipAndStore(pProjection, offset, pClosePath, true, null)
        val mapSize = pProjection.worldMapSize
        val screenRect = pProjection.intrinsicScreenRect
        val screenWidth = screenRect.width()
        val screenHeight = screenRect.height()
        var startX = pixel!!.x.toDouble() // in order to deal with world replication
        while (startX - mapSize >= 0) {
            startX -= mapSize
        }
        var startY = pixel.y.toDouble()
        while (startY - mapSize >= 0) {
            startY -= mapSize
        }
        val squaredTolerance = tolerance * tolerance
        val point0 = PointL()
        val point1 = PointL()
        var first = true
        var index = 0
        for (point in mPointsForMilestones) {
            point1.set(point ?: continue)
            if (first) {
                first = false
            } else {
                var x = startX
                while (x < screenWidth) {
                    var y = startY
                    while (y < screenHeight) {
                        val projectionFactor = Distance.getProjectionFactorToSegment(
                            x,
                            y,
                            point0.x.toDouble(),
                            point0.y.toDouble(),
                            point1.x.toDouble(),
                            point1.y.toDouble()
                        )
                        val squaredDistance = Distance.getSquaredDistanceToProjection(
                            x,
                            y,
                            point0.x.toDouble(),
                            point0.y.toDouble(),
                            point1.x.toDouble(),
                            point1.y.toDouble(),
                            projectionFactor
                        )
                        if (squaredTolerance > squaredDistance) {
                            val pointAX = mProjectedPoints!![2 * (index - 1)]
                            val pointAY = mProjectedPoints!![2 * (index - 1) + 1]
                            val pointBX = mProjectedPoints!![2 * index]
                            val pointBY = mProjectedPoints!![2 * index + 1]
                            val projectionX = (pointAX + (pointBX - pointAX) * projectionFactor).toLong()
                            val projectionY = (pointAY + (pointBY - pointAY) * projectionFactor).toLong()
                            return getTileSystem().getGeoFromMercator(
                                projectionX, projectionY, Projection.Companion.mProjectedMapSize,
                                null, false, false
                            )
                        }
                        y += mapSize
                    }
                    x += mapSize
                }
            }
            point0.set(point1)
            index++
        }
        return null
    }

    /**
     * @since 6.0.0
     * Mandatory use before clipping.
     */
    fun setClipArea(pXMin: Long, pYMin: Long, pXMax: Long, pYMax: Long) {
        mSegmentClipper.set(pXMin, pYMin, pXMax, pYMax, mPointAccepter, mIntegerAccepter, mPath != null)
    }

    /**
     * @since 6.0.0
     * Mandatory use before clipping.
     */
    fun setClipArea(pProjection: Projection) {
        val border = .1
        val rect = pProjection.intrinsicScreenRect
        val halfWidth = rect.width() / 2
        val halfHeight = rect.height() / 2
        // People less lazy than me would do more refined computations for width and height
        // that include the map orientation: the covered area would be smaller but still big enough
        // Now we use the circle which contains the `MapView`'s 4 corners
        val radius = sqrt((halfWidth * halfWidth + halfHeight * halfHeight).toDouble())
        // cf. https://github.com/osmdroid/osmdroid/issues/1528
        // People less lazy than me would not double the radius and rather fix the pixel coordinates:
        // using Projection.getScreenCenterX() and Y() would certainly make sense
        // instead of halfWidth and halfHeight, and that could improve performances
        // to have a smaller radius (in that case, not doubled)
        val doubleRadius = 2 * radius
        val scaledRadius = (doubleRadius * (1 + border)).toInt()
        setClipArea(
            (halfWidth - scaledRadius).toLong(), (halfHeight - scaledRadius).toLong(),
            (halfWidth + scaledRadius).toLong(), (halfHeight + scaledRadius).toLong()
        )
        // TODO: Not sure if this is the correct approach
        this.isHorizontalRepeating = pProjection.isHorizontalWrapEnabled
        this.isVerticalRepeating = pProjection.isVerticalWrapEnabled
    }

    /**
     * @since 6.0.2
     */
    fun getCenter(pReuse: GeoPoint?): GeoPoint {
        val out = if (pReuse != null) pReuse else GeoPoint(0.0, 0.0)
        val boundingBox = getBoundingBox()
        out.latitude = boundingBox.centerLatitude
        out.longitude = boundingBox.centerLongitude
        return out
    }

    /**
     * @since 6.0.3
     * Code comes from now gone method computeProjectedAndDistances
     */
    private fun computeProjected() {
        if (mProjectedPrecomputed) {
            return
        }
        mProjectedPrecomputed = true
        if (mProjectedPoints == null || mProjectedPoints!!.size != mOriginalPoints.size * 2) {
            mProjectedPoints = LongArray(mOriginalPoints.size * 2)
        }
        var minX: Long = 0
        var maxX: Long = 0
        var minY: Long = 0
        var maxY: Long = 0
        var north = 0.0
        var east = 0.0
        var south = 0.0
        var west = 0.0
        var index = 0
        val previous = PointL()
        val current = PointL()
        val tileSystem = getTileSystem()
        val projectedMapSize: Double = Projection.Companion.mProjectedMapSize
        for (currentGeo in mOriginalPoints) {
            val latitude = currentGeo.latitude
            val longitude = currentGeo.longitude
            tileSystem.getMercatorFromGeo(latitude, longitude, projectedMapSize, current, false)
            if (index == 0) {
                maxX = current.x
                minX = maxX
                maxY = current.y
                minY = maxY
                south = latitude
                north = south
                west = longitude
                east = west
            } else {
                setCloserPoint(previous, current, projectedMapSize)
                if (minX > current.x) {
                    minX = current.x
                    west = longitude
                }
                if (maxX < current.x) {
                    maxX = current.x
                    east = longitude
                }
                if (minY > current.y) {
                    minY = current.y
                    north = latitude
                }
                if (maxY < current.y) {
                    maxY = current.y
                    south = latitude
                }
            }
            mProjectedPoints!![2 * index] = current.x
            mProjectedPoints!![2 * index + 1] = current.y
            previous.set(current.x, current.y)
            index++
        }
        mProjectedWidth = maxX - minX
        mProjectedHeight = maxY - minY
        mProjectedCenter.set((minX + maxX) / 2, (minY + maxY) / 2)
        mBoundingBox.set(north, east, south, west)
    }

    /**
     * @since 6.0.3
     * Code comes from now gone method computeProjectedAndDistances
     */
    private fun computeDistances() {
        if (mDistancesPrecomputed) {
            return
        }
        mDistancesPrecomputed = true
        if (mDistances == null || mDistances!!.size != mOriginalPoints.size) {
            mDistances = DoubleArray(mOriginalPoints.size)
        }
        var index = 0
        val previousGeo = GeoPoint(0.0, 0.0)
        for (currentGeo in mOriginalPoints) {
            if (index == 0) {
                mDistances!![index] = 0.0
            } else {
                mDistances!![index] = currentGeo.distanceToAsDouble(previousGeo)
            }
            previousGeo.setCoords(currentGeo.latitude, currentGeo.longitude)
            index++
        }
    }

    fun getBoundingBox(): BoundingBox {
        if (!mProjectedPrecomputed) {
            computeProjected()
        }
        return mBoundingBox
    }

    /**
     * @since 6.2.0
     */
    fun clear() {
        mOriginalPoints.clear()
        if (mPath != null) {
            mPath.reset()
        }

        mPointsForMilestones.clear()
    }

    /**
     * Computes the list of points of a polyline that would be the projection of the GeoPoints
     * on a centered size*size square
     *
     * @since 6.2.0
     */
    fun computeDowngradePointList(pSize: Int): FloatArray? {
        if (pSize == 0) {
            return null
        }
        if (mDowngradePixelSize == pSize) {
            return mDowngradePointList
        }
        computeProjected()
        val projectedSize = if (mProjectedWidth > mProjectedHeight) mProjectedWidth else mProjectedHeight
        if (projectedSize == 0L) {
            return null
        }
        val listPointAccepter = ListPointAccepter(true)
        val pointAccepter: PointAccepter = SideOptimizationPointAccepter(listPointAccepter)
        val factor = (projectedSize * 1.0) / pSize
        run {
            var i = 0
            while (i < mProjectedPoints!!.size) {
                val x = mProjectedPoints!![i++]
                val y = mProjectedPoints!![i++]
                val squareX = Math.round((x - mProjectedCenter.x) / factor)
                val squareY = Math.round((y - mProjectedCenter.y) / factor)
                pointAccepter.add(squareX, squareY)
            }
        }
        mDowngradePixelSize = pSize
        mDowngradePointList = FloatArray(listPointAccepter.list.size)
        for (i in mDowngradePointList!!.indices) {
            mDowngradePointList!![i] = listPointAccepter.list[i]!!.toFloat()
        }
        return mDowngradePointList
    }

    companion object {
        /**
         * @since 6.2.0
         */
        @JvmStatic
        fun getCloserValue(pPrevious: Double, pNext: Double, pWorldSize: Double): Double {
            var pNext = pNext
            while (abs(pNext - pWorldSize - pPrevious) < abs(pNext - pPrevious)) {
                pNext -= pWorldSize
            }
            while (abs(pNext + pWorldSize - pPrevious) < abs(pNext - pPrevious)) {
                pNext += pWorldSize
            }
            return pNext
        }
    }
}
