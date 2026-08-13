package org.osmdroid.views.overlay

import android.graphics.Canvas
import android.graphics.Point
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import org.osmdroid.api.IMapView
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay.Snappable
import java.util.AbstractList
import java.util.concurrent.CopyOnWriteArrayList

/**
 * https://github.com/osmdroid/osmdroid/issues/154
 *
 * @author dozd
 * @since 5.0.0
 */
class DefaultOverlayManager(tilesOverlay: TilesOverlay?) : AbstractList<Overlay?>(), OverlayManager {
    private var mTilesOverlay: TilesOverlay? = null

    private val mOverlayList: CopyOnWriteArrayList<Overlay?>

    init {
        setTilesOverlay(tilesOverlay)
        mOverlayList = CopyOnWriteArrayList<Overlay?>()
    }

    override fun get(pIndex: Int): Overlay? {
        return mOverlayList.get(pIndex)
    }

    override val size: Int
        get() = mOverlayList.size

    override fun add(pIndex: Int, pElement: Overlay?) {
        if (pElement == null) {
            //#396 fix, null check
            val ex = Exception()
            Log.e(IMapView.LOGTAG, "Attempt to add a null overlay to the collection. This is probably a bug and should be reported!", ex)
        } else {
            mOverlayList.add(pIndex, pElement)
        }
    }

    override fun removeAt(pIndex: Int): Overlay? {
        return mOverlayList.removeAt(pIndex)
    }

    override fun set(pIndex: Int, pElement: Overlay?): Overlay? {
        //#396 fix, null check
        if (pElement == null) {
            val ex = Exception()
            Log.e(IMapView.LOGTAG, "Attempt to set a null overlay to the collection. This is probably a bug and should be reported!", ex)
            return null
        } else {
            val overlay = mOverlayList.set(pIndex, pElement)
            return overlay
        }
    }


    override fun getTilesOverlay(): TilesOverlay? {
        return mTilesOverlay
    }

    override fun setTilesOverlay(tilesOverlay: TilesOverlay?) {
        mTilesOverlay = tilesOverlay
    }

    override fun overlaysReversed(): Iterable<Overlay> {
        return object : Iterable<Overlay> {
            /**
             * @since 6.1.0
             */
            fun bulletProofReverseListIterator(): MutableListIterator<Overlay?> {
                while (true) {
                    try {
                        return mOverlayList.listIterator(mOverlayList.size)
                    } catch (e: IndexOutOfBoundsException) {
                        // thread-concurrency fix - in case an item is removed in a very inappropriate time
                        // cf. https://github.com/osmdroid/osmdroid/issues/1260
                    }
                }
            }

            override fun iterator(): MutableIterator<Overlay> {
                val i = bulletProofReverseListIterator()

                return object : MutableIterator<Overlay> {
                    override fun hasNext(): Boolean {
                        return i.hasPrevious()
                    }

                    override fun next(): Overlay {
                        return i.previous()!!
                    }

                    override fun remove() {
                        i.remove()
                    }
                }
            }
        }
    }

    override fun overlays(): MutableList<Overlay?> {
        return mOverlayList
    }


    override fun onDraw(c: Canvas, pMapView: MapView) {
        onDrawHelper(c, pMapView, pMapView.projection)
    }

    /**
     * @since 6.1.0
     */
    override fun onDraw(c: Canvas, pProjection: Projection) {
        onDrawHelper(c, null, pProjection)
    }

    /**
     * @param pMapView    may be null
     * @param pProjection may NOT be null
     * @since 6.1.0
     */
    private fun onDrawHelper(c: Canvas, pMapView: MapView?, pProjection: Projection) {
        //fix for https://github.com/osmdroid/osmdroid/issues/904
        if (mTilesOverlay != null) mTilesOverlay!!.protectDisplayedTilesForCache(c, pProjection)
        for (overlay in mOverlayList) {
            if (overlay != null && overlay.isEnabled() && overlay is TilesOverlay) {
                overlay.protectDisplayedTilesForCache(c, pProjection)
            }
        }

        //always pass false, the shadow parameter will be removed in a later version of osmdroid, this change should result in the on draw being called twice
        if (mTilesOverlay != null && mTilesOverlay!!.isEnabled()) {
            if (pMapView != null) {
                mTilesOverlay!!.draw(c, pMapView, false)
            } else {
                mTilesOverlay!!.draw(c, pProjection)
            }
        }

        //always pass false, the shadow parameter will be removed in a later version of osmdroid, this change should result in the on draw being called twice
        for (overlay in mOverlayList) {
            //#396 fix, null check
            if (overlay != null && overlay.isEnabled()) {
                if (pMapView != null) {
                    overlay.draw(c, pMapView, false)
                } else {
                    overlay.draw(c, pProjection)
                }
            }
        }
        //potential fix for #52 pMapView.invalidate();
    }

