package org.osmdroid.util

/**
 * An optimized list of map tile indices
 */
class MapTileList : MapTileContainer {
    private var mTileIndices: LongArray? = null
    var size: Int = 0
        private set

    fun clear() {
        this.size = 0
    }

    fun get(pIndex: Int): Long {
        return mTileIndices!![pIndex]
    }

    fun put(pTileIndex: Long) {
        ensureCapacity(this.size + 1)
        mTileIndices!![this.size++] = pTileIndex
    }

    /**
     * @since 6.0.2
     */
    fun put(pZoom: Int, pLeft: Int, pTop: Int, pRight: Int, pBottom: Int) {
        val max = 1 shl pZoom
        val spanX = (pRight - pLeft + 1) + (if (pRight < pLeft) max else 0)
        val spanY = (pBottom - pTop + 1) + (if (pBottom < pTop) max else 0)
        ensureCapacity(this.size + spanX * spanY)
        for (i in 0 until spanX) {
            for (j in 0 until spanY) {
                val x = (pLeft + i) % max
                val y = (pTop + j) % max
                put(MapTileIndex.getTileIndex(pZoom, x, y))
            }
        }
    }

    /**
     * @since 6.0.2
     */
    fun put(pZoom: Int) {
        val max = 1 shl pZoom
        put(pZoom, 0, 0, max - 1, max - 1)
    }

    fun ensureCapacity(pCapacity: Int) {
        if (pCapacity == 0) {
            return
        }
        if (mTileIndices != null && mTileIndices!!.size >= pCapacity) {
            return
        }
        synchronized(this) {
            val tmp = LongArray(pCapacity)
            if (mTileIndices != null) {
                System.arraycopy(mTileIndices, 0, tmp, 0, mTileIndices!!.size)
            }
            mTileIndices = tmp
        }
    }

    override fun contains(pTileIndex: Long): Boolean {
        if (mTileIndices == null) {
            return false
        }
        for (i in 0 until this.size) {
            if (mTileIndices!![i] == pTileIndex) {
                return true
            }
        }
        return false
    }

    /**
     * @since 6.0.0
     */
    fun toArray(): LongArray {
        val result = LongArray(this.size)
        if (mTileIndices != null) {
            System.arraycopy(mTileIndices, 0, result, 0, this.size)
        }
        return result
    }
}
