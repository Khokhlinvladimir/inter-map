package org.osmdroid.util

/**
 * Helper class for [SpeechBalloonOverlay]
 * whose main interest is method [.compute]
 */
class SpeechBalloonHelper {
    private val mTrianglePoint = PointL()
    private var mRect: RectL? = null
    private var mPoint: PointL? = null

    /**
     * Computes the intersection between a rectangle and the triangle that starts from a Point
     * and goes to a circle centered on the rectangle's center
     *
     * @return CORNER_INSIDE if the Point is within the rectangle, CORNER_NONE if both intersections
     * are on the same border, or a combination of CORNER_% that tells which rectangle's corner is
     * included between both intersections
     */
    fun compute(
        pInputRect: RectL, pInputPoint: PointL, pInputRadius: Double,
        pOutputIntersection1: PointL?, pOutputIntersection2: PointL?
    ): Int {
        mRect = pInputRect
        mPoint = pInputPoint

        if (pInputRect.contains(mPoint!!.x, mPoint!!.y)) {
            return CORNER_INSIDE
        }

        val angle = MyMath.computeAngle(mRect!!.centerX(), mRect!!.centerY(), mPoint!!.x, mPoint!!.y)

        computeCirclePoint(mTrianglePoint, pInputRadius, angle, false)
        val corner1 = checkIntersection(pOutputIntersection1)
        computeCirclePoint(mTrianglePoint, pInputRadius, angle, true)
        val corner2 = checkIntersection(pOutputIntersection2)
        if (corner1 == corner2) {
            return CORNER_NONE
        }
        return corner1 or corner2
    }

    private fun checkIntersection(pIntersection: PointL?): Int {
        if (mPoint!!.y <= mRect!!.top && checkIntersectionY(mRect!!.top, pIntersection)) {
            return CORNER_TOP
        }
        if (mPoint!!.y >= mRect!!.bottom && checkIntersectionY(mRect!!.bottom, pIntersection)) {
            return CORNER_BOTTOM
        }
        if (mPoint!!.x <= mRect!!.left && checkIntersectionX(mRect!!.left, pIntersection)) {
            return CORNER_LEFT
        }
        if (mPoint!!.x >= mRect!!.right && checkIntersectionX(mRect!!.right, pIntersection)) {
            return CORNER_RIGHT
        }
        throw IllegalArgumentException()
    }

    private fun checkIntersectionX(pX: Long, pIntersection: PointL?): Boolean {
        return SegmentIntersection.intersection(
            mPoint!!.x.toDouble(), mPoint!!.y.toDouble(), mTrianglePoint.x.toDouble(), mTrianglePoint.y.toDouble(),
            pX.toDouble(), mRect!!.top.toDouble(), pX.toDouble(), mRect!!.bottom.toDouble(),
            pIntersection
        )
    }

    private fun checkIntersectionY(pY: Long, pIntersection: PointL?): Boolean {
        return SegmentIntersection.intersection(
            mPoint!!.x.toDouble(), mPoint!!.y.toDouble(), mTrianglePoint.x.toDouble(), mTrianglePoint.y.toDouble(),
            mRect!!.left.toDouble(), pY.toDouble(), mRect!!.right.toDouble(), pY.toDouble(),
            pIntersection
        )
    }

    private fun computeCirclePoint(
        pDestination: PointL, pRadius: Double,
        pAngle: Double, pFirst: Boolean
    ) {
        MyMath.computeCirclePoint(
            mRect!!.centerX(), mRect!!.centerY(), pRadius,
            pAngle + Math.PI / 2 * (if (pFirst) 1 else -1), pDestination
        )
    }

    companion object {
        const val CORNER_INSIDE: Int = -1
        const val CORNER_NONE: Int = 0
        const val CORNER_LEFT: Int = 1
        const val CORNER_RIGHT: Int = 2
        const val CORNER_TOP: Int = 4
        const val CORNER_BOTTOM: Int = 8
    }
}
