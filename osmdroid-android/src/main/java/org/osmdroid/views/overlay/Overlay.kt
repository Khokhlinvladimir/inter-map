// Created by plusminus on 20:32:01 - 27.09.2008
package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.MotionEvent
import org.osmdroid.api.IMapView
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.Projection
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.util.constants.OverlayConstants
import java.util.concurrent.atomic.AtomicInteger

/**
 * [Overlay]: Base class representing an overlay which may be displayed on top of a [MapView].
 *
 *
 * To add an overlay, subclass this class, create an instance, and add it to the list obtained from
 * getOverlays() of [MapView].
 *
 *
 * This class implements a form of Gesture Handling similar to
 * [android.view.GestureDetector.SimpleOnGestureListener] and
 * [GestureDetector.OnGestureListener]. The difference is there is an additional argument for
 * the item.
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='./doc-files/marker-classes.png'></img>
 *
 * @author Nicolas Gramlich
 */
abstract class Overlay : OverlayConstants {
    /**
     * Specifies if the Overlay is marked to be enabled. This should be checked before calling
     * draw().
     *
     * @return true if the Overlay is marked enabled, false otherwise
     */
    /**
     * Sets whether the Overlay is marked to be enabled. This setting does nothing by default, but
     * should be checked before calling draw().
     */
    private var mEnabled = true
    private val tileSystem = getTileSystem() // used only for the default bounding box

    /**
     * Gets the bounds of the overlay, useful for skipping draw cycles on overlays
     * that are not in the current bounding box of the view
     *
     * @return
     * @since 6.0.0
     */
    protected var mBounds: BoundingBox =
        BoundingBox(tileSystem.maxLatitude, tileSystem.maxLongitude, tileSystem.minLatitude, tileSystem.minLongitude)

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * Use [.Overlay] instead
     */
    @Deprecated("")
    constructor(ctx: Context?)

    constructor()

    open fun getBounds(): BoundingBox = mBounds

    open fun setEnabled(pEnabled: Boolean) {
        mEnabled = pEnabled
    }

    open fun isEnabled(): Boolean = mEnabled

    // ===========================================================
    // Methods for SuperClass/Interfaces
    // ===========================================================
    /**
     * Draw the overlay over the map. This will be called on all active overlays with shadow=true,
     * to lay down the shadow layer, and then again on all overlays with shadow=false. Callers
     * should check isEnabled() before calling draw(). By default, draws nothing.
     *
     *
     * changed for 5.6 to be public see https://github.com/osmdroid/osmdroid/issues/466
     * If possible, use [.draw] instead (cf. [MapSnapshot]
     */
    open fun draw(pCanvas: Canvas, pMapView: MapView, pShadow: Boolean) {
        if (pShadow) {
            return
        }
        draw(pCanvas, pMapView.projection)
    }

    /**
     * @since 6.1.0
     */
    open fun draw(pCanvas: Canvas, pProjection: Projection) {
        // display nothing by default
    }

