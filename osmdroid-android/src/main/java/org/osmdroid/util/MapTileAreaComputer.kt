package org.osmdroid.util

/**
 * Compute a map tile area from a map tile area source
 */

interface MapTileAreaComputer {
    fun computeFromSource(pSource: MapTileArea?, pReuse: MapTileArea?): MapTileArea?
}