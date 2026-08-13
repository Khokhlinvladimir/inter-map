package org.osmdroid.views.overlay.milestones

import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * Displayer of `MilestoneStep`s as `Bitmap`s
 * Created by Fabrice Fontaine on 22/12/2017.
 *
 * @since 6.0.0
 */
open class MilestoneBitmapDisplayer(
    pInitialOrientation: Double, pFollowTrajectory: Boolean,
    private val mBitmap: Bitmap, private val mOffsetX: Int, private val mOffsetY: Int
) : MilestoneDisplayer(pInitialOrientation, pFollowTrajectory) {
    override fun draw(pCanvas: Canvas, pParameter: Any?) {
        pCanvas.drawBitmap(mBitmap, -mOffsetX.toFloat(), -mOffsetY.toFloat(), null)
    }
}
