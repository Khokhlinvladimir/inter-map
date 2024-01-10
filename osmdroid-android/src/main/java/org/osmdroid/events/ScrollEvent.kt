package org.osmdroid.events

import org.osmdroid.views.MapView

/*
 * The event generated when a map has finished scrolling to the coordinates (<code>x</code>,<code>y</code>).
 *
 * note coordinates represent integer based scroll coordinates, not latitude/longitude. They are
 * subject to integer overflow and will be revisited in a later version of osmdroid to use long values.
 *
 * In some cases, osmdroid will always send a 0,0 coordinate.
 * As such, this event is more useful to be notified when the map moves, but not really where it ended up.
 * Use other functions in {@link MapView#getBoundingBox()} and {@link MapView#getMapCenter()} for that.
 * @author Theodore Hong
 */
class ScrollEvent
/**
 * note coordinates represent integer based scroll coordinates, not latitude/longitude. They are
 * subject to integer overflow and will be revisited in a later version of osmdroid to use long values.
 * In some cases, osmdroid will always send a 0,0 coordinate.
 * As such, this event is more useful to be notified when the map moves, but not really where it ended up.
 * Use other functions in [MapView.getBoundingBox] and [MapView.getMapCenter] for that.
 *
 * @param source
 * @param x
 * @param y
 */(
        /**
         * Return the map which generated this event.
         */
        var source: MapView,
        /**
         * Return the x-coordinate scrolled to.
         * note coordinates represent integer based scroll coordinates, not latitude/longitude. They are
         * subject to integer overflow and will be revisited in a later version of osmdroid to use long values.
         * In some cases, osmdroid will always send a 0,0 coordinate.
         * As such, this event is more useful to be notified when the map moves, but not really where it ended up.
         * Use other functions in [MapView.getBoundingBox] and [MapView.getMapCenter] for that.
         */
        var x: Int,
        /**
         * note coordinates represent integer based scroll coordinates, not latitude/longitude. They are
         * subject to integer overflow and will be revisited in a later version of osmdroid to use long values.
         * Return the y-coordinate scrolled to. In some cases, osmdroid will always send a 0,0 coordinate.
         * As such, this event is more useful to be notified when the map moves, but not really where it ended up.
         * Use other functions in [MapView.getBoundingBox] and [MapView.getMapCenter] for that.
         */
        var y: Int
) : MapEvent {
    override fun toString(): String {
        return "ScrollEvent [source=$source, x=$x, y=$y]"
    }
}