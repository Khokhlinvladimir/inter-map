package org.osmdroid.tileprovider.modules

import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.TileSystem
import java.util.concurrent.atomic.AtomicReference

/**
 * Implements a file system cache and provides cached tiles. This functions as a tile provider by
 * serving cached tiles for the supplied tile source.
 *
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 */
class MapTileFilesystemProvider @JvmOverloads constructor(
    pRegisterReceiver: IRegisterReceiver,
    pTileSource: ITileSource?, pMaximumCachedFileAge: Long, pThreadPoolSize: Int = instance!!.tileFileSystemThreads.toInt(),
    pPendingQueueSize: Int = instance!!.tileFileSystemMaxQueueSize.toInt()
) : MapTileFileStorageProviderBase(pRegisterReceiver, pThreadPoolSize, pPendingQueueSize) {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    private val mWriter = TileWriter()
    private val mTileSource = AtomicReference<ITileSource?>()

    // ===========================================================
    // Constructors
    // ===========================================================
    @JvmOverloads
    constructor(
        pRegisterReceiver: IRegisterReceiver,
        aTileSource: ITileSource? = TileSourceFactory.DEFAULT_TILE_SOURCE
    ) : this(
        pRegisterReceiver,
        aTileSource,
        instance!!.expirationExtendedDuration + OpenStreetMapTileProviderConstants.DEFAULT_MAXIMUM_CACHED_FILE_AGE
    )

    /**
     * Provides a file system based cache tile provider. Other providers can register and store data
     * in the cache.
     *
     * @param pRegisterReceiver
     */
    init {
        setTileSource(pTileSource)

        mWriter.setMaximumCachedFileAge(pMaximumCachedFileAge)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun getUsesDataConnection(): Boolean {
        return false
    }

    override fun getName(): String {
        return "File System Cache Provider"
    }

    override fun getThreadGroupName(): String {
        return "filesystem"
    }

    override fun getTileLoader(): MapTileModuleProviderBase.TileLoader {
        return TileLoader()
    }

    override fun getMinimumZoomLevel(): Int {
        val tileSource = mTileSource.get()
        return if (tileSource != null) tileSource.minimumZoomLevel else OpenStreetMapTileProviderConstants.MINIMUM_ZOOM_LEVEL
    }

    override fun getMaximumZoomLevel(): Int {
        val tileSource = mTileSource.get()
        return if (tileSource != null)
            tileSource.maximumZoomLevel
        else
            TileSystem.maximumZoomLevel
    }

    override fun setTileSource(pTileSource: ITileSource?) {
        mTileSource.set(pTileSource)
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    protected inner class TileLoader : MapTileModuleProviderBase.TileLoader() {
        @Throws(CantContinueException::class)
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            val tileSource = mTileSource.get()
            if (tileSource == null) {
                return null
            }

            try {
                val result = mWriter.loadTile(tileSource, pMapTileIndex)
                if (result == null) {
                    Counters.fileCacheMiss++
                } else {
                    Counters.fileCacheHit++
                }
                return result
            } catch (e: LowMemoryException) {
                // low memory so empty the queue
                Log.w(IMapView.LOGTAG, "LowMemoryException downloading MapTile: " + MapTileIndex.toString(pMapTileIndex) + " : " + e)
                Counters.fileCacheOOM++
                throw CantContinueException(e)
            } catch (e: Throwable) {
                Log.e(IMapView.LOGTAG, "Error loading tile", e)
                return null
            }
        }
    }
}
