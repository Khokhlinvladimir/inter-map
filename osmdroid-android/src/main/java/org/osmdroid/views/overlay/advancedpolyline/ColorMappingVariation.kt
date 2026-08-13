package org.osmdroid.views.overlay.advancedpolyline

/**
 * Abstract base class for color variation mappings.
 *
 * @author Matthias Dittmer
 */
abstract class ColorMappingVariation : ColorMappingForScalar() {
    /**
     * All mapping variables.
     */
    private var mStart = 0f
    private var mEnd = 0f
    private var mScalarStart = 0f
    private var mScalarEnd = 0f
    private var mSlope = 0f

    /**
     * Init function will be called from sub classes.
     *
     * @param scalarStart start of scalar
     * @param scalarEnd   end of scalar
     * @param start       start of one HSL value
     * @param end         end of one HSL value
     */
    fun init(
        scalarStart: Float, scalarEnd: Float,
        start: Float, end: Float
    ) {
        mScalarStart = scalarStart
        mScalarEnd = scalarEnd
        mStart = start
        mEnd = end

        // calc slope once here for linear interpolation
        mSlope = if (mScalarEnd == mScalarStart) 1f else (mEnd - mStart) / (mScalarEnd - mScalarStart)
    }

    override fun computeColor(pScalar: Float): Int {
        return ColorHelper.HSLToColor(getHue(pScalar), getSaturation(pScalar), getLuminance(pScalar))
    }

    protected abstract fun getHue(pScalar: Float): Float

    protected abstract fun getSaturation(pScalar: Float): Float

    protected abstract fun getLuminance(pScalar: Float): Float

    /**
     * Map a scalar with clipping on lower and upper bound.
     */
    fun mapScalar(scalar: Float): Float {
        if (scalar >= mScalarEnd) {
            return mEnd
        } else if (scalar <= mScalarStart) {
            return mStart
        } else {
            // scalar is between start and end
            // do a linear mapping
            return (scalar - mScalarStart) * mSlope + mStart
        }
    }
}
