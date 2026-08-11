package org.osmdroid.tileprovider.tilesource

import org.osmdroid.util.MapTileIndex

open class QuadTreeTileSource(
    aName: String?,
    aZoomMinLevel: Int, aZoomMaxLevel: Int, aTileSizePixels: Int,
    aImageFilenameEnding: String?, aBaseUrl: Array<out String?>?
) : OnlineTileSourceBase(
    aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels,
    aImageFilenameEnding, aBaseUrl
) {
    override fun getTileURLString(pMapTileIndex: Long): String? {
        return baseUrl + quadTree(pMapTileIndex) + mImageFilenameEnding
    }

    /**
     * Converts TMS tile coordinates to QuadTree
     *
     * @param pMapTileIndex The tile coordinates to convert
     * @return The QuadTree as String.
     */
    protected fun quadTree(pMapTileIndex: Long): String {
        val quadKey = StringBuilder()
        for (i in MapTileIndex.getZoom(pMapTileIndex) downTo 1) {
            var digit = 0
            val mask = 1 shl (i - 1)
            if ((MapTileIndex.getX(pMapTileIndex) and mask) != 0) digit += 1
            if ((MapTileIndex.getY(pMapTileIndex) and mask) != 0) digit += 2
            quadKey.append("" + digit)
        }

        return quadKey.toString()
    }
}
