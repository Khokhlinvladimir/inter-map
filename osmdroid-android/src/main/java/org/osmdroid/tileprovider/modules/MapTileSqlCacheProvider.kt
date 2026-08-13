package org.osmdroid.tileprovider.modules

import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.TileSystem
import java.util.concurrent.atomic.AtomicReference

/**
 * Sqlite based tile cache mechansism
 *
 * @see SqlTileWriter
 * Created by alex on 1/16/16.
 *
 * @since 5.1
 */
class MapTileSqlCacheProvider(
    pRegisterReceiver: IRegisterReceiver,
    pTileSource: ITileSource?
) : MapTileFileStorageProviderBase(
    pRegisterReceiver,
    instance!!.tileFileSystemThreads.toInt(),
    instance!!.tileFileSystemMaxQueueSize.toInt()
) {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    private val mTileSource = AtomicReference<ITileSource?>()
    private var mWriter: SqlTileWriter?

    // ===========================================================
    // Constructors
    // ===========================================================
    @Deprecated("")
    constructor(
        pRegisterReceiver: IRegisterReceiver,
        pTileSource: ITileSource?, pMaximumCachedFileAge: Long
    ) : this(pRegisterReceiver, pTileSource)

    /**
     * The tiles may be found on several media. This one works with tiles stored on database.
     * It and its friends are typically created and controlled by [MapTileProviderBase].
     */
    init {
        setTileSource(pTileSource)
        mWriter = SqlTileWriter()
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
        return "SQL Cache Archive Provider"
    }

    override fun getThreadGroupName(): String {
        return "sqlcache"
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

    override fun onMediaMounted() {
    }

    override fun onMediaUnmounted() {
        if (mWriter != null) mWriter!!.onDetach()
        mWriter = SqlTileWriter()
    }

    override fun setTileSource(pTileSource: ITileSource?) {
        mTileSource.set(pTileSource)
    }

    override fun detach() {
        if (mWriter != null) mWriter!!.onDetach()
        mWriter = null
        super.detach()
    }

    // ===========================================================
    // Methods
    // ===========================================================
    /**
     * returns true if the given tile for the current map source exists in the cache db
     */
    fun hasTile(pMapTileIndex: Long): Boolean {
        val tileSource = mTileSource.get()
        if (tileSource == null) {
            return false
        }
        return mWriter!!.getExpirationTimestamp(tileSource, pMapTileIndex) != null
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

            if (mWriter != null) {
                try {
                    val result = mWriter!!.loadTile(tileSource, pMapTileIndex)
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
            } else {
                Log.d(IMapView.LOGTAG, "TileLoader failed to load tile due to mWriter being null (map shutdown?)")
            }
            return null
        }
    }

    companion object {
        private val columns = arrayOf<String?>(DatabaseFileArchive.Companion.COLUMN_TILE, SqlTileWriter.Companion.COLUMN_EXPIRES)
    }
}
