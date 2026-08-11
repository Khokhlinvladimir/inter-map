package org.osmdroid.gpkg.tiles.feature

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import mil.nga.geopackage.tiles.features.FeatureTiles
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.modules.TileWriter
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex

/**
 * created on 8/19/2017.
 *
 * @author Alex O'Ree
 */
class GeoPackageFeatureTileProvider(pTileSource: ITileSource?) : MapTileProviderBase(pTileSource) {
    @JvmField
    protected var tileWriter: IFilesystemCache? = null
    protected var minzoom: Int = 0
    protected var featureTiles: FeatureTiles? = null

    init {
        Log.i(IMapView.LOGTAG, "Geopackage support is BETA. Please report any issues")
        if (Build.VERSION.SDK_INT < 10) {
            tileWriter = TileWriter()
        } else {
            tileWriter = SqlTileWriter()
        }
    }


    override fun getMapTile(pMapTileIndex: Long): Drawable? {
        if (featureTiles != null) {
            val tile =
                featureTiles!!.drawTile(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex))
            if (tile != null) {
                val d: Drawable = BitmapDrawable(tile)
                return d
            }
        }
        return null
    }

    override fun getMinimumZoomLevel(): Int {
        return minzoom
    }

    override fun getMaximumZoomLevel(): Int {
        return 22
    }

    override fun getTileWriter(): IFilesystemCache? {
        return tileWriter
    }

    override fun getQueueSize(): Long {
        return 0
    }

    fun set(minZoom: Int, featureTiles: FeatureTiles?) {
        this.featureTiles = featureTiles
        minzoom = minZoom
    }


    override fun detach() {
        super.detach()
        featureTiles = null
    }
}
