package org.osmdroid.events

import org.osmdroid.views.MapView

/*
 * The event generated when a map has finished zooming to the level <code>zoomLevel</code>.
 *
 * @author Theodore Hong
 */
class ZoomEvent(/*
     * Return the map which generated this event.
     */var source: MapView,
                /*
* Return the zoom level zoomed to.
* Used to be an int, but is a double since 6.0
*/var zoomLevel: Double
) : MapEvent {
    override fun toString(): String {
        return "ZoomEvent [source=$source, zoomLevel=$zoomLevel]"
    }
}