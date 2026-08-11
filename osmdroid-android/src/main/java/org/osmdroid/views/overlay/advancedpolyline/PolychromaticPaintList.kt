package org.osmdroid.views.overlay.advancedpolyline

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import org.osmdroid.views.overlay.PaintList

/**
 * A real [PaintList] with potentially different colors for each segment, and linear gradients
 *
 * @author Fabrice Fontaine
 * @since 6.2.0
 */
class PolychromaticPaintList
/**
 * @param mPaint        Basis Paint
 * @param mColorMapping from where we get the color to use for each geo segment
 * @param mUseGradient  should we use a gradient from this segment's color to the next segment's
 */(private val mPaint: Paint, private val mColorMapping: ColorMapping, private val mUseGradient: Boolean) : PaintList {
    override val paint: Paint?
        get() = null

    override fun getPaint(pIndex: Int, pX0: Float, pY0: Float, pX1: Float, pY1: Float): Paint {
        val startColor = mColorMapping.getColorForIndex(pIndex)
        if (mUseGradient) {
            val endColor = mColorMapping.getColorForIndex(pIndex + 1)
            if (startColor != endColor) {
                val shader: Shader = LinearGradient(pX0, pY0, pX1, pY1, startColor, endColor, Shader.TileMode.CLAMP)
                mPaint.setShader(shader)
                return mPaint
            }
            mPaint.setShader(null)
        }
        mPaint.setColor(startColor)
        return mPaint
    }
}
