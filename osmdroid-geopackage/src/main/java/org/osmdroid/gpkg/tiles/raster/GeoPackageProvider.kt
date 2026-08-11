package org.osmdroid.gpkg.tiles.raster

import android.content.Context
import android.os.Build
import android.util.Log
import mil.nga.geopackage.BoundingBox
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.IMapTileProviderCallback
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.modules.TileWriter
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import java.io.File

/**
 * GeoPackage +
 * created on 1/5/2017.
 *
 * @author Alex O'Ree
 */
class GeoPackageProvider(
    pRegisterReceiver: IRegisterReceiver?,
    aNetworkAvailablityCheck: INetworkAvailablityCheck?, pTileSource: ITileSource?,
    pContext: Context?, cacheWriter: IFilesystemCache?, databases: Array<File?>
) : MapTileProviderArray(pTileSource, pRegisterReceiver), IMapTileProviderCallback {
    protected var geopackage: GeoPackageMapTileModuleProvider
    @JvmField
    protected var tileWriter: IFilesystemCache? = null

    constructor(db: Array<File?>, context: Context) : this(
        SimpleRegisterReceiver(context), NetworkAvailabliltyCheck(context),
        TileSourceFactory.DEFAULT_TILE_SOURCE, context, null, db
    )


    init {
        Log.i(IMapView.LOGTAG, "Geopackage support is BETA. Please report any issues")

        if (cacheWriter != null) {
            tileWriter = cacheWriter
        } else {
            if (Build.VERSION.SDK_INT < 10) {
                tileWriter = TileWriter()
            } else {
                tileWriter = SqlTileWriter()
            }
        }

        mTileProviderList.add(MapTileProviderBasic.getMapTileFileStorageProviderBase(pRegisterReceiver, pTileSource, tileWriter))
        geopackage = GeoPackageMapTileModuleProvider(databases, pContext, tileWriter)
        mTileProviderList.add(geopackage)
    }

    fun geoPackageMapTileModuleProvider(): GeoPackageMapTileModuleProvider {
        return geopackage
    }


    override fun getTileWriter(): IFilesystemCache? {
        return tileWriter
    }

    override fun detach() {
        //https://github.com/osmdroid/osmdroid/issues/213
        //close the writer
        if (tileWriter != null) tileWriter!!.onDetach()
        tileWriter = null
        geopackage.detach()
        super.detach()
    }

    fun getTileSource(database: String?, table: String?): GeopackageRasterTileSource? {
        for (next in geopackage.tileSources!!) {
            if (next.getName().equals(database, ignoreCase = true)) {
                //found the database
                if (next.getTileTables().contains(table)) {
                    //find the tile table
                    val tileDao = next.getTileDao(table)
                    var boundingBox = tileDao.getBoundingBox()
                    val transformation = tileDao.getProjection().getTransformation(tileDao.getProjection())
                    val transformed = transformation.transform(
                        boundingBox.getMinLongitude(),
                        boundingBox.getMinLatitude(),
                        boundingBox.getMaxLongitude(),
                        boundingBox.getMaxLatitude()
                    )
                    boundingBox = BoundingBox(transformed[0], transformed[1], transformed[2], transformed[3])
                    val bounds = org.osmdroid.util.BoundingBox(
                        boundingBox.getMaxLatitude(),
                        boundingBox.getMaxLongitude(),
                        boundingBox.getMinLatitude(),
                        boundingBox.getMinLongitude()
                    )
                    return GeopackageRasterTileSource(database, table, tileDao.getMinZoom().toInt(), tileDao.getMaxZoom().toInt(), bounds)
                }
            }
        }

        return null
    }


    override fun setTileSource(aTileSource: ITileSource?) {
        super.setTileSource(aTileSource)
        geopackage.setTileSource(aTileSource)
    }
}
