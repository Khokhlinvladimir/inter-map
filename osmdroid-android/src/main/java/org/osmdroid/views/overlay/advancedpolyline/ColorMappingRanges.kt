package org.osmdroid.views.overlay.advancedpolyline

import java.util.SortedMap

/**
 * Color mapping to map ranges to specific colors.
 *
 * @author Matthias Dittmer
 */
class ColorMappingRanges(
    /**
     * Using a sorted map to define borders of ranges.
     * Borders are sorted from low to high.
     */
    private val mColorRanges: SortedMap<Float?, Int?>, private val mStrictComparison: Boolean
) : ColorMappingForScalar() {
    override fun computeColor(pScalar: Float): Int {
        var lastArrayIndexFromLoop = 0
        // iterate over array and sort point in
        for (entry in mColorRanges.entries) {
            if (mStrictComparison) {
                if (pScalar < entry.key!!) {
                    return entry.value!!
                }
            } else {
                if (pScalar <= entry.key!!) {
                    return entry.value!!
                }
            }
            lastArrayIndexFromLoop++
        }
        // assign last color if scalar is above highest border
        if (lastArrayIndexFromLoop == mColorRanges.size) {
            return mColorRanges.get(mColorRanges.lastKey())!!
        }
        return 0
    }
}
