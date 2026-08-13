package org.osmdroid.util

/**
 * A tool to clip segments
 */
class SegmentClipper : PointAccepter {
    // for optimization reasons: avoiding to create objects all the time
    private val mOptimIntersection = PointL()
    private val mOptimIntersection1 = PointL()
    private val mOptimIntersection2 = PointL()

    private var mXMin: Long = 0
    private var mYMin: Long = 0
    private var mXMax: Long = 0
    private var mYMax: Long = 0
    private var mPointAccepter: PointAccepter? = null
    private var mIntegerAccepter: IntegerAccepter? = null
    private val cornerX = LongArray(4)
    private val cornerY = LongArray(4)
    private val mPoint0 = PointL()
    private val mPoint1 = PointL()
    private var mFirstPoint = false

    /**
     * If true we keep the invisible segments: they have an impact on Path inner area
     */
    private var mPathMode = false

    private var mCurrentSegmentIndex = 0

    fun set(
        pXMin: Long, pYMin: Long, pXMax: Long, pYMax: Long,
        pPointAccepter: PointAccepter, pIntegerAccepter: IntegerAccepter?, pPathMode: Boolean
    ) {
        mXMin = pXMin
        mYMin = pYMin
        mXMax = pXMax
        mYMax = pYMax
        cornerX[1] = mXMin
        cornerX[0] = cornerX[1]
        cornerX[3] = mXMax
        cornerX[2] = cornerX[3]
        cornerY[2] = mYMin
        cornerY[0] = cornerY[2]
        cornerY[3] = mYMax
        cornerY[1] = cornerY[3]
        mPointAccepter = pPointAccepter
        mIntegerAccepter = pIntegerAccepter
        mPathMode = pPathMode
    }

    fun set(
        pXMin: Long, pYMin: Long, pXMax: Long, pYMax: Long,
        pPointAccepter: PointAccepter, pPathMode: Boolean
    ) {
        set(pXMin, pYMin, pXMax, pYMax, pPointAccepter, null, pPathMode)
    }

    override fun init() {
        mFirstPoint = true
        if (mIntegerAccepter != null) {
            mIntegerAccepter!!.init()
        }
        mPointAccepter!!.init()
    }

    override fun add(pX: Long, pY: Long) {
        mPoint1.set(pX, pY)
        if (mFirstPoint) {
            mFirstPoint = false
            mCurrentSegmentIndex = 0
        } else {
            clip(mPoint0.x, mPoint0.y, mPoint1.x, mPoint1.y)
            mCurrentSegmentIndex++
        }
        mPoint0.set(mPoint1)
    }

    override fun end() {
        if (mIntegerAccepter != null) {
            mIntegerAccepter!!.end()
        }
        mPointAccepter!!.end()
    }

    /**
     * Clip a segment into the clip area
     */
    fun clip(pX0: Long, pY0: Long, pX1: Long, pY1: Long) {
        if (!mPathMode) {
            if (isOnTheSameSideOut(pX0, pY0, pX1, pY1)) {
                return
            }
        }
        if (isInClipArea(pX0, pY0)) {
            if (isInClipArea(pX1, pY1)) {
                nextVertex(pX0, pY0)
                nextVertex(pX1, pY1)
                return
            }
            if (intersection(pX0, pY0, pX1, pY1)) {
                nextVertex(pX0, pY0)
                nextVertex(mOptimIntersection.x, mOptimIntersection.y)
                if (mPathMode) {
                    nextVertex(clipX(pX1), clipY(pY1))
                }
                return
            }
            throw RuntimeException("Cannot find expected mOptimIntersection for " + RectL(pX0, pY0, pX1, pY1))
        }
        if (isInClipArea(pX1, pY1)) {
            if (intersection(pX0, pY0, pX1, pY1)) {
                if (mPathMode) {
                    nextVertex(clipX(pX0), clipY(pY0))
                }
                nextVertex(mOptimIntersection.x, mOptimIntersection.y)
                nextVertex(pX1, pY1)
                return
            }
            throw RuntimeException("Cannot find expected mOptimIntersection for " + RectL(pX0, pY0, pX1, pY1))
        }
        // no point is on the screen
        var count = 0
        if (intersection(pX0, pY0, pX1, pY1, mXMin, mYMin, mXMin, mYMax)) { // x mClipMin segment
            val point = if (count++ == 0) mOptimIntersection1 else mOptimIntersection2
            point.set(mOptimIntersection)
        }
        if (intersection(pX0, pY0, pX1, pY1, mXMax, mYMin, mXMax, mYMax)) { // x mClipMax segment
            val point = if (count++ == 0) mOptimIntersection1 else mOptimIntersection2
            point.set(mOptimIntersection)
        }
        if (intersection(pX0, pY0, pX1, pY1, mXMin, mYMin, mXMax, mYMin)) { // y mClipMin segment
            val point = if (count++ == 0) mOptimIntersection1 else mOptimIntersection2
            point.set(mOptimIntersection)
        }
        if (intersection(pX0, pY0, pX1, pY1, mXMin, mYMax, mXMax, mYMax)) { // y mClipMax segment
            val point = if (count++ == 0) mOptimIntersection1 else mOptimIntersection2
            point.set(mOptimIntersection)
        }
        if (count == 2) {
            val distance1 = Distance.getSquaredDistanceToPoint(
                mOptimIntersection1.x.toDouble(), mOptimIntersection1.y.toDouble(), pX0.toDouble(), pY0.toDouble()
            )
            val distance2 = Distance.getSquaredDistanceToPoint(
                mOptimIntersection2.x.toDouble(), mOptimIntersection2.y.toDouble(), pX0.toDouble(), pY0.toDouble()
            )
            val start = if (distance1 < distance2) mOptimIntersection1 else mOptimIntersection2
            val end = if (distance1 < distance2) mOptimIntersection2 else mOptimIntersection1
            if (mPathMode) {
                nextVertex(clipX(pX0), clipY(pY0))
            }
            nextVertex(start.x, start.y)
            nextVertex(end.x, end.y)
            if (mPathMode) {
                nextVertex(clipX(pX1), clipY(pY1))
            }
            return
        }
        if (count == 1) {
            if (mPathMode) {
                nextVertex(clipX(pX0), clipY(pY0))
                nextVertex(mOptimIntersection1.x, mOptimIntersection1.y)
                nextVertex(clipX(pX1), clipY(pY1))
            }
            return
        }
        if (count == 0) {
            if (mPathMode) {
                nextVertex(clipX(pX0), clipY(pY0))
                val corner = getClosestCorner(pX0, pY0, pX1, pY1)
                nextVertex(cornerX[corner], cornerY[corner])
                nextVertex(clipX(pX1), clipY(pY1))
            }
            return
        }
        throw RuntimeException("Impossible mOptimIntersection count (" + count + ")")
    }

