package org.osmdroid.util

/**
 * Compute a map tile list from a map tile list source, on another zoom level
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
@Deprecated("Use {@link MapTileAreaZoomComputer} instead")
class MapTileListZoomComputer(val zoomDelta: Int) : MapTileListComputer {
    override fun computeFromSource(pSource: MapTileList, pReuse: MapTileList?): MapTileList {
        val out = if (pReuse != null) pReuse else MapTileList()
        for (i in 0 until pSource.size) {
            val sourceIndex = pSource.get(i)
            val sourceZoom = MapTileIndex.getZoom(sourceIndex)
            val destZoom = sourceZoom + this.zoomDelta
            if (destZoom < 0 || destZoom > MapTileIndex.mMaxZoomLevel) {
                continue
            }
            val sourceX = MapTileIndex.getX(sourceIndex)
            val sourceY = MapTileIndex.getY(sourceIndex)
            if (this.zoomDelta <= 0) {
                out.put(MapTileIndex.getTileIndex(destZoom, sourceX shr -this.zoomDelta, sourceY shr -this.zoomDelta))
                continue
            }
            val power = 1 shl this.zoomDelta
            val destX = sourceX shl this.zoomDelta
            val destY = sourceY shl this.zoomDelta
            for (j in 0 until power) {
                for (k in 0 until power) {
                    out.put(MapTileIndex.getTileIndex(destZoom, destX + j, destY + k))
                }
            }
        }
        return out
    }
}
