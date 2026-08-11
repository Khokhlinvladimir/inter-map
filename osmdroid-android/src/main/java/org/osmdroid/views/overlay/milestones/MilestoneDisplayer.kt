package org.osmdroid.views.overlay.milestones

import android.graphics.Canvas

/**
 * Displayer of `MilestoneStep`s
 * Created by Fabrice Fontaine on 22/12/2017.
 *
 * @since 6.0.0
 */
abstract class MilestoneDisplayer(
    /**
     * Initial orientation (in degrees) of the milestone display.
     * For instance, if we're talking about bitmaps,
     * a "up arrow" would use 90 and a "right arrow" would use 0.
     */
    private val mInitialOrientation: Double,
    /**
     * "Should we follow the trajectory?"
     * For instance, if we're talking about bitmaps,
     * an arrow would use true - in order to follow the polyline's trajectory,
     * and a work-in-progress logo would use false - in order to display always the same orientation
     * of logo, regardless of the polyline's trajectory
     */
    private val mFollowTrajectory: Boolean
) {
    open fun draw(pCanvas: Canvas, pStep: MilestoneStep) {
        val orientation = mInitialOrientation + (if (mFollowTrajectory) pStep.orientation else 0.0)
        pCanvas.save()
        pCanvas.rotate(orientation.toFloat(), pStep.x.toFloat(), pStep.y.toFloat())
        pCanvas.translate(pStep.x.toFloat(), pStep.y.toFloat())
        draw(pCanvas, pStep.`object`)
        pCanvas.restore()
    }

    /**
     * Draw on pixel (0,0) with no rotation
     */
    protected abstract fun draw(pCanvas: Canvas, pParameter: Any?)

    /**
     * @since 6.0.2
     */
    open fun drawBegin(pCanvas: Canvas) {
    }

    /**
     * @since 6.0.2
     */
    open fun drawEnd(pCanvas: Canvas) {
    }
}
