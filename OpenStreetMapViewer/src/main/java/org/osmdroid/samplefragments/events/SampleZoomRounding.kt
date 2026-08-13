package org.osmdroid.samplefragments.events

import org.osmdroid.samplefragments.BaseSampleFragment

/**
 * @author Fabrice Fontaine
 * @since 6.0.2
 * cf. https://github.com/osmdroid/osmdroid/issues/944
 */
class SampleZoomRounding : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    override fun addOverlays() {
        super.addOverlays()
        mMapView!!.setZoomRounding(true)
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private const val SAMPLE_TITLE: String = "Zoom Rounding"
    }
}
