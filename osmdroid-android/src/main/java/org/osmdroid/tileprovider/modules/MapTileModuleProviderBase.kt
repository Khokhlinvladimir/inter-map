package org.osmdroid.tileprovider.modules

import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.ExpirableBitmapDrawable
import org.osmdroid.tileprovider.MapTileRequestState
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * An abstract base class for modular tile providers
 *
 * @author Marc Kurtz
 * @author Neil Boyd
 */
abstract class MapTileModuleProviderBase(pThreadPoolSize: Int, pPendingQueueSize: Int) {
    /**
     * Gets the human-friendly name assigned to this tile provider.
     *
     * @return the thread name
     */
    protected abstract fun getName(): String

    /**
     * Gets the name assigned to the thread for this provider.
     *
     * @return the thread name
     */
    protected abstract fun getThreadGroupName(): String

    /**
     * It is expected that the implementation will construct an internal member which internally
     * implements a [TileLoader]. This method is expected to return a that internal member to
     * methods of the parent methods.
     *
     * @return the internal member of this tile provider.
     */
    abstract fun getTileLoader(): TileLoader

    /**
     * Returns true if implementation uses a data connection, false otherwise. This value is used to
     * determine if this provider should be skipped if there is no data connection.
     *
     * @return true if implementation uses a data connection, false otherwise
     */
    abstract fun getUsesDataConnection(): Boolean

    /**
     * Gets the minimum zoom level this tile provider can provide
     *
     * @return the minimum zoom level
     */
    abstract fun getMinimumZoomLevel(): Int

    /**
     * Gets the maximum zoom level this tile provider can provide
     *
     * @return the maximum zoom level
     */
    abstract fun getMaximumZoomLevel(): Int

    /**
     * @since 6.1.3
     */
    fun isTileReachable(pMapTileIndex: Long): Boolean {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        return zoom >= getMinimumZoomLevel() && zoom <= getMaximumZoomLevel()
    }

    /**
     * Sets the tile source for this tile provider.
     *
     * @param tileSource the tile source
     */
    abstract fun setTileSource(tileSource: ITileSource?)

    private val mExecutor: ExecutorService

    protected val mQueueLockObject: Any = Any()
    protected val mWorking: HashMap<Long?, MapTileRequestState?>
    val mPending: LinkedHashMap<Long?, MapTileRequestState?>

    init {
        var pThreadPoolSize = pThreadPoolSize
        if (pPendingQueueSize < pThreadPoolSize) {
            Log.w(IMapView.LOGTAG, "The pending queue size is smaller than the thread pool size. Automatically reducing the thread pool size.")
            pThreadPoolSize = pPendingQueueSize
        }
        mExecutor = Executors.newFixedThreadPool(
            pThreadPoolSize,
            ConfigurablePriorityThreadFactory(Thread.NORM_PRIORITY, getThreadGroupName())
        )

        mWorking = HashMap<Long?, MapTileRequestState?>()
        mPending = object : LinkedHashMap<Long?, MapTileRequestState?>(
            pPendingQueueSize + 2, 0.1f,
            true
        ) {
            private val serialVersionUID = 6455337315681858866L

            override fun removeEldestEntry(
                pEldest: MutableMap.MutableEntry<Long?, MapTileRequestState?>?
            ): Boolean {
                if (size <= pPendingQueueSize) {
                    return false
                }
                // get the oldest tile that isn't in the mWorking queue
                val iterator = keys.iterator()
                while (iterator.hasNext()) {
                    val mapTileIndex: Long = iterator.next()!!
                    if (!mWorking.containsKey(mapTileIndex)) {
                        val state = get(mapTileIndex)
                        if (state != null) { // check for concurrency reasons
                            removeTileFromQueues(mapTileIndex)
                            state.callback?.mapTileRequestFailedExceedsMaxQueueSize(state)
                            return false
                        }
                    }
                }
                return false
            }
        }
    }

