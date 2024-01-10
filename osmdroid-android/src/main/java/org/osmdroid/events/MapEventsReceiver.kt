package org.osmdroid.events

import org.osmdroid.util.GeoPoint

/**
 * Interface for objects that need to handle map events thrown by a MapEventsOverlay.
 *
 * @author M.Kergall
 * @see MapEventsOverlay
 */
interface MapEventsReceiver {
    /**
     * @param p the position where the event occurred.
     * @return true if the event has been "consumed" and should not be handled by other objects.
     */
    fun singleTapConfirmedHelper(p: GeoPoint?): Boolean

    /**
     * @param p the position where the event occurred.
     * @return true if the event has been "consumed" and should not be handled by other objects.
     */
    fun longPressHelper(p: GeoPoint?): Boolean
}