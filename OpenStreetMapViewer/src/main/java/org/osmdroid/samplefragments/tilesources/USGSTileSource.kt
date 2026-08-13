package org.osmdroid.samplefragments.tilesources

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * sample custom tile source
 * Created by alex on 6/20/16.
 */
class USGSTileSource
/**
 * Constructor
 *
 * @param aName                a human-friendly name for this tile source
 * @param aZoomMinLevel        the minimum zoom level this tile source can provide
 * @param aZoomMaxLevel        the maximum zoom level this tile source can provide
 * @param aTileSizePixels      the tile size in pixels this tile source provides
 * @param aImageFilenameEnding the file name extension used when constructing the filename
 * @param aBaseUrl             the base url(s) of the tile server used when constructing the url to download the tiles
 */
@JvmOverloads constructor(
    aName: String? = "USGS Topo",
    aZoomMinLevel: Int = 0,
    aZoomMaxLevel: Int = 18,
    aTileSizePixels: Int = 256,
    aImageFilenameEnding: String? = "",
    aBaseUrl: Array<String?>? = arrayOf<String?>("http://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/")
) : OnlineTileSourceBase(aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding, aBaseUrl, "USGS") {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return (baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
                + mImageFilenameEnding)
    }
}
