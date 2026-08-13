package org.osmdroid.util

import org.osmdroid.views.util.constants.MathConstants
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

object MyMath : MathConstants {
    @JvmStatic
    fun gudermannInverse(aLatitude: Double): Double {
        return ln(tan(MathConstants.Companion.PI_4 + (MathConstants.Companion.DEG2RAD * aLatitude / 2)))
    }

    @JvmStatic
    fun gudermann(y: Double): Double {
        return MathConstants.Companion.RAD2DEG * atan(sinh(y))
    }

    @JvmStatic
    fun mod(number: Int, modulus: Int): Int {
        var number = number
        if (number > 0) return number % modulus

        while (number < 0) number += modulus

        return number
    }

    /**
     * Casting a _negative_ double into a long has a counter-intuitive result.
     * E.g. (long)(-0.4) = 0, though -1 would be expected.
     * Math.floor would be the answer, but I assume we could go faster than (long)Math.floor
     */
    @JvmStatic
    fun floorToLong(pValue: Double): Long {
        val result = pValue.toLong()
        if (result <= pValue) {
            return result
        }
        return result - 1
    }


    @JvmStatic
    fun floorToInt(pValue: Double): Int {
        val result = pValue.toInt()
        if (result <= pValue) {
            return result
        }
        return result - 1
    }

    /**
     * Moved from another MyMath (org.osmdroid.views.util)
     *
     *
     * Calculates i.e. the increase of zoomlevel needed when the visible latitude needs to be bigger
     * by `factor`.
     *
     *
     * Assert.assertEquals(1, getNextSquareNumberAbove(1.1f)); Assert.assertEquals(2,
     * getNextSquareNumberAbove(2.1f)); Assert.assertEquals(2, getNextSquareNumberAbove(3.9f));
     * Assert.assertEquals(3, getNextSquareNumberAbove(4.1f)); Assert.assertEquals(3,
     * getNextSquareNumberAbove(7.9f)); Assert.assertEquals(4, getNextSquareNumberAbove(8.1f));
     * Assert.assertEquals(5, getNextSquareNumberAbove(16.1f));
     *
     *
     * Assert.assertEquals(-1, - getNextSquareNumberAbove(1 / 0.4f) + 1); Assert.assertEquals(-2, -
     * getNextSquareNumberAbove(1 / 0.24f) + 1);
     */
    @JvmStatic
    fun getNextSquareNumberAbove(factor: Float): Int {
        var out = 0
        var cur = 1
        var i = 1
        while (true) {
            if (cur > factor) return out

            out = i
            cur *= 2
            i++
        }
    }

    /**
     * @param pStart     start angle
     * @param pEnd       end angle
     * @param pClockwise if null, get the smallest difference (in absolute value)
     * if true, go clockwise
     * if false, go anticlockwise
     */
    @JvmStatic
    fun getAngleDifference(pStart: Double, pEnd: Double, pClockwise: Boolean?): Double {
        val difference = cleanPositiveAngle(pEnd - pStart)
        if (pClockwise != null) {
            if (pClockwise) {
                return difference
            } else {
                return difference - 360
            }
        }
        if (difference < 180) {
            return difference
        }
        return difference - 360
    }

    /**
     * @param pAngle angle in degree
     * @return the same angle in [0,360[
     */
    @JvmStatic
    fun cleanPositiveAngle(pAngle: Double): Double {
        var pAngle = pAngle
        while (pAngle < 0) {
            pAngle += 360.0
        }
        while (pAngle >= 360) {
            pAngle -= 360.0
        }
        return pAngle
    }

    /**
     * Computes the angle of a vector
     */
    @JvmStatic
    fun computeAngle(pX1: Long, pY1: Long, pX2: Long, pY2: Long): Double {
        return atan2((pY2 - pY1).toDouble(), (pX2 - pX1).toDouble())
    }

    /**
     * @param pAngle clockwise angle, in radian, value 0 being 3 o'clock
     * Computes the point of a circle from its center, its radius and the angle
     */
    @JvmStatic
    fun computeCirclePoint(
        pCenterX: Long, pCenterY: Long, pRadius: Double,
        pAngle: Double, pOutput: PointL
    ) {
        pOutput.x = pCenterX + (pRadius * cos(pAngle)).toLong()
        pOutput.y = pCenterY + (pRadius * sin(pAngle)).toLong()
    }
}
