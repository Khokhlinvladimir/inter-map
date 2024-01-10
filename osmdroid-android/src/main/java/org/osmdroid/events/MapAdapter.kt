package org.osmdroid.events

/**
 * An abstract adapter class for receiving map events. The methods in this class are empty.
 * This class exists as convenience for creating listener objects.
 */

abstract class MapAdapter : MapListener {
    override fun onScroll(event: ScrollEvent): Boolean {
        // do nothing
        return false
    }

    override fun onZoom(event: ZoomEvent): Boolean {
        // do nothing
        return false
    }
}