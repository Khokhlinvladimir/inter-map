package org.osmdroid.samplefragments.drawing

/**
 * A simple sample to plot markers with a long press. It's a bit of noise this in the class
 * that is used to help the osmdroid devs troubleshoot things.
 *
 *
 * Map replication is OFF for this sample (only viewable for numerically lower zoom levels (higher altitude))
 *
 * **Note**
 *
 *           't. We are leaving it up to you,
 * the developer using osmdroid to decide on what is right for your application. See
 * [https://github.com/osmdroid/osmdroid/pull/722](https://github.com/osmdroid/osmdroid/pull/722)
 * for more information and the discussion associated with this.
 *
 *
 * created on 11/19/2017.
 *
 * @author Alex O'Ree
 * @since 6.0.0
 */
class PressToPlotWithoutWrapping : PressToPlot() {
    override val sampleTitle: String?
        get() = "Long Press to Plot Marker without wrapping"

    public override fun addOverlays() {
        super.addOverlays()
        this.mMapView!!.setHorizontalMapRepetitionEnabled(false)
        this.mMapView!!.setVerticalMapRepetitionEnabled(false)
    }
}
