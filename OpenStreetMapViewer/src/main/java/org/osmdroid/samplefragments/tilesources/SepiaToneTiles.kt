package org.osmdroid.samplefragments.tilesources

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.samplefragments.BaseSampleFragment

/**
 * created on 1/8/2017.
 *
 * @author Alex O'Ree
 */
class SepiaToneTiles : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Sepia tone tiles"

    public override fun addOverlays() {
        super.addOverlays()
        val matrixA = ColorMatrix()
        // making image B&W
        matrixA.setSaturation(0f)

        val matrixB = ColorMatrix()
        // applying scales for RGB color values
        matrixB.setScale(1f, .95f, .82f, 1.0f)
        matrixA.setConcat(matrixB, matrixA)

        val filter = ColorMatrixColorFilter(matrixA)

        mMapView!!.getOverlayManager().getTilesOverlay().setColorFilter(filter)
    }
}
