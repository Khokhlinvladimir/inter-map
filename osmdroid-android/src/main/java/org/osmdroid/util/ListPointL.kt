package org.osmdroid.util

class ListPointL : Iterable<PointL?> {
    private val mList: MutableList<PointL> = ArrayList<PointL>()
    private var mSize = 0

    fun clear() {
        mSize = 0
    }

    fun size(): Int {
        return mSize
    }

    fun get(pIndex: Int): PointL? {
        return mList.get(pIndex)
    }

    fun add(pX: Long, pY: Long) {
        val point: PointL
        if (mSize >= mList.size) {
            point = PointL()
            mList.add(point)
        } else {
            point = mList.get(mSize)
        }
        mSize++
        point.set(pX, pY)
    }

    override fun iterator(): MutableIterator<PointL?> {
        return object : MutableIterator<PointL?> {
            private var mIndex = 0

            override fun hasNext(): Boolean {
                return mIndex < mSize
            }

            override fun next(): PointL? {
                return get(mIndex++)
            }

            override fun remove() {
                throw UnsupportedOperationException()
            }
        }
    }
}
