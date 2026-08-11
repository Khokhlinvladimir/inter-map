package org.osmdroid.tileprovider.cachemanager

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Point
import android.graphics.Rect
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.library.R
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.modules.CantContinueException
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.TileDownloader
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicyException
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.IterableWithSize
import org.osmdroid.util.MapTileArea
import org.osmdroid.util.MapTileAreaList
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.MyMath
import org.osmdroid.util.TileSystem
import org.osmdroid.util.constants.GeoConstants
import org.osmdroid.views.MapView
import org.osmdroid.views.MapView.Companion.getTileSystem
import java.io.File
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Provides various methods for managing the local filesystem cache of osmdroid tiles: <br></br>
 * - Dowloading of tiles inside a specified area, <br></br>
 * - Cleaning of tiles inside a specified area,<br></br>
 * - Information about cache capacity and current cache usage. <br></br>
 *
 *
 * Important note 1: <br></br>
 * These methods only make sense for a MapView using an OnlineTileSourceBase:
 * bitmap tiles downloaded from urls. <br></br>
 *
 *
 * Important note 2 - about Bulk Downloading:<br></br>
 * When using OSM Mapnik tile server as the tile source, take care about OSM Tile usage policy
 * (http://wiki.openstreetmap.org/wiki/Tile_usage_policy).
 * Do not let to end-users the ability to download significant areas of tiles. <br></br>
 *
 * @author M.Kergall
 * @author Alex
 * @author 2ndGAB
 * @author F.Fontaine
 */
class CacheManager(
    pTileSource: ITileSource?,
    pWriter: IFilesystemCache,
    pMinZoomLevel: Int, pMaxZoomLevel: Int
) {
    private var mTileDownloader = TileDownloader() // default value
    protected val mTileSource: ITileSource?
    protected val mTileWriter: IFilesystemCache
    protected val mMinZoomLevel: Int
    protected val mMaxZoomLevel: Int
    protected var mPendingTasks: MutableSet<CacheManagerTask> = HashSet()

    /*
      * verifyCancel decides wether user has to confirm the cancel action via a alert
      *
      * @param state
      */
    var verifyCancel: Boolean = true

    @JvmOverloads
    constructor(mapView: MapView, writer: IFilesystemCache = mapView.getTileProvider()!!.getTileWriter()!!) : this(
        mapView.getTileProvider()!!,
        writer,
        mapView.getMinZoomLevel().toInt(),
        mapView.maxZoomLevel.toInt()
    )

    /**
     * See https://github.com/osmdroid/osmdroid/issues/619
     *
     * @since 5.6.5
     */
    constructor(
        pTileProvider: MapTileProviderBase,
        pWriter: IFilesystemCache,
        pMinZoomLevel: Int, pMaxZoomLevel: Int
    ) : this(pTileProvider.getTileSource(), pWriter, pMinZoomLevel, pMaxZoomLevel)

    /**
     * @since 6.0
     */
    init {
        mTileSource = pTileSource
        mTileWriter = pWriter
        mMinZoomLevel = pMinZoomLevel
        mMaxZoomLevel = pMaxZoomLevel
    }

    val pendingJobs: Int
        /**
         * @return
         * @since 5.6.3
         */
        get() = mPendingTasks.size

    /**
     * @return true if success, false if error
     */
    fun loadTile(tileSource: OnlineTileSourceBase, pMapTileIndex: Long): Boolean {
        //check if file is already downloaded:
        val file: File = getFileName(tileSource, pMapTileIndex)
        if (file.exists()) {
            return true
        }
        //check if the destination already has the file
        if (mTileWriter.exists(tileSource, pMapTileIndex)) {
            return true
        }

        return forceLoadTile(tileSource, pMapTileIndex)
    }

    /**
     * Actual tile download, regardless of the tile being already present in the cache
     *
     * @return true if success, false if error
     * @since 5.6.5
     */
    fun forceLoadTile(tileSource: OnlineTileSourceBase?, pMapTileIndex: Long): Boolean {
        if (tileSource == null) return false
        try {
            val drawable = mTileDownloader.downloadTile(pMapTileIndex, mTileWriter, tileSource)
            return drawable != null
        } catch (e: CantContinueException) {
            return false
        }
    }

    /** Returns *TRUE* if deletion was not possible  */
    private fun deleteTileError(pMapTileIndex: Long): Boolean {
        return this.checkTile(pMapTileIndex) && !mTileWriter.remove(mTileSource, pMapTileIndex)
    }

    fun deleteTile(pMapTileIndex: Long): Boolean {
        return !this.checkTile(pMapTileIndex) || mTileWriter.remove(mTileSource, pMapTileIndex)
    }

    fun checkTile(pMapTileIndex: Long): Boolean {
        return mTileWriter.exists(mTileSource, pMapTileIndex)
    }

    /**
     * "Should we download this tile?", either because it's not cached yet or because it's expired
     *
     * @since 5.6.5
     */
    fun isTileToBeDownloaded(pTileSource: ITileSource?, pMapTileIndex: Long): Boolean {
        val expiration = mTileWriter.getExpirationTimestamp(pTileSource, pMapTileIndex)
        if (expiration == null) {
            return true
        }
        val now = System.currentTimeMillis()
        return now > expiration
    }

    /**
     * @return the theoretical number of tiles in the specified area
     */
    fun possibleTilesInArea(pBB: BoundingBox, pZoomMin: Int, pZoomMax: Int): Int {
        return getTilesCoverageIterable(pBB, pZoomMin, pZoomMax).size()
    }

    /**
     * @return the theoretical number of tiles covered by the list of points
     * Calculation done based on http://www.movable-type.co.uk/scripts/latlong.html
     */
    fun possibleTilesCovered(
        pGeoPoints: ArrayList<GeoPoint>,
        pZoomMin: Int, pZoomMax: Int
    ): Int {
        return getTilesCoverage(pGeoPoints, pZoomMin, pZoomMax).size
    }

    fun execute(pTask: CacheManagerTask): CacheManagerTask {
        pTask.execute()
        mPendingTasks.add(pTask)
        return pTask
    }

    /**
     * Download in background all tiles of the specified area in osmdroid cache.
     *
     * @param ctx
     * @param bb
     * @param zoomMin
     * @param zoomMax
     */
    fun downloadAreaAsync(ctx: Context, bb: BoundingBox, zoomMin: Int, zoomMax: Int): CacheManagerTask {
        val task = CacheManagerTask(
            this,
            getDownloadingAction(ctx),
            bb,
            zoomMin,
            zoomMax
        )
        task.addCallback(getDownloadingDialog(ctx, task))
        return execute(task)
    }

    /**
     * Download in background all tiles of the specified area in osmdroid cache.
     *
     * @param ctx
     * @param geoPoints
     * @param zoomMin
     * @param zoomMax
     */
    fun downloadAreaAsync(ctx: Context, geoPoints: ArrayList<GeoPoint>, zoomMin: Int, zoomMax: Int): CacheManagerTask {
        val task = CacheManagerTask(
            this,
            getDownloadingAction(ctx),
            geoPoints,
            zoomMin,
            zoomMax
        )
        task.addCallback(getDownloadingDialog(ctx, task))
        return execute(task)
    }

    /**
     * Download in background all tiles of the specified area in osmdroid cache.
     *
     * @param ctx
     * @param bb
     * @param zoomMin
     * @param zoomMax
     */
    fun downloadAreaAsync(ctx: Context, bb: BoundingBox, zoomMin: Int, zoomMax: Int, callback: CacheManagerCallback?): CacheManagerTask {
        val task = CacheManagerTask(
            this,
            getDownloadingAction(ctx),
            bb,
            zoomMin,
            zoomMax
        )
        task.addCallback(callback)
        task.addCallback(getDownloadingDialog(ctx, task))
        return execute(task)
    }

    /**
     * Download in background all tiles covered by the GePoints list in osmdroid cache.
     *
     * @param ctx
     * @param geoPoints
     * @param zoomMin
     * @param zoomMax
     */
    fun downloadAreaAsync(
        ctx: Context,
        geoPoints: ArrayList<GeoPoint>,
        zoomMin: Int,
        zoomMax: Int,
        callback: CacheManagerCallback?
    ): CacheManagerTask {
        val task = CacheManagerTask(
            this,
            getDownloadingAction(ctx),
            geoPoints,
            zoomMin,
            zoomMax
        )
        task.addCallback(callback)
        task.addCallback(getDownloadingDialog(ctx, task))
        return execute(task)
    }

    /**
     * Download in background all tiles covered by the GeoPoints list in osmdroid cache without a user interface.
     *
     * @param ctx
     * @param geoPoints
     * @param zoomMin
     * @param zoomMax
     * @since
     */
    fun downloadAreaAsyncNoUI(
        ctx: Context,
        geoPoints: ArrayList<GeoPoint>,
        zoomMin: Int,
        zoomMax: Int,
        callback: CacheManagerCallback?
    ): CacheManagerTask {
        val task = CacheManagerTask(
            this,
            getDownloadingAction(ctx),
            geoPoints,
            zoomMin,
            zoomMax
        )
        task.addCallback(callback)
        return execute(task)
    }

    /**
     * Download in background all tiles of the specified area in osmdroid cache without a user interface.
     *
     * @param ctx
     * @param bb
     * @param zoomMin
     * @param zoomMax
     * @since 5.3
     */
    fun downloadAreaAsyncNoUI(ctx: Context, bb: BoundingBox, zoomMin: Int, zoomMax: Int, callback: CacheManagerCallback?): CacheManagerTask {
        val task = CacheManagerTask(
            this,
            getDownloadingAction(ctx),
            bb,
            zoomMin,
            zoomMax
        )
        task.addCallback(callback)
        execute(task)
        return task
    }

    /**
     * cancels all tasks
     *
     * @since 5.6.3
     */
    fun cancelAllJobs() {
        val iterator = mPendingTasks.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            next.cancel(true)
        }
        mPendingTasks.clear()
    }

    /**
     * Download in background all tiles of the specified area in osmdroid cache.
     *
     * @param ctx
     * @param pTiles
     * @param zoomMin
     * @param zoomMax
     */
    fun downloadAreaAsync(ctx: Context, pTiles: MutableList<Long?>, zoomMin: Int, zoomMax: Int): CacheManagerTask {
        val task = CacheManagerTask(
            this,
            getDownloadingAction(ctx),
            pTiles,
            zoomMin,
            zoomMax
        )
        task.addCallback(getDownloadingDialog(ctx, task))
        return execute(task)
    }

    /**
     *
     */
    interface CacheManagerCallback {
        /**
         * fired when the download job is done.
         */
        fun onTaskComplete()

        /**
         * this is fired periodically, useful for updating dialogs, progress bars, etc
         *
         * @param progress
         * @param currentZoomLevel
         * @param zoomMin
         * @param zoomMax
         */
        fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int)

        /**
         * as soon as the download is started, this is fired
         */
        fun downloadStarted()

        /**
         * this is fired right before the download starts
         *
         * @param total
         */
        fun setPossibleTilesInArea(total: Int)

        /**
         * this is fired when the task has been completed but had at least one download error.
         *
         * @param errors
         */
        fun onTaskFailed(errors: Int)
    }

    abstract class CacheManagerDialog(pCtx: Context, pTask: CacheManagerTask) : CacheManagerCallback {
        private val mTask: CacheManagerTask
        private val mProgressDialog: ProgressDialog
        private val handleMessage: String

        init {
            mTask = pTask
            handleMessage = pCtx.getString(R.string.cacheManagerHandlingMessage)
            mProgressDialog = ProgressDialog(pCtx)
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            mProgressDialog.setCancelable(true)
            // If verifyCancel is set to true, ask for verification before canceling
            if (pTask.mManager.verifyCancel) {
                mProgressDialog.setOnCancelListener(object : DialogInterface.OnCancelListener {
                    override fun onCancel(cancelDialog: DialogInterface?) {
                        val builder = AlertDialog.Builder(pCtx)
                        builder.setTitle(pCtx.getString(R.string.cacheManagerCancelTitle))
                        builder.setMessage(pCtx.getString(R.string.cacheManagerCancelBody))
                        builder.setPositiveButton(pCtx.getString(R.string.cacheManagerYes), object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                mTask.cancel(true)
                            }
                        })
                        builder.setNegativeButton(pCtx.getString(R.string.cacheManagerNo), object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface, which: Int) {
                                dialog.dismiss()
                                mProgressDialog.show()
                            }
                        })
                        builder.show()
                    }
                })
            } else {
                mProgressDialog.setOnCancelListener(object : DialogInterface.OnCancelListener {
                    override fun onCancel(dialog: DialogInterface?) {
                        mTask.cancel(true)
                    }
                })
            }
        }

        protected fun zoomMessage(zoomLevel: Int, zoomMin: Int, zoomMax: Int): String {
            return String.format(handleMessage, zoomLevel, zoomMin, zoomMax)
        }

        protected abstract val uITitle: String?

        override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
            mProgressDialog.setProgress(progress)
            mProgressDialog.setMessage(zoomMessage(currentZoomLevel, zoomMin, zoomMax))
        }

        override fun downloadStarted() {
            mProgressDialog.setTitle(this.uITitle)
            mProgressDialog.show()
        }

        override fun setPossibleTilesInArea(total: Int) {
            mProgressDialog.setMax(total)
        }

        override fun onTaskComplete() {
            dismiss()
        }

        override fun onTaskFailed(errors: Int) {
            dismiss()
        }

        private fun dismiss() {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss()
            }
        }
    }

    /**
     * generic class for common code related to AsyncTask management
     * - performing an action
     * - within a manager
     * - on a list of tiles (potentially sorted by ascending zoom level)
     * - and with callbacks for task progression
     */
    class CacheManagerTask private constructor(
        pManager: CacheManager, pAction: CacheManagerAction,
        pTiles: IterableWithSize<Long?>,
        pZoomMin: Int, pZoomMax: Int
    ) : AsyncTask<Any?, Int?, Int?>() {
        val mManager: CacheManager
        private val mAction: CacheManagerAction
        private val mTiles: IterableWithSize<Long?>
        private val mZoomMin: Int
        private val mZoomMax: Int
        private val mCallbacks = ArrayList<CacheManagerCallback>()

        init {
            mManager = pManager
            mAction = pAction
            mTiles = pTiles
            mZoomMin = max(pZoomMin, pManager.mMinZoomLevel)
            mZoomMax = min(pZoomMax, pManager.mMaxZoomLevel)
        }

        constructor(
            pManager: CacheManager, pAction: CacheManagerAction,
            pTiles: MutableList<Long?>,
            pZoomMin: Int, pZoomMax: Int
        ) : this(pManager, pAction, ListWrapper<Long?>(pTiles), pZoomMin, pZoomMax)

        constructor(
            pManager: CacheManager, pAction: CacheManagerAction,
            pGeoPoints: ArrayList<out GeoPoint?>,
            pZoomMin: Int, pZoomMax: Int
        ) : this(pManager, pAction, getTilesCoverage(ArrayList(pGeoPoints.filterNotNull()), pZoomMin, pZoomMax), pZoomMin, pZoomMax)

        constructor(
            pManager: CacheManager, pAction: CacheManagerAction,
            pBB: BoundingBox,
            pZoomMin: Int, pZoomMax: Int
        ) : this(pManager, pAction, getTilesCoverageIterable(pBB, pZoomMin, pZoomMax), pZoomMin, pZoomMax)

        fun addCallback(pCallback: CacheManagerCallback?) {
            if (pCallback != null) {
                mCallbacks.add(pCallback)
            }
        }

        override fun onPreExecute() {
            val total = mTiles.size()
            for (callback in mCallbacks) {
                try {
                    callback.setPossibleTilesInArea(total)
                    callback.downloadStarted()
                    callback.updateProgress(0, mZoomMin, mZoomMin, mZoomMax)
                } catch (t: Throwable) {
                    logFaultyCallback(t)
                }
            }
        }

        private fun logFaultyCallback(pThrowable: Throwable?) {
            Log.w(IMapView.LOGTAG, "Error caught processing cachemanager callback, your implementation is faulty", pThrowable)
        }

        override fun onProgressUpdate(vararg count: Int?) {
            //count[0] = tile counter, count[1] = current zoom level
            for (callback in mCallbacks) {
                try {
                    callback.updateProgress(count[0]!!, count[1]!!, mZoomMin, mZoomMax)
                } catch (t: Throwable) {
                    logFaultyCallback(t)
                }
            }
        }

        override fun onCancelled() {
            mManager.mPendingTasks.remove(this)
        }

        override fun onPostExecute(specialCount: Int?) {
            mManager.mPendingTasks.remove(this)
            val result = specialCount ?: 0
            for (callback in mCallbacks) {
                try {
                    if (result == 0) {
                        callback.onTaskComplete()
                    } else {
                        callback.onTaskFailed(result)
                    }
                } catch (t: Throwable) {
                    logFaultyCallback(t)
                }
            }
        }

        override fun doInBackground(vararg params: Any?): Int {
            if (!mAction.preCheck()) {
                return 0
            }

            var tileCounter = 0
            var errors = 0

            for (tile in mTiles) {
                val zoom = MapTileIndex.getZoom(tile!!)
                if (zoom >= mZoomMin && zoom <= mZoomMax) {
                    if (mAction.tileAction(tile)) {
                        errors++
                    }
                }
                tileCounter++
                if (tileCounter % mAction.progressModulo == 0) {
                    if (isCancelled()) {
                        return errors
                    }
                    publishProgress(tileCounter, MapTileIndex.getZoom(tile))
                }
            }
            return errors
        }
    }

    fun getDownloadingDialog(pCtx: Context, pTask: CacheManagerTask): CacheManagerDialog {
        return object : CacheManagerDialog(pCtx, pTask) {
            override val uITitle: String
                get() = pCtx.getString(R.string.cacheManagerDownloadingTitle)

            override fun onTaskFailed(errors: Int) {
                super.onTaskFailed(errors)
                Toast.makeText(
                    pCtx,
                    String.format(pCtx.getString(R.string.cacheManagerFailed), errors.toString() + ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getCleaningDialog(pCtx: Context, pTask: CacheManagerTask): CacheManagerDialog {
        return object : CacheManagerDialog(pCtx, pTask) {
            override val uITitle: String
                get() = pCtx.getString(R.string.cacheManagerCleaningTitle)

            override fun onTaskFailed(deleted: Int) {
                super.onTaskFailed(deleted)

                Toast.makeText(
                    pCtx,
                    String.format(pCtx.getString(R.string.cacheManagerCleanFailed), deleted.toString() + ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Action to perform on a tile within a CacheManagerTask
     *
     * @author F.Fontaine
     */
    interface CacheManagerAction {
        /**
         * Preconditions to check before bulk action
         *
         * @return true if we pass the check
         */
        fun preCheck(): Boolean

        /**
         * We will update the callbacks not for every tile, but at this rate
         */
        val progressModulo: Int

        /**
         * The action to perform on a single tile
         *
         * @return true if you want to increment the action counter
         */
        fun tileAction(pMapTileIndex: Long): Boolean
    }

    private class ListWrapper<T>(list: MutableList<T?>) : IterableWithSize<T?> {
        private val list: MutableList<T?>

        init {
            this.list = list
        }

        override fun size(): Int {
            return list.size
        }

        override fun iterator(): MutableIterator<T?> {
            return list.iterator()
        }
    }

    fun getDownloadingAction(pCtx: Context): CacheManagerAction {
        return object : CacheManagerAction {
            override fun preCheck(): Boolean {
                if (mTileSource is OnlineTileSourceBase) {
                    if (!mTileSource.tileSourcePolicy.acceptsBulkDownload()) {
                        throw TileSourcePolicyException(pCtx.getString(R.string.cacheManagerUnsupportedSource))
                    }
                    return true
                } else {
                    Log.e(IMapView.LOGTAG, "TileSource is not an online tile source")
                    return false
                }
            }

            override val progressModulo: Int
                get() = 10

            override fun tileAction(pMapTileIndex: Long): Boolean {
                return !loadTile((mTileSource as OnlineTileSourceBase?)!!, pMapTileIndex)
            }
        }
    }

    val cleaningAction: CacheManagerAction
        get() = object : CacheManagerAction {
            override fun preCheck(): Boolean {
                return true
            }

            override val progressModulo: Int
                get() = 1000

            override fun tileAction(pMapTileIndex: Long): Boolean {
                return deleteTileError(pMapTileIndex)
            }
        }

    /**
     * Remove all cached tiles in the specified area.
     *
     * @param ctx
     * @param bb
     * @param zoomMin
     * @param zoomMax
     */
    fun cleanAreaAsync(ctx: Context, bb: BoundingBox, zoomMin: Int, zoomMax: Int): CacheManagerTask {
        val task = CacheManagerTask(this, this.cleaningAction, bb, zoomMin, zoomMax)
        task.addCallback(getCleaningDialog(ctx, task))
        return execute(task)
    }

    /**
     * Remove all cached tiles covered by the GeoPoints list.
     *
     * @param ctx
     * @param geoPoints
     * @param zoomMin
     * @param zoomMax
     */
    fun cleanAreaAsync(ctx: Context, geoPoints: ArrayList<GeoPoint?>, zoomMin: Int, zoomMax: Int): CacheManagerTask {
        val extendedBounds = extendedBoundsFromGeoPoints(geoPoints, zoomMin)
        return cleanAreaAsync(ctx, extendedBounds, zoomMin, zoomMax)
    }

    /**
     * Remove all cached tiles in the specified area.
     */
    fun cleanAreaAsync(ctx: Context, tiles: MutableList<Long?>, zoomMin: Int, zoomMax: Int): CacheManagerTask {
        val task = CacheManagerTask(this, this.cleaningAction, tiles, zoomMin, zoomMax)
        task.addCallback(getCleaningDialog(ctx, task))
        return execute(task)
    }

    /**
     *
     */
    fun extendedBoundsFromGeoPoints(geoPoints: ArrayList<GeoPoint?>, minZoomLevel: Int): BoundingBox {
        val bb: BoundingBox = BoundingBox.Companion.fromGeoPoints(ArrayList(geoPoints.filterNotNull()))
        val right = getTileSystem().getTileXFromLongitude(bb.lonEast, minZoomLevel)
        val bottom = getTileSystem().getTileYFromLatitude(bb.latSouth, minZoomLevel)
        val left = getTileSystem().getTileXFromLongitude(bb.lonWest, minZoomLevel)
        val top = getTileSystem().getTileYFromLatitude(bb.latNorth, minZoomLevel)
        return BoundingBox(
            getTileSystem().getLatitudeFromTileY(top - 1, minZoomLevel),
            getTileSystem().getLongitudeFromTileX(right + 1, minZoomLevel),
            getTileSystem().getLatitudeFromTileY(bottom + 1, minZoomLevel),
            getTileSystem().getLongitudeFromTileX(left - 1, minZoomLevel)
        )
    }

    /**
     * @return volume currently use in the osmdroid local filesystem cache, in bytes.
     * Note that this method currently takes a while.
     */
    fun currentCacheUsage(): Long {
        //return TileWriter.getUsedCacheSpace(); //returned value is not stable! Increase and decrease, for unknown reasons.
        return directorySize(instance!!.osmdroidTileCache!!)
    }

    /**
     * @return the capacity of the osmdroid local filesystem cache, in bytes.
     * This capacity is currently a hard-coded constant inside osmdroid.
     */
    fun cacheCapacity(): Long {
        return instance!!.tileFileSystemCacheMaxBytes
    }

    /**
     * @return the total size of a directory and of its whole content, recursively
     */
    fun directorySize(pDirectory: File): Long {
        var usedCacheSpace: Long = 0
        val z = pDirectory.listFiles()
        if (z != null) {
            for (file in z) {
                if (file.isFile()) {
                    usedCacheSpace += file.length()
                } else {
                    if (file.isDirectory()) {
                        usedCacheSpace += directorySize(file)
                    }
                }
            }
        }
        return usedCacheSpace
    }

    /**
     * @since 6.0.2
     */
    fun setTileDownloader(pTileDownloader: TileDownloader) {
        mTileDownloader = pTileDownloader
    }

    companion object {
        @Deprecated(
            """Use {@link TileSystem#getTileXFromLongitude(double, int)} and
      {@link TileSystem#getTileYFromLatitude(double, int)} instead"""
        )
        @JvmStatic
        fun getMapTileFromCoordinates(aLat: Double, aLon: Double, zoom: Int): Point {
            val y = getTileSystem().getTileYFromLatitude(aLat, zoom)
            val x = getTileSystem().getTileXFromLongitude(aLon, zoom)
            return Point(x, y)
        }

        @Deprecated(
            """Use {@link TileSystem#getLatitudeFromTileY(int, int)} and
      {@link TileSystem#getLongitudeFromTileX(int, int)} instead"""
        )
        @JvmStatic
        fun getCoordinatesFromMapTile(x: Int, y: Int, zoom: Int): GeoPoint {
            val lat = getTileSystem().getLatitudeFromTileY(y, zoom)
            val lon = getTileSystem().getLongitudeFromTileX(x, zoom)
            return GeoPoint(lat, lon)
        }

        @JvmStatic
        fun getFileName(tileSource: ITileSource, pMapTileIndex: Long): File {
            val file = File(
                instance!!.osmdroidTileCache,
                tileSource.getTileRelativeFilenameString(pMapTileIndex) + OpenStreetMapTileProviderConstants.TILE_PATH_EXTENSION
            )
            return file
        }

        /**
         * Computes the theoretical tiles covered by the bounding box
         *
         * @return list of tiles, sorted by ascending zoom level
         */
        @JvmStatic
        fun getTilesCoverage(
            pBB: BoundingBox,
            pZoomMin: Int, pZoomMax: Int
        ): MutableList<Long?> {
            val result: MutableList<Long?> = ArrayList<Long?>()
            for (zoomLevel in pZoomMin..pZoomMax) {
                val resultForZoom: MutableCollection<Long?> = getTilesCoverage(pBB, zoomLevel)
                result.addAll(resultForZoom)
            }
            return result
        }

        /**
         * Computes the theoretical tiles covered by the bounding box
         *
         * @return list of tiles for that zoom level, without any specific order
         */
        @JvmStatic
        fun getTilesCoverage(pBB: BoundingBox, pZoomLevel: Int): MutableCollection<Long?> {
            val result: MutableSet<Long?> = LinkedHashSet<Long?>()
            for (mapTile in getTilesCoverageIterable(pBB, pZoomLevel, pZoomLevel)) {
                result.add(mapTile)
            }
            return result
        }

        /**
         * Iterable returning tiles covered by the bounding box sorted by ascending zoom level
         *
         * @param pBB      the given bounding box
         * @param pZoomMin the given minimum zoom level
         * @param pZoomMax the given maximum zoom level
         * @return the iterable described above
         */
        @JvmStatic
        fun getTilesCoverageIterable(
            pBB: BoundingBox,
            pZoomMin: Int, pZoomMax: Int
        ): IterableWithSize<Long?> {
            val list = MapTileAreaList()
            for (zoomLevel in pZoomMin..pZoomMax) {
                list.list.add(MapTileArea().set(zoomLevel, getTilesRect(pBB, zoomLevel)))
            }
            return list
        }

        /**
         * Retrieve upper left and lower right points(exclusive) corresponding to the tiles coverage for
         * the selected zoom level.
         *
         * @param pBB        the given bounding box
         * @param pZoomLevel the given zoom level
         * @return the [Rect] reflecting the tiles coverage
         */
        @JvmStatic
        fun getTilesRect(
            pBB: BoundingBox,
            pZoomLevel: Int
        ): Rect {
            val mapTileUpperBound = 1 shl pZoomLevel
            val right = getTileSystem().getTileXFromLongitude(pBB.lonEast, pZoomLevel)
            val bottom = getTileSystem().getTileYFromLatitude(pBB.latSouth, pZoomLevel)
            val left = getTileSystem().getTileXFromLongitude(pBB.lonWest, pZoomLevel)
            val top = getTileSystem().getTileYFromLatitude(pBB.latNorth, pZoomLevel)
            var width = right - left + 1 // handling the modulo
            if (width <= 0) {
                width += mapTileUpperBound
            }
            var height = bottom - top + 1 // handling the modulo
            if (height <= 0) {
                height += mapTileUpperBound
            }
            return Rect(left, top, left + width - 1, top + height - 1)
        }

        /**
         * Computes the theoretical tiles covered by the list of points
         *
         * @return list of tiles, sorted by ascending zoom level
         */
        @JvmStatic
        fun getTilesCoverage(
            pGeoPoints: ArrayList<GeoPoint>,
            pZoomMin: Int, pZoomMax: Int
        ): MutableList<Long?> {
            val result: MutableList<Long?> = ArrayList<Long?>()
            for (zoomLevel in pZoomMin..pZoomMax) {
                val resultForZoom: MutableCollection<Long?> = getTilesCoverage(pGeoPoints, zoomLevel)
                result.addAll(resultForZoom)
            }
            return result
        }

        /**
         * Computes the theoretical tiles covered by the list of points
         * Calculation done based on http://www.movable-type.co.uk/scripts/latlong.html
         */
        @JvmStatic
        fun getTilesCoverage(
            pGeoPoints: ArrayList<GeoPoint>,
            pZoomLevel: Int
        ): MutableCollection<Long?> {
            val result: MutableSet<Long?> = HashSet<Long?>()

            var prevPoint: GeoPoint? = null
            var tile: Point?
            var prevTile: Point? = null

            val mapTileUpperBound = 1 shl pZoomLevel
            for (geoPoint in pGeoPoints) {
                val d: Double = TileSystem.Companion.GroundResolution(geoPoint.latitude, pZoomLevel)

                if (result.size != 0) {
                    if (prevPoint != null) {
                        val leadCoef = (geoPoint.latitude - prevPoint.latitude) / (geoPoint.longitude - prevPoint.longitude)
                        val brng: Double
                        if (geoPoint.longitude > prevPoint.longitude) {
                            brng = Math.PI / 2 - atan(leadCoef)
                        } else {
                            brng = 3 * Math.PI / 2 - atan(leadCoef)
                        }

                        val wayPoint = GeoPoint(prevPoint.latitude, prevPoint.longitude)

                        while ((((geoPoint.latitude > prevPoint.latitude) && (wayPoint.latitude < geoPoint.latitude)) ||
                                    (geoPoint.latitude < prevPoint.latitude) && (wayPoint.latitude > geoPoint.latitude)) &&
                            (((geoPoint.longitude > prevPoint.longitude) && (wayPoint.longitude < geoPoint.longitude)) ||
                                    ((geoPoint.longitude < prevPoint.longitude) && (wayPoint.longitude > geoPoint.longitude)))
                        ) {
                            val prevLatRad = wayPoint.latitude * Math.PI / 180.0
                            val prevLonRad = wayPoint.longitude * Math.PI / 180.0

                            val latRad = asin(
                                sin(prevLatRad) * cos(d / GeoConstants.RADIUS_EARTH_METERS) + cos(prevLatRad) * sin(d / GeoConstants.RADIUS_EARTH_METERS) * cos(
                                    brng
                                )
                            )
                            val lonRad = prevLonRad + atan2(
                                sin(brng) * sin(d / GeoConstants.RADIUS_EARTH_METERS) * cos(prevLatRad),
                                cos(d / GeoConstants.RADIUS_EARTH_METERS) - sin(prevLatRad) * sin(latRad)
                            )

                            wayPoint.latitude = latRad * 180.0 / Math.PI
                            wayPoint.longitude = lonRad * 180.0 / Math.PI

                            tile = Point(
                                getTileSystem().getTileXFromLongitude(wayPoint.longitude, pZoomLevel),
                                getTileSystem().getTileYFromLatitude(wayPoint.latitude, pZoomLevel)
                            )

                            if (tile != prevTile) {
//Log.d(Constants.APP_TAG, "New Tile lat " + tile.x + " lon " + tile.y);
                                val ofsx = if (tile.x >= 0) 0 else -tile.x
                                val ofsy = if (tile.y >= 0) 0 else -tile.y
                                for (xAround in tile.x + ofsx..tile.x + 1 + ofsx) {
                                    for (yAround in tile.y + ofsy..tile.y + 1 + ofsy) {
                                        val tileY = MyMath.mod(yAround, mapTileUpperBound)
                                        val tileX = MyMath.mod(xAround, mapTileUpperBound)
                                        result.add(MapTileIndex.getTileIndex(pZoomLevel, tileX, tileY))
                                    }
                                }

                                prevTile = tile
                            }
                        }
                    }
                } else {
                    tile = Point(
                        getTileSystem().getTileXFromLongitude(geoPoint.longitude, pZoomLevel),
                        getTileSystem().getTileYFromLatitude(geoPoint.latitude, pZoomLevel)
                    )
                    prevTile = tile

                    val ofsx = if (tile.x >= 0) 0 else -tile.x
                    val ofsy = if (tile.y >= 0) 0 else -tile.y
                    for (xAround in tile.x + ofsx..tile.x + 1 + ofsx) {
                        for (yAround in tile.y + ofsy..tile.y + 1 + ofsy) {
                            val tileY = MyMath.mod(yAround, mapTileUpperBound)
                            val tileX = MyMath.mod(xAround, mapTileUpperBound)
                            result.add(MapTileIndex.getTileIndex(pZoomLevel, tileX, tileY))
                        }
                    }
                }

                prevPoint = geoPoint
            }
            return result
        }
    }
}
