package org.osmdroid.util

import android.graphics.Path

class PathBuilder(private val mPath: Path) : PointAccepter {
    private val mLatestPoint = PointL()
    private var mFirst = false

    override fun init() {
        mFirst = true
    }

    override fun add(pX: Long, pY: Long) {
        if (mFirst) {
            mFirst = false
            mPath.moveTo(pX.toFloat(), pY.toFloat())
            mLatestPoint.set(pX, pY)
        } else if (mLatestPoint.x != pX || mLatestPoint.y != pY) {
            mPath.lineTo(pX.toFloat(), pY.toFloat())
            mLatestPoint.set(pX, pY)
        }
    }

    override fun end() {
    }
}
