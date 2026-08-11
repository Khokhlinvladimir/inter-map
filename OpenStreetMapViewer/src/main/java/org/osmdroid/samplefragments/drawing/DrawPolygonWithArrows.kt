package org.osmdroid.samplefragments.drawing

/**
 * created on 26/12/2017.
 *
 * @author Fabrice Fontaine
 * @since 6.0.0
 */
class DrawPolygonWithArrows : DrawPolygon() {
    override val sampleTitle: String?
        get() = "Draw a polygon with arrows"

    public override fun addOverlays() {
        super.addOverlays()
        paint!!.withArrows = true
    }
}
