package org.osmdroid.views.overlay.advancedpolyline

/**
 * Abstract base class for all implemented color mappings.
 *
 * @author Matthias Dittmer
 */
interface ColorMapping {
    fun getColorForIndex(pSegmentIndex: Int): Int
}
