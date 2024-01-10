package org.osmdroid.views.overlay.milestones

import org.osmdroid.util.Distance

/**
 * Listing all the vertices' middle, provided that there are enough pixels between them
 * Created by Fabrice on 23/12/2017.
 *
 * @since 6.0.0
 */
class MilestoneMiddleLister(pMinimumPixelDistance: Double) : MilestoneLister() {
    private val mMinimumSquaredPixelDistance: Double

    init {
        mMinimumSquaredPixelDistance = pMinimumPixelDistance * pMinimumPixelDistance
    }

    override fun add(x0: Long, y0: Long, x1: Long, y1: Long) {
        if (Distance.getSquaredDistanceToPoint(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble()) <= mMinimumSquaredPixelDistance) {
            return
        }
        val centerX = (x0 + x1) / 2
        val centerY = (y0 + y1) / 2
        val orientation = getOrientation(x0, y0, x1, y1)
        add(MilestoneStep(centerX, centerY, orientation))
    }
}