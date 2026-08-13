package org.osmdroid.views.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import org.osmdroid.tileprovider.ExpirableBitmapDrawable
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.util.RectL
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Create a bitmap in the background from [MapView]-like data but without a [MapView]
 *
 * @author Fabrice Fontaine
 * @since 6.1.0
 */
open class MapSnapshot(
    private var mMapSnapshotable: MapSnapshotable?,
    private val mIncludeFlags: Int,
    private var mTileProvider: MapTileProviderBase?,
    private var mOverlays: MutableList<Overlay?>?,
    private var mProjection: Projection?
) : Runnable {
    interface MapSnapshotable {
        fun callback(pMapSnapshot: MapSnapshot?)
    }

    enum class Status {
        NOTHING,
        STARTED,
        TILES_OK,
        PAINTING,
        CANVAS_OK
    }

    private val mViewPort = RectL()
    private var mHandler: MapSnapshotHandler?
    private var mTilesOverlay: TilesOverlay?
    var status: Status = Status.NOTHING
        private set
    var bitmap: Bitmap? = null
        private set
    private var mIsDetached = false

    constructor(
        pMapSnapshotable: MapSnapshotable?,
        pIncludeFlags: Int,
        pMapView: MapView
    ) : this(
        pMapSnapshotable, pIncludeFlags,
        pMapView.getTileProvider(),
        pMapView.getOverlays(),
        pMapView.projection
    )

    override fun run() {
        this.status = Status.STARTED
        refreshASAP()
    }

    fun save(pFile: File): Boolean {
        return Companion.save(this.bitmap!!, pFile)
    }

    fun onDetach() {
        mIsDetached = true
        mProjection = null
        mTileProvider!!.tileRequestCompleteHandlers.remove(mHandler)
        mTileProvider!!.detach()
        mTileProvider = null
        mHandler!!.destroy()
        mHandler = null
        mMapSnapshotable = null
        mTilesOverlay = null
        mOverlays = null
        this.bitmap = null
    }

    private fun draw() {
        val projection = requireNotNull(mProjection)
        val overlays = mOverlays
        this.bitmap = Bitmap.createBitmap(projection.width, projection.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(this.bitmap!!)
        projection.save(canvas, true, false)
        mTilesOverlay!!.drawTiles(canvas, projection, projection.zoomLevel, mViewPort)
        if (overlays != null) {
            for (overlay in overlays) {
                if (overlay != null && overlay.isEnabled()) {
                    overlay.draw(canvas, projection)
                }
            }
        }
        projection.restore(canvas, false)
    }

    /**
     * Putting the tile in the memory cache by trying to draw them (but on a null Canvas)
     */
    private fun refresh() {
        if (!refreshCheckStart()) {
            return
        }
        val tileStates = mTilesOverlay!!.tileStates
        val projection = requireNotNull(mProjection)
        do {
            mTilesOverlay!!.drawTiles(null, projection, projection.zoomLevel, mViewPort)
            var ready = true
            if (mIncludeFlags != 0 && mIncludeFlags != INCLUDE_FLAGS_ALL) {
                if (ready && (mIncludeFlags and INCLUDE_FLAG_UPTODATE) == 0 && tileStates.upToDate != 0) {
                    ready = false
                }
                if (ready && (mIncludeFlags and INCLUDE_FLAG_EXPIRED) == 0 && tileStates.expired != 0) {
                    ready = false
                }
                if (ready && (mIncludeFlags and INCLUDE_FLAG_SCALED) == 0 && tileStates.scaled != 0) {
                    ready = false
                }
                if (ready && (mIncludeFlags and INCLUDE_FLAG_NOTFOUND) == 0 && tileStates.notFound != 0) {
                    ready = false
                }
            }
            if (ready) {
                if (this.status == Status.CANVAS_OK || this.status == Status.PAINTING) {
                    return
                }
                if (!refreshCheckFinish()) {
                    return
                }
                this.status = Status.PAINTING
                if (mIsDetached) {
                    return
                }
                draw()
                this.status = Status.CANVAS_OK
                val mapSnapshotable = mMapSnapshotable
                if (mapSnapshotable != null) {
                    mapSnapshotable.callback(this@MapSnapshot)
                }
            }
        } while (refreshCheckEnd())
    }

    @Synchronized
    private fun refreshCheckStart(): Boolean {
        if (mIsDetached) {
            return false
        }
        if (mAlreadyFinished) {
            return false
        }
        if (!mOneMoreTime) {
            return false
        }
        if (mCurrentlyRunning) {
            return false
        }
        mOneMoreTime = false
        mCurrentlyRunning = true
        return true
    }

    @Synchronized
    private fun refreshCheckEnd(): Boolean {
        if (mIsDetached) {
            return false
        }
        if (mAlreadyFinished) {
            return false
        }
        if (!mOneMoreTime) {
            mCurrentlyRunning = false
            return false
        }
        mOneMoreTime = false
        return true
    }

    @Synchronized
    private fun refreshCheckFinish(): Boolean {
        val result = !mAlreadyFinished
        mAlreadyFinished = true
        return result
    }

    @Synchronized
    private fun refreshAgain(): Boolean {
        mOneMoreTime = true
        return !mCurrentlyRunning
    }

    fun refreshASAP() {
        if (refreshAgain()) {
            refresh()
        }
    }

    private var mOneMoreTime = false
    private var mCurrentlyRunning = false
    private var mAlreadyFinished = false

    init {
        val projection = requireNotNull(mProjection)
        val tileProvider = requireNotNull(mTileProvider)
        projection.getMercatorViewPort(mViewPort)
        mTilesOverlay = TilesOverlay(tileProvider, null)
        mTilesOverlay!!.isHorizontalWrapEnabled = projection.isHorizontalWrapEnabled
        mTilesOverlay!!.isVerticalWrapEnabled = projection.isVerticalWrapEnabled
        mHandler = MapSnapshotHandler(this)
        tileProvider.tileRequestCompleteHandlers.add(mHandler)
    }

    companion object {
        /**
         * The INCLUDE_FLAGs let you precise the tiles you accept in your snapshot,
         * depending on their states.
         * For instance, if your flag includes INCLUDE_FLAG_SCALED, that means that you accept
         * scaled tiles in your output.
         * If your flag equals INCLUDE_FLAG_UPTODATE, that means that you accept only up-to-date tiles,
         * and implicitly that you may have to wait, and need background downloads.
         * Cf. [ExpirableBitmapDrawable]
         */
        const val INCLUDE_FLAG_UPTODATE: Int = 1
        const val INCLUDE_FLAG_EXPIRED: Int = 2
        const val INCLUDE_FLAG_SCALED: Int = 4
        const val INCLUDE_FLAG_NOTFOUND: Int = 8
        val INCLUDE_FLAGS_ALL: Int = INCLUDE_FLAG_UPTODATE + INCLUDE_FLAG_EXPIRED + INCLUDE_FLAG_SCALED + INCLUDE_FLAG_NOTFOUND

        val isUIThread: Boolean
            /**
             * To be used in View-related Overlay's draw methods.
             * Not only are we not able to include View's in the snapshots,
             * but drawing those View's can make the app crash.
             * A solution is to catch an Exception when drawing,
             * and to be lenient when we're not on the UI thread
             */
            get() = Looper.myLooper() == Looper.getMainLooper()

        private fun save(pBitmap: Bitmap, pFile: File): Boolean {
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(pFile.getAbsolutePath())
                pBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    if (out != null) {
                        out.close()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return false
        }
    }
}
