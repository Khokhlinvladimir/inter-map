package org.osmdroid.samplefragments.tilesources

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.IMapTileProviderCallback
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.CantContinueException
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.modules.MapTileAssetsProvider
import org.osmdroid.tileprovider.modules.MapTileDownloader
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Lie Fi demo: we emulate a slow online source in order to show the offline first behavior
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
class SampleLieFi : BaseSampleFragment() {
    private val mInitialCenter = GeoPoint(41.8905495, 12.4924348) // Rome, Italy
    private val mInitialZoomLevel = 5.0
    private val mLieFieLagInMillis = 1000

    override val sampleTitle: String
        get() = "Lie Fi - slow online source"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val provider: MapTileProviderArray = MapTileProviderLieFi(inflater.getContext())
        mMapView = MapView(inflater.getContext(), provider)
        return mMapView
    }

    override fun addOverlays() {
        super.addOverlays()

        mMapView!!.post(object : Runnable {
            // "post" because we need View.getWidth() to be set
            override fun run() {
                mMapView!!.controller!!.setZoom(mInitialZoomLevel)
                mMapView!!.setExpectedCenter(mInitialCenter)
            }
        })
    }

    private inner class MapTileProviderLieFi(
        pRegisterReceiver: IRegisterReceiver?,
        private val mNetworkAvailabilityCheck: INetworkAvailablityCheck?, pTileSource: ITileSource?,
        pContext: Context, cacheWriter: IFilesystemCache?
    ) : MapTileProviderArray(pTileSource, pRegisterReceiver), IMapTileProviderCallback {
        private var tileWriter: IFilesystemCache? = null

        constructor(pContext: Context) : this(
            SimpleRegisterReceiver(pContext), NetworkAvailabliltyCheck(pContext),
            TileSourceFactory.DEFAULT_TILE_SOURCE, pContext, null
        )

        init {
            if (cacheWriter != null) {
                tileWriter = cacheWriter
            } else {
                tileWriter = SqlTileWriter()
            }
            val assetsProvider = MapTileAssetsProvider(
                pRegisterReceiver, pContext.getAssets(), pTileSource
            )
            mTileProviderList.add(assetsProvider)

            val cacheProvider =
                MapTileProviderBasic.getMapTileFileStorageProviderBase(pRegisterReceiver, pTileSource, tileWriter)
            mTileProviderList.add(cacheProvider)

            val archiveProvider = MapTileFileArchiveProvider(
                pRegisterReceiver, pTileSource
            )
            mTileProviderList.add(archiveProvider)

            val approximationProvider = MapTileApproximater()
            mTileProviderList.add(approximationProvider)
            approximationProvider.addProvider(assetsProvider)
            approximationProvider.addProvider(cacheProvider)
            approximationProvider.addProvider(archiveProvider)

            val downloaderProvider: MapTileDownloader = MapTileDownloaderLieFi(
                pTileSource, tileWriter,
                mNetworkAvailabilityCheck
            )
            mTileProviderList.add(downloaderProvider)

            getTileCache().getProtectedTileContainers().add(this)
        }

        override fun getTileWriter(): IFilesystemCache? {
            return tileWriter
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
            return (mNetworkAvailabilityCheck != null && !mNetworkAvailabilityCheck.networkAvailable)
                    || !useDataConnection()
        }
    }

    private inner class MapTileDownloaderLieFi(
        pTileSource: ITileSource?,
        pFilesystemCache: IFilesystemCache?,
        pNetworkAvailablityCheck: INetworkAvailablityCheck?
    ) : MapTileDownloader(pTileSource, pFilesystemCache, pNetworkAvailablityCheck) {
        private val mTileLoader: MapTileDownloader.TileLoader = TileLoader()

        override fun getTileLoader(): MapTileDownloader.TileLoader {
            return mTileLoader
        }

        private inner class TileLoader : MapTileDownloader.TileLoader() {
            @Throws(CantContinueException::class)
            override fun downloadTile(pMapTileIndex: Long, redirectCount: Int, targetUrl: String?): Drawable? {
                try {
                    Thread.sleep(mLieFieLagInMillis.toLong())
                } catch (e: InterruptedException) {
                    //
                }
                return super.downloadTile(pMapTileIndex, redirectCount, targetUrl)
            }
        }
    }
}
