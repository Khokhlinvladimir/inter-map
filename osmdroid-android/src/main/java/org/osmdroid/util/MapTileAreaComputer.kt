package org.osmdroid.util

/**
 * Compute a map tile area from a map tile area source
 *
 * @author Fabrice Fontaine
 * @since 6.0.3
 */
interface MapTileAreaComputer {
    fun computeFromSource(pSource: MapTileArea?, pReuse: MapTileArea?): MapTileArea?
}