package org.osmdroid.events

/*
 * The listener interface for receiving map movement events. To process a map event, either implement
 * this interface or extend MapAdapter, then register with the MapView using
 * setMapListener.
 *
 * @author Theodore Hong
 */
interface MapListener {
    /*
     * Called when a map is scrolled.
     */
    fun onScroll(event: ScrollEvent): Boolean

    /*
     * Called when a map is zoomed.
     */
    fun onZoom(event: ZoomEvent): Boolean
}