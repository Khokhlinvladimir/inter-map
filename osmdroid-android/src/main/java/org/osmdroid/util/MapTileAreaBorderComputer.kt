package org.osmdroid.util

/**
 * Compute a map tile area from a map tile area source: the source with a border
 */
class MapTileAreaBorderComputer(val border: Int) : MapTileAreaComputer {
    override fun computeFromSource(pSource: MapTileArea, pReuse: MapTileArea?): MapTileArea {
        val out = if (pReuse != null) pReuse else MapTileArea()
        if (pSource.size() == 0) {
            out.reset()
            return out
        }
        val left = pSource.left - this.border
        val top = pSource.top - this.border
        val additional = 2 * this.border - 1
        out.set(
            pSource.zoom,
            left, top,
            left + pSource.width + additional, top + pSource.height + additional
        )
        return out
    }
}
