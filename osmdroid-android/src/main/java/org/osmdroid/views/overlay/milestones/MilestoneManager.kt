package org.osmdroid.views.overlay.milestones

import android.graphics.Canvas
import org.osmdroid.util.PointAccepter

class MilestoneManager(private val mLister: MilestoneLister, private val mDisplayer: MilestoneDisplayer) : PointAccepter {
    fun draw(pCanvas: Canvas?) {
        mDisplayer.drawBegin(pCanvas)
        for (step in mLister.milestones) {
            mDisplayer.draw(pCanvas, step)
        }
        mDisplayer.drawEnd(pCanvas)
    }

    override fun init() {
        mLister.init()
    }

    override fun add(pX: Long, pY: Long) {
        mLister.add(pX, pY)
    }

    override fun end() {
        mLister.end()
    }

    fun setDistances(pDistances: DoubleArray?) {
        mLister.setDistances(pDistances)
    }
}