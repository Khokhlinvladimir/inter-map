// Created by plusminus on 23:18:23 - 02.10.2008
package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import org.osmdroid.util.RectL
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay.Snappable
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace
import kotlin.math.min

/**
 * Draws a list of [OverlayItem] as markers to a map. The item with the lowest index is drawn
 * as last and therefore the 'topmost' marker. It also gets checked for onTap first. This class is
 * generic, because you then you get your custom item-class passed back in onTap().
 *
 * @param <Item>
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Theodore Hong
 * @author Fred Eisele
</Item> */
abstract class ItemizedOverlay<Item : OverlayItem?>(pDefaultMarker: Drawable) : Overlay(), Snappable {
    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Constants
    // ===========================================================
    // ===========================================================
    // Fields
    // ===========================================================
    var drawnItemsLimit: Int = Int.Companion.MAX_VALUE
    protected val mDefaultMarker: Drawable
    private val mInternalItemList: ArrayList<Item?>
    private var mInternalItemDisplayedList: BooleanArray? = null
    private val mRect = Rect()
    private val mMarkerRect = Rect()
    private val mOrientedMarkerRect = Rect()
    private val mCurScreenCoords = Point()
    protected var mDrawFocusedItem: Boolean = true
    private var mFocusedItem: Item? = null
    private var mPendingFocusChangedEvent = false
    private var mOnFocusChangeListener: OnFocusChangeListener? = null

    private val itemRect = Rect()
    private val screenRect = Rect()

    // ===========================================================
    // Abstract methods
    // ===========================================================
    /**
     * Method by which subclasses create the actual Items. This will only be called from populate()
     * we'll cache them for later use.
     */
    protected abstract fun createItem(i: Int): Item?

    /**
     * The number of items in this overlay.
     */
    abstract fun size(): Int

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * Use [.ItemizedOverlay] instead
     */
    @Deprecated("")
    constructor(ctx: Context?, pDefaultMarker: Drawable) : this(pDefaultMarker)

