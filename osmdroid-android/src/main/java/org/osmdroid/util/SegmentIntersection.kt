package org.osmdroid.util

import kotlin.math.max
import kotlin.math.min

/**
 * A class dedicated to the computation of 2D segments intersection points
 */
object SegmentIntersection {
    /**
     * @param pXA           Starting point of Segment 1 [AB]
     * @param pYA           Starting point of Segment 1
     * @param pXB           Ending point of Segment 1
     * @param pYB           Ending point of Segment 1
     * @param pXC           Starting point of Segment 2 [CD]
     * @param pYC           Starting point of Segment 2
     * @param pXD           Ending point of Segment 2
     * @param pYD           Ending point of Segment 2
     * @param pIntersection Intersection point as output; can be null
     * @return true if the segments intersect
     * Parameters are typed as double for overflow and precision reasons.
     */
    @JvmStatic
    fun intersection(
        pXA: Double, pYA: Double, pXB: Double, pYB: Double,
        pXC: Double, pYC: Double, pXD: Double, pYD: Double,
        pIntersection: PointL?
    ): Boolean {
        if (parallelSideEffect(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection)) {
            return true
        }
        if (divisionByZeroSideEffect(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection)) {
            return true
        }
        val d = (pXA - pXB) * (pYC - pYD) - (pYA - pYB) * (pXC - pXD)
        if (d == 0.0) {
            return false
        }
        val xi = ((pXC - pXD) * (pXA * pYB - pYA * pXB) - (pXA - pXB) * (pXC * pYD - pYC * pXD)) / d
        val yi = ((pYC - pYD) * (pXA * pYB - pYA * pXB) - (pYA - pYB) * (pXC * pYD - pYC * pXD)) / d
        return check(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection, xi, yi)
    }

    /**
     * When the segments are parallels and overlap, the middle of the overlap is considered as the intersection
     */
    private fun parallelSideEffect(
        pXA: Double, pYA: Double, pXB: Double, pYB: Double,
        pXC: Double, pYC: Double, pXD: Double, pYD: Double,
        pIntersection: PointL?
    ): Boolean {
        if (pXA == pXB) {
            return parallelSideEffectSameX(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection)
        }
        if (pXC == pXD) {
            return parallelSideEffectSameX(pXC, pYC, pXD, pYD, pXA, pYA, pXB, pYB, pIntersection)
        }
        // formula like "y = k*x + b"
        val k1 = (pYB - pYA) / (pXB - pXA)
        val k2 = (pYD - pYC) / (pXD - pXC)
        if (k1 != k2) { // not parallel
            return false
        }
        val b1 = pYA - k1 * pXA
        val b2 = pYC - k2 * pXC
        if (b1 != b2) { // strictly parallel, no overlap
            return false
        }
        val xi = middle(pXA, pXB, pXC, pXD)
        val yi = middle(pYA, pYB, pYC, pYD)
        return check(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection, xi, yi)
    }

    private fun middle(pA: Double, pB: Double, pC: Double, pD: Double): Double {
        return (min(max(pA, pB), max(pC, pD)) + max(min(pA, pB), min(pC, pD))) / 2
    }

    /**
     * Checks if computed intersection is valid and sets output accordingly
     *
     * @param pXI intersection x
     * @param pYI intersection y
     * @return true if OK
     */
    private fun check(
        pXA: Double, pYA: Double, pXB: Double, pYB: Double,
        pXC: Double, pYC: Double, pXD: Double, pYD: Double,
        pIntersection: PointL?,
        pXI: Double, pYI: Double
    ): Boolean {
        if (pXI < min(pXA, pXB) || pXI > max(pXA, pXB)) {
            return false
        }
        if (pXI < min(pXC, pXD) || pXI > max(pXC, pXD)) {
            return false
        }
        if (pYI < min(pYA, pYB) || pYI > max(pYA, pYB)) {
            return false
        }
        if (pYI < min(pYC, pYD) || pYI > max(pYC, pYD)) {
            return false
        }
        if (pIntersection != null) {
            pIntersection.x = Math.round(pXI)
            pIntersection.y = Math.round(pYI)
        }
        return true
    }

    /**
     * Used when we cannot use the "y = a*x + b" formula
     */
    private fun parallelSideEffectSameX(
        pXA: Double, pYA: Double, pXB: Double, pYB: Double,
        pXC: Double, pYC: Double, pXD: Double, pYD: Double,
        pIntersection: PointL?
    ): Boolean {
        if (pXA != pXB) {
            return false
        }
        if (pXC != pXD) {
            return false // cannot be parallel
        }
        if (pXA != pXC) {
            return false // not the same x
        }
        val yi = middle(pYA, pYB, pYC, pYD)
        return check(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection, pXA, yi)
    }

    /**
     * Main intersection formula works only without division by zero
     */
    private fun divisionByZeroSideEffect(
        pXA: Double, pYA: Double, pXB: Double, pYB: Double,
        pXC: Double, pYC: Double, pXD: Double, pYD: Double,
        pIntersection: PointL?
    ): Boolean {
        return divisionByZeroSideEffectX(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection)
                || divisionByZeroSideEffectX(pXC, pYC, pXD, pYD, pXA, pYA, pXB, pYB, pIntersection)
                || divisionByZeroSideEffectY(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection)
                || divisionByZeroSideEffectY(pXC, pYC, pXD, pYD, pXA, pYA, pXB, pYB, pIntersection)
    }

    private fun divisionByZeroSideEffectX(
        pXA: Double, pYA: Double, pXB: Double, pYB: Double,
        pXC: Double, pYC: Double, pXD: Double, pYD: Double,
        pIntersection: PointL?
    ): Boolean {
        if (pXA != pXB) {
            return false
        }
        if (pXC == pXD) {
            return false // should be handled by the "parallel" side effect
        }
        val k = (pXA - pXC) / (pXD - pXC)
        val yi = k * (pYD - pYC) + pYC
        return check(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection, pXA, yi)
    }

    private fun divisionByZeroSideEffectY(
        pXA: Double, pYA: Double, pXB: Double, pYB: Double,
        pXC: Double, pYC: Double, pXD: Double, pYD: Double,
        pIntersection: PointL?
    ): Boolean {
        if (pYA != pYB) {
            return false
        }
        if (pYC == pYD) {
            return false // should be handled by the "parallel" side effect
        }
        val k = (pYA - pYC) / (pYD - pYC)
        val xi = k * (pXD - pXC) + pXC
        return check(pXA, pYA, pXB, pYB, pXC, pYC, pXD, pYD, pIntersection, xi, pYA)
    }
}
