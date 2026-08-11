package org.osmdroid.samplefragments.data

/**
 * Calls a public rest endpoint for the current location of the IIS, but's an icon at that location
 * and centers the map at that location.
 *
 *
 * http://api.open-notify.org/iss-now.json
 *
 *
 *
 *
 * created on 1/6/2017.
 *
 * @author Alex O'Ree
 * @since 5.6.3
 */
class SampleIISTracker : IISTrackerBase() {
    override val sampleTitle: String?
        get() = "Internal Space Station Tracker (Network connection required)"

    override val isMotionTrail: Boolean
        get() = false
}
