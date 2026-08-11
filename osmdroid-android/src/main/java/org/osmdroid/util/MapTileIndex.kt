package org.osmdroid.util

/**
 * Computes a map tile index as `long` to/from zoom/x/y
 * Algorithm unfortunately different from SqlTileWriter.getIndex for historical reasons.
 * This version is better, because it's easy to get zoom, X and Y back from the index.
 * This version is limited to zooms between 0 and 29, which should be enough.
 */
object MapTileIndex {
    @JvmField
    var mMaxZoomLevel: Int = TileSystem.Companion.primaryKeyMaxZoomLevel
    private val mModulo = 1 shl mMaxZoomLevel

    @JvmStatic
    fun getTileIndex(pZoom: Int, pX: Int, pY: Int): Long {
        checkValues(pZoom, pX, pY)
        return (((pZoom.toLong()) shl (mMaxZoomLevel * 2))
                + ((pX.toLong()) shl mMaxZoomLevel)
                + pY.toLong())
    }

    @JvmStatic
    fun getZoom(pTileIndex: Long): Int {
        return (pTileIndex shr (mMaxZoomLevel * 2)).toInt()
    }

    @JvmStatic
    fun getX(pTileIndex: Long): Int {
        return ((pTileIndex shr mMaxZoomLevel) % mModulo).toInt()
    }

    @JvmStatic
    fun getY(pTileIndex: Long): Int {
        return (pTileIndex % mModulo).toInt()
    }

    /**
     * @since 6.0.0
     */
    @JvmStatic
    fun toString(pZoom: Int, pX: Int, pY: Int): String {
        return "/" + pZoom + "/" + pX + "/" + pY
    }

    /**
     * @since 6.0.0
     */
    @JvmStatic
    fun toString(pIndex: Long): String {
        return toString(getZoom(pIndex), getX(pIndex), getY(pIndex))
    }

    /**
     * @since 6.0.0
     */
    private fun checkValues(pZoom: Int, pX: Int, pY: Int) {
        if (pZoom < 0 || pZoom > mMaxZoomLevel) {
            throwIllegalValue(pZoom, pZoom, "Zoom")
        }
        val max = (1 shl pZoom).toLong()
        if (pX < 0 || pX >= max) {
            throwIllegalValue(pZoom, pX, "X")
        }
        if (pY < 0 || pY >= max) {
            throwIllegalValue(pZoom, pY, "Y")
        }
    }

    /**
     * @since 6.0.0
     */
    private fun throwIllegalValue(pZoom: Int, pValue: Int, pTag: String?) {
        throw IllegalArgumentException(
            "MapTileIndex: " + pTag + " (" + pValue + ") is too big (zoom=" + pZoom + ")"
        )
    }
}
