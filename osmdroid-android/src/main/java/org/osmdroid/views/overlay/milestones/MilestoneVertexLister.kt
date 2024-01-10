package org.osmdroid.views.overlay.milestones

class MilestoneVertexLister : MilestoneLister() {
    private var mLatestOrientation = 0.0
    private var mLatestX: Long = 0
    private var mLatestY: Long = 0
    private var mIndex = 0
    override fun init() {
        super.init()
        mIndex = 0
    }

    override fun add(x0: Long, y0: Long, x1: Long, y1: Long) {
        mLatestOrientation = getOrientation(x0, y0, x1, y1)
        innerAdd(x0, y0, mIndex++)
        mLatestX = x1
        mLatestY = y1
    }

    override fun end() {
        super.end()
        innerAdd(mLatestX, mLatestY, -mIndex) // how do we know if it's the last vertex? If it's negative!
    }

    private fun innerAdd(pX: Long, pY: Long, pIndex: Int) {
        add(MilestoneStep(pX, pY, mLatestOrientation, pIndex))
    }
}