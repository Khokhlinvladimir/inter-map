package org.osmdroid.views.overlay.simplefastpoint

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions.LabelPolicy
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions.RenderingAlgorithm
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.floor

/**
 * Overlay to draw a layer of clickable simple points, optimized for rendering speed. Nice
 * performance up to 100k points. Supports styling each point and label individually, or shared style
 * for all points in a theme. Does not support rotated maps.
 * There are three rendering algorithms:
 * NO_OPTIMIZATION: all points all drawn in every draw event
 * MEDIUM_OPTIMIZATION: not recommended for >10k points. Recalculates the grid index on each draw
 * event and only draws one point per grid cell.
 * MAXIMUM_OPTIMIZATION: for >10k points, only recalculates the grid on touch up, hence much faster.
 * Performs well for 100k points.
 *
 *
 * TODO: a quadtree index would improve rendering speed!
 * TODO: an alternative to the CIRCLE shape, cause this is slow to render
 * Created by Miguel Porto on 25-10-2016.
 */
open class SimpleFastPointOverlay @JvmOverloads constructor(
    private val mPointList: PointAdapter,
    /**
     * @return fast point overlay options (applied to all points)
     */
    val style: SimpleFastPointOverlayOptions = SimpleFastPointOverlayOptions.defaultStyle
) : Overlay() {
    val boundingBox: BoundingBox?
    private var mSelectedPoint: Int? = null
    private var clickListener: OnClickListener? = null
    private var gridIndex: MutableList<StyledLabelledPoint>? = null

    // grid index for optimizing drawing k's of points
    private var gridBool: Array<BooleanArray>? = null
    private var gridWid = 0
    private var gridHei = 0
    private var viewWid = 0
    private var viewHei = 0
    private var hasMoved = false
    private var numLabels = 0
    private var startBoundingBox: BoundingBox? = null
    private var startProjection: Projection? = null
    private var prevBoundingBox = BoundingBox()

    /**
     * Just a light internal class for storing point data
     */
    inner class StyledLabelledPoint(point: Point, val label: String?, val pointStyle: Paint?, val textStyle: Paint?) :
        Point(point)

    interface PointAdapter : Iterable<IGeoPoint?> {
        fun size(): Int

        fun get(i: Int): IGeoPoint?

        /**
         * Whether this adapter has labels
         *
         * @return
         */
        val isLabelled: Boolean

        /**
         * Whether the points are individually styled
         *
         * @return
         */
        val isStyled: Boolean
    }

    interface OnClickListener {
        fun onClick(points: PointAdapter?, point: Int?)
    }

    init {
        var east: Double? = null
        var west: Double? = null
        var north: Double? = null
        var south: Double? = null
        for (p in mPointList) {
            if (p == null) continue
            if (east == null || p.longitude > east) east = p.longitude
            if (west == null || p.longitude < west) west = p.longitude
            if (north == null || p.latitude > north) north = p.latitude
            if (south == null || p.latitude < south) south = p.latitude
        }

        if (east != null) this.boundingBox = BoundingBox(north!!, east, south!!, west!!)
        else this.boundingBox = null
    }

    private fun updateGrid(mapView: MapView) {
        viewWid = mapView.getWidth()
        viewHei = mapView.getHeight()
        gridWid = floor((viewWid.toFloat() / style.cellSize).toDouble()).toInt() + 1
        gridHei = floor((viewHei.toFloat() / style.cellSize).toDouble()).toInt() + 1
        gridBool = Array(gridWid) { BooleanArray(gridHei) }

        // TODO the measures on first draw are not the final values.
        // MapView should propagate onLayout to overlays
    }

    /**
     * Re-calculates which points to be shown and their coordinates
     * TODO: this could be further optimized for speed, for example, the grid could be calculated
     * in geographic coordinates, instead of projected. N.B. the speed bottleneck is pj.toPixels()
     *
     * @param pMapView
     */
    private fun computeGrid(pMapView: MapView) {
        // TODO: 15-11-2016 should take map orientation into account in the BBox!
        val viewBBox = pMapView.getBoundingBox()

        startBoundingBox = viewBBox
        startProjection = pMapView.projection

        // do not compute grid if BBox is the same
        if (viewBBox!!.latNorth != prevBoundingBox.latNorth || viewBBox.latSouth != prevBoundingBox.latSouth || viewBBox.lonWest != prevBoundingBox.lonWest || viewBBox.lonEast != prevBoundingBox.lonEast) {
            prevBoundingBox = BoundingBox(
                viewBBox.latNorth, viewBBox.lonEast,
                viewBBox.latSouth, viewBBox.lonWest
            )

            if (gridBool == null || viewHei != pMapView.getHeight() || viewWid != pMapView.getWidth()) {
                updateGrid(pMapView)
            } else {
                // empty grid.
                // TODO: we might leave the grid as it was before to avoid the "flickering"?
                for (row in requireNotNull(gridBool)) Arrays.fill(row, false)
            }

            var gridX: Int
            var gridY: Int
            val mPositionPixels = Point()
            val pj = pMapView.projection
            gridIndex = ArrayList<StyledLabelledPoint>()
            numLabels = 0

            for (pt1 in mPointList) {
                if (pt1 == null) continue
                if (pt1.latitude > viewBBox.latSouth && pt1.latitude < viewBBox.latNorth && pt1.longitude > viewBBox.lonWest && pt1.longitude < viewBBox.lonEast) {
                    pj.toPixels(pt1, mPositionPixels)
                    // test whether in this grid cell there is already a point, skip if yes
                    gridX = floor((mPositionPixels.x.toFloat() / style.cellSize).toDouble()).toInt()
                    gridY = floor((mPositionPixels.y.toFloat() / style.cellSize).toDouble()).toInt()
                    if (gridX >= gridWid || gridY >= gridHei || gridX < 0 || gridY < 0 || gridBool!![gridX][gridY]) continue
                    gridBool!![gridX][gridY] = true
                    gridIndex!!.add(
                        StyledLabelledPoint(
                            mPositionPixels,
                            if (mPointList.isLabelled) (pt1 as LabelledGeoPoint).label else null,
                            if (mPointList.isStyled) (pt1 as StyledLabelledGeoPoint).pointStyle else null,
                            if (mPointList.isStyled) (pt1 as StyledLabelledGeoPoint).textStyle else null
                        )
                    )
                    numLabels++
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView?): Boolean {
        val activeMapView = mapView ?: return false
        if (style.algorithm !=
            RenderingAlgorithm.MAXIMUM_OPTIMIZATION
        ) return false
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                startBoundingBox = activeMapView.getBoundingBox()
                startProjection = activeMapView.projection
            }

            MotionEvent.ACTION_MOVE -> hasMoved = true
            MotionEvent.ACTION_UP -> {
                hasMoved = false
                startBoundingBox = activeMapView.getBoundingBox()
                startProjection = activeMapView.projection
                activeMapView.invalidate()
            }
        }
        return false
    }

    /**
     * Default action on tap is to select the nearest point.
     */
    override fun onSingleTapConfirmed(event: MotionEvent, mapView: MapView?): Boolean {
        val activeMapView = mapView ?: return false
        if (!style.isClickable) return false
        var hyp: Float
        var minHyp: Float? = null
        var closest = -1
        val tmp = Point()
        val pj = activeMapView.projection

        for (i in 0 until mPointList.size()) {
            if (mPointList.get(i) == null) continue
            // TODO avoid projecting coordinates, do a test before calling next line
            pj.toPixels(mPointList.get(i), tmp)
            if (abs(event.getX() - tmp.x) > 50 || abs(event.getY() - tmp.y) > 50) continue
            hyp = ((event.getX() - tmp.x) * (event.getX() - tmp.x)
                    + (event.getY() - tmp.y) * (event.getY() - tmp.y))
            if (minHyp == null || hyp < minHyp) {
                minHyp = hyp
                closest = i
            }
        }
        if (minHyp == null) return false
        this.selectedPoint = closest
        activeMapView.invalidate()
        if (clickListener != null) clickListener!!.onClick(mPointList, closest)
        return true
    }

    var selectedPoint: Int?
        get() = mSelectedPoint
        /**
         * Sets the highlighted point. App must invalidate the MapView.
         *
         * @param toSelect The index of the point (zero-based) in the original list.
         */
        set(toSelect) {
            if (toSelect == null || toSelect < 0 || toSelect >= mPointList.size()) mSelectedPoint = null
            else mSelectedPoint = toSelect
        }

    fun setOnClickListener(listener: OnClickListener?) {
        clickListener = listener
    }

    override fun draw(canvas: Canvas, mapView: MapView, b: Boolean) {
        if (b) return
        val mPositionPixels = Point()
        val pj = mapView.projection

        if (mPointList.isStyled || style.pointStyle != null) {
            when (style.algorithm) {
                RenderingAlgorithm.MAXIMUM_OPTIMIZATION -> {
                    // optimized for speed, recommended for > 10k points
                    // recompute grid only on specific events - only onDraw but when not animating
                    // and not in the middle of a touch scroll gesture
                    if (gridBool == null || (!hasMoved && !mapView.isAnimating())) computeGrid(mapView)

                    // compute the coordinates of each visible point in the new viewbox
                    val nw: IGeoPoint = GeoPoint(startBoundingBox!!.latNorth, startBoundingBox!!.lonWest)
                    val se: IGeoPoint = GeoPoint(startBoundingBox!!.latSouth, startBoundingBox!!.lonEast)
                    val pNw = pj.toPixels(nw, null)
                    val pSe = pj.toPixels(se, null)
                    val pStartSe = startProjection!!.toPixels(se, null)
                    val dGz = Point(pSe!!.x - pStartSe!!.x, pSe.y - pStartSe.y)
                    val dd = Point(dGz.x - pNw!!.x, dGz.y - pNw.y)
                    val showLabels =
                        ((style.labelPolicy == LabelPolicy.DENSITY_THRESHOLD
                                && numLabels <= style.maxNShownLabels)
                                || (style.labelPolicy == LabelPolicy.ZOOM_THRESHOLD
                                && mapView.zoomLevelDouble >= style.minZoomShowLabels))
                    // draw points
                    for (slp in gridIndex!!) {
                        val tx = ((slp.x * dd.x) / pStartSe.x).toFloat()
                        val ty = ((slp.y * dd.y) / pStartSe.y).toFloat()
                        val pointStyle = slp.pointStyle ?: style.pointStyle
                        val textStyle = slp.textStyle ?: style.textStyle

                        drawPointAt(
                            canvas, slp.x + pNw.x + tx, slp.y + pNw.y + ty,
                            mPointList.isLabelled && showLabels, slp.label,
                            pointStyle, textStyle, mapView
                        )
                    }
                }

                RenderingAlgorithm.MEDIUM_OPTIMIZATION -> {
                    // recompute grid index on every draw
                    if (gridBool == null || viewHei != mapView.getHeight() || viewWid != mapView.getWidth()) updateGrid(mapView)
                    else for (row in requireNotNull(gridBool)) Arrays.fill(row, false)

                    val showLabels = (style.labelPolicy == LabelPolicy.ZOOM_THRESHOLD
                            && mapView.zoomLevelDouble >= style.minZoomShowLabels)

                    val viewBBox = requireNotNull(mapView.getBoundingBox())
                    for (pt1 in mPointList) {
                        if (pt1 == null) continue
                        if (pt1.latitude > viewBBox.latSouth && pt1.latitude < viewBBox.latNorth && pt1.longitude > viewBBox.lonWest && pt1.longitude < viewBBox.lonEast) {
                            pj.toPixels(pt1, mPositionPixels)
                            // test whether in this grid cell there is already a point, skip if yes
                            // this makes a lot of difference in rendering speed
                            val gridX = floor((mPositionPixels.x.toFloat() / style.cellSize).toDouble()).toInt()
                            val gridY = floor((mPositionPixels.y.toFloat() / style.cellSize).toDouble()).toInt()
                            if (gridX >= gridWid || gridY >= gridHei || gridX < 0 || gridY < 0 || gridBool!![gridX][gridY]) continue
                            gridBool!![gridX][gridY] = true

                            // style may come individually or from the whole theme setting
                            val styledPoint = pt1 as? StyledLabelledGeoPoint
                            drawPointAt(
                                canvas, mPositionPixels.x.toFloat(), mPositionPixels.y.toFloat(), mPointList.isLabelled && showLabels,
                                if (mPointList.isLabelled) (pt1 as LabelledGeoPoint).label else null,
                                styledPoint?.pointStyle ?: style.pointStyle,
                                styledPoint?.textStyle ?: style.textStyle, mapView
                            )
                        }
                    }
                }

                RenderingAlgorithm.NO_OPTIMIZATION -> {
                    // draw all points
                    val showLabels = (style.labelPolicy == LabelPolicy.ZOOM_THRESHOLD
                            && mapView.zoomLevelDouble >= style.minZoomShowLabels)
                    val viewBBox = requireNotNull(mapView.getBoundingBox())
                    for (pt1 in mPointList) {
                        if (pt1 == null) continue
                        if (pt1.latitude > viewBBox.latSouth && pt1.latitude < viewBBox.latNorth && pt1.longitude > viewBBox.lonWest && pt1.longitude < viewBBox.lonEast) {
                            pj.toPixels(pt1, mPositionPixels)

                            // style may come individually or from the whole theme setting
                            val styledPoint = pt1 as? StyledLabelledGeoPoint
                            drawPointAt(
                                canvas, mPositionPixels.x.toFloat(), mPositionPixels.y.toFloat(), mPointList.isLabelled && showLabels,
                                if (mPointList.isLabelled) (pt1 as LabelledGeoPoint).label else null,
                                styledPoint?.pointStyle ?: style.pointStyle,
                                styledPoint?.textStyle ?: style.textStyle, mapView
                            )
                        }
                    }
                }

                null -> Unit
            }
        }

        if (mSelectedPoint != null && mSelectedPoint!! < mPointList.size() && mPointList.get(mSelectedPoint!!) != null) {
            pj.toPixels(mPointList.get(mSelectedPoint!!), mPositionPixels)
            if (style.symbol == SimpleFastPointOverlayOptions.Shape.CIRCLE) canvas.drawCircle(
                mPositionPixels.x.toFloat(), mPositionPixels.y.toFloat(),
                style.selectedCircleRadius, style.selectedPointStyle
            )
            else canvas.drawRect(
                mPositionPixels.x.toFloat() - style.selectedCircleRadius,
                mPositionPixels.y.toFloat() - style.selectedCircleRadius,
                mPositionPixels.x.toFloat() + style.selectedCircleRadius,
                mPositionPixels.y.toFloat() + style.selectedCircleRadius,
                style.selectedPointStyle
            )
        }
    }

    protected open fun drawPointAt(
        canvas: Canvas,
        x: Float,
        y: Float,
        showLabel: Boolean,
        label: String?,
        pointStyle: Paint,
        textStyle: Paint,
        pMapView: MapView
    ) {
        canvas.save()
        canvas.rotate(-pMapView.getMapOrientation(), x, y)
        if (style.symbol == SimpleFastPointOverlayOptions.Shape.CIRCLE) canvas.drawCircle(x, y, style.circleRadius, pointStyle)
        else canvas.drawRect(
            x - style.circleRadius, y - style.circleRadius,
            x + style.circleRadius, y + style.circleRadius,
            pointStyle
        )

        if (showLabel && label != null) canvas.drawText(label, x, y - style.circleRadius - 5, textStyle)
        canvas.restore()
    }
}
