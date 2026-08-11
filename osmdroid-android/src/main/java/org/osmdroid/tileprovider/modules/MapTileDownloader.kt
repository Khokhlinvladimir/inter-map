package org.osmdroid.tileprovider.modules

import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.BitmapPool
import org.osmdroid.tileprovider.MapTileRequestState
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.TileSystem
import org.osmdroid.util.UrlBackoff
import java.util.concurrent.atomic.AtomicReference

/**
 * The [MapTileDownloader] loads tiles from an HTTP server. It saves downloaded tiles to an
 * IFilesystemCache if available.
 *
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Manuel Stahl
 */
open class MapTileDownloader @JvmOverloads constructor(
    pTileSource: ITileSource?,
    pFilesystemCache: IFilesystemCache? = null,
    pNetworkAvailablityCheck: INetworkAvailablityCheck? = null, pThreadPoolSize: Int = instance!!.tileDownloadThreads.toInt(),
    pPendingQueueSize: Int = instance!!.tileDownloadMaxQueueSize.toInt()
) : MapTileModuleProviderBase(pThreadPoolSize, pPendingQueueSize) {
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    private val mFilesystemCache: IFilesystemCache?

    private val mTileSource = AtomicReference<OnlineTileSourceBase?>()

    private val mNetworkAvailablityCheck: INetworkAvailablityCheck?

    /**
     * @since 6.0.2
     */
    private val mTileLoader: TileLoader = TileLoader()

    private val mUrlBackoff = UrlBackoff()

    private var mTileDownloader = TileDownloader() // default value

    // ===========================================================
    // Constructors
    // ===========================================================
    init {
        mFilesystemCache = pFilesystemCache
        mNetworkAvailablityCheck = pNetworkAvailablityCheck
        setTileSource(pTileSource)
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    fun getTileSource(): ITileSource? {
        return mTileSource.get()
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun getUsesDataConnection(): Boolean {
        return true
    }

    override fun getName(): String {
        return "Online Tile Download Provider"
    }

    override fun getThreadGroupName(): String {
        return "downloader"
    }

    override fun getTileLoader(): MapTileModuleProviderBase.TileLoader {
        return mTileLoader
    }

    override fun detach() {
        super.detach()
        if (this.mFilesystemCache != null) this.mFilesystemCache.onDetach()
    }

    override fun getMinimumZoomLevel(): Int {
        val tileSource = mTileSource.get()
        return (if (tileSource != null) tileSource.minimumZoomLevel else OpenStreetMapTileProviderConstants.MINIMUM_ZOOM_LEVEL)
    }

    override fun getMaximumZoomLevel(): Int {
        val tileSource = mTileSource.get()
        return (if (tileSource != null)
            tileSource.maximumZoomLevel
        else
            TileSystem.maximumZoomLevel)
    }

    override fun setTileSource(tileSource: ITileSource?) {
        // We are only interested in OnlineTileSourceBase tile sources
        if (tileSource is OnlineTileSourceBase) {
            mTileSource.set(tileSource)
        } else {
            // Otherwise shut down the tile downloader
            mTileSource.set(null)
        }
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    protected open inner class TileLoader : MapTileModuleProviderBase.TileLoader() {
        /**
         * downloads a tile and follows http redirects
         */
        @Throws(CantContinueException::class)
        protected open fun downloadTile(pMapTileIndex: Long, redirectCount: Int, targetUrl: String?): Drawable? {
            val tileSource = mTileSource.get()
            if (tileSource == null) {
                return null
            }
            try {
                tileSource.acquire()
            } catch (e: InterruptedException) {
                return null
            }
            try {
                return mTileDownloader.downloadTile(pMapTileIndex, redirectCount, targetUrl, mFilesystemCache, tileSource)
            } finally {
                tileSource.release()
            }
        }

        @Throws(CantContinueException::class)
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            val tileSource = mTileSource.get()
            if (tileSource == null) {
                return null
            }


            if (mNetworkAvailablityCheck != null
                && !mNetworkAvailablityCheck.networkAvailable
            ) {
                if (instance!!.isDebugMode) {
                    Log.d(IMapView.LOGTAG, "Skipping " + getName() + " due to NetworkAvailabliltyCheck.")
                }
                return null
            }

            val tileURLString = tileSource.getTileURLString(pMapTileIndex)

            if (TextUtils.isEmpty(tileURLString)) {
                return null //unlikely but just in case
            }

            if (mUrlBackoff.shouldWait(tileURLString)) {
                return null
            }
            val result = downloadTile(pMapTileIndex, 0, tileURLString)
            if (result == null) {
                mUrlBackoff.next(tileURLString)
            } else {
                mUrlBackoff.remove(tileURLString)
            }
            return result
        }

        override fun tileLoaded(pState: MapTileRequestState, pDrawable: Drawable) {
            removeTileFromQueues(pState.mapTile)
            // don't return the tile because we'll wait for the fs provider to ask for it
            // this prevent flickering when a load of delayed downloads complete for tiles
            // that we might not even be interested in any more
            pState.callback?.mapTileRequestCompleted(pState, null)
            // We want to return the Bitmap to the BitmapPool if applicable
            BitmapPool.instance.asyncRecycle(pDrawable)
        }
    }

    /**
     * @since 6.0.2
     */
    fun setTileDownloader(pTileDownloader: TileDownloader) {
        mTileDownloader = pTileDownloader
    }
}
