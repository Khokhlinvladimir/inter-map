package org.osmdroid.views.overlay.advancedpolyline

import android.graphics.Paint
import org.osmdroid.views.overlay.PaintList

/**
 * A [PaintList] with always the same color
 *
 * @author Fabrice Fontaine
 * @since 6.2.0
 */
class MonochromaticPaintList(override val paint: Paint?) : PaintList {
    override fun getPaint(
        pIndex: Int,
        pX0: Float, pY0: Float, pX1: Float, pY1: Float
    ): Paint? {
        return null
    }
}
