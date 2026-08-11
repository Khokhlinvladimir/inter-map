package org.osmdroid.views.drawing

import android.graphics.Path
import android.graphics.Point
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.Projection

/**
 * Since the osmdroid canvas coordinate system is changing with every scroll, the x/y coordinates of
 * lat/long points is also always changing. Converting from lat/long to pixel values is a
 * potentially expensive operation and shouldn't be performed in the draw() cycle. Instead of
 * recalculating your [Path] points every draw cycle, you can use an OsmPath and call
 * [.onDrawCycle] at the start of your draw call. This will simply shift the Path
 * the proper amount so that it is in the correct pixel position.
 *
 * @author Marc Kurtz
 */
@Deprecated("Use {@link Polyline} or {@link Polygon} instead")
open class OsmPath : Path {
    protected val mReferencePoint: Point = Point()
    private var mLastZoomLevel = -1.0

    constructor() : super()

    constructor(src: Path?) : super(src)

    /**
     * Call this method at the beginning of every draw() call.
     */
    fun onDrawCycle(proj: Projection) {
        if (mLastZoomLevel != proj.zoomLevel) {
            proj.toPixels(sReferenceGeoPoint, mReferencePoint)
            mLastZoomLevel = proj.zoomLevel
        }
        val x = mReferencePoint.x
        val y = mReferencePoint.y
        proj.toPixels(sReferenceGeoPoint, mReferencePoint)
        val deltaX = mReferencePoint.x - x
        val deltaY = mReferencePoint.y - y

        offset(deltaX.toFloat(), deltaY.toFloat())
    }

    companion object {
        private val sReferenceGeoPoint = GeoPoint(0, 0)
    }
}
