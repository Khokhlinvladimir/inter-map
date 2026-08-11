package org.osmdroid.views.overlay

import android.graphics.Canvas
import android.graphics.Point
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import org.osmdroid.api.IMapView
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.drawing.MapSnapshot

interface OverlayManager : MutableList<Overlay?> {
    override fun get(pIndex: Int): Overlay?

    override val size: Int

    override fun add(pIndex: Int, pElement: Overlay?)

    override fun removeAt(pIndex: Int): Overlay?

    override fun set(pIndex: Int, pElement: Overlay?): Overlay?

    /**
     * Gets the optional TilesOverlay class.
     *
     * @return the tilesOverlay
     */
    /**
     * Sets the optional TilesOverlay class. If set, this overlay will be drawn before all other
     * overlays and will not be included in the editable list of overlays and can't be cleared
     * except by a subsequent call to setTilesOverlay().
     *
     * @param tilesOverlay the tilesOverlay to set
     */
    fun getTilesOverlay(): TilesOverlay?

    fun setTilesOverlay(tilesOverlay: TilesOverlay?)

    fun overlays(): MutableList<Overlay?>?

    fun overlaysReversed(): Iterable<Overlay>

    /**
     * If possible, use [.onDraw] instead (cf. [MapSnapshot]
     */
    fun onDraw(c: Canvas, pMapView: MapView)

    /**
     * @since 6.1.0
     */
    fun onDraw(c: Canvas, pProjection: Projection)

    fun onDetach(pMapView: MapView?)

    fun onKeyDown(keyCode: Int, event: KeyEvent?, pMapView: MapView?): Boolean

    fun onKeyUp(keyCode: Int, event: KeyEvent?, pMapView: MapView?): Boolean

    fun onTouchEvent(event: MotionEvent, pMapView: MapView?): Boolean

    fun onTrackballEvent(event: MotionEvent?, pMapView: MapView?): Boolean

    fun onSnapToItem(x: Int, y: Int, snapPoint: Point, pMapView: IMapView?): Boolean

    fun onDoubleTap(e: MotionEvent, pMapView: MapView?): Boolean

    fun onDoubleTapEvent(e: MotionEvent, pMapView: MapView?): Boolean

    fun onSingleTapConfirmed(e: MotionEvent, pMapView: MapView?): Boolean

    fun onDown(pEvent: MotionEvent?, pMapView: MapView?): Boolean

    fun onFling(
        pEvent1: MotionEvent?, pEvent2: MotionEvent?,
        pVelocityX: Float, pVelocityY: Float, pMapView: MapView?
    ): Boolean

    fun onLongPress(pEvent: MotionEvent, pMapView: MapView?): Boolean

    fun onScroll(
        pEvent1: MotionEvent?, pEvent2: MotionEvent?,
        pDistanceX: Float, pDistanceY: Float, pMapView: MapView?
    ): Boolean

    fun onShowPress(pEvent: MotionEvent?, pMapView: MapView?)

    fun onSingleTapUp(pEvent: MotionEvent, pMapView: MapView?): Boolean

    fun setOptionsMenusEnabled(pEnabled: Boolean)

    fun onCreateOptionsMenu(pMenu: Menu, menuIdOffset: Int, mapView: MapView?): Boolean

    fun onPrepareOptionsMenu(pMenu: Menu, menuIdOffset: Int, mapView: MapView?): Boolean

    fun onOptionsItemSelected(item: MenuItem, menuIdOffset: Int, mapView: MapView?): Boolean

    /**
     * @since 6.0.0
     */
    fun onPause()

    /**
     * @since 6.0.0
     */
    fun onResume()
}
