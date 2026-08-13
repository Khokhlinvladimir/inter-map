package org.osmdroid.samplefragments.tilesources

import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.samplefragments.BaseSampleFragment
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Created by alex on 12/24/15.
 */
class SampleWhackyColorFilter : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Whacky Color Tiles"

    public override fun addOverlays() {
        //p.s. there's a ton of examples here
        //http://stackoverflow.com/questions/4354939/understanding-the-use-of-colormatrix-and-colormatrixcolorfilter-to-modify-a-draw
        //http://developer.android.com/reference/android/graphics/ColorMatrix.html

        //this will make things look pinkish
        //this.mMapView.getOverlayManager().getTilesOverlay().setColorFilter(adjustHue(160));

        val cm = ColorMatrix()
        val brightness = .5f // reduce color's by 50%. i.e. just make it darker
        cm.set(
            floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,  //red
                0f, brightness, 0f, 0f, 0f,  //green
                0f, 0f, brightness, 0f, 0f,  //blue
                0f, 0f, 0f, 1f, 0f
            )
        ) //alpha

        this.mMapView!!.getOverlayManager().getTilesOverlay()!!.setColorFilter(ColorMatrixColorFilter(cm))

        //pro tip, set the color filter to null to reset to normal viewing
    }

    companion object {
        /**
         * Creates a HUE adjustment ColorFilter +- 180
         * http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
         * http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
         *
         * @param value degrees to shift the hue.
         * @return
         */
        fun adjustHue(value: Float): ColorFilter {
            val cm = ColorMatrix()

            adjustHue(cm, value)

            return ColorMatrixColorFilter(cm)
        }

        /**
         * http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
         * http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
         *
         * @param cm
         * @param value
         */
        fun adjustHue(cm: ColorMatrix, value: Float) {
            var value = value
            value = cleanValue(value, 180f) / 180f * Math.PI.toFloat()
            if (value == 0f) {
                return
            }
            val cosVal = cos(value.toDouble()).toFloat()
            val sinVal = sin(value.toDouble()).toFloat()
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            val mat = floatArrayOf(
                lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
                lumG + cosVal * (-lumG) + sinVal * (-lumG),
                lumB + cosVal * (-lumB) + sinVal * (1 - lumB),
                0f,
                0f,
                lumR + cosVal * (-lumR) + sinVal * (0.143f),
                lumG + cosVal * (1 - lumG) + sinVal * (0.140f),
                lumB + cosVal * (-lumB) + sinVal * (-0.283f),
                0f,
                0f,
                lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
                lumG + cosVal * (-lumG) + sinVal * (lumG),
                lumB + cosVal * (1 - lumB) + sinVal * (lumB),
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f
            )
            cm.postConcat(ColorMatrix(mat))
        }

        protected fun cleanValue(p_val: Float, p_limit: Float): Float {
            return min(p_limit, max(-p_limit, p_val))
        }
    }
}
