package org.osmdroid.views.overlay.advancedpolyline

/**
 * Color mapping with just one color. Not really a mapping.
 *
 * @author Matthias Dittmer
 */
class ColorMappingPlain(
    /**
     * Line color
     */
    private val mColorPlain: Int
) : ColorMapping {
    override fun getColorForIndex(pSegmentIndex: Int): Int {
        return mColorPlain
    }
}
