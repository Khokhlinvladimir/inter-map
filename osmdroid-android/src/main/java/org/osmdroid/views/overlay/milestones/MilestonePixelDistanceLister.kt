package org.osmdroid.views.overlay.milestones

import org.osmdroid.util.Distance
import org.osmdroid.views.util.constants.MathConstants
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class MilestonePixelDistanceLister(private val mNbPixelsInit: Double, private val mNbPixelsRecurrence: Double) : MilestoneLister() {
    private var mDistance = 0.0

    override fun init() {
        super.init()
        mDistance = mNbPixelsRecurrence - mNbPixelsInit // might be tricky if negative
    }

    override fun add(x0: Long, y0: Long, x1: Long, y1: Long) {
        var currentDistance = sqrt(Distance.getSquaredDistanceToPoint(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble()))
        if (currentDistance == 0.0) {
            return
        }
        val orientation: Double = MilestoneLister.Companion.getOrientation(x0, y0, x1, y1)
        var x = x0.toDouble()
        var y = y0.toDouble()
        while (true) {
            val latestMilestone = floor(mDistance / mNbPixelsRecurrence) * mNbPixelsRecurrence
            val neededForNext = latestMilestone + mNbPixelsRecurrence - mDistance
            if (currentDistance < neededForNext) {
                mDistance += currentDistance
                return
            }
            mDistance += neededForNext
            currentDistance -= neededForNext
            x += neededForNext * cos(MathConstants.DEG2RAD * orientation)
            y += neededForNext * sin(MathConstants.DEG2RAD * orientation)
            add(MilestoneStep(x.toLong(), y.toLong(), orientation, mDistance))
        }
    }
}
