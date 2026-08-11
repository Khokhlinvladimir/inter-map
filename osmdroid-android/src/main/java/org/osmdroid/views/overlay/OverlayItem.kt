// Created by plusminus on 00:02:58 - 03.10.2008
package org.osmdroid.views.overlay

import android.R
import android.graphics.Point
import android.graphics.drawable.Drawable
import org.osmdroid.api.IGeoPoint

/**
 * An Item that can be displayed in a [ItemizedOverlay] or [ItemizedIconOverlay].
 *
 *
 * Immutable class describing a GeoPoint with a Title and a Description.
 *
 * @author Nicolas Gramlich
 * @author Theodore Hong
 * @author Fred Eisele
 */
open class OverlayItem(
    protected val mUid: String?,
    protected val mTitle: String?,
    protected val mSnippet: String?,
    protected val mGeoPoint: IGeoPoint?,
) {
    /**
     * Indicates a hotspot for an area. This is where the origin (0,0) of a point will be located
     * relative to the area. In otherwords this acts as an offset. NONE indicates that no adjustment
     * should be made.
     */
    enum class HotspotPlace {
        NONE, CENTER, BOTTOM_CENTER, TOP_CENTER, RIGHT_CENTER, LEFT_CENTER, UPPER_RIGHT_CORNER, LOWER_RIGHT_CORNER, UPPER_LEFT_CORNER, LOWER_LEFT_CORNER
    }

    protected var mMarker: Drawable? = null
    protected var mHotspotPlace: HotspotPlace? = null

    // ===========================================================
    // Constructors
    // ===========================================================
    /**
     * @param aTitle    this should be **singleLine** (no `'\n'` )
     * @param aSnippet  a **multiLine** description ( `'\n'` possible)
     * @param aGeoPoint
     */
    constructor(aTitle: String?, aSnippet: String?, aGeoPoint: IGeoPoint?) : this(null, aTitle, aSnippet, aGeoPoint)

    open fun getUid(): String? = mUid

    open fun getTitle(): String? = mTitle

    open fun getSnippet(): String? = mSnippet

    open fun getPoint(): IGeoPoint? = mGeoPoint

    /*
      * (copied from Google API docs) Returns the marker that should be used when drawing this item
      * on the map. A null value means that the default marker should be drawn. Different markers can
      * be returned for different states. The different markers can have different bounds. The
      * default behavior is to call {@link setState(android.graphics.drawable.Drawable, int)} on the
      * overlay item's marker, if it exists, and then return it.
      *
      * @param stateBitset The current state.
      *
      * @return The marker for the current state, or null if the default marker for the overlay
      * should be used.
      */
    fun getMarker(stateBitset: Int): Drawable? {
        // marker not specified
        if (mMarker == null) {
            return null
        }

        // set marker state appropriately
        setState(mMarker!!, stateBitset)
        return mMarker
    }

    fun setMarker(marker: Drawable?) {
        mMarker = marker
    }

    fun setMarkerHotspot(place: HotspotPlace?) {
        mHotspotPlace = place ?: HotspotPlace.BOTTOM_CENTER
    }

    fun getMarkerHotspot(): HotspotPlace? = mHotspotPlace

    fun getDrawable(): Drawable? = mMarker

    fun getWidth(): Int = mMarker!!.intrinsicWidth

    fun getHeight(): Int = mMarker!!.intrinsicHeight // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        const val ITEM_STATE_FOCUSED_MASK: Int = 4
        const val ITEM_STATE_PRESSED_MASK: Int = 1
        const val ITEM_STATE_SELECTED_MASK: Int = 2

        protected val DEFAULT_MARKER_SIZE: Point = Point(26, 94)

        // ===========================================================
        // Methods from SuperClass/Interfaces
        // ===========================================================
        // ===========================================================
        // Methods
        // ===========================================================
        /*
     * (copied from the Google API docs) Sets the state of a drawable to match a given state bitset.
     * This is done by converting the state bitset bits into a state set of R.attr.state_pressed,
     * R.attr.state_selected and R.attr.state_focused attributes, and then calling {@link
     * Drawable.setState(int[])}.
     */
        @JvmStatic
        fun setState(drawable: Drawable, stateBitset: Int) {
            val states = IntArray(3)
            var index = 0
            if ((stateBitset and ITEM_STATE_PRESSED_MASK) > 0) states[index++] = R.attr.state_pressed
            if ((stateBitset and ITEM_STATE_SELECTED_MASK) > 0) states[index++] = R.attr.state_selected
            if ((stateBitset and ITEM_STATE_FOCUSED_MASK) > 0) states[index++] = R.attr.state_focused

            drawable.setState(states)
        }
    }
}
