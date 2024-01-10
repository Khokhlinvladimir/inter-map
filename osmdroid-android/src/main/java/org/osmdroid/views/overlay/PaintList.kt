package org.osmdroid.views.overlay

import android.graphics.Paint

/**
 * Interface PaintList
 *
 * @author Matthias Dittmer
 */
interface PaintList {
    val paint: Paint?
    fun getPaint(pIndex: Int, pX0: Float, pY0: Float, pX1: Float, pY1: Float): Paint?
}