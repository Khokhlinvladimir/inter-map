package org.osmdroid.tileprovider

import android.content.Context
import android.os.Build
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.modules.MapTileAssetsProvider
import org.osmdroid.tileprovider.modules.MapTileDownloader
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.modules.MapTileFileStorageProviderBase
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider
import org.osmdroid.tileprovider.modules.MapTileSqlCacheProvider
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.modules.TileWriter
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.MapTileAreaBorderComputer
import org.osmdroid.util.MapTileAreaZoomComputer
import org.osmdroid.util.MapTileIndex

/**
 * This top-level tile provider implements a basic tile request chain which includes a
 * [MapTileFilesystemProvider] (a file-system cache), a [MapTileFileArchiveProvider]
 * (archive provider), and a [MapTileDownloader] (downloads map tiles via tile source).
 *
 *
 * Behavior change since osmdroid 5.3: If the device is less than API 10, the file system based cache and writer are used
 * otherwise, the sqlite based
 *
 * @author Marc Kurtz
 * @see TileWriter
 *
 * @see SqlTileWriter
 *
 * @see MapTileFilesystemProvider
 *
 * @see MapTileSqlCacheProvider
 */
class MapTileProviderBasic(
    pRegisterReceiver: IRegisterReceiver,
    aNetworkAvailablityCheck: INetworkAvailablityCheck?, pTileSource: ITileSource?,
    pContext: Context, cacheWriter: IFilesystemCache?
) : MapTileProviderArray(pTileSource, pRegisterReceiver), IMapTileProviderCallback {
    @JvmField
    protected var tileWriter: IFilesystemCache? = null
    private val mNetworkAvailabilityCheck: INetworkAvailablityCheck?

    /**
     * @since 6.1.0
     */
    private val mDownloaderProvider: MapTileDownloader
    private val mApproximationProvider: MapTileApproximater

    /**
     * Creates a [MapTileProviderBasic].
     */
    /**
     * Creates a [MapTileProviderBasic].
     */
    /**
     * Creates a [MapTileProviderBasic].
     */
    @JvmOverloads
    constructor(pContext: Context, pTileSource: ITileSource? = TileSourceFactory.DEFAULT_TILE_SOURCE, cacheWriter: IFilesystemCache? = null) : this(
        SimpleRegisterReceiver(pContext), NetworkAvailabliltyCheck(pContext),
        pTileSource, pContext, cacheWriter
    )

    /**
     * Creates a [MapTileProviderBasic].
     */
    init {
        mNetworkAvailabilityCheck = aNetworkAvailablityCheck

        if (cacheWriter != null) {
            tileWriter = cacheWriter
        } else {
            if (Build.VERSION.SDK_INT < 10) {
                tileWriter = TileWriter()
            } else {
                tileWriter = SqlTileWriter()
            }
        }
        val assetsProvider =
            createAssetsProvider(pRegisterReceiver, pTileSource, pContext)
        mTileProviderList.add(assetsProvider)

        val cacheProvider: MapTileFileStorageProviderBase =
            getMapTileFileStorageProviderBase(pRegisterReceiver, pTileSource, tileWriter)
        mTileProviderList.add(cacheProvider)

        val archiveProvider =
            createArchiveProvider(pRegisterReceiver, pTileSource)
        mTileProviderList.add(archiveProvider)

        mApproximationProvider =
            createApproximater(assetsProvider, cacheProvider, archiveProvider)
        mTileProviderList.add(mApproximationProvider)

        mDownloaderProvider =
            createDownloaderProvider(aNetworkAvailablityCheck, pTileSource)
        mTileProviderList.add(mDownloaderProvider)

        // protected-cache-tile computers
        tileCache.protectedTileComputers.add(MapTileAreaZoomComputer(-1))
        tileCache.protectedTileComputers.add(MapTileAreaBorderComputer(1))
        tileCache.setAutoEnsureCapacity(false)
        tileCache.setStressedMemory(false)

        // pre-cache providers
        tileCache.preCache.addProvider(assetsProvider)
        tileCache.preCache.addProvider(cacheProvider)
        tileCache.preCache.addProvider(archiveProvider)
        tileCache.preCache.addProvider(mDownloaderProvider)

        // tiles currently being processed
        tileCache.protectedTileContainers.add(this)

        setOfflineFirst(true)
    }

    protected fun createApproximater(
        assetsProvider: MapTileFileStorageProviderBase?,
        cacheProvider: MapTileFileStorageProviderBase?,
        archiveProvider: MapTileFileStorageProviderBase?
    ): MapTileApproximater {
        val approximationProvider = MapTileApproximater()
        approximationProvider.addProvider(assetsProvider)
        approximationProvider.addProvider(cacheProvider)
        approximationProvider.addProvider(archiveProvider)
        return approximationProvider
    }

    protected fun createArchiveProvider(pRegisterReceiver: IRegisterReceiver, pTileSource: ITileSource?): MapTileFileStorageProviderBase {
        return MapTileFileArchiveProvider(
            pRegisterReceiver, pTileSource
        )
    }

    protected fun createAssetsProvider(
        pRegisterReceiver: IRegisterReceiver,
        pTileSource: ITileSource?,
        pContext: Context
    ): MapTileFileStorageProviderBase {
        return MapTileAssetsProvider(
            pRegisterReceiver, pContext.getAssets(), pTileSource
        )
    }

    override fun getTileWriter(): IFilesystemCache? {
        return tileWriter
    }

    /**
     * @since 6.1.7
     * allow customization of tile downloader per tile source
     */
    protected fun createDownloaderProvider(aNetworkAvailablityCheck: INetworkAvailablityCheck?, pTileSource: ITileSource?): MapTileDownloader {
        return MapTileDownloader(pTileSource, this.tileWriter, aNetworkAvailablityCheck)
    }

    override fun detach() {
        //https://github.com/osmdroid/osmdroid/issues/213
        //close the writer
        if (tileWriter != null) tileWriter!!.onDetach()
        tileWriter = null
        super.detach()
    }

    /**
     * @since 6.0.3
     */
    override fun isDowngradedMode(pMapTileIndex: Long): Boolean {
        if ((mNetworkAvailabilityCheck != null && !mNetworkAvailabilityCheck.networkAvailable)
            || !useDataConnection()
        ) {
            return true
        }
        var zoomMin = -1
        var zoomMax = -1
        for (provider in mTileProviderList) {
            if (provider.getUsesDataConnection()) {
                var tmp: Int
                tmp = provider.getMinimumZoomLevel()
                if (zoomMin == -1 || zoomMin > tmp) {
                    zoomMin = tmp
                }
                tmp = provider.getMaximumZoomLevel()
                if (zoomMax == -1 || zoomMax < tmp) {
                    zoomMax = tmp
                }
            }
        }
        if (zoomMin == -1 || zoomMax == -1) {
            return true
        }
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        return zoom < zoomMin || zoom > zoomMax
    }

    /**
     * @since 6.1.0
     * @return true if possible and done
     */
    fun setOfflineFirst(pOfflineFirst: Boolean): Boolean {
        var downloaderIndex = -1
        var approximationIndex = -1
        var i = 0
        for (provider in mTileProviderList) {
            if (downloaderIndex == -1 && provider === mDownloaderProvider) {
                downloaderIndex = i
            }
            if (approximationIndex == -1 && provider === mApproximationProvider) {
                approximationIndex = i
            }
            i++
        }
        if (downloaderIndex == -1 || approximationIndex == -1) {
            return false
        }
        if (approximationIndex < downloaderIndex && pOfflineFirst) {
            return true
        }
        if (approximationIndex > downloaderIndex && !pOfflineFirst) {
            return true
        }
        mTileProviderList.set(downloaderIndex, mApproximationProvider)
        mTileProviderList.set(approximationIndex, mDownloaderProvider)
        return true
    }

    companion object {
        /**
         * @since 6.0.3
         * cf. https://github.com/osmdroid/osmdroid/issues/1172
         */
        fun getMapTileFileStorageProviderBase(
            pRegisterReceiver: IRegisterReceiver,
            pTileSource: ITileSource?,
            pTileWriter: IFilesystemCache?
        ): MapTileFileStorageProviderBase {
            if (pTileWriter is TileWriter) {
                return MapTileFilesystemProvider(pRegisterReceiver, pTileSource)
            }
            return MapTileSqlCacheProvider(pRegisterReceiver, pTileSource)
        }
    }
}
