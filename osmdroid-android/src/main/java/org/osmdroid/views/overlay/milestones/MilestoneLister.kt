package org.osmdroid.views.overlay.milestones

import org.osmdroid.util.PointAccepter
import org.osmdroid.util.PointL
import org.osmdroid.views.util.constants.MathConstants
import kotlin.math.atan

/**
 * Creating a list of `MilestoneStep`s from a list of `PointL`
 * Created by Fabrice on 22/12/2017.
 *
 * @since 6.0.0
 */
abstract class MilestoneLister : PointAccepter {
    val milestones: MutableList<MilestoneStep> = ArrayList()
    private val mLatestPoint = PointL()
    private var mFirst = false
    private lateinit var mDistances: DoubleArray

    fun setDistances(pDistances: DoubleArray) {
        mDistances = pDistances
    }

    protected fun getDistance(pIndex: Int): Double {
        return mDistances[pIndex]
    }

    override fun init() {
        milestones.clear()
        mFirst = true
    }

    override fun add(pX: Long, pY: Long) {
        if (mFirst) {
            mFirst = false
            mLatestPoint.set(pX, pY)
        } else {
            add(mLatestPoint.x, mLatestPoint.y, pX, pY)
            mLatestPoint.set(pX, pY)
        }
    }

    override fun end() {
    }

    protected fun add(pMilestoneStep: MilestoneStep) {
        milestones.add(pMilestoneStep)
    }

    protected abstract fun add(x0: Long, y0: Long, x1: Long, y1: Long)

    companion object {
        /**
         * @return the orientation (in degrees) of the slope between point p0 and p1, or 0 if same point
         * @since 6.0.0
         */
        @JvmStatic
        fun getOrientation(x0: Long, y0: Long, x1: Long, y1: Long): Double {
            if (x0 == x1) {
                if (y0 == y1) {
                    return 0.0
                }
                if (y0 > y1) {
                    return -90.0
                }
                return 90.0
            }
            val slope = ((y1 - y0).toDouble()) / (x1 - x0)
            val isBeyondHalfPI = x1 < x0
            return MathConstants.RAD2DEG * atan(slope) + (if (isBeyondHalfPI) 180 else 0)
        }
    }
}