    // ===========================================================
    // Methods
    // ===========================================================
    /**
     * Override to perform clean up of resources before shutdown. By default does nothing.
     */
    open fun onDetach(mapView: MapView?) {
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onKeyDown(keyCode: Int, event: KeyEvent?, mapView: MapView?): Boolean {
        return false
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onKeyUp(keyCode: Int, event: KeyEvent?, mapView: MapView?): Boolean {
        return false
    }

    /**
     * **You can prevent all(!) other Touch-related events from happening!**<br></br>
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onTouchEvent(event: MotionEvent, mapView: MapView?): Boolean {
        return false
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onTrackballEvent(event: MotionEvent?, mapView: MapView?): Boolean {
        return false
    }

    /** GestureDetector.OnDoubleTapListener  */
    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onDoubleTap(e: MotionEvent, mapView: MapView?): Boolean {
        return false
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onDoubleTapEvent(e: MotionEvent, mapView: MapView?): Boolean {
        return false
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView?): Boolean {
        return false
    }

    /** OnGestureListener  */
    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onDown(e: MotionEvent?, mapView: MapView?): Boolean {
        return false
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onFling(
        pEvent1: MotionEvent?, pEvent2: MotionEvent?,
        pVelocityX: Float, pVelocityY: Float, pMapView: MapView?
    ): Boolean {
        return false
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onLongPress(e: MotionEvent, mapView: MapView?): Boolean {
        return false
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onScroll(
        pEvent1: MotionEvent?, pEvent2: MotionEvent?,
        pDistanceX: Float, pDistanceY: Float, pMapView: MapView?
    ): Boolean {
        return false
    }

    open fun onShowPress(pEvent: MotionEvent?, pMapView: MapView?) {
        return
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    open fun onSingleTapUp(e: MotionEvent, mapView: MapView?): Boolean {
        return false
    }

    /**
     * Triggered on application lifecycle changes, assuming the mapview is triggered appropriately
     * related issue https://github.com/osmdroid/osmdroid/issues/823
     * https://github.com/osmdroid/osmdroid/issues/806
     *
     * @since 6.0.0
     */
    open fun onPause() {
    }

    /**
     * Triggered on application lifecycle changes, assuming the mapview is triggered appropriately
     * related issue https://github.com/osmdroid/osmdroid/issues/823
     * https://github.com/osmdroid/osmdroid/issues/806
     *
     * @since 6.0.0
     */
    open fun onResume() {
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    /**
     * Interface definition for overlays that contain items that can be snapped to (for example,
     * when the user invokes a zoom, this could be called allowing the user to snap the zoom to an
     * interesting point.)
     */
    interface Snappable {
        /**
         * Checks to see if the given x and y are close enough to an item resulting in snapping the
         * current action (e.g. zoom) to the item.
         *
         * @param x         The x in screen coordinates.
         * @param y         The y in screen coordinates.
         * @param snapPoint To be filled with the the interesting point (in screen coordinates) that is
         * closest to the given x and y. Can be untouched if not snapping.
         * @param mapView   The [MapView] that is requesting the snap. Use MapView.getProjection()
         * to convert between on-screen pixels and latitude/longitude pairs.
         * @return Whether or not to snap to the interesting point.
         */
        fun onSnapToItem(x: Int, y: Int, snapPoint: Point, mapView: IMapView?): Boolean
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private val sOrdinal = AtomicInteger()

        // From Google Maps API
        protected val SHADOW_X_SKEW: Float = -0.8999999761581421f
        protected const val SHADOW_Y_SCALE: Float = 0.5f

        // ===========================================================
        // Fields
        // ===========================================================
        private val mRect = Rect()
        @JvmStatic
        protected fun getSafeMenuId(): Int = sOrdinal.getAndIncrement()

        /**
         * Similar to [.getSafeMenuId], except this reserves a sequence of IDs of length
         * `count`. The returned number is the starting index of that sequential list.
         *
         * @return an integer suitable to be used as a menu identifier
         * @see .getSafeMenuId
         */
        @JvmStatic
        protected fun getSafeMenuIdSequence(count: Int): Int {
            return sOrdinal.getAndAdd(count)
        }

        /**
         * Convenience method to draw a Drawable at an offset. x and y are pixel coordinates. You can
         * find appropriate coordinates from latitude/longitude using the MapView.getProjection() method
         * on the MapView passed to you in draw(Canvas, MapView, boolean).
         *
         * @param shadow          If true, draw only the drawable's shadow. Otherwise, draw the drawable itself.
         * @param aMapOrientation
         */
        @JvmStatic
        @Synchronized
        protected fun drawAt(
            canvas: Canvas, drawable: Drawable,
            x: Int, y: Int, shadow: Boolean,
            aMapOrientation: Float
        ) {
            canvas.save()
            canvas.rotate(-aMapOrientation, x.toFloat(), y.toFloat())
            drawable.copyBounds(mRect)
            drawable.setBounds(mRect.left + x, mRect.top + y, mRect.right + x, mRect.bottom + y)
            drawable.draw(canvas)
            drawable.setBounds(mRect)
            canvas.restore()
        }
    }
}
