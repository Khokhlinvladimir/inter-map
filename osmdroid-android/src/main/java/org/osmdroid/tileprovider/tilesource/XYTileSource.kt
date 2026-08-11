package org.osmdroid.tileprovider.tilesource

import org.osmdroid.util.MapTileIndex

/**
 * An implementation of [OnlineTileSourceBase]
 */
open class XYTileSource : OnlineTileSourceBase {
    constructor(
        aName: String?, aZoomMinLevel: Int,
        aZoomMaxLevel: Int, aTileSizePixels: Int, aImageFilenameEnding: String?,
        aBaseUrl: Array<out String?>?
    ) : super(
        aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels,
        aImageFilenameEnding, aBaseUrl
    )

    constructor(
        aName: String?, aZoomMinLevel: Int,
        aZoomMaxLevel: Int, aTileSizePixels: Int, aImageFilenameEnding: String?,
        aBaseUrl: Array<out String?>?, copyright: String?
    ) : super(
        aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels,
        aImageFilenameEnding, aBaseUrl, copyright
    )

    /**
     * @param aName this is used for caching purposes, make sure it is consistent and unique
     * @since 6.1.0
     */
    constructor(
        aName: String?, aZoomMinLevel: Int,
        aZoomMaxLevel: Int, aTileSizePixels: Int, aImageFilenameEnding: String?,
        aBaseUrl: Array<out String?>?, copyright: String?,
        pTileSourcePolicy: TileSourcePolicy?
    ) : super(
        aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels,
        aImageFilenameEnding, aBaseUrl, copyright, pTileSourcePolicy ?: TileSourcePolicy()
    )

    override fun toString(): String {
        return name()!!
    }

    override fun getTileURLString(pMapTileIndex: Long): String {
        return (baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex)
                + mImageFilenameEnding)
    }
}