    init {
        requireNotNull(pDefaultMarker) { "You must pass a default marker to ItemizedOverlay." }

        this.mDefaultMarker = pDefaultMarker

        mInternalItemList = ArrayList<Item?>()
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces (and supporting methods)
    // ===========================================================
    override fun onDetach(mapView: MapView?) {
        if (mDefaultMarker != null) {
            //release the bitmap
        }
    }

    /**
     * Draw a marker on each of our items. populate() must have been called first.<br></br>
     * <br></br>
     * The marker will be drawn twice for each Item in the Overlay--once in the shadow phase, skewed
     * and darkened, then again in the non-shadow phase. The bottom-center of the marker will be
     * aligned with the geographical coordinates of the Item.<br></br>
     * <br></br>
     * The order of drawing may be changed by overriding the getIndexToDraw(int) method. An item may
     * provide an alternate marker via its OverlayItem.getMarker(int) method. If that method returns
     * null, the default marker is used.<br></br>
     * <br></br>
     * The focused item is always drawn last, which puts it visually on top of the other items.<br></br>
     */
    override fun draw(canvas: Canvas, pj: Projection) {
        if (mPendingFocusChangedEvent && mOnFocusChangeListener != null) mOnFocusChangeListener!!.onFocusChanged(this, mFocusedItem)
        mPendingFocusChangedEvent = false

        val size = min(this.mInternalItemList.size, this.drawnItemsLimit)

        if (mInternalItemDisplayedList == null || mInternalItemDisplayedList!!.size != size) {
            mInternalItemDisplayedList = BooleanArray(size)
        }

        /* Draw in backward cycle, so the items with the least index are on the front. */
        for (i in size - 1 downTo 0) {
            val item = getItem(i)
            if (item == null) {
                continue
            }

            pj.toPixels(item.getPoint(), mCurScreenCoords)
            calculateItemRect(item, mCurScreenCoords, itemRect)

            mInternalItemDisplayedList!![i] = onDrawItem(canvas, item, mCurScreenCoords, pj)
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================
    /**
     * Utility method to perform all processing on a new ItemizedOverlay. Subclasses provide Items
     * through the createItem(int) method. The subclass should call this as soon as it has data,
     * before anything else gets called.
     */
    protected fun populate() {
        val size = size()
        mInternalItemList.clear()
        mInternalItemList.ensureCapacity(size)
        for (a in 0 until size) {
            mInternalItemList.add(createItem(a))
        }
        mInternalItemDisplayedList = null
    }

    /**
     * Returns the Item at the given index.
     *
     * @param position the position of the item to return
     * @return the Item of the given index, or null if not found at position
     */
    fun getItem(position: Int): Item? {
        try {
            return mInternalItemList.get(position)
        } catch (e: IndexOutOfBoundsException) {
            return null
        }
    }

    /**
     * Draws an item located at the provided screen coordinates to the canvas.
     *
     * @param canvas          what the item is drawn upon
     * @param item            the item to be drawn
     * @param curScreenCoords
     * @param pProjection
     * @return true if the item was actually drawn
     */
    protected fun onDrawItem(
        canvas: Canvas, item: Item?, curScreenCoords: Point?,
        pProjection: Projection
    ): Boolean {
        val state = (if (mDrawFocusedItem && (mFocusedItem === item))
            OverlayItem.Companion.ITEM_STATE_FOCUSED_MASK
        else
            0)
        val marker = item!!.getMarker(state) ?: getDefaultMarker(state)
        val hotspot = item.getMarkerHotspot()

        boundToHotspot(marker, hotspot)

        val x = mCurScreenCoords.x
        val y = mCurScreenCoords.y

        marker.copyBounds(mRect)
        mMarkerRect.set(mRect)
        mRect.offset(x, y)
        RectL.Companion.getBounds(mRect, x, y, pProjection.orientation.toDouble(), mOrientedMarkerRect)
        val displayed = Rect.intersects(mOrientedMarkerRect, canvas.getClipBounds())
        if (displayed) {
            if (pProjection.orientation != 0f) { // optimization: step 1/2
                canvas.save()
                canvas.rotate(-pProjection.orientation, x.toFloat(), y.toFloat())
            }
            marker.setBounds(mRect)
            marker.draw(canvas)
            if (pProjection.orientation != 0f) { // optimization: step 2/2
                canvas.restore()
            }
            marker.setBounds(mMarkerRect)
        }

        return displayed
    }

    val displayedItems: MutableList<Item?>
        /**
         * Get the list of all the items that are currently drawn on the canvas.
         * The obvious use case is a "share" or "export" button on a map, restricted to what is displayed.
         * The order of the items is kept
         *
         * @return the items that have actually been drawn
         * @since 5.6.7
         */
        get() {
            val result: MutableList<Item?> = ArrayList<Item?>()
            if (mInternalItemDisplayedList == null) {
                return result
            }
            for (i in mInternalItemDisplayedList!!.indices) {
                if (mInternalItemDisplayedList!![i]) {
                    result.add(getItem(i))
                }
            }
            return result
        }

    protected fun getDefaultMarker(state: Int): Drawable {
        OverlayItem.Companion.setState(mDefaultMarker, state)
        return mDefaultMarker
    }

    /**
     * See if a given hit point is within the bounds of an item's marker. Override to modify the way
     * an item is hit tested. The hit point is relative to the marker's bounds. The default
     * implementation just checks to see if the hit point is within the touchable bounds of the
     * marker.
     *
     * @param item   the item to hit test
     * @param marker the item's marker
     * @param hitX   x coordinate of point to check
     * @param hitY   y coordinate of point to check
     * @return true if the hit point is within the marker
     */
    protected fun hitTest(
        item: Item?, marker: Drawable, hitX: Int,
        hitY: Int
    ): Boolean {
        return marker.getBounds().contains(hitX, hitY)
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView?): Boolean {
        val size = this.size()
        val eventX = Math.round(e.getX())
        val eventY = Math.round(e.getY())
        for (i in 0 until size) {
            if (isEventOnItem(getItem(i), eventX, eventY, mapView)) {
                if (onTap(i)) {
                    // We got a response so consume the event
                    return true
                }
            }
        }

        return super.onSingleTapConfirmed(e, mapView)
    }

    /**
     * Override this method to handle a "tap" on an item. This could be from a touchscreen tap on an
     * onscreen Item, or from a trackball click on a centered, selected Item. By default, does
     * nothing and returns false.
     *
     * @return true if you handled the tap, false if you want the event that generated it to pass to
     * other overlays.
     */
    protected open fun onTap(index: Int): Boolean {
        return false
    }

    /**
     * Set whether or not to draw the focused item. The default is to draw it, but some clients may
     * prefer to draw the focused item themselves.
     */
    fun setDrawFocusedItem(drawFocusedItem: Boolean) {
        mDrawFocusedItem = drawFocusedItem
    }

    var focus: Item?
        /**
         * @return the currently-focused item, or null if no item is currently focused.
         */
        get() = mFocusedItem
        /**
         * If the given Item is found in the overlay, force it to be the current focus-bearer. Any
         * registered [OnFocusChangeListener] will be notified. This does not move the map, so if
         * the Item isn't already centered, the user may get confused. If the Item is not found, this is
         * a no-op. You can also pass null to remove focus.
         */
        set(item) {
            mPendingFocusChangedEvent = item !== mFocusedItem
            mFocusedItem = item
        }

    /**
     * Adjusts a drawable's bounds so that (0,0) is a pixel in the location described by the hotspot
     * parameter. Useful for "pin"-like graphics. For convenience, returns the same drawable that
     * was passed in.
     *
     * @param marker  the drawable to adjust
     * @param hotspot the hotspot for the drawable
     * @return the same drawable that was passed in.
     */
    protected fun boundToHotspot(marker: Drawable, hotspot: HotspotPlace?): Drawable {
        var hotspot = hotspot
        if (hotspot == null) {
            hotspot = HotspotPlace.BOTTOM_CENTER
        }
        val markerWidth = marker.getIntrinsicWidth()
        val markerHeight = marker.getIntrinsicHeight()
        val offsetX: Int
        val offsetY: Int
        when (hotspot) {
            HotspotPlace.NONE, HotspotPlace.LEFT_CENTER, HotspotPlace.UPPER_LEFT_CORNER, HotspotPlace.LOWER_LEFT_CORNER -> offsetX = 0
            HotspotPlace.CENTER, HotspotPlace.BOTTOM_CENTER, HotspotPlace.TOP_CENTER -> offsetX = -markerWidth / 2
            HotspotPlace.RIGHT_CENTER, HotspotPlace.UPPER_RIGHT_CORNER, HotspotPlace.LOWER_RIGHT_CORNER -> offsetX = -markerWidth
            else -> offsetX = 0
        }
        when (hotspot) {
            HotspotPlace.NONE, HotspotPlace.TOP_CENTER, HotspotPlace.UPPER_LEFT_CORNER, HotspotPlace.UPPER_RIGHT_CORNER -> offsetY = 0
            HotspotPlace.CENTER, HotspotPlace.RIGHT_CENTER, HotspotPlace.LEFT_CENTER -> offsetY = -markerHeight / 2
            HotspotPlace.BOTTOM_CENTER, HotspotPlace.LOWER_RIGHT_CORNER, HotspotPlace.LOWER_LEFT_CORNER -> offsetY = -markerHeight
            else -> offsetY = 0
        }
        marker.setBounds(offsetX, offsetY, offsetX + markerWidth, offsetY + markerHeight)
        return marker
    }

    /**
     * Calculates the screen rect for an item.
     *
     * @param item
     * @param coords
     * @param reuse
     * @return
     */
    protected fun calculateItemRect(item: Item?, coords: Point, reuse: Rect?): Rect {
        val out = if (reuse != null) reuse else Rect()

        var hotspot = item!!.getMarkerHotspot()
        if (hotspot == null) {
            hotspot = HotspotPlace.BOTTOM_CENTER
        }

        val state = (if (mDrawFocusedItem && (mFocusedItem === item)) OverlayItem.Companion.ITEM_STATE_FOCUSED_MASK else 0)
        val marker = item.getMarker(state) ?: getDefaultMarker(state)
        val itemWidth = marker.getIntrinsicWidth()
        val itemHeight = marker.getIntrinsicHeight()

        when (hotspot) {
            HotspotPlace.NONE -> out.set(
                coords.x - itemWidth / 2,
                coords.y - itemHeight / 2,
                coords.x + itemWidth / 2,
                coords.y + itemHeight / 2
            )

            HotspotPlace.CENTER -> out.set(
                coords.x - itemWidth / 2,
                coords.y - itemHeight / 2,
                coords.x + itemWidth / 2,
                coords.y + itemHeight / 2
            )

            HotspotPlace.BOTTOM_CENTER -> out.set(
                coords.x - itemWidth / 2,
                coords.y - itemHeight,
                coords.x + itemWidth / 2,
                coords.y
            )

            HotspotPlace.TOP_CENTER -> out.set(
                coords.x - itemWidth / 2,
                coords.y,
                coords.x + itemWidth / 2,
                coords.y + itemHeight
            )

            HotspotPlace.RIGHT_CENTER -> out.set(
                coords.x - itemWidth,
                coords.y - itemHeight / 2,
                coords.x,
                coords.y + itemHeight / 2
            )

            HotspotPlace.LEFT_CENTER -> out.set(
                coords.x,
                coords.y - itemHeight / 2,
                coords.x + itemWidth,
                coords.y + itemHeight / 2
            )

            HotspotPlace.UPPER_RIGHT_CORNER -> out.set(
                coords.x - itemWidth,
                coords.y,
                coords.x,
                coords.y + itemHeight
            )

            HotspotPlace.LOWER_RIGHT_CORNER -> out.set(
                coords.x - itemWidth,
                coords.y - itemHeight,
                coords.x,
                coords.y
            )

            HotspotPlace.UPPER_LEFT_CORNER -> out.set(
                coords.x,
                coords.y,
                coords.x + itemWidth,
                coords.y + itemHeight
            )

            HotspotPlace.LOWER_LEFT_CORNER -> out.set(
                coords.x,
                coords.y - itemHeight,
                coords.x + itemWidth,
                coords.y
            )
        }

        return out
    }

    fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        mOnFocusChangeListener = l
    }

    interface OnFocusChangeListener {
        fun onFocusChanged(overlay: ItemizedOverlay<*>?, newFocus: OverlayItem?)
    }


    protected fun isEventOnItem(pItem: Item?, pEventX: Int, pEventY: Int, pMapView: MapView?): Boolean {
        if (pItem == null) {
            return false
        }
        pMapView ?: return false
        pMapView.projection.toPixels(pItem.getPoint(), mCurScreenCoords)
        val state = (if (mDrawFocusedItem && (mFocusedItem === pItem)) OverlayItem.Companion.ITEM_STATE_FOCUSED_MASK else 0)
        var marker = pItem.getMarker(state)
        if (marker == null) {
            marker = getDefaultMarker(state)
        }
        boundToHotspot(marker, pItem.getMarkerHotspot())
        marker.copyBounds(mRect)
        mRect.offset(mCurScreenCoords.x, mCurScreenCoords.y)
        RectL.Companion.getBounds(mRect, mCurScreenCoords.x, mCurScreenCoords.y, -pMapView.getMapOrientation().toDouble(), mOrientedMarkerRect)
        return mOrientedMarkerRect.contains(pEventX, pEventY)
    }
}
