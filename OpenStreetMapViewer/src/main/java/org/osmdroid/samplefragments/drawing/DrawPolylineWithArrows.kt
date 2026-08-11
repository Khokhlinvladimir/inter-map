package org.osmdroid.samplefragments.drawing

/**
 * created on 11/28/2017.
 * https://github.com/osmdroid/osmdroid/issues/791
 *
 * @author Alex O'Ree
 */
class DrawPolylineWithArrows : SampleDrawPolyline() {
    override val sampleTitle: String
        get() = "Draw a polyline with arrows"

    public override fun addOverlays() {
        super.addOverlays()
        paint!!.withArrows = true
    }
}
