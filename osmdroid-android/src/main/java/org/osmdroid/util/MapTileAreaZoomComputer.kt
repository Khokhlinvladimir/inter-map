package org.osmdroid.util

/**
 * Compute a map tile area from a map tile area source: the source on another zoom level
 */
class MapTileAreaZoomComputer(private val mZoomDelta: Int) : MapTileAreaComputer {
    override fun computeFromSource(pSource: MapTileArea, pReuse: MapTileArea?): MapTileArea {
        val out = if (pReuse != null) pReuse else MapTileArea()
        if (pSource.size() == 0) {
            out.reset()
            return out
        }
        val sourceZoom = pSource.zoom
        val destZoom = sourceZoom + mZoomDelta
        if (destZoom < 0 || destZoom > MapTileIndex.mMaxZoomLevel) {
            out.reset()
            return out
        }
        if (mZoomDelta <= 0) {
            out.set(
                destZoom,
                pSource.left shr -mZoomDelta, pSource.top shr -mZoomDelta,
                pSource.right shr -mZoomDelta, pSource.bottom shr -mZoomDelta
            )
            return out
        }
        out.set(
            destZoom,
            pSource.left shl mZoomDelta, pSource.top shl mZoomDelta,
            ((1 + pSource.right) shl mZoomDelta) - 1, ((1 + pSource.bottom) shl mZoomDelta) - 1
        )
        return out
    }
}
