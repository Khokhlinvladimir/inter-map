package org.osmdroid.util

/**
 * Compute a map tile list from a map tile list source
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
@Deprecated("Use {@link MapTileAreaComputer} instead")
interface MapTileListComputer {
    fun computeFromSource(pSource: MapTileList, pReuse: MapTileList?): MapTileList
}
