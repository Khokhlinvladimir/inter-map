package org.osmdroid.util


class Delay {
    private val mDurations: LongArray?
    private var mDuration: Long = 0
    private var mNextTime: Long = 0
    private var mIndex = 0

    constructor(pDuration: Long) {
        mDurations = null
        mDuration = pDuration
        next()
    }

    constructor(pDurations: LongArray) {
        require(!(pDurations == null || pDurations.size == 0))
        mDurations = pDurations
        next()
    }

    fun next(): Long {
        val duration: Long
        if (mDurations == null) {
            duration = mDuration
        } else {
            duration = mDurations[mIndex]
            if (mIndex < mDurations.size - 1) {
                mIndex++
            }
        }
        mNextTime = now() + duration
        return duration
    }

    fun shouldWait(): Boolean {
        return now() < mNextTime
    }

    private fun now(): Long {
        return System.nanoTime() / 1000000L
    }
}
