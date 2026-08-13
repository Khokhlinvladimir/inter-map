package org.osmdroid.views.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Region
import android.view.MotionEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.Distance
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.PointL
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.milestones.MilestoneManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Repository of common methods for Polyline and Polygon
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
abstract class PolyOverlayWithIW protected constructor(pMapView: MapView?, pUsePath: Boolean, pClosePath: Boolean) : OverlayWithIW() {
    protected var mOutline: LinearRing? = null
    protected var mHoles: MutableList<LinearRing>? = ArrayList<LinearRing>()
    protected var mOutlinePaint: Paint = Paint()
    protected var mFillPaint: Paint? = null
    private val mOutlinePaintLists: MutableList<PaintList> = ArrayList<PaintList>()
    private var mMilestoneManagers: MutableList<MilestoneManager>? = ArrayList<MilestoneManager>()
    /**
     * @return the geopoint location where the infowindow should point at.
     * Doesn't matter if the infowindow is currently opened or not.
     */
    /**
     * Sets the info window anchor point to a geopoint location
     */
    private var mInfoWindowLocation: GeoPoint? = null

    private var mLineDrawer: LineDrawer? = null
    protected var mPath: Path? = null
    protected var mDensity: Float = 1.0f

    /**
     * @since 6.2.0
     */
    private var mIsPaintOrPaintList = true
    private val mVisibilityProjectedCenter = PointL()
    private val mVisibilityProjectedCorner = PointL()
    private val mVisibilityRectangleCenter = PointL()
    private val mVisibilityRectangleCorner = PointL()

    /**
     * @since 6.2.0
     */
    private var mDowngradeMaximumPixelSize = 0
    private var mDowngradeMaximumRectanglePixelSize = 0
    private var mDowngradeDisplay = false
    private val mDowngradeTopLeft = Point()
    private val mDowngradeBottomRight = Point()
    private val mDowngradeCenter = PointL()
    private val mDowngradeOffset = PointL()
    private var mDowngradeSegments: FloatArray? = null

    /**
     * @since 6.2.0
     * Used to be in [Polyline]
     */
    private var mDensityMultiplier = 1.0f
    private val mClosePath: Boolean

    init {
        mClosePath = pClosePath
        if (pMapView != null) {
            setInfoWindow(pMapView.getRepository().defaultPolylineInfoWindow)
            mDensity = pMapView.getContext().getResources().getDisplayMetrics().density
        }
        usePath(pUsePath)
    }

    /**
     * @since 6.2.0
     * Use Path or not for the display
     * drawPath can be notoriously slower than drawLines, therefore when relevant "Polygon"s
     * would be better off if displayed with drawLines.
     * On the other hand, drawPath sometimes looks better, therefore when relevant "Polyline"s
     * would be better off if displayed with drawPath
     */
    fun usePath(pUsePath: Boolean) {
        val previousPoints = if (mOutline == null) null else mOutline!!.getPoints()
        if (pUsePath) {
            val path = Path()
            mPath = path
            mLineDrawer = null
            mOutline = LinearRing(path, mClosePath)
        } else {
            mPath = null
            val lineDrawer = LineDrawer(256)
            mLineDrawer = lineDrawer
            mOutline = LinearRing(lineDrawer, mClosePath)
            lineDrawer.setPaint(mOutlinePaint)
        }
        if (previousPoints != null) {
            setPoints(previousPoints)
        }
    }

    fun setVisible(visible: Boolean) = setEnabled(visible)

    fun isVisible(): Boolean = isEnabled()

    fun getOutlinePaint(): Paint
        /**
         * @return the Paint used for the outline. This allows to set advanced Paint settings.
         */
        {
            mIsPaintOrPaintList = true
            return mOutlinePaint
        }

    fun getOutlinePaintLists(): MutableList<PaintList>
        /**
         * assuming if someone uses this method, someone wants to use List<PaintList>
         * instead of mere Paint
        </PaintList> */
        {
            mIsPaintOrPaintList = false
            return mOutlinePaintLists
        }

    protected open fun getFillPaint(): Paint?
        /**
         * @return the Paint used for the filling. This allows to set advanced Paint settings.
         */
        = mFillPaint

    fun isGeodesic(): Boolean = mOutline!!.isGeodesic()

        /**
         * Sets whether to draw each segment of the line as a geodesic or not.
         * Warning: it takes effect only if set before setting the points in the Polyline.
         */
    fun setGeodesic(geodesic: Boolean) = mOutline!!.setGeodesic(geodesic)

