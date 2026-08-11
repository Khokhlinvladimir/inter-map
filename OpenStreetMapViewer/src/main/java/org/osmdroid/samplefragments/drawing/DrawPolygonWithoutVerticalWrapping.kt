package org.osmdroid.samplefragments.drawing

/**
 * Map replication is OFF for this sample (only viewable for numerically lower zoom levels (higher altitude))
 * Created by Maradox on 11/26/17.
 */
class DrawPolygonWithoutVerticalWrapping : DrawPolygon() {
    override val sampleTitle: String?
        get() = "Draw a polygon on screen without vertical wrapping"


    public override fun addOverlays() {
        super.addOverlays()
        this.mMapView!!.setHorizontalMapRepetitionEnabled(true)
        this.mMapView!!.setVerticalMapRepetitionEnabled(false)
    }
}
