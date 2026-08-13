package org.osmdroid.views.overlay.advancedpolyline

/**
 * Color mapping to cycle through an array of colors.
 *
 * @author Matthias Dittmer
 */
class ColorMappingCycle : ColorMapping {
    private val mColorList: MutableList<Int?>?
    private val mColorArray: IntArray?
    private var mGeoPointNumber = 0

    constructor(pColors: MutableList<Int?>?) {
        mColorList = pColors
        mColorArray = null
    }

    constructor(pColors: IntArray?) {
        mColorList = null
        mColorArray = pColors
    }

    /**
     * Ignore most of the time.
     * Only useful if you display a closed polyline with gradients:
     * * when displaying a segment with a gradient,
     * you compute the gradient from the current segment's color to the next segment's color
     * * in the closing segment case, we compute normally the color of the last segment,
     * but we also need to know the color of the next segment, which is the very first segment
     * That's why we need to know the number of segments.
     * Without that information, we would just give the next color of the cycle.
     *
     * @param pGeoPointNumber Number of GeoPoints of the polyline
     */
    fun setGeoPointNumber(pGeoPointNumber: Int) {
        mGeoPointNumber = pGeoPointNumber
    }

    override fun getColorForIndex(pSegmentIndex: Int): Int {
        var pSegmentIndex = pSegmentIndex
        if (mGeoPointNumber > 0 && pSegmentIndex >= mGeoPointNumber) {
            pSegmentIndex = 0
        }
        if (mColorArray != null) {
            return mColorArray[pSegmentIndex % mColorArray.size]
        }
        if (mColorList != null) {
            return mColorList.get(pSegmentIndex % mColorList.size)!!
        }
        throw IllegalArgumentException()
    }
}
