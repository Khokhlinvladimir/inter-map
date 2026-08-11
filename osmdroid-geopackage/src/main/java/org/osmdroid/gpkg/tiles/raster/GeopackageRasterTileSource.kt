package org.osmdroid.gpkg.tiles.raster

import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox

/**
 * created on 9/3/2017.
 *
 * @author Alex O'Ree
 */
class GeopackageRasterTileSource(database: String?, table: String?, aZoomMinLevel: Int, aZoomMaxLevel: Int, bbox: BoundingBox?) :
    XYTileSource(database + ":" + table, aZoomMinLevel, aZoomMaxLevel, 256, "png", arrayOf<String>("")) {
    var database: String?
    var tableDao: String?
    @JvmField
    var bounds: BoundingBox?

    init {
        Log.i(IMapView.LOGTAG, "Geopackage support is BETA. Please report any issues")
        this.database = database
        this.tableDao = table
        this.bounds = bbox
    }
}
