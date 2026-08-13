package org.osmdroid.util

import kotlin.math.max
import kotlin.math.min

/**
 * [PointAccepter] that simplifies the list of consecutive points with the same X or Y.
 * One goal is to have faster Path rendering.
 * As we clip the Path with a rectangle, additional segments are created by [SegmentClipper].
 * When most of the Polygon is out of the screen, many consecutive segments are on the same side
 * of the clip rectangle (e.g. same X or same Y). Do we need to render all those segments? No.
 * We can simplify this list of consecutive segments into a tiny list of maximum 3 segments.
 * And that makes the Path rendering much faster.
 */
class SideOptimizationPointAccepter
/**
 * We optimize on top of another [PointAccepter]
 */(private val mPointAccepter: PointAccepter) : PointAccepter {
    private val mLatestPoint = PointL()
    private val mStartPoint = PointL()
    private var mFirst = false
    private var mMin: Long = 0
    private var mMax: Long = 0
    private var mStatus = 0

    override fun init() {
        mFirst = true
        mStatus = STATUS_DIFFERENT
        mPointAccepter.init()
    }

    override fun add(pX: Long, pY: Long) {
        if (mFirst) {
            mFirst = false
            addToAccepter(pX, pY)
            mLatestPoint.set(pX, pY)
            return
        }
        if (mLatestPoint.x == pX && mLatestPoint.y == pY) {
            return
        }
        if (mLatestPoint.x == pX) {
            if (mStatus == STATUS_SAME_X) {
                if (mMin > pY) {
                    mMin = pY
                }
                if (mMax < pY) {
                    mMax = pY
                }
            } else {
                flushSides()
                mStatus = STATUS_SAME_X
                mStartPoint.set(mLatestPoint)
                mMin = min(pY, mLatestPoint.y)
                mMax = max(pY, mLatestPoint.y)
            }
        } else if (mLatestPoint.y == pY) {
            if (mStatus == STATUS_SAME_Y) {
                if (mMin > pX) {
                    mMin = pX
                }
                if (mMax < pX) {
                    mMax = pX
                }
            } else {
                flushSides()
                mStatus = STATUS_SAME_Y
                mStartPoint.set(mLatestPoint)
                mMin = min(pX, mLatestPoint.x)
                mMax = max(pX, mLatestPoint.x)
            }
        } else {
            flushSides()
            addToAccepter(pX, pY)
        }
        mLatestPoint.set(pX, pY)
    }

    override fun end() {
        flushSides()
        mPointAccepter.end()
    }

    /**
     * Flushing the side (same X or same Y) computed so far
     */
    private fun flushSides() {
        val segmentMin: Long
        val segmentMax: Long
        when (mStatus) {
            STATUS_DIFFERENT -> {}
            STATUS_SAME_X -> {
                val x = mStartPoint.x
                if (mStartPoint.y <= mLatestPoint.y) {
                    segmentMin = mStartPoint.y
                    segmentMax = mLatestPoint.y
                } else {
                    segmentMin = mLatestPoint.y
                    segmentMax = mStartPoint.y
                }
                if (mMin < segmentMin) {
                    addToAccepter(x, mMin)
                }
                if (mMax > segmentMax) {
                    addToAccepter(x, mMax)
                }
                addToAccepter(x, mLatestPoint.y)
            }

            STATUS_SAME_Y -> {
                val y = mStartPoint.y
                if (mStartPoint.x <= mLatestPoint.x) {
                    segmentMin = mStartPoint.x
                    segmentMax = mLatestPoint.x
                } else {
                    segmentMin = mLatestPoint.x
                    segmentMax = mStartPoint.x
                }
                if (mMin < segmentMin) {
                    addToAccepter(mMin, y)
                }
                if (mMax > segmentMax) {
                    addToAccepter(mMax, y)
                }
                addToAccepter(mLatestPoint.x, y)
            }
        }
        mStatus = STATUS_DIFFERENT
    }

    /**
     * Actually adding the point to the embedded [PointAccepter]
     */
    private fun addToAccepter(pX: Long, pY: Long) {
        mPointAccepter.add(pX, pY)
    }

    companion object {
        private const val STATUS_DIFFERENT = 0
        private const val STATUS_SAME_X = 1
        private const val STATUS_SAME_Y = 2
    }
}
