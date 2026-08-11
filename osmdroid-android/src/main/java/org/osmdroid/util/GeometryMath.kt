package org.osmdroid.util

import android.graphics.Point
import android.graphics.Rect
import org.osmdroid.views.util.constants.MathConstants
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * @author Marc Kurtz
 */
object GeometryMath {
    @Deprecated("")
    val DEG2RAD: Double = MathConstants.DEG2RAD

    @Deprecated("")
    val RAD2DEG: Double = MathConstants.RAD2DEG

    fun getBoundingBoxForRotatatedRectangle(rect: Rect, angle: Float, reuse: Rect?): Rect {
        return getBoundingBoxForRotatatedRectangle(
            rect, rect.centerX(), rect.centerY(), angle,
            reuse
        )
    }

    fun getBoundingBoxForRotatatedRectangle(
        rect: Rect, centerPoint: Point,
        angle: Float, reuse: Rect?
    ): Rect {
        return getBoundingBoxForRotatatedRectangle(rect, centerPoint.x, centerPoint.y, angle, reuse)
    }

    fun getBoundingBoxForRotatatedRectangle(
        rect: Rect, centerX: Int,
        centerY: Int, angle: Float, reuse: Rect?
    ): Rect {
        var reuse = reuse
        if (reuse == null) reuse = Rect()

        val theta = angle * MathConstants.DEG2RAD
        val sinTheta = sin(theta)
        val cosTheta = cos(theta)
        val dx1 = (rect.left - centerX).toDouble()
        val dy1 = (rect.top - centerY).toDouble()
        val newX1 = centerX - dx1 * cosTheta + dy1 * sinTheta
        val newY1 = centerY - dx1 * sinTheta - dy1 * cosTheta
        val dx2 = (rect.right - centerX).toDouble()
        val dy2 = (rect.top - centerY).toDouble()
        val newX2 = centerX - dx2 * cosTheta + dy2 * sinTheta
        val newY2 = centerY - dx2 * sinTheta - dy2 * cosTheta
        val dx3 = (rect.left - centerX).toDouble()
        val dy3 = (rect.bottom - centerY).toDouble()
        val newX3 = centerX - dx3 * cosTheta + dy3 * sinTheta
        val newY3 = centerY - dx3 * sinTheta - dy3 * cosTheta
        val dx4 = (rect.right - centerX).toDouble()
        val dy4 = (rect.bottom - centerY).toDouble()
        val newX4 = centerX - dx4 * cosTheta + dy4 * sinTheta
        val newY4 = centerY - dx4 * sinTheta - dy4 * cosTheta
        reuse.left = MyMath.floorToInt(Min4(newX1, newX2, newX3, newX4))
        reuse.top = MyMath.floorToInt(Min4(newY1, newY2, newY3, newY4))
        reuse.right = MyMath.floorToInt(Max4(newX1, newX2, newX3, newX4))
        reuse.bottom = MyMath.floorToInt(Max4(newY1, newY2, newY3, newY4))

        return reuse
    }

    private fun Min4(a: Double, b: Double, c: Double, d: Double): Double {
        return floor(min(min(a, b), min(c, d)))
    }

    private fun Max4(a: Double, b: Double, c: Double, d: Double): Double {
        return ceil(max(max(a, b), max(c, d)))
    }
}
