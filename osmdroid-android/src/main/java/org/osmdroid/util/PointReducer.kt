package org.osmdroid.util

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Reduces the number of points in a shape using the Douglas-Peucker algorithm. <br></br>
 *
 *
 * From: http://www.phpriot.com/articles/reducing-map-path-douglas-peucker-algorithm/4<br></br>
 * Ported from PHP to Java. "marked" array added to optimize.
 *
 * @author M.Kergall
 *
 *
 * Generously dontated by osmbouspack
 * @since 6.0.0
 */
object PointReducer {
    /**
     * Reduce the number of points in a shape using the Douglas-Peucker algorithm
     * Suggested usage
     * <pre>
     * `//get the screen bounds BoundingBox boundingBox = map.getBoundingBox(); final double latSpanDegrees = boundingBox.getLatitudeSpan(); //get the degree difference, divide by dpi double tolerance = latSpanDegrees /densityDpi; //each latitude degree on screen is represented by this many dip points = PointReducer.reduceWithTolerance(points, tolerance); `
     *
    </pre> *
     *
     * @param tolerance The tolerance to decide whether or not
     * to keep a point, in the coordinate system
     * of the points (micro-degrees here)
     * @param shape     The shape to reduce
     * @return the reduced shape
     */
    @JvmStatic
    fun reduceWithTolerance(shape: ArrayList<GeoPoint>, tolerance: Double): ArrayList<GeoPoint> {
        val n = shape.size
        // if a shape has 2 or less points it cannot be reduced
        if (tolerance <= 0 || n < 3) {
            return shape
        }

        val marked = BooleanArray(n) //vertex indexes to keep will be marked as "true"
        for (i in 1 until n - 1) marked[i] = false
        // automatically add the first and last point to the returned shape
        marked[n - 1] = true
        marked[0] = marked[n - 1]

        // the first and last points in the original shape are
        // used as the entry point to the algorithm.
        douglasPeuckerReduction(
            shape,  // original shape
            marked,  // reduced shape
            tolerance,  // tolerance
            0,  // index of first point
            n - 1 // index of last point
        )

        // all done, return the reduced shape
        val newShape = ArrayList<GeoPoint>(n) // the new shape to return
        for (i in 0 until n) {
            if (marked[i]) newShape.add(shape.get(i))
        }
        return newShape
    }

    /**
     * Reduce the points in shape between the specified first and last
     * index. Mark the points to keep in marked[]
     *
     * @param shape     The original shape
     * @param marked    The points to keep (marked as true)
     * @param tolerance The tolerance to determine if a point is kept
     * @param firstIdx  The index in original shape's point of
     * the starting point for this line segment
     * @param lastIdx   The index in original shape's point of
     * the ending point for this line segment
     */
    private fun douglasPeuckerReduction(shape: ArrayList<GeoPoint>, marked: BooleanArray, tolerance: Double, firstIdx: Int, lastIdx: Int) {
        if (lastIdx <= firstIdx + 1) {
            // overlapping indexes, just return
            return
        }

        // loop over the points between the first and last points
        // and find the point that is the farthest away
        var maxDistance = 0.0
        var indexFarthest = 0

        val firstPoint = shape.get(firstIdx)
        val lastPoint = shape.get(lastIdx)

        for (idx in firstIdx + 1 until lastIdx) {
            val point = shape.get(idx)

            val distance = orthogonalDistance(point, firstPoint, lastPoint)

            // keep the point with the greatest distance
            if (distance > maxDistance) {
                maxDistance = distance
                indexFarthest = idx
            }
        }

        if (maxDistance > tolerance) {
            //The farthest point is outside the tolerance: it is marked and the algorithm continues.
            marked[indexFarthest] = true

            // reduce the shape between the starting point to newly found point
            douglasPeuckerReduction(shape, marked, tolerance, firstIdx, indexFarthest)

            // reduce the shape between the newly found point and the finishing point
            douglasPeuckerReduction(shape, marked, tolerance, indexFarthest, lastIdx)
        }
        //else: the farthest point is within the tolerance, the whole segment is discarded.
    }

    /**
     * Calculate the orthogonal distance from the line joining the
     * lineStart and lineEnd points to point
     *
     * @param point     The point the distance is being calculated for
     * @param lineStart The point that starts the line
     * @param lineEnd   The point that ends the line
     * @return The distance in points coordinate system
     */
    @JvmStatic
    fun orthogonalDistance(point: GeoPoint, lineStart: GeoPoint, lineEnd: GeoPoint): Double {
        val area = abs(
            ((lineStart.latitude * lineEnd.longitude + lineEnd.latitude * point.longitude + point.latitude * lineStart.longitude
                    ) - lineEnd.latitude * lineStart.longitude - point.latitude * lineEnd.longitude - lineStart.latitude * point.longitude
                    ) / 2.0
        )

        val bottom = hypot(
            lineStart.latitude - lineEnd.latitude,
            lineStart.longitude - lineEnd.longitude
        )

        return (area / bottom * 2.0)
    }
}
