package org.osmdroid.util

import android.graphics.Rect
import kotlin.math.min

/**
 * An area of map tiles.
 */
class MapTileArea : MapTileContainer, IterableWithSize<Long?> {
    var zoom: Int = 0
        private set
    var left: Int = 0
        private set
    var top: Int = 0
        private set
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    private var mMapTileUpperBound = 0

    fun set(pZoom: Int, pLeft: Int, pTop: Int, pRight: Int, pBottom: Int): MapTileArea {
        this.zoom = pZoom
        mMapTileUpperBound = 1 shl this.zoom
        this.width = computeSize(pLeft, pRight)
        this.height = computeSize(pTop, pBottom)
        this.left = cleanValue(pLeft)
        this.top = cleanValue(pTop)
        return this
    }

    fun set(pZoom: Int, pRect: Rect): MapTileArea {
        return set(pZoom, pRect.left, pRect.top, pRect.right, pRect.bottom)
    }

    fun set(pArea: MapTileArea): MapTileArea {
        if (pArea.size() == 0) {
            return reset()
        } else {
            return set(pArea.zoom, pArea.left, pArea.top, pArea.right, pArea.bottom)
        }
    }

    /**
     * Set the area as an empty area
     */
    fun reset(): MapTileArea {
        this.width = 0
        return this
    }

    val right: Int
        get() = (this.left + this.width) % mMapTileUpperBound

    val bottom: Int
        get() = (this.top + this.height) % mMapTileUpperBound

    override fun size(): Int {
        return this.width * this.height
    }

    override fun iterator(): MutableIterator<Long?> {
        return object : MutableIterator<Long?> {
            private var mIndex = 0

            override fun hasNext(): Boolean {
                return mIndex < size()
            }

            override fun next(): Long? {
                if (!hasNext()) {
                    return null
                }
                var x: Int = this@MapTileArea.left + mIndex % this@MapTileArea.width
                var y: Int = this@MapTileArea.top + mIndex / this@MapTileArea.width
                mIndex++
                while (x >= mMapTileUpperBound) {
                    x -= mMapTileUpperBound
                }
                while (y >= mMapTileUpperBound) {
                    y -= mMapTileUpperBound
                }
                return MapTileIndex.getTileIndex(this@MapTileArea.zoom, x, y)
            }

            override fun remove() {
                throw UnsupportedOperationException()
            }
        }
    }

    override fun contains(pTileIndex: Long): Boolean {
        if (MapTileIndex.getZoom(pTileIndex) != this.zoom) {
            return false
        }
        if (!contains(MapTileIndex.getX(pTileIndex), this.left, this.width)) {
            return false
        }
        return contains(MapTileIndex.getY(pTileIndex), this.top, this.height)
    }

    private fun contains(pValue: Int, pFirst: Int, pSize: Int): Boolean {
        var pValue = pValue
        while (pValue < pFirst) {
            pValue += mMapTileUpperBound
        }
        return pValue < pFirst + pSize
    }

    private fun cleanValue(pValue: Int): Int {
        var pValue = pValue
        while (pValue < 0) {
            pValue += mMapTileUpperBound
        }
        while (pValue >= mMapTileUpperBound) {
            pValue -= mMapTileUpperBound
        }
        return pValue
    }

    private fun computeSize(pTopLeft: Int, pBottomRight: Int): Int {
        var pBottomRight = pBottomRight
        while (pTopLeft > pBottomRight) {
            pBottomRight += mMapTileUpperBound
        }
        return min(mMapTileUpperBound, pBottomRight - pTopLeft + 1)
    }

    override fun toString(): String {
        if (this.width == 0) {
            return "MapTileArea:empty"
        }
        return "MapTileArea:zoom=" + this.zoom + ",left=" + this.left + ",top=" + this.top + ",width=" + this.width + ",height=" + this.height
    }
}
