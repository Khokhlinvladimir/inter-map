package org.osmdroid.views.overlay.milestones

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * Displayer of `MilestoneStep`s as `Path`s
 * Created by Fabrice Fontaine on 22/12/2017.
 *
 * @since 6.0.0
 */
open class MilestonePathDisplayer(
    pInitialOrientation: Double, pFollowTrajectory: Boolean,
    private val mPath: Path, private val mPaint: Paint
) : MilestoneDisplayer(pInitialOrientation, pFollowTrajectory) {
    override fun draw(pCanvas: Canvas, pParameter: Any?) {
        pCanvas.drawPath(mPath, mPaint)
    }
}
