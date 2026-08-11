package org.osmdroid.views.overlay.advancedpolyline

/**
 * Color mapping for saturation variation.
 *
 * @author Matthias Dittmer
 */
class ColorMappingVariationSaturation(
    scalarStart: Float, scalarEnd: Float, saturationStart: Float, saturationEnd: Float,
    hue: Float, luminance: Float
) : ColorMappingVariation() {
    /**
     * Fixed HSL values.
     */
    private val mHue: Float
    private val mLuminance: Float

    /**
     * Constructor
     *
     * @param scalarStart     start of scalar
     * @param scalarEnd       end of scalar
     * @param saturationStart saturation start value
     * @param saturationEnd   saturation end value
     * @param hue             fixed hue value
     * @param luminance       fixed luminance value
     */
    init {
        // do basic clipping for saturation value
        // please note: end can be lower than start for inverse mapping

        var saturationStart = saturationStart
        var saturationEnd = saturationEnd
        saturationStart = ColorHelper.constrain(saturationStart, 0.0f, 1.0f)
        saturationEnd = ColorHelper.constrain(saturationEnd, 0.0f, 1.0f)

        // do clipping for hue and luminance
        mHue = ColorHelper.constrain(hue, 0.0f, 360.0f)
        mLuminance = ColorHelper.constrain(luminance, 0.0f, 1.0f)

        init(scalarStart, scalarEnd, saturationStart, saturationEnd)
    }

    override fun getHue(pScalar: Float): Float {
        return mHue
    }

    override fun getSaturation(pScalar: Float): Float {
        return mapScalar(pScalar)
    }

    override fun getLuminance(pScalar: Float): Float {
        return mLuminance
    }
}
