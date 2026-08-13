package org.osmdroid.views.overlay.advancedpolyline

/**
 * Color mapping for hue variation.
 *
 * @author Matthias Dittmer
 */
class ColorMappingVariationHue(
    scalarStart: Float, scalarEnd: Float, hueStart: Float, hueEnd: Float,
    saturation: Float, luminance: Float
) : ColorMappingVariation() {
    /**
     * Fixed HSL values.
     */
    private val mSaturation: Float
    private val mLuminance: Float

    /**
     * Constructor
     *
     * @param scalarStart start of scalar
     * @param scalarEnd   end of scalar
     * @param hueStart    hue start value
     * @param hueEnd      hue end value
     * @param saturation  fixed saturation value
     * @param luminance   fixed luminance value
     */
    init {
        // do basic clipping for hue value
        // please note: end can be lower than start for inverse mapping

        var hueStart = hueStart
        var hueEnd = hueEnd
        hueStart = ColorHelper.constrain(hueStart, 0.0f, 360.0f)
        hueEnd = ColorHelper.constrain(hueEnd, 0.0f, 360.0f)

        // do clipping for saturation and luminance
        mSaturation = ColorHelper.constrain(saturation, 0.0f, 1.0f)
        mLuminance = ColorHelper.constrain(luminance, 0.0f, 1.0f)

        init(scalarStart, scalarEnd, hueStart, hueEnd)
    }

    override fun getHue(pScalar: Float): Float {
        return mapScalar(pScalar)
    }

    override fun getSaturation(pScalar: Float): Float {
        return mSaturation
    }

    override fun getLuminance(pScalar: Float): Float {
        return mLuminance
    }
}