    /**
     * Check if a point is in the clip area
     */
    fun isInClipArea(pX: Long, pY: Long): Boolean {
        return pX > mXMin && pX < mXMax && pY > mYMin && pY < mYMax
    }

    private fun clipX(pX: Long): Long {
        return clip(pX, mXMin, mXMax)
    }

    private fun clipY(pY: Long): Long {
        return clip(pY, mYMin, mYMax)
    }

    private fun nextVertex(pX: Long, pY: Long) {
        if (mIntegerAccepter != null) {
            mIntegerAccepter!!.add(mCurrentSegmentIndex)
        }
        mPointAccepter!!.add(pX, pY)
    }

    /**
     * Intersection of two segments
     */
    private fun intersection(
        pX0: Long, pY0: Long, pX1: Long, pY1: Long,
        pX2: Long, pY2: Long, pX3: Long, pY3: Long
    ): Boolean {
        return SegmentIntersection.intersection(
            pX0.toDouble(), pY0.toDouble(), pX1.toDouble(), pY1.toDouble(),
            pX2.toDouble(), pY2.toDouble(), pX3.toDouble(), pY3.toDouble(), mOptimIntersection
        )
    }

    /**
     * Intersection of a segment with the 4 segments of the clip area
     */
    private fun intersection(pX0: Long, pY0: Long, pX1: Long, pY1: Long): Boolean {
        return intersection(pX0, pY0, pX1, pY1, mXMin, mYMin, mXMin, mYMax) // x min segment
                || intersection(pX0, pY0, pX1, pY1, mXMax, mYMin, mXMax, mYMax) // x max segment
                || intersection(pX0, pY0, pX1, pY1, mXMin, mYMin, mXMax, mYMin) // y min segment
                || intersection(pX0, pY0, pX1, pY1, mXMin, mYMax, mXMax, mYMax) // y max segment
    }

    /**
     * Gets the clip area corner which is the closest to the given segment
     * We have a clip area and we have a segment with no intersection with this clip area.
     * The question is: how do we clip this segment?
     * If we only clip both segment ends, we may end up with a (min,min) x (max,max)
     * clip approximation that displays a backslash on the screen.
     * The idea is to compute the clip area corner which is the closest to the segment,
     * and to use it as a clip step.
     * Which will do something like:
     * (min,min)[first segment point] x (min,max)[closest corner] x (max,max)[second segment point]
     * or
     * (min,min)[first segment point] x (max,min)[closest corner] x (max,max)[second segment point]
     */
    private fun getClosestCorner(pX0: Long, pY0: Long, pX1: Long, pY1: Long): Int {
        var min = Double.Companion.MAX_VALUE
        var corner = 0
        for (i in cornerX.indices) {
            val distance = Distance.getSquaredDistanceToSegment(
                cornerX[i].toDouble(), cornerY[i].toDouble(),
                pX0.toDouble(), pY0.toDouble(), pX1.toDouble(), pY1.toDouble()
            )
            if (min > distance) {
                min = distance
                corner = i
            }
        }
        return corner
    }

    /**
     * Optimization for lines (as opposed to Path)
     * If both points are outside of the clip area and "on the same side of the outside" (sic)
     * we don't need to compute anything anymore as it won't draw a line in the end
     */
    private fun isOnTheSameSideOut(pX0: Long, pY0: Long, pX1: Long, pY1: Long): Boolean {
        return (pX0 < mXMin && pX1 < mXMin)
                || (pX0 > mXMax && pX1 > mXMax)
                || (pY0 < mYMin && pY1 < mYMin)
                || (pY0 > mYMax && pY1 > mYMax)
    }

    companion object {
        /**
         * Clip a value into the clip area min/max
         */
        private fun clip(pValue: Long, pMin: Long, pMax: Long): Long {
            return if (pValue <= pMin) pMin else if (pValue >= pMax) pMax else pValue
        }
    }
}
