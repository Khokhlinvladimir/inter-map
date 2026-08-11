package org.osmdroid.views.overlay.milestones

import org.osmdroid.util.Distance
import org.osmdroid.views.util.constants.MathConstants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Listing every given meters of the `Path`
 * Created by Fabrice on 28/12/2017.
 *
 * @since 6.0.0
 */
class MilestoneMeterDistanceLister : MilestoneLister {
    private val mNbMetersRecurrence: Double
    private var mDistance = 0.0
    private var mIndex = 0

    /**
     * @since 6.0.3
     */
    private val mMilestoneMeters: DoubleArray?
    private var mMilestoneMetersIndex = 0
    private var mNeededForNext = 0.0

    // handling last milestone's side effect (with all the roundings and double/float conversions)
    private var mSideEffectLastFlag = false
    private var mSideEffectLastEpsilon = 1E-5
    private var mSideEffectLastX: Long = 0
    private var mSideEffectLastY: Long = 0
    private var mSideEffectLastOrientation = 0.0

    /**
     * Use it if you want a milestone every x meters
     */
    constructor(pNbMetersRecurrence: Double) {
        mNbMetersRecurrence = pNbMetersRecurrence
        mMilestoneMeters = null
    }

    /**
     * @since 6.0.3
     * Use it if you want milestones separated by different length (in meters)
     * All the distances are from the origin and must be increasing.
     * E.g for a marathon: [0, 10000, 20000, 21097, 30000, 40000, 42195]
     */
    constructor(pMilestoneMeters: DoubleArray?) {
        mNbMetersRecurrence = 0.0
        mMilestoneMeters = pMilestoneMeters
    }

    override fun init() {
        super.init()
        mDistance = 0.0
        mIndex = 0
        if (mMilestoneMeters != null) {
            mMilestoneMetersIndex = 0
        }
        mNeededForNext = this.newNeededForNext
        mSideEffectLastFlag = false
    }

    override fun add(x0: Long, y0: Long, x1: Long, y1: Long) {
        mSideEffectLastFlag = false
        if (mNeededForNext == -1.0) {
            return
        }
        var currentDistance = getDistance(++mIndex)
        if (currentDistance == 0.0) {
            return
        }
        val pixelDistance = sqrt(Distance.getSquaredDistanceToPoint(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble()))
        val metersToPixels = pixelDistance / currentDistance
        val orientation: Double = MilestoneLister.Companion.getOrientation(x0, y0, x1, y1)
        var x = x0.toDouble()
        var y = y0.toDouble()
        while (true) {
            if (currentDistance < mNeededForNext) {
                mDistance += currentDistance
                mNeededForNext -= currentDistance
                mSideEffectLastFlag = true
                mSideEffectLastX = x1
                mSideEffectLastY = y1
                mSideEffectLastOrientation = orientation
                return
            }
            mDistance += mNeededForNext
            currentDistance -= mNeededForNext
            x += mNeededForNext * cos(MathConstants.DEG2RAD * orientation) * metersToPixels
            y += mNeededForNext * sin(MathConstants.DEG2RAD * orientation) * metersToPixels
            add(x.toLong(), y.toLong(), orientation)
            mNeededForNext = this.newNeededForNext
            if (mNeededForNext == -1.0) {
                return
            }
        }
    }

    private val newNeededForNext: Double
        /**
         * @since 6.0.3
         */
        get() {
            if (mMilestoneMeters == null) {
                return mNbMetersRecurrence
            }
            if (mMilestoneMetersIndex >= mMilestoneMeters.size) {
                return -1.0
            }
            val before = if (mMilestoneMetersIndex == 0) 0.0 else mMilestoneMeters[mMilestoneMetersIndex - 1]
            val needed = mMilestoneMeters[mMilestoneMetersIndex++] - before
            require(!(needed < 0))
            return needed
        }

    /**
     * @since 6.0.3
     */
    override fun end() {
        if (mSideEffectLastFlag && mNeededForNext < mSideEffectLastEpsilon) {
            add(mSideEffectLastX, mSideEffectLastY, mSideEffectLastOrientation)
        }
        super.end()
    }

    /**
     * @since 6.0.3
     */
    fun setSideEffectLastEpsilon(pSideEffectLastEpsilon: Double) {
        mSideEffectLastEpsilon = pSideEffectLastEpsilon
    }

    /**
     * @since 6.0.3
     */
    private fun add(pX: Long, pY: Long, pOrientation: Double) {
        add(MilestoneStep(pX, pY, pOrientation, mDistance))
    }
}
