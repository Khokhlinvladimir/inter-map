package org.osmdroid.views.overlay.advancedpolyline

import android.graphics.Color
import kotlin.math.abs

/**
 * Class with color helper functions.
 * Please note the functions were copied over from:
 * https://developer.android.com/reference/android/support/v4/graphics/ColorUtils (old support lib)
 * https://developer.android.com/reference/kotlin/androidx/core/graphics/ColorUtils (new Androidx lib)
 * Maybe include one lib directly.
 *
 * @author Matthias Dittmer
 */
object ColorHelper {
    /**
     * Convert HSL to color value.
     *
     * @param h float h value form HSL color
     * @param s float s value form HSL color
     * @param l float l value form HSL color
     * @return int color value
     */
    fun HSLToColor(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2 * l - 1f)) * s
        val m = l - 0.5f * c
        val x = c * (1f - abs((h / 60f % 2f) - 1f))

        val hueSegment = h.toInt() / 60

        var r = 0
        var g = 0
        var b = 0

        when (hueSegment) {
            0 -> {
                r = Math.round(255 * (c + m))
                g = Math.round(255 * (x + m))
                b = Math.round(255 * m)
            }

            1 -> {
                r = Math.round(255 * (x + m))
                g = Math.round(255 * (c + m))
                b = Math.round(255 * m)
            }

            2 -> {
                r = Math.round(255 * m)
                g = Math.round(255 * (c + m))
                b = Math.round(255 * (x + m))
            }

            3 -> {
                r = Math.round(255 * m)
                g = Math.round(255 * (x + m))
                b = Math.round(255 * (c + m))
            }

            4 -> {
                r = Math.round(255 * (x + m))
                g = Math.round(255 * m)
                b = Math.round(255 * (c + m))
            }

            5, 6 -> {
                r = Math.round(255 * (c + m))
                g = Math.round(255 * m)
                b = Math.round(255 * (x + m))
            }
        }

        r = constrain(r, 0, 255)
        g = constrain(g, 0, 255)
        b = constrain(b, 0, 255)

        return Color.rgb(r, g, b)
    }

    /**
     * Constrain int value.
     *
     * @param amount input value
     * @param low    lower bound
     * @param high   upper bound
     * @return constrained value
     */
    private fun constrain(amount: Int, low: Int, high: Int): Int {
        return if (amount < low) low else (if (amount > high) high else amount)
    }

    /**
     * Constrain float value.
     *
     * @param amount input value
     * @param low    lower bound
     * @param high   upper bound
     * @return constrained value
     */
    fun constrain(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else (if (amount > high) high else amount)
    }
}
