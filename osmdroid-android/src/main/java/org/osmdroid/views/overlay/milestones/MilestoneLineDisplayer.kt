package org.osmdroid.views.overlay.milestones

import android.graphics.Canvas
import android.graphics.Paint
import org.osmdroid.views.overlay.LineDrawer

/**
 * Display lines between milestone steps
 *
 * @author Fabrice Fontaine
 * @since 6.0.3
 */
class MilestoneLineDisplayer(pPaint: Paint?) : MilestoneDisplayer(0.0, false) {
    private var mFirst = true

    /**
     * @since 6.2.0
     */
    private var mPreviousX: Long = 0
    private var mPreviousY: Long = 0

    private val mLineDrawer: LineDrawer = object : LineDrawer(256) {
        override fun flush() {
            super.flush()
            mFirst = true
        }
    }

    init {
        mLineDrawer.setPaint(pPaint)
    }

    override fun drawBegin(pCanvas: Canvas) {
        mLineDrawer.init()
        mLineDrawer.setCanvas(pCanvas)
        mFirst = true
    }

    /**
     * Overriding the "standard" milestone behavior (where we display something at each milestone)
     * Instead, we populate a line drawer that will connect the steps
     */
    override fun draw(pCanvas: Canvas, pStep: MilestoneStep) {
        val nextX = pStep.x
        val nextY = pStep.y
        if (mFirst) {
            mFirst = false
        } else if (mPreviousX != nextX || mPreviousY != nextY) {
            mLineDrawer.add(mPreviousX, mPreviousY)
            mLineDrawer.add(nextX, nextY)
        }
        mPreviousX = nextX
        mPreviousY = nextY
    }

    override fun drawEnd(pCanvas: Canvas) {
        mLineDrawer.end()
    }

    override fun draw(pCanvas: Canvas, pParameter: Any?) {
        // do nothing as we override the draw method that calls this one
    }
}
