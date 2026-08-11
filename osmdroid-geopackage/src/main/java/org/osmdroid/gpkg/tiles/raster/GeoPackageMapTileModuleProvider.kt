package org.osmdroid.gpkg.tiles.raster

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import mil.nga.geopackage.BoundingBox
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.GeoPackageManager
import mil.nga.geopackage.tiles.retriever.GeoPackageTileRetriever
import mil.nga.proj.ProjectionConstants
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView.Companion.getTileSystem
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Geopackage raster tile provider
 * Created by alex on 10/29/15.
 */
class GeoPackageMapTileModuleProvider(
    pFile: Array<File?>,
    context: Context?, cache: IFilesystemCache?
) : MapTileModuleProviderBase(instance!!.tileFileSystemThreads.toInt(), instance!!.tileFileSystemMaxQueueSize.toInt()) {
    private val tileSystem = getTileSystem()

    //TileRetriever retriever;
    protected var tileWriter: IFilesystemCache? = null
    protected var manager: GeoPackageManager?

    protected var currentTileSource: GeopackageRasterTileSource? = null
    @JvmField
    var tileSources: MutableSet<GeoPackage>? = HashSet<GeoPackage>()

    init {
        //int pThreadPoolSize, final int pPendingQueueSize
        Log.i(IMapView.LOGTAG, "Geopackage support is BETA. Please report any issues")
        tileWriter = cache
        // Get a manager
        manager = GeoPackageFactory.getManager(context)


        // Available databases


        // Import database
        for (file in pFile) {
            try {
                manager!!.importGeoPackage(file)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // Available databases
        val databases = manager!!.databases()
        // Open database
        for (i in databases.indices) {
            tileSources!!.add(manager!!.open(databases.get(i)))
        }
    }


    fun getMapTile(pMapTileIndex: Long): Drawable? {
        var tile: Drawable? = null

        val database = currentTileSource!!.database
        val table = currentTileSource!!.tableDao
        val next = manager!!.open(database)

        val tileDao = next.getTileDao(table)
        val retriever = GeoPackageTileRetriever(tileDao)

        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)


        val geoPackageTile = retriever.getTile(x, y, zoom)
        if (geoPackageTile != null && geoPackageTile.data != null) {
            val image = geoPackageTile.data
            if (image != null) {
                val opt = BitmapFactory.Options()
                opt.outHeight = 256 //360
                opt.outWidth = 256 //248
                val imageBitmap = BitmapFactory.decodeByteArray(image, 0, image.size, opt)
                tile = BitmapDrawable(imageBitmap)
            }
        }
        next.close()

        return tile
    }


    /**
     * returns ALL available raster tile sources for all "imported" geopackage databases
     *
     * @return
     */
    fun getTileSources(): MutableList<GeopackageRasterTileSource?> {
        val srcs: MutableList<GeopackageRasterTileSource?> = ArrayList<GeopackageRasterTileSource?>()

        val databases = manager!!.databases()
        for (i in databases.indices) {
            val open = manager!!.open(databases.get(i))
            val tileTables = open.getTileTables()
            for (k in tileTables.indices) {
                val tileDao = open.getTileDao(tileTables.get(k))

                val transform = tileDao.getProjection().getTransformation(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong())
                val transformed = transform.transform(
                    tileDao.getBoundingBox().getMinLongitude(),
                    tileDao.getBoundingBox().getMinLatitude(),
                    tileDao.getBoundingBox().getMaxLongitude(),
                    tileDao.getBoundingBox().getMaxLatitude()
                )
                val boundingBox = BoundingBox(transformed[0], transformed[1], transformed[2], transformed[3])
                val bounds = org.osmdroid.util.BoundingBox(
                    min(tileSystem.maxLatitude, boundingBox.getMaxLatitude()),
                    boundingBox.getMaxLongitude(),
                    max(tileSystem.minLatitude, boundingBox.getMinLatitude()),
                    boundingBox.getMinLongitude()
                )

                srcs.add(
                    GeopackageRasterTileSource(
                        databases.get(i),
                        tileTables.get(k),
                        tileDao.getMinZoom().toInt(),
                        tileDao.getMaxZoom().toInt(),
                        bounds
                    )
                )
            }
            open.close()
        }

        return srcs
    }

    /**
     * returns ALL available raster tile sources for the specified database.
     * This will throw if the database doesn't exist or isn't registered
     *
     * @return
     */
    fun getTileSources(database: String?): MutableList<GeopackageRasterTileSource?> {
        val srcs: MutableList<GeopackageRasterTileSource?> = ArrayList<GeopackageRasterTileSource?>()

        val open = manager!!.open(database)
        val tileTables = open.getTileTables()
        for (k in tileTables.indices) {
            val tileDao = open.getTileDao(tileTables.get(k))

            val transform = tileDao.getProjection().getTransformation(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong())
            val transformed = transform.transform(
                tileDao.getBoundingBox().getMinLongitude(),
                tileDao.getBoundingBox().getMinLatitude(),
                tileDao.getBoundingBox().getMaxLongitude(),
                tileDao.getBoundingBox().getMaxLatitude()
            )
            val boundingBox = BoundingBox(transformed[0], transformed[1], transformed[2], transformed[3])
            val bounds = org.osmdroid.util.BoundingBox(
                min(tileSystem.maxLatitude, boundingBox.getMaxLatitude()),
                boundingBox.getMaxLongitude(),
                max(tileSystem.minLatitude, boundingBox.getMinLatitude()),
                boundingBox.getMinLongitude()
            )
            srcs.add(GeopackageRasterTileSource(database, tileTables.get(k), tileDao.getMinZoom().toInt(), tileDao.getMaxZoom().toInt(), bounds))
        }
        open.close()

        return srcs
    }

    override fun detach() {
        tileSources?.let { sources ->
            for (tileSource in sources) {
                tileSource.close()
            }
            sources.clear()
        }
        manager = null
    }


    inner class TileLoader : MapTileModuleProviderBase.TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            try {
                return getMapTile(pMapTileIndex)
            } catch (e: Throwable) {
                Log.e(IMapView.LOGTAG, "Error loading tile", e)
            }

            return null
        }
    }

    override fun getName(): String {
        return "Geopackage"
    }

    override fun getThreadGroupName(): String {
        return getName()
    }

    override fun getTileLoader(): TileLoader {
        return TileLoader()
    }

    override fun getUsesDataConnection(): Boolean {
        return false
    }

    override fun getMinimumZoomLevel(): Int {
        if (currentTileSource != null) return currentTileSource!!.minimumZoomLevel
        return 0
    }

    override fun getMaximumZoomLevel(): Int {
        if (currentTileSource != null) return currentTileSource!!.maximumZoomLevel
        return 22
    }

    override fun setTileSource(tileSource: ITileSource?) {
        if (tileSource is GeopackageRasterTileSource) currentTileSource = tileSource
    }
}
