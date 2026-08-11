package org.osmdroid.util

/**
 * Compute a map tile list from a map tile list source: its border
 */
@Deprecated("Use {@link MapTileAreaBorderComputer} instead")
class MapTileListBorderComputer(val border: Int, val isIncludeAll: Boolean) : MapTileListComputer {
    override fun computeFromSource(pSource: MapTileList, pReuse: MapTileList?): MapTileList {
        val out = if (pReuse != null) pReuse else MapTileList()
        for (i in 0 until pSource.size) {
            val sourceIndex = pSource.get(i)
            val zoom = MapTileIndex.getZoom(sourceIndex)
            val sourceX = MapTileIndex.getX(sourceIndex)
            val sourceY = MapTileIndex.getY(sourceIndex)
            val power = 1 shl zoom
            for (j in -this.border..this.border) {
                for (k in -this.border..this.border) {
                    var destX = sourceX + j
                    var destY = sourceY + k
                    while (destX < 0) {
                        destX += power
                    }
                    while (destY < 0) {
                        destY += power
                    }
                    while (destX >= power) {
                        destX -= power
                    }
                    while (destY >= power) {
                        destY -= power
                    }
                    val index = MapTileIndex.getTileIndex(zoom, destX, destY)
                    if (out.contains(index)) {
                        continue
                    }
                    if (pSource.contains(index) && !this.isIncludeAll) {
                        continue
                    }
                    out.put(index)
                }
            }
        }
        return out
    }
}