    override fun onDetach(pMapView: MapView?) {
        if (mTilesOverlay != null) {
            mTilesOverlay!!.onDetach(pMapView)
        }

        for (overlay in this.overlaysReversed()) {
            overlay.onDetach(pMapView)
        }
        this.clear()
    }

    override fun onPause() {
        if (mTilesOverlay != null) {
            mTilesOverlay!!.onPause()
        }

        for (overlay in this.overlaysReversed()) {
            overlay.onPause()
        }
    }

    override fun onResume() {
        if (mTilesOverlay != null) {
            mTilesOverlay!!.onResume()
        }

        for (overlay in this.overlaysReversed()) {
            overlay.onResume()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onKeyDown(keyCode, event, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onKeyUp(keyCode, event, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onTouchEvent(event: MotionEvent, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onTouchEvent(event, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onTrackballEvent(event: MotionEvent?, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onTrackballEvent(event, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onSnapToItem(x: Int, y: Int, snapPoint: Point, pMapView: IMapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay is Snappable) {
                if ((overlay as Snappable).onSnapToItem(x, y, snapPoint, pMapView)) {
                    return true
                }
            }
        }

        return false
    }

    /* GestureDetector.OnDoubleTapListener */
    override fun onDoubleTap(e: MotionEvent, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onDoubleTap(e, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onDoubleTapEvent(e, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onSingleTapConfirmed(e, pMapView)) {
                return true
            }
        }

        return false
    }

    /* OnGestureListener */
    override fun onDown(pEvent: MotionEvent?, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onDown(pEvent, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onFling(
        pEvent1: MotionEvent?, pEvent2: MotionEvent?,
        pVelocityX: Float, pVelocityY: Float, pMapView: MapView?
    ): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onFling(pEvent1, pEvent2, pVelocityX, pVelocityY, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onLongPress(pEvent: MotionEvent, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onLongPress(pEvent, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onScroll(
        pEvent1: MotionEvent?, pEvent2: MotionEvent?,
        pDistanceX: Float, pDistanceY: Float, pMapView: MapView?
    ): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView)) {
                return true
            }
        }

        return false
    }

    override fun onShowPress(pEvent: MotionEvent?, pMapView: MapView?) {
        for (overlay in this.overlaysReversed()) {
            overlay.onShowPress(pEvent, pMapView)
        }
    }

    override fun onSingleTapUp(pEvent: MotionEvent, pMapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay.onSingleTapUp(pEvent, pMapView)) {
                return true
            }
        }

        return false
    }

    // ** Options Menu **//
    override fun setOptionsMenusEnabled(pEnabled: Boolean) {
        for (overlay in mOverlayList) {
            if ((overlay is IOverlayMenuProvider)
                && (overlay as IOverlayMenuProvider).isOptionsMenuEnabled
            ) {
                (overlay as IOverlayMenuProvider).isOptionsMenuEnabled = pEnabled
            }
        }
    }

    override fun onCreateOptionsMenu(pMenu: Menu, menuIdOffset: Int, mapView: MapView?): Boolean {
        var result = true
        for (overlay in this.overlaysReversed()) {
            if (overlay is IOverlayMenuProvider) {
                val overlayMenuProvider = overlay as IOverlayMenuProvider
                if (overlayMenuProvider.isOptionsMenuEnabled) {
                    result = result and overlayMenuProvider.onCreateOptionsMenu(pMenu, menuIdOffset, mapView)
                }
            }
        }

        if (mTilesOverlay != null && mTilesOverlay!!.isOptionsMenuEnabled) {
            result = result and mTilesOverlay!!.onCreateOptionsMenu(pMenu, menuIdOffset, mapView)
        }

        return result
    }

    override fun onPrepareOptionsMenu(pMenu: Menu, menuIdOffset: Int, mapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay is IOverlayMenuProvider) {
                val overlayMenuProvider = overlay as IOverlayMenuProvider
                if (overlayMenuProvider.isOptionsMenuEnabled) {
                    overlayMenuProvider.onPrepareOptionsMenu(pMenu, menuIdOffset, mapView)
                }
            }
        }

        if (mTilesOverlay != null && mTilesOverlay!!.isOptionsMenuEnabled) {
            mTilesOverlay!!.onPrepareOptionsMenu(pMenu, menuIdOffset, mapView)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem, menuIdOffset: Int, mapView: MapView?): Boolean {
        for (overlay in this.overlaysReversed()) {
            if (overlay is IOverlayMenuProvider) {
                val overlayMenuProvider = overlay as IOverlayMenuProvider
                if (overlayMenuProvider.isOptionsMenuEnabled &&
                    overlayMenuProvider.onOptionsItemSelected(item, menuIdOffset, mapView)
                ) {
                    return true
                }
            }
        }

        if (mTilesOverlay != null &&
            mTilesOverlay!!.isOptionsMenuEnabled &&
            mTilesOverlay!!.onOptionsItemSelected(item, menuIdOffset, mapView)
        ) {
            return true
        }

        return false
    }
}
