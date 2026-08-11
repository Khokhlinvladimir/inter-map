package org.osmdroid.samplefragments.data

/**
 * created on 1/7/2017.
 *
 * @author Alex O'Ree
 */
class SampleIISTrackerMotionTrails : IISTrackerBase() {
    override val sampleTitle: String?
        get() = "Internal Space Station Tracker with motion trails"

    override val isMotionTrail: Boolean
        get() = true
}
