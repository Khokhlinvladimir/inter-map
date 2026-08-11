package org.osmdroid.views.overlay.milestones

import org.osmdroid.util.Distance
import org.osmdroid.views.util.constants.MathConstants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Listing the vertices for a slice of the path between two distances
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
class MilestoneMeterDistanceSliceLister : MilestoneLister() {
    private enum class Step {
        STEP_INIT,
        STEP_STARTED,
        STEP_ENDED
    }

    private var mNbMetersStart = 0.0
    private var mNbMetersEnd = 0.0
    private var mDistance = 0.0
    private var mIndex = 0
    private var mStep: Step? = null

    fun setMeterDistanceSlice(pNbMetersStart: Double, pNbMetersEnd: Double) {
        mNbMetersStart = pNbMetersStart
        mNbMetersEnd = pNbMetersEnd
    }

    override fun init() {
        super.init()
        mDistance = 0.0
        mIndex = 0
        mStep = Step.STEP_INIT
    }

    override fun add(x0: Long, y0: Long, x1: Long, y1: Long) {
        if (mStep == Step.STEP_ENDED) {
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
        if (mStep == Step.STEP_INIT) { // looking for the first distance
            val neededForNext = mNbMetersStart - mDistance
            if (neededForNext > currentDistance) { // not reached yet
                mDistance += currentDistance
                return
            }
            mStep = Step.STEP_STARTED
            mDistance += neededForNext
            currentDistance -= neededForNext
            x += neededForNext * cos(MathConstants.DEG2RAD * orientation) * metersToPixels
            y += neededForNext * sin(MathConstants.DEG2RAD * orientation) * metersToPixels
            add(MilestoneStep(x.toLong(), y.toLong(), orientation, null))
            if (mNbMetersStart == mNbMetersEnd) {
                mStep = Step.STEP_ENDED
                return
            }
        }
        if (mStep == Step.STEP_STARTED) { // looking for the second/last distance
            val neededForNext = mNbMetersEnd - mDistance
            if (neededForNext > currentDistance) { // not reached yet
                mDistance += currentDistance
                add(MilestoneStep(x1, y1, orientation, null))
                return
            }
            mStep = Step.STEP_ENDED
            x += neededForNext * cos(MathConstants.DEG2RAD * orientation) * metersToPixels
            y += neededForNext * sin(MathConstants.DEG2RAD * orientation) * metersToPixels
            add(MilestoneStep(x.toLong(), y.toLong(), orientation, null))
        }
    }
}