    fun loadMapTileAsync(pState: MapTileRequestState) {
        // Make sure we're not detached
        if (mExecutor.isShutdown()) return

        synchronized(mQueueLockObject) {
            if (instance!!.isDebugTileProviders) {
                Log.d(
                    IMapView.LOGTAG, ("MapTileModuleProviderBase.loadMaptileAsync() on provider: "
                            + getName() + " for tile: " + MapTileIndex.toString(pState.mapTile))
                )
                if (mPending.containsKey(pState.mapTile)) Log.d(
                    IMapView.LOGTAG,
                    "MapTileModuleProviderBase.loadMaptileAsync() tile already exists in request queue for modular provider. Moving to front of queue."
                )
                else Log.d(IMapView.LOGTAG, "MapTileModuleProviderBase.loadMaptileAsync() adding tile to request queue for modular provider.")
            }
            // this will put the tile in the queue, or move it to the front of
            // the queue if it's already present
            mPending.put(pState.mapTile, pState)
        }
        try {
            mExecutor.execute(getTileLoader())
        } catch (e: RejectedExecutionException) {
            Log.w(IMapView.LOGTAG, "RejectedExecutionException", e)
        }
    }

    private fun clearQueue() {
        synchronized(mQueueLockObject) {
            mPending.clear()
            mWorking.clear()
        }
    }

    /**
     * Detach, we're shutting down - Stops all workers.
     */
    open fun detach() {
        this.clearQueue()
        this.mExecutor.shutdown()
    }

    protected fun removeTileFromQueues(pMapTileIndex: Long) {
        synchronized(mQueueLockObject) {
            if (instance!!.isDebugTileProviders) {
                Log.d(
                    IMapView.LOGTAG, ("MapTileModuleProviderBase.removeTileFromQueues() on provider: "
                            + getName() + " for tile: " + MapTileIndex.toString(pMapTileIndex))
                )
            }
            mPending.remove(pMapTileIndex)
            mWorking.remove(pMapTileIndex)
        }
    }

