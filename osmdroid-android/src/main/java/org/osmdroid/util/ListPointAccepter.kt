package org.osmdroid.util

/**
 * A [PointAccepter] that builds a [List] of [PointL] as a list of long, long
 *
 * @author Fabrice Fontaine
 * @since 6.2.0
 */
class ListPointAccepter(private val mRemoveConsecutiveDuplicates: Boolean) : PointAccepter {
    val list: MutableList<Long?> = ArrayList<Long?>()
    private val mLatestPoint = PointL()
    private var mFirst = false

    override fun init() {
        list.clear()
        mFirst = true
    }

    override fun add(pX: Long, pY: Long) {
        if (!mRemoveConsecutiveDuplicates) {
            list.add(pX)
            list.add(pY)
            return
        }
        if (mFirst) {
            mFirst = false
            list.add(pX)
            list.add(pY)
            mLatestPoint.set(pX, pY)
        } else if (mLatestPoint.x != pX || mLatestPoint.y != pY) {
            list.add(pX)
            list.add(pY)
            mLatestPoint.set(pX, pY)
        }
    }

    override fun end() {
    }
}
