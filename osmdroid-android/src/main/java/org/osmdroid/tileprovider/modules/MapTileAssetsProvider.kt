package org.osmdroid.tileprovider.modules

import android.content.res.AssetManager
import android.graphics.drawable.Drawable
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.TileSystem
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

/**
 * Implements a file system cache and provides cached tiles from Assets. This
 * functions as a tile provider by serving cached tiles for the supplied tile
 * source.
 *
 *
 * tiles should be put into apk's assets directory just like following example:
 *
 *
 * assets/Mapnik/11/1316/806.png
 *
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Behrooz Shabani (everplays)
 */
class MapTileAssetsProvider @JvmOverloads constructor(
    pRegisterReceiver: IRegisterReceiver,
    pAssets: AssetManager?,
    pTileSource: ITileSource? = TileSourceFactory.DEFAULT_TILE_SOURCE, pThreadPoolSize: Int = instance!!.tileDownloadThreads.toInt(),
    pPendingQueueSize: Int = instance!!.tileDownloadMaxQueueSize.toInt()
) : MapTileFileStorageProviderBase(pRegisterReceiver, pThreadPoolSize, pPendingQueueSize) {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    private val mAssets: AssetManager?

    private val mTileSource = AtomicReference<ITileSource?>()

    // ===========================================================
    // Constructors
    // ===========================================================
    init {
        setTileSource(pTileSource)

        mAssets = pAssets
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
        return "Assets Cache Provider"
    }

    override fun getThreadGroupName(): String {
        return "assets"
    }

    override fun getTileLoader(): MapTileModuleProviderBase.TileLoader {
        return TileLoader(mAssets)
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
    protected inner class TileLoader(pAssets: AssetManager?) : MapTileModuleProviderBase.TileLoader() {
        private var mAssets: AssetManager? = null

        init {
            mAssets = pAssets
        }

        @Throws(CantContinueException::class)
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            val tileSource = mTileSource.get()
            if (tileSource == null) {
                return null
            }

            try {
                val `is` = mAssets!!.open(tileSource.getTileRelativeFilenameString(pMapTileIndex)!!)
                val drawable = tileSource.getDrawable(`is`)
                if (drawable != null) {
                    //https://github.com/osmdroid/osmdroid/issues/272 why was this set to expired?
                    //ExpirableBitmapDrawable.setDrawableExpired(drawable);
                }
                return drawable
            } catch (e: IOException) {
            } catch (e: LowMemoryException) {
                throw CantContinueException(e)
            }

            // If we get here then there is no file in the file cache
            return null
        }
    }
}