    fun setInfoWindowLocation(location: GeoPoint?) {
        mInfoWindowLocation = location
    }

    fun getInfoWindowLocation(): GeoPoint? = mInfoWindowLocation

    /**
     * Set the InfoWindow to be used.
     * Default is a BasicInfoWindow, with the layout named "bonuspack_bubble".
     * You can use this method either to use your own layout, or to use your own sub-class of InfoWindow.
     * If you don't want any InfoWindow to open, you can set it to null.
     */
    override fun setInfoWindow(infoWindow: InfoWindow?) {
        if (mInfoWindow != null) {
            if (mInfoWindow!!.relatedObject === this) mInfoWindow!!.relatedObject = null
        }
        mInfoWindow = infoWindow
    }

    /**
     * Show the infowindow, if any. It will be opened either at the latest location, if any,
     * or to a default location computed by setDefaultInfoWindowLocation method.
     * Note that you can manually set this location with: setInfoWindowLocation
     */
    fun showInfoWindow() {
        if (mInfoWindow != null && mInfoWindowLocation != null) mInfoWindow!!.open(this, mInfoWindowLocation, 0, 0)
    }

    fun setMilestoneManagers(pMilestoneManagers: MutableList<MilestoneManager>?) {
        if (pMilestoneManagers == null) {
            if (mMilestoneManagers!!.size > 0) {
                mMilestoneManagers!!.clear()
            }
        } else {
            mMilestoneManagers = pMilestoneManagers
        }
    }

    open fun getDistance(): Double
        /**
         * @return aggregate distance (in meters)
         */
        = mOutline!!.getDistance()

    /**
     * Internal method used to ensure that the infowindow will have a default position in all cases,
     * so that the user can call showInfoWindow even if no tap occured before.
     * Currently, set the position on the center of the polygon bounding box.
     */
    protected fun setDefaultInfoWindowLocation() {
        val s = mOutline!!.getPoints().size
        if (s == 0) {
            mInfoWindowLocation = GeoPoint(0.0, 0.0)
            return
        }
        if (mInfoWindowLocation == null) {
            mInfoWindowLocation = GeoPoint(0.0, 0.0)
        }
        mOutline!!.getCenter(mInfoWindowLocation!!)
    }

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        if (!isVisible(pProjection)) {
            return
        }

        if (mDowngradeMaximumPixelSize > 0) {
            if (!isWorthDisplaying(pProjection)) {
                if (mDowngradeDisplay) {
                    displayDowngrade(pCanvas, pProjection)
                }
                return
            }
        }

