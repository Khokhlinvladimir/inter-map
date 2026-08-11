package org.osmdroid.tileprovider

import android.graphics.drawable.Drawable
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileContainer
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.TileSystem
import java.util.Collections

/**
 * This top-level tile provider allows a consumer to provide an array of modular asynchronous tile
 * providers to be used to obtain map tiles. When a tile is requested, the
 * [MapTileProviderArray] first checks the [MapTileCache] (synchronously) and returns
 * the tile if available. If not, then the [MapTileProviderArray] returns null and sends the
 * tile request through the asynchronous tile request chain. Each asynchronous tile provider returns
 * success/failure to the [MapTileProviderArray]. If successful, the
 * [MapTileProviderArray] passes the result to the base class. If failed, then the next
 * asynchronous tile provider is called in the chain. If there are no more asynchronous tile
 * providers in the chain, then the failure result is passed to the base class. The
 * [MapTileProviderArray] provides a mechanism so that only one unique tile-request can be in
 * the map tile request chain at a time.
 *
 * @author Marc Kurtz
 */
open class MapTileProviderArray(
    pTileSource: ITileSource?,
    aRegisterReceiver: IRegisterReceiver?,
    pTileProviderArray: Array<MapTileModuleProviderBase?>
) : MapTileProviderBase(pTileSource), MapTileContainer {
    private val mWorking: MutableMap<Long?, Int?> = HashMap<Long?, Int?>()
    private var mRegisterReceiver: IRegisterReceiver? = null
    protected val mTileProviderList: MutableList<MapTileModuleProviderBase>

    /**
     * Creates an [MapTileProviderArray] with no tile providers.
     *
     * @param pRegisterReceiver a [IRegisterReceiver]
     */
    protected constructor(
        pTileSource: ITileSource?,
        pRegisterReceiver: IRegisterReceiver?
    ) : this(pTileSource, pRegisterReceiver, arrayOfNulls<MapTileModuleProviderBase>(0))

    /**
     * Creates an [MapTileProviderArray] with the specified tile providers.
     *
     * @param aRegisterReceiver  a [IRegisterReceiver]
     * @param pTileProviderArray an array of [MapTileModuleProviderBase]
     */
    init {
        mRegisterReceiver = aRegisterReceiver
        mTileProviderList = ArrayList<MapTileModuleProviderBase>()
        mTileProviderList.addAll(pTileProviderArray.filterNotNull())
    }

    override fun detach() {
        synchronized(mTileProviderList) {
            for (tileProvider in mTileProviderList) {
                tileProvider.detach()
            }
        }
        synchronized(mWorking) {
            mWorking.clear()
        }
        if (mRegisterReceiver != null) {
            mRegisterReceiver!!.destroy()
            mRegisterReceiver = null
        }
        super.detach()
    }

    /**
     * @since 6.0.2
     */
    override fun contains(pTileIndex: Long): Boolean {
        synchronized(mWorking) {
            return mWorking.containsKey(pTileIndex)
        }
    }

    @get:Deprecated("Not used anymore. Use {@link #isDowngradedMode(long)} instead")
    protected val isDowngradedMode: Boolean
        /**
         * @since 6.0
         */
        get() = false

    /**
     * @since 6.0.3
     */
    protected open fun isDowngradedMode(pMapTileIndex: Long): Boolean {
        return false
    }

    override fun getMapTile(pMapTileIndex: Long): Drawable? {
        val tile = tileCache.getMapTile(pMapTileIndex)
        if (tile != null) {
            if (ExpirableBitmapDrawable.Companion.getState(tile) == ExpirableBitmapDrawable.Companion.UP_TO_DATE) {
                return tile // best scenario ever
            }
            if (isDowngradedMode(pMapTileIndex)) {
                return tile // best we can, considering
            }
        }

        synchronized(mWorking) {
            if (mWorking.containsKey(pMapTileIndex)) {
                return tile
            }
            mWorking.put(pMapTileIndex, WORKING_STATUS_STARTED)
        }

        val state = MapTileRequestState(pMapTileIndex, mTileProviderList, this@MapTileProviderArray)
        runAsyncNextProvider(state)

        return tile
    }

    /**
     * @since 6.0.0
     */
    private fun remove(pMapTileIndex: Long) {
        synchronized(mWorking) {
            mWorking.remove(pMapTileIndex)
        }
    }

    override fun mapTileRequestCompleted(aState: MapTileRequestState?, aDrawable: Drawable?) {
        if (aState == null) return
        super.mapTileRequestCompleted(aState, aDrawable)
        remove(aState.mapTile)
    }

    override fun mapTileRequestFailed(aState: MapTileRequestState?) {
        if (aState == null) return
        runAsyncNextProvider(aState)
    }

    override fun mapTileRequestFailedExceedsMaxQueueSize(aState: MapTileRequestState?) {
        if (aState == null) return
        super.mapTileRequestFailed(aState)
        remove(aState.mapTile)
    }

    override fun mapTileRequestExpiredTile(aState: MapTileRequestState?, aDrawable: Drawable?) {
        if (aState == null) return
        super.mapTileRequestExpiredTile(aState, aDrawable)
        synchronized(mWorking) {
            mWorking.put(aState.mapTile, WORKING_STATUS_FOUND)
        }

        // Continue through the provider chain
        runAsyncNextProvider(aState)
    }

    override fun getTileWriter(): IFilesystemCache? {
        return null
    }

    override fun getQueueSize(): Long {
        synchronized(mWorking) {
            return mWorking.size.toLong()
        }
    }

    /**
     * We want to not use a provider that doesn't exist anymore in the chain, and we want to not use
     * a provider that requires a data connection when one is not available.
     */
    protected fun findNextAppropriateProvider(aState: MapTileRequestState): MapTileModuleProviderBase? {
        var provider: MapTileModuleProviderBase?
        var providerDoesntExist = false
        var providerCantGetDataConnection = false
        var providerCantServiceZoomlevel = false
        // The logic of the while statement is
        // "Keep looping until you get null, or a provider that still exists
        // and has a data connection if it needs one and can service the zoom level,"
        do {
            provider = aState.nextProvider
            // Perform some checks to see if we can use this provider
            // If any of these are true, then that disqualifies the provider for this tile request.
            if (provider != null) {
                providerDoesntExist = !this.getProviderExists(provider)
                providerCantGetDataConnection = !useDataConnection()
                        && provider.getUsesDataConnection()
                val zoomLevel = MapTileIndex.getZoom(aState.mapTile)
                providerCantServiceZoomlevel = zoomLevel > provider.getMaximumZoomLevel()
                        || zoomLevel < provider.getMinimumZoomLevel()
            }
        } while ((provider != null)
            && (providerDoesntExist || providerCantGetDataConnection || providerCantServiceZoomlevel)
        )
        return provider
    }

    /**
     * @since 6.0.2
     */
    private fun runAsyncNextProvider(pState: MapTileRequestState) {
        val nextProvider = findNextAppropriateProvider(pState)
        if (nextProvider != null) {
            nextProvider.loadMapTileAsync(pState)
            return
        }
        val status: Int? // as Integer (and not int) for concurrency reasons
        synchronized(mWorking) {
            status = mWorking.get(pState.mapTile)
        }
        if (status != null && status == WORKING_STATUS_STARTED) {
            super.mapTileRequestFailed(pState)
        }
        remove(pState.mapTile)
    }

    fun getProviderExists(provider: MapTileModuleProviderBase?): Boolean {
        return mTileProviderList.contains(provider)
    }

    override fun getMinimumZoomLevel(): Int {
        var result: Int = TileSystem.maximumZoomLevel
        synchronized(mTileProviderList) {
            for (tileProvider in mTileProviderList) {
                if (tileProvider.getMinimumZoomLevel() < result) {
                    result = tileProvider.getMinimumZoomLevel()
                }
            }
        }
        return result
    }

    override fun getMaximumZoomLevel(): Int {
        var result = OpenStreetMapTileProviderConstants.MINIMUM_ZOOM_LEVEL
        synchronized(mTileProviderList) {
            for (tileProvider in mTileProviderList) {
                if (tileProvider.getMaximumZoomLevel() > result) {
                    result = tileProvider.getMaximumZoomLevel()
                }
            }
        }
        return result
    }

    override fun setTileSource(aTileSource: ITileSource?) {
        super.setTileSource(aTileSource)

        synchronized(mTileProviderList) {
            for (tileProvider in mTileProviderList) {
                tileProvider.setTileSource(aTileSource)
                clearTileCache()
            }
        }
    }

    companion object {
        /**
         * @since 6.0.2
         */
        private const val WORKING_STATUS_STARTED = 0
        private const val WORKING_STATUS_FOUND = 1
    }
}
