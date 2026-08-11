package org.osmdroid.mapsforge

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Tile
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.graphics.AndroidTileBitmap
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.datastore.MultiMapDataStore.DataPolicy
import org.mapsforge.map.layer.hills.HillsRenderConfig
import org.mapsforge.map.layer.renderer.DirectRenderer
import org.mapsforge.map.layer.renderer.DirectRenderer.TileRefresher
import org.mapsforge.map.layer.renderer.RendererJob
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView.Companion.getTileSystem
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import kotlin.math.max
import kotlin.math.min

/**
 * Adapted from code from here: https://github.com/MKergall/osmbonuspack, which is LGPL
 * http://www.salidasoftware.com/how-to-render-mapsforge-tiles-in-osmdroid/
 *
 * @author Salida Software
 * Adapted from code found here : http://www.sieswerda.net/2012/08/15/upping-the-developer-friendliness/
 */
open class MapsForgeTileSource protected constructor(
    cacheTileSourceName: String?,
    minZoom: Int,
    maxZoom: Int,
    tileSizePixels: Int,
    fileInputStream: Array<FileInputStream?>,
    xmlRenderTheme: XmlRenderTheme?,
    dataPolicy: DataPolicy?,
    hillsRenderConfig: HillsRenderConfig?,
    language: String? = null
) : BitmapTileSourceBase(cacheTileSourceName, minZoom, maxZoom, tileSizePixels, ".png", "© OpenStreetMap contributors") {
    private val model = DisplayModel()
    private val scale = DisplayModel.getDefaultUserScaleFactor()
    private var theme: RenderThemeFuture? = null
    private val mXmlRenderTheme: XmlRenderTheme? = null
    private var renderer: DirectRenderer?
    private var hillsRenderConfig: HillsRenderConfig? = null

    private var mapDatabase: MultiMapDataStore?

    /**
     * The reason this constructor is protected is because all parameters,
     * except file should be determined from the archive file. Therefore a
     * factory method is necessary.
     *
     * @param cacheTileSourceName
     * @param minZoom
     * @param maxZoom
     * @param tileSizePixels
     * @param fileInputStream
     * @param xmlRenderTheme      the theme to render tiles with
     * @param hillsRenderConfig   the hillshading setup to be used (can be null)
     * @param language            preferred language for map labels as defined in ISO 639-1 or ISO 639-2 (can be null)
     */
    /**
     * The reason this constructor is protected is because all parameters,
     * except file should be determined from the archive file. Therefore a
     * factory method is necessary.
     *
     * @param cacheTileSourceName
     * @param minZoom
     * @param maxZoom
     * @param tileSizePixels
     * @param fileInputStream
     * @param xmlRenderTheme      the theme to render tiles with
     * @param hillsRenderConfig   the hillshading setup to be used (can be null)
     */
    init {
        var minZoom = minZoom
        var maxZoom = maxZoom
        var xmlRenderTheme = xmlRenderTheme
        mapDatabase = MultiMapDataStore(dataPolicy)
        for (i in fileInputStream.indices) mapDatabase!!.addMapDataStore(MapFile(fileInputStream[i], language), false, false)

        if (AndroidGraphicFactory.INSTANCE == null) {
            throw RuntimeException("Must call MapsForgeTileSource.createInstance(context.getApplication()); once before MapsForgeTileSource.createFromFiles().")
        }

        // mapsforge0.6
        // renderer = new DatabaseRenderer(mapDatabase, AndroidGraphicFactory.INSTANCE, new InMemoryTileCache(2));
        // mapsforge0.6.1
        //InMemoryTileCache tileCache = new InMemoryTileCache(2);
        //renderer = new DatabaseRenderer(mapDatabase, AndroidGraphicFactory.INSTANCE, tileCache,
        //        new TileBasedLabelStore(tileCache.getCapacityFirstLevel()), true, true);
        // mapsforge0.8.0
        //InMemoryTileCache tileCache = new InMemoryTileCache(2);
        //renderer = new DatabaseRenderer(mapDatabase, AndroidGraphicFactory.INSTANCE, tileCache,
        //        new TileBasedLabelStore(tileCache.getCapacityFirstLevel()), true, true, hillsRenderConfig);
        // mapsforge0.11.0
        renderer = DirectRenderer(mapDatabase, AndroidGraphicFactory.INSTANCE, true, hillsRenderConfig)

        minZoom = MIN_ZOOM
        maxZoom = renderer!!.getZoomLevelMax().toInt()

        Log.d(IMapView.LOGTAG, "min=" + minZoom + " max=" + maxZoom + " tilesize=" + tileSizePixels)

        if (xmlRenderTheme == null) xmlRenderTheme = InternalRenderTheme.OSMARENDER
        //we the passed in theme is different that the existing one, or the theme is currently null, create it
        if (xmlRenderTheme !== mXmlRenderTheme || theme == null) {
            theme = RenderThemeFuture(AndroidGraphicFactory.INSTANCE, xmlRenderTheme, model)
            //super important!! without the following line, all rendering activities will block until the theme is created.
            Thread(theme).start()
        }
    }

    open val bounds: BoundingBox
        get() = mapDatabase!!.boundingBox()

    open val boundsOsmdroid: org.osmdroid.util.BoundingBox
        get() {
            val boundingBox = mapDatabase!!.boundingBox()
            val latNorth = min(getTileSystem().getMaxLatitude(), boundingBox.maxLatitude)
            val latSouth = max(getTileSystem().getMinLatitude(), boundingBox.minLatitude)
            return org.osmdroid.util.BoundingBox(
                latNorth, boundingBox.maxLongitude,
                latSouth, boundingBox.minLongitude
            )
        }

    //The synchronized here is VERY important.  If missing, the mapDatabase read gets corrupted by multiple threads reading the file at once.
    @Synchronized
    open fun renderTile(pMapTileIndex: Long): Drawable? {
        val tile = Tile(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex).toByte(), 256)
        model.setFixedTileSize(256)


        //You could try something like this to load a custom theme
        //try{
        //	jobTheme = new ExternalRenderTheme(themeFile);
        //}
        //catch(Exception e){
        //	jobTheme = InternalRenderTheme.OSMARENDER;
        //}
        if (mapDatabase == null) return null
        try {
            //Draw the tile
            val mapGeneratorJob = RendererJob(tile, mapDatabase, theme, model, scale, false, false)
            val bmp = renderer!!.executeJob(mapGeneratorJob) as AndroidTileBitmap?
            if (bmp != null) return BitmapDrawable(AndroidGraphicFactory.getBitmap(bmp))
        } catch (ex: Exception) {
            Log.d(IMapView.LOGTAG, "###################### Mapsforge tile generation failed", ex)
        }
        return null
    }

    open fun dispose() {
        theme!!.decrementRefCount()
        theme = null
        renderer = null
        if (mapDatabase != null) mapDatabase!!.close()
        mapDatabase = null
    }

    /**
     * @since 6.0.3
     */
    open fun addTileRefresher(pDirectTileRefresher: TileRefresher?) {
        if (pDirectTileRefresher != null) {
            renderer!!.addTileRefresher(pDirectTileRefresher)
        }
    }

    // for example a scaleFactor of .6F
    open fun setUserScaleFactor(scaleFactor: Float) {
        model.setUserScaleFactor(scaleFactor)
    }


    companion object {
        // Reasonable defaults ..
        @JvmField
        var MIN_ZOOM: Int = 3
        @JvmField
        var MAX_ZOOM: Int = 29
        const val TILE_SIZE_PIXELS: Int = 256

        /**
         * Creates a new MapsForgeTileSource from file.
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param file
         * @return the tile source
         */
        @JvmStatic
        fun createFromFiles(file: Array<File?>): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS
            val fileInputStream: Array<FileInputStream?> = convertFilesToInputStreams(file)

            return MapsForgeTileSource(
                InternalRenderTheme.OSMARENDER.name,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                InternalRenderTheme.OSMARENDER,
                DataPolicy.RETURN_ALL,
                null,
                null
            )
        }

        /**
         * Creates a new MapsForgeTileSource from file[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param file
         * @param theme     this can be null, in which case the default them will be used
         * @param themeName when using a custom theme, this sets up the osmdroid caching correctly
         * @return
         */
        @JvmStatic
        fun createFromFiles(file: Array<File?>, theme: XmlRenderTheme?, themeName: String?): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS
            val fileInputStream: Array<FileInputStream?> = convertFilesToInputStreams(file)

            return MapsForgeTileSource(
                themeName,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                theme,
                DataPolicy.RETURN_ALL,
                null,
                null
            )
        }

        /**
         * Creates a new MapsForgeTileSource from file[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param file
         * @param theme     this can be null, in which case the default them will be used
         * @param themeName when using a custom theme, this sets up the osmdroid caching correctly
         * @param language  preferred language for map labels as defined in ISO 639-1 or ISO 639-2 (can be null)
         * @return
         */
        @JvmStatic
        fun createFromFiles(file: Array<File?>, theme: XmlRenderTheme?, themeName: String?, language: String?): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS
            val fileInputStream: Array<FileInputStream?> = convertFilesToInputStreams(file)

            return MapsForgeTileSource(
                themeName,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                theme,
                DataPolicy.RETURN_ALL,
                null,
                language
            )
        }

        /**
         * Creates a new MapsForgeTileSource from file[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param file
         * @param theme             this can be null, in which case the default them will be used
         * @param themeName         when using a custom theme, this sets up the osmdroid caching correctly
         * @param dataPolicy        use this to override the default, which is "RETURN_ALL"
         * @param hillsRenderConfig the hillshading setup to be used (can be null)
         * @return
         */
        @JvmStatic
        fun createFromFiles(
            file: Array<File?>,
            theme: XmlRenderTheme?,
            themeName: String?,
            dataPolicy: DataPolicy?,
            hillsRenderConfig: HillsRenderConfig?
        ): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS
            val fileInputStream: Array<FileInputStream?> = convertFilesToInputStreams(file)

            return MapsForgeTileSource(
                themeName,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                theme,
                dataPolicy,
                hillsRenderConfig,
                null
            )
        }

        /**
         * Creates a new MapsForgeTileSource from file[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param file
         * @param theme             this can be null, in which case the default them will be used
         * @param themeName         when using a custom theme, this sets up the osmdroid caching correctly
         * @param dataPolicy        use this to override the default, which is "RETURN_ALL"
         * @param hillsRenderConfig the hillshading setup to be used (can be null)
         * @param language          preferred language for map labels as defined in ISO 639-1 or ISO 639-2 (can be null)
         * @return
         */
        @JvmStatic
        fun createFromFiles(
            file: Array<File?>,
            theme: XmlRenderTheme?,
            themeName: String?,
            dataPolicy: DataPolicy?,
            hillsRenderConfig: HillsRenderConfig?,
            language: String?
        ): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS
            val fileInputStream: Array<FileInputStream?> = convertFilesToInputStreams(file)

            return MapsForgeTileSource(
                themeName,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                theme,
                dataPolicy,
                hillsRenderConfig,
                language
            )
        }

        /**
         * Creates a new MapsForgeTileSource from FileInputStream[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param fileInputStream
         * @return the tile source
         */
        @JvmStatic
        fun createFromFileInputStream(fileInputStream: Array<FileInputStream?>): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS

            return MapsForgeTileSource(
                InternalRenderTheme.OSMARENDER.name,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                InternalRenderTheme.OSMARENDER,
                DataPolicy.RETURN_ALL,
                null,
                null
            )
        }

        /**
         * Creates a new MapsForgeTileSource from FileInputStream[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param fileInputStream
         * @param theme     this can be null, in which case the default them will be used
         * @param themeName when using a custom theme, this sets up the osmdroid caching correctly
         * @return
         */
        @JvmStatic
        fun createFromFileInputStream(fileInputStream: Array<FileInputStream?>, theme: XmlRenderTheme?, themeName: String?): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS

            return MapsForgeTileSource(
                themeName,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                theme,
                DataPolicy.RETURN_ALL,
                null,
                null
            )
        }

        /**
         * Creates a new MapsForgeTileSource from FileInputStream[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param fileInputStream
         * @param theme     this can be null, in which case the default them will be used
         * @param themeName when using a custom theme, this sets up the osmdroid caching correctly
         * @param language  preferred language for map labels as defined in ISO 639-1 or ISO 639-2 (can be null)
         * @return
         */
        @JvmStatic
        fun createFromFileInputStream(
            fileInputStream: Array<FileInputStream?>,
            theme: XmlRenderTheme?,
            themeName: String?,
            language: String?
        ): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS

            return MapsForgeTileSource(
                themeName,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                theme,
                DataPolicy.RETURN_ALL,
                null,
                language
            )
        }

        /**
         * Creates a new MapsForgeTileSource from FileInputStream[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param fileInputStream
         * @param theme             this can be null, in which case the default them will be used
         * @param themeName         when using a custom theme, this sets up the osmdroid caching correctly
         * @param dataPolicy        use this to override the default, which is "RETURN_ALL"
         * @param hillsRenderConfig the hillshading setup to be used (can be null)
         * @return
         */
        @JvmStatic
        fun createFromFileInputStream(
            fileInputStream: Array<FileInputStream?>,
            theme: XmlRenderTheme?,
            themeName: String?,
            dataPolicy: DataPolicy?,
            hillsRenderConfig: HillsRenderConfig?
        ): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS

            return MapsForgeTileSource(
                themeName,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                theme,
                dataPolicy,
                hillsRenderConfig,
                null
            )
        }

        /**
         * Creates a new MapsForgeTileSource from FileInputStream[].
         *
         *
         * Parameters minZoom and maxZoom are obtained from the
         * database. If they cannot be obtained from the DB, the default values as
         * defined by this class are used, which is zoom = 3-20
         *
         * @param fileInputStream
         * @param theme             this can be null, in which case the default them will be used
         * @param themeName         when using a custom theme, this sets up the osmdroid caching correctly
         * @param dataPolicy        use this to override the default, which is "RETURN_ALL"
         * @param hillsRenderConfig the hillshading setup to be used (can be null)
         * @param language          preferred language for map labels as defined in ISO 639-1 or ISO 639-2 (can be null)
         * @return
         */
        @JvmStatic
        fun createFromFileInputStream(
            fileInputStream: Array<FileInputStream?>,
            theme: XmlRenderTheme?,
            themeName: String?,
            dataPolicy: DataPolicy?,
            hillsRenderConfig: HillsRenderConfig?,
            language: String?
        ): MapsForgeTileSource {
            //these settings are ignored and are set based on .map file info
            val minZoomLevel: Int = MIN_ZOOM
            val maxZoomLevel: Int = MAX_ZOOM
            val tileSizePixels: Int = TILE_SIZE_PIXELS

            return MapsForgeTileSource(
                themeName,
                minZoomLevel,
                maxZoomLevel,
                tileSizePixels,
                fileInputStream,
                theme,
                dataPolicy,
                hillsRenderConfig,
                language
            )
        }

        @JvmStatic
        fun createInstance(app: Application?) {
            AndroidGraphicFactory.createInstance(app)
        }


        private fun convertFilesToInputStreams(files: Array<File?>): Array<FileInputStream?> {
            val fileInputStreams = arrayOfNulls<FileInputStream>(files.size)
            for (i in files.indices) {
                try {
                    fileInputStreams[i] = FileInputStream(files[i])
                } catch (ex: FileNotFoundException) {
                    Log.d(IMapView.LOGTAG, "###################### Mapsforge file input stream conversion failed", ex)
                }
            }
            return fileInputStreams
        }
    }
}