        if (mPath != null) {
            drawWithPath(pCanvas, pProjection)
        } else {
            drawWithLines(pCanvas, pProjection)
        }
    }

    /**
     * @since 6.2.0
     * We pre-check if it's worth computing and drawing a poly.
     * How do we do that? As an approximation, we consider both the poly and the screen as disks.
     * Then, we compute the distance between both centers: if it's greater than the sum of the radii
     * the poly won't be visible.
     */
    private fun isVisible(pProjection: Projection): Boolean {
        // projecting the center and a corner of the bounding box to the screen, close to the screen center
        val boundingBox = getBounds()
        pProjection.toProjectedPixels(
            boundingBox.centerLatitude, boundingBox.centerLongitude,
            mVisibilityProjectedCenter
        )
        pProjection.toProjectedPixels(
            boundingBox.latNorth, boundingBox.lonEast,
            mVisibilityProjectedCorner
        )
        pProjection.getLongPixelsFromProjected(
            mVisibilityProjectedCenter,
            pProjection.projectedPowerDifference, true, mVisibilityRectangleCenter
        )
        pProjection.getLongPixelsFromProjected(
            mVisibilityProjectedCorner,
            pProjection.projectedPowerDifference, true, mVisibilityRectangleCorner
        )

        // computing the distance and the radii
        val screenCenterX = pProjection.width / 2
        val screenCenterY = pProjection.height / 2
        val radius = sqrt(
            Distance.getSquaredDistanceToPoint(
                mVisibilityRectangleCenter.x.toDouble(), mVisibilityRectangleCenter.y.toDouble(),
                mVisibilityRectangleCorner.x.toDouble(), mVisibilityRectangleCorner.y.toDouble()
            )
        )
        val distanceBetweenCenters = sqrt(
            Distance.getSquaredDistanceToPoint(
                mVisibilityRectangleCenter.x.toDouble(), mVisibilityRectangleCenter.y.toDouble(),
                screenCenterX.toDouble(), screenCenterY.toDouble()
            )
        )
        val screenRadius = sqrt(
            Distance.getSquaredDistanceToPoint(
                0.0, 0.0,
                screenCenterX.toDouble(), screenCenterY.toDouble()
            )
        )

        return distanceBetweenCenters <= radius + screenRadius
    }

    private fun drawWithPath(pCanvas: Canvas, pProjection: Projection) {
        mPath!!.rewind()

        mOutline!!.setClipArea(pProjection)
        val offset = mOutline!!.buildPathPortion(pProjection, null, mMilestoneManagers!!.size > 0)
        for (milestoneManager in mMilestoneManagers!!) {
            milestoneManager.init()
            milestoneManager.setDistances(mOutline!!.getDistances())
            for (point in mOutline!!.getPointsForMilestones()) {
                point ?: continue
                milestoneManager.add(point.x, point.y)
            }
            milestoneManager.end()
        }

        val holes = mHoles
        if (holes != null) {
            for (hole in holes) {
                hole.setClipArea(pProjection)
                hole.buildPathPortion(pProjection, offset, mMilestoneManagers!!.size > 0)
            }
            mPath!!.setFillType(Path.FillType.EVEN_ODD) //for correct support of holes
        }

        if (isVisible(mFillPaint)) {
            pCanvas.drawPath(mPath!!, mFillPaint!!)
        }
        if (isVisible(mOutlinePaint)) {
            pCanvas.drawPath(mPath!!, mOutlinePaint)
        }

        for (milestoneManager in mMilestoneManagers!!) {
            milestoneManager.draw(pCanvas)
        }

        if (isInfoWindowOpen() && mInfoWindow?.relatedObject === this) {
            mInfoWindow!!.draw()
        }
    }

    private fun drawWithLines(pCanvas: Canvas, pProjection: Projection) {
        mLineDrawer!!.setCanvas(pCanvas)
        mOutline!!.setClipArea(pProjection)
        var storePoints = mMilestoneManagers!!.size > 0
        if (mIsPaintOrPaintList) {
            val paint = getOutlinePaint()
            mLineDrawer!!.setPaint(paint)
            mOutline!!.buildLinePortion(pProjection, storePoints)
        } else {
            for (paintList in getOutlinePaintLists()) {
                mLineDrawer!!.setPaint(paintList)
                mOutline!!.buildLinePortion(pProjection, storePoints)
                storePoints = false
            }
        }
        for (milestoneManager in mMilestoneManagers!!) {
            milestoneManager.init()
            milestoneManager.setDistances(mOutline!!.getDistances())
            for (point in mOutline!!.getPointsForMilestones()) {
                point ?: continue
                milestoneManager.add(point.x, point.y)
            }
            milestoneManager.end()
        }

        for (milestoneManager in mMilestoneManagers!!) {
            milestoneManager.draw(pCanvas)
        }
        if (isInfoWindowOpen() && mInfoWindow?.relatedObject === this) {
            mInfoWindow!!.draw()
        }
    }

    override fun onDetach(mapView: MapView?) {
        if (mOutline != null) {
            mOutline!!.clear()
            mOutline = null
        }
        mHoles!!.clear()
        mMilestoneManagers!!.clear()
        onDestroy()
    }

    /**
     * @since 6.2.0
     */
    override fun getBounds(): BoundingBox {
        return mOutline!!.getBoundingBox()
    }

    /**
     * @since 6.2.0
     * Used to be in [Polygon] and [Polyline]
     * Set the points of the outline.
     * Note that a later change in the original points List will have no effect.
     * To remove/change points, you must call setPoints again.
     * If geodesic mode has been set, the long segments will follow the earth "great circle".
     */
    fun setPoints(points: List<GeoPoint>) {
        mOutline!!.setPoints(points)
        setDefaultInfoWindowLocation()
    }

    /**
     * @since 6.2.0
     * Used to be in [Polygon] and [Polyline]
     * Add the point at the end of the outline
     * If geodesic mode has been set, the long segments will follow the earth "great circle".
     */
    fun addPoint(p: GeoPoint) {
        mOutline!!.addPoint(p)
    }

    fun getActualPoints(): MutableList<GeoPoint>
        /**
         * @return a direct link to the list of polygon's vertices,
         * which are the original points if we didn't use the geodesic feature
         * Warning: changes on this list may cause strange results on the display.
         * @since 6.2.0
         */
        = mOutline!!.getPoints()

    /**
     * @since 6.2.0
     * See [.setDowngradePixelSizes]
     */
    fun setDowngradeDisplay(pDowngradeDisplay: Boolean) {
        mDowngradeDisplay = pDowngradeDisplay
    }

    /**
     * @param pPolySize      Size in pixels below which we will display an optimized list of segments
     * @param pRectangleSize Size in pixels below which we will display a mere rectangle (faster);
     * supposed to be lower than pPolySize
     * @since 6.2.0
     * If the size of the Poly (width or height) projected on the screen is lower than the parameter,
     * the Poly won't be displayed as a real Poly, but downgraded to an optimized list of segments
     * or an even more optimized rectangle
     * (or not displayed at all, depending on [.setDowngradeDisplay]
     */
    fun setDowngradePixelSizes(pPolySize: Int, pRectangleSize: Int) {
        mDowngradeMaximumRectanglePixelSize = pRectangleSize
        mDowngradeMaximumPixelSize = max(pPolySize, pRectangleSize)
    }

    /**
     * @since 6.2.0
     */
    private fun isWorthDisplaying(pProjection: Projection): Boolean {
        val boundingBox = getBounds()
        pProjection.toPixels(GeoPoint(boundingBox.latNorth, boundingBox.lonEast), mDowngradeTopLeft)
        pProjection.toPixels(GeoPoint(boundingBox.latSouth, boundingBox.lonWest), mDowngradeBottomRight)
        val worldSize = pProjection.worldMapSize
        val right = Math.round(LinearRing.getCloserValue(mDowngradeTopLeft.x.toDouble(), mDowngradeBottomRight.x.toDouble(), worldSize))
        val bottom = Math.round(LinearRing.getCloserValue(mDowngradeTopLeft.y.toDouble(), mDowngradeBottomRight.y.toDouble(), worldSize))
        if (abs(mDowngradeTopLeft.x - mDowngradeBottomRight.x) < mDowngradeMaximumPixelSize) {
            return false
        }
        if (abs(mDowngradeTopLeft.x - right) < mDowngradeMaximumPixelSize) {
            return false
        }
        if (abs(mDowngradeTopLeft.y - mDowngradeBottomRight.y) < mDowngradeMaximumPixelSize) {
            return false
        }
        if (abs(mDowngradeTopLeft.y - bottom) < mDowngradeMaximumPixelSize) {
            return false
        }
        return true
    }

    /**
     * @since 6.2.0
     */
    private fun isVisible(pPaint: Paint?): Boolean {
        return pPaint != null && pPaint.getColor() != Color.TRANSPARENT
    }

    /**
     * @since 6.2.0
     */
    private fun displayDowngrade(pCanvas: Canvas, pProjection: Projection) {
        val boundingBox = mOutline!!.getBoundingBox()
        pProjection.toPixels(GeoPoint(boundingBox.latNorth, boundingBox.lonEast), mDowngradeTopLeft)
        pProjection.toPixels(GeoPoint(boundingBox.latSouth, boundingBox.lonWest), mDowngradeBottomRight)
        val worldSize = pProjection.worldMapSize
        var left = mDowngradeTopLeft.x.toLong()
        var top = mDowngradeTopLeft.y.toLong()
        val right = Math.round(LinearRing.getCloserValue(left.toDouble(), mDowngradeBottomRight.x.toDouble(), worldSize))
        val bottom = Math.round(LinearRing.getCloserValue(top.toDouble(), mDowngradeBottomRight.y.toDouble(), worldSize))
        val width: Long
        if (left == right) {
            width = 1
        } else if (left > right) {
            width = left - right
            left = right
        } else {
            width = right - left
        }
        val height: Long
        if (top == bottom) {
            height = 1
        } else if (top > bottom) {
            height = top - bottom
            top = bottom
        } else {
            height = bottom - top
        }
        mDowngradeCenter.set(left + width / 2, top + height / 2)
        mOutline!!.getBestOffset(pProjection, mDowngradeOffset, mDowngradeCenter)
        left += mDowngradeOffset.x
        top += mDowngradeOffset.y

        var paint: Paint? = null
        if (mIsPaintOrPaintList) {
            paint = getOutlinePaint()
        } else if (getOutlinePaintLists().size > 0) {
            val paintList = getOutlinePaintLists()[0]
            paint = paintList.paint
            if (paint == null) { // polychromatic
                paint = paintList.getPaint(0, left.toFloat(), top.toFloat(), (left + width).toFloat(), (top + height).toFloat())
            }
        }
        if (!isVisible(paint)) {
            return
        }

        val maxWidthHeight = if (width > height) width else height
        if (maxWidthHeight <= mDowngradeMaximumRectanglePixelSize) {
            pCanvas.drawRect(left.toFloat(), top.toFloat(), (left + width).toFloat(), (top + height).toFloat(), paint!!)
            return
        }

        val downgradeList = mOutline!!.computeDowngradePointList(mDowngradeMaximumPixelSize)
        if (downgradeList == null || downgradeList.size == 0) {
            return
        }
        val size = downgradeList.size * 2
        if (mDowngradeSegments == null || mDowngradeSegments!!.size < size) {
            mDowngradeSegments = FloatArray(size)
        }
        val factor = maxWidthHeight * 1f / mDowngradeMaximumPixelSize
        var index = 0
        var firstX = 0f
        var firstY = 0f
        var i = 0
        while (i < downgradeList.size) {
            val currentX = mDowngradeCenter.x + downgradeList[i++] * factor
            val currentY = mDowngradeCenter.y + downgradeList[i++] * factor
            if (index == 0) {
                firstX = currentX
                firstY = currentY
            } else {
                mDowngradeSegments!![index++] = currentX
                mDowngradeSegments!![index++] = currentY
            }
            mDowngradeSegments!![index++] = currentX
            mDowngradeSegments!![index++] = currentY
        }
        // close
        mDowngradeSegments!![index++] = firstX
        mDowngradeSegments!![index++] = firstY
        if (index <= 4) {
            return
        }
        pCanvas.drawLines(mDowngradeSegments!!, 0, index, paint!!)
    }

    /**
     * @since 6.2.0
     */
    protected abstract fun click(pMapView: MapView?, pEventPos: GeoPoint?): Boolean

    /**
     * @since 6.2.0
     * Used to be in [Polyline]
     */
    fun setDensityMultiplier(pDensityMultiplier: Float) {
        mDensityMultiplier = pDensityMultiplier
    }

    /**
     * Used to be if [Polygon]
     * Important note: this function returns correct results only if the Poly has been drawn before,
     * and if the MapView positioning has not changed.
     *
     * @return true if the Poly contains the event position.
     */
    fun contains(pEvent: MotionEvent): Boolean {
        if (mPath!!.isEmpty()) {
            return false
        }
        val bounds = RectF() //bounds of the Path
        mPath!!.computeBounds(bounds, true)
        val region = Region()
        //Path has been computed in #draw (we assume that if it can be clicked, it has been drawn before).
        region.setPath(
            mPath!!, Region(
                bounds.left.toInt(), bounds.top.toInt(),
                (bounds.right).toInt(), (bounds.bottom).toInt()
            )
        )
        return region.contains(pEvent.getX().toInt(), pEvent.getY().toInt())
    }

    /**
     * @param pTolerance in pixels
     * @return true if the Poly is close enough to the point.
     * @since 6.2.0
     * Used to be in [Polyline]
     * Detection is done is screen coordinates.
     */
    fun isCloseTo(pPoint: GeoPoint?, pTolerance: Double, pMapView: MapView): Boolean {
        return getCloseTo(pPoint, pTolerance, pMapView) != null
    }

    /**
     * @param pTolerance in pixels
     * @return the first GeoPoint of the Poly close enough to the point
     * @since 6.2.0
     * Used to be in [Polyline]
     * Detection is done is screen coordinates.
     */
    fun getCloseTo(pPoint: GeoPoint?, pTolerance: Double, pMapView: MapView): GeoPoint? {
        return mOutline!!.getCloseTo(pPoint, pTolerance, pMapView.projection, mClosePath)
    }

    /**
     * Used to be in both [Polyline] and [Polygon]
     * Default listener for a single tap event on a Poly:
     * set the infowindow at the tapped position, and open the infowindow (if any).
     *
     * @return true if tapped
     */
    override fun onSingleTapConfirmed(pEvent: MotionEvent, pMapView: MapView?): Boolean {
        pMapView ?: return false
        val projection = pMapView.projection
        val eventPos = projection.fromPixels(pEvent.getX().toInt(), pEvent.getY().toInt()) as GeoPoint?
        val geoPoint: GeoPoint?
        if (mPath != null) {
            val tapped = contains(pEvent)
            if (tapped) {
                geoPoint = eventPos
            } else {
                geoPoint = null
            }
        } else {
            val tolerance = (mOutlinePaint.getStrokeWidth() * mDensity * mDensityMultiplier).toDouble()
            geoPoint = getCloseTo(eventPos, tolerance, pMapView)
        }
        if (geoPoint != null) {
            return click(pMapView, geoPoint)
        }
        return false
    }
}