    /**
     * Load the requested tile. An abstract internal class whose objects are used by worker threads
     * to acquire tiles from servers. It processes tiles from the 'pending' set to the 'working' set
     * as they become available. The key unimplemented method is 'loadTile'.
     */
    abstract inner class TileLoader : Runnable {
        /**
         * Actual load of the requested tile.
         * Do implement this method, but call [.loadTileIfReachable] instead
         *
         * @return the tile if it was loaded successfully, or null if failed to
         * load and other tile providers need to be called
         * @throws CantContinueException
         * @since 6.0.0
         */
        @Throws(CantContinueException::class)
        abstract fun loadTile(pMapTileIndex: Long): Drawable?

        /**
         * @since 6.1.3
         */
        @Throws(CantContinueException::class)
        fun loadTileIfReachable(pMapTileIndex: Long): Drawable? {
            if (!isTileReachable(pMapTileIndex)) {
                return null
            }
            return loadTile(pMapTileIndex)
        }

        @Deprecated("")
        @Throws(CantContinueException::class)
        protected fun loadTile(pState: MapTileRequestState): Drawable? {
            return loadTileIfReachable(pState.mapTile)
        }

        protected fun onTileLoaderInit() {
            // Do nothing by default
        }

        protected fun onTileLoaderShutdown() {
            // Do nothing by default
        }

        protected fun nextTile(): MapTileRequestState? {
            synchronized(mQueueLockObject) {
                var result: Long? = null
                // get the most recently accessed tile
                // - the last item in the iterator that's not already being
                // processed
                val iterator = mPending.keys.iterator()

                // TODO this iterates the whole list, make this faster...
                while (iterator.hasNext()) {
                    val mapTileIndex = iterator.next()
                    if (!mWorking.containsKey(mapTileIndex)) {
                        if (instance!!.isDebugTileProviders) {
                            Log.d(
                                IMapView.LOGTAG, ("TileLoader.nextTile() on provider: " + this@MapTileModuleProviderBase.getName()
                                        + " found tile in working queue: " + MapTileIndex.toString(mapTileIndex!!))
                            )
                        }
                        result = mapTileIndex
                    }
                }

                if (result != null) {
                    if (instance!!.isDebugTileProviders) {
                        Log.d(
                            IMapView.LOGTAG, ("TileLoader.nextTile() on provider: " + this@MapTileModuleProviderBase.getName()
                                    + " adding tile to working queue: " + result)
                        )
                    }
                    mWorking.put(result, mPending.get(result))
                }
                return (if (result != null) mPending.get(result) else null)
            }
        }

        /**
         * A tile has loaded.
         */
        protected open fun tileLoaded(pState: MapTileRequestState, pDrawable: Drawable) {
            if (instance!!.isDebugTileProviders) {
                Log.d(
                    IMapView.LOGTAG, ("TileLoader.tileLoaded() on provider: " + this@MapTileModuleProviderBase.getName() + " with tile: "
                            + MapTileIndex.toString(pState.mapTile))
                )
            }
            removeTileFromQueues(pState.mapTile)
            ExpirableBitmapDrawable.Companion.setState(pDrawable, ExpirableBitmapDrawable.Companion.UP_TO_DATE)
            pState.callback?.mapTileRequestCompleted(pState, pDrawable)
        }

        /**
         * A tile has loaded but it's expired.
         * Return it **and** send request to next provider.
         */
        protected fun tileLoadedExpired(pState: MapTileRequestState, pDrawable: Drawable) {
            if (instance!!.isDebugTileProviders) {
                Log.d(
                    IMapView.LOGTAG, ("TileLoader.tileLoadedExpired() on provider: " + this@MapTileModuleProviderBase.getName()
                            + " with tile: " + MapTileIndex.toString(pState.mapTile))
                )
            }
            removeTileFromQueues(pState.mapTile)
            ExpirableBitmapDrawable.Companion.setState(pDrawable, ExpirableBitmapDrawable.Companion.EXPIRED)
            pState.callback?.mapTileRequestExpiredTile(pState, pDrawable)
        }

        protected fun tileLoadedScaled(pState: MapTileRequestState, pDrawable: Drawable) {
            if (instance!!.isDebugTileProviders) {
                Log.d(
                    IMapView.LOGTAG, ("TileLoader.tileLoadedScaled() on provider: " + this@MapTileModuleProviderBase.getName()
                            + " with tile: " + MapTileIndex.toString(pState.mapTile))
                )
            }
            removeTileFromQueues(pState.mapTile)
            ExpirableBitmapDrawable.Companion.setState(pDrawable, ExpirableBitmapDrawable.Companion.SCALED)
            pState.callback?.mapTileRequestExpiredTile(pState, pDrawable)
        }


        protected fun tileLoadedFailed(pState: MapTileRequestState) {
            if (instance!!.isDebugTileProviders) {
                Log.d(
                    IMapView.LOGTAG, ("TileLoader.tileLoadedFailed() on provider: " + this@MapTileModuleProviderBase.getName()
                            + " with tile: " + MapTileIndex.toString(pState.mapTile))
                )
            }
            removeTileFromQueues(pState.mapTile)
            pState.callback?.mapTileRequestFailed(pState)
        }

        /**
         * This is a functor class of type Runnable. The run method is the encapsulated function.
         */
        override fun run() {
            onTileLoaderInit()

            var state: MapTileRequestState?
            var result: Drawable? = null
            while ((nextTile().also { state = it }) != null) {
                if (instance!!.isDebugTileProviders) {
                    Log.d(
                        IMapView.LOGTAG, ("TileLoader.run() processing next tile: "
                                + MapTileIndex.toString(state!!.mapTile)
                                + ", pending:" + mPending.size
                                + ", working:" + mWorking.size)
                    )
                }
                try {
                    result = null
                    result = loadTileIfReachable(state!!.mapTile)
                } catch (e: CantContinueException) {
                    Log.i(IMapView.LOGTAG, "Tile loader can't continue: " + MapTileIndex.toString(state!!.mapTile), e)
                    clearQueue()
                } catch (e: Throwable) {
                    Log.i(IMapView.LOGTAG, "Error downloading tile: " + MapTileIndex.toString(state!!.mapTile), e)
                }

                if (result == null) {
                    tileLoadedFailed(state!!)
                } else if (ExpirableBitmapDrawable.Companion.getState(result) == ExpirableBitmapDrawable.Companion.EXPIRED) {
                    tileLoadedExpired(state!!, result)
                } else if (ExpirableBitmapDrawable.Companion.getState(result) == ExpirableBitmapDrawable.Companion.SCALED) {
                    tileLoadedScaled(state!!, result)
                } else {
                    tileLoaded(state!!, result)
                }
            }

            onTileLoaderShutdown()
        }
    }
}
