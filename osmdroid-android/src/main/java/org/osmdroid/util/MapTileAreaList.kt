package org.osmdroid.util

/**
 * A list of areas of map tiles
 */
class MapTileAreaList : MapTileContainer, IterableWithSize<Long?> {
    val list: MutableList<MapTileArea> = ArrayList<MapTileArea>()

    override fun size(): Int {
        var size = 0
        for (area in this.list) {
            size += area.size()
        }
        return size
    }

    override fun iterator(): MutableIterator<Long?> {
        return object : MutableIterator<Long?> {
            private var mIndex = 0
            private var mCurrent: MutableIterator<Long?>? = null

            override fun hasNext(): Boolean {
                val current = this.current
                return current != null && current.hasNext()
            }

            override fun next(): Long {
                val result: Long = this.current!!.next()!!
                if (!this.current!!.hasNext()) {
                    mCurrent = null // in order to force the next item
                }
                return result
            }

            override fun remove() {
                throw UnsupportedOperationException()
            }

            val current: MutableIterator<Long?>?
                get() {
                    if (mCurrent != null) {
                        return mCurrent
                    }
                    if (mIndex < list.size) {
                        return list.get(mIndex++).iterator().also { mCurrent = it }
                    }
                    return null
                }
        }
    }

    override fun contains(pTileIndex: Long): Boolean {
        for (area in this.list) {
            if (area.contains(pTileIndex)) {
                return true
            }
        }
        return false
    }
}
