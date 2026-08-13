package org.osmdroid.views.overlay.advancedpolyline

/**
 * Color mapping for luminance variation.
 *
 * @author Matthias Dittmer
 */
class ColorMappingVariationLuminance(
    scalarStart: Float, scalarEnd: Float, luminanceStart: Float, luminanceEnd: Float,
    hue: Float, saturation: Float
) : ColorMappingVariation() {
    /**
     * Fixed HSL values.
     */
    private val mHue: Float
    private val mSaturation: Float

    /**
     * Constructor
     *
     * @param scalarStart    start of scalar
     * @param scalarEnd      end of scalar
     * @param luminanceStart luminance start value
     * @param luminanceEnd   luminance end value
     * @param hue            fixed hue value
     * @param saturation     fixed saturation value
     */
    init {
        // do basic clipping for luminance value
        // please note: end can be lower than start for inverse mapping

        var luminanceStart = luminanceStart
        var luminanceEnd = luminanceEnd
        luminanceStart = ColorHelper.constrain(luminanceStart, 0.0f, 1.0f)
        luminanceEnd = ColorHelper.constrain(luminanceEnd, 0.0f, 1.0f)

        // do clipping for hue and saturation
        mHue = ColorHelper.constrain(hue, 0.0f, 360.0f)
        mSaturation = ColorHelper.constrain(saturation, 0.0f, 1.0f)

        init(scalarStart, scalarEnd, luminanceStart, luminanceEnd)
    }

    override fun getHue(pScalar: Float): Float {
        return mHue
    }

    override fun getSaturation(pScalar: Float): Float {
        return mSaturation
    }

    override fun getLuminance(pScalar: Float): Float {
        return mapScalar(pScalar)
    }
}
