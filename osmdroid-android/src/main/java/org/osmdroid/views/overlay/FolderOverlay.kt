package org.osmdroid.views.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.Projection
import kotlin.math.max
import kotlin.math.min

/**
 * A [org.osmdroid.views.overlay.FolderOverlay] is just a group of other [Overlay]s.
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='./doc-files/marker-classes.png'></img>
 *
 * @author M.Kergall
 */
class FolderOverlay() : Overlay() {
    protected var mOverlayManager: OverlayManager?
    var name: String?
    var description: String?

    /**
     * Use [.FolderOverlay] instead
     */
    @Deprecated("")
    constructor(ctx: Context?) : this()

    init {
        mOverlayManager = DefaultOverlayManager(null)
        this.name = ""
        this.description = ""
    }

    val items: MutableList<Overlay?>?
        /**
         * @return the list of components of this folder.
         * Doesn't provide a copy, but the actual list.
         */
        get() = mOverlayManager

    fun add(item: Overlay?): Boolean {
        val b = mOverlayManager!!.add(item)
        if (b) recalculateBounds()
        return b
    }

    private fun recalculateBounds() {
        var minLat = Double.Companion.MAX_VALUE
        var minLon = Double.Companion.MAX_VALUE
        var maxLat = -Double.Companion.MAX_VALUE
        var maxLon = -Double.Companion.MAX_VALUE
        for (gp in mOverlayManager!!) {
            val box = gp?.getBounds() ?: continue


            minLat = min(minLat, box.latSouth)
            minLon = min(minLon, box.lonWest)
            maxLat = max(maxLat, box.latNorth)
            maxLon = max(maxLon, box.lonEast)
        }

        if (minLat == Double.Companion.MAX_VALUE) { // no overlay
            val tileSystem = getTileSystem()
            mBounds = BoundingBox( // default values
                tileSystem.maxLatitude, tileSystem.maxLongitude,
                tileSystem.minLatitude, tileSystem.minLongitude
            )
        } else {
            mBounds = BoundingBox(maxLat, maxLon, minLat, minLon)
        }
    }

    fun remove(item: Overlay?): Boolean {
        val b = mOverlayManager!!.remove(item)
        if (b) recalculateBounds()
        return b
    }

    @SuppressLint("WrongCall")
    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        mOverlayManager!!.onDraw(pCanvas, pProjection)
    }

    @SuppressLint("WrongCall")
    override fun draw(pCanvas: Canvas, pMapView: MapView, pShadow: Boolean) {
        if (pShadow) {
            return
        }
        mOverlayManager!!.onDraw(pCanvas, pMapView)
    }

    override fun onSingleTapUp(e: MotionEvent, mapView: MapView?): Boolean {
        if (isEnabled()) return mOverlayManager!!.onSingleTapUp(e, mapView)
        else return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView?): Boolean {
        if (isEnabled()) return mOverlayManager!!.onSingleTapConfirmed(e, mapView)
        else return false
    }

    override fun onLongPress(e: MotionEvent, mapView: MapView?): Boolean {
        if (isEnabled()) return mOverlayManager!!.onLongPress(e, mapView)
        else return false
    }

    override fun onTouchEvent(e: MotionEvent, mapView: MapView?): Boolean {
        if (isEnabled()) return mOverlayManager!!.onTouchEvent(e, mapView)
        else return false
    }

    override fun onDoubleTap(e: MotionEvent, mapView: MapView?): Boolean {
        if (isEnabled()) return mOverlayManager!!.onDoubleTap(e, mapView)
        else return false
    }

    //TODO: implement other events...
    /**
     * Close all opened InfoWindows of overlays it contains.
     * This only operates on overlays that inherit from OverlayWithIW.
     */
    fun closeAllInfoWindows() {
        for (overlay in mOverlayManager!!) {
            if (overlay is FolderOverlay) {
                overlay.closeAllInfoWindows()
            } else if (overlay is OverlayWithIW) {
                overlay.closeInfoWindow()
            }
        }
    }

    override fun onDetach(mapView: MapView?) {
        if (mOverlayManager != null) mOverlayManager!!.onDetach(mapView)
        mOverlayManager = null
    }
}
