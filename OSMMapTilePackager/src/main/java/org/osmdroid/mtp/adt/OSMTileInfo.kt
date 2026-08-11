// Created by plusminus on 00:37:01 - 19.12.2008
package org.osmdroid.mtp.adt

class OSMTileInfo // ===========================================================
// Constructors
// ===========================================================
    (// ===========================================================
    // Fields
    // ===========================================================
    @JvmField val x: Int, @JvmField val y: Int, @JvmField val zoom: Int
) {
    val parentTile: OSMTileInfo
        // ===========================================================
        get() = OSMTileInfo(this.x / 2, this.y / 2, this.zoom - 1)

    /**
     * @param child
     * @param parent
     * @return
     */
    fun getPositionInParent(pParent: OSMTileInfo): Int {
        val childShouldUpperLeftX = pParent.x * 2
        val childShouldUpperLeftY = pParent.y * 2

        var out: Int = if (childShouldUpperLeftX == this.x) POSITION_IN_PARENT_LEFT else POSITION_IN_PARENT_RIGHT
        out += if (childShouldUpperLeftY == this.y) POSITION_IN_PARENT_TOP else POSITION_IN_PARENT_BOTTOM
        return out
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    override fun hashCode(): Int {
        return ((x shl 19) and -0x80000) or ((y shl 6) and 0x0007FFC0) or zoom
    }

    override fun equals(o: Any?): Boolean {
        if (o != null && o is OSMTileInfo) {
            val other = o
            return other.x == x && other.y == y && other.zoom == zoom
        } else {
            return super.equals(o)
        }
    }

    override fun toString(): String {
        return StringBuilder()
            .append("z=").append(this.zoom)
            .append(" x=").append(this.x)
            .append(" y=").append(this.y)
            .toString()
    } // ===========================================================
    // Methods
    // ===========================================================
    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private const val POSITION_IN_PARENT_LEFT = 1
        private const val POSITION_IN_PARENT_RIGHT = 2
        private const val POSITION_IN_PARENT_BOTTOM = 4
        private const val POSITION_IN_PARENT_TOP = 8

        val POSITION_IN_PARENT_TOPLEFT: Int = POSITION_IN_PARENT_LEFT or POSITION_IN_PARENT_TOP
        val POSITION_IN_PARENT_TOPRIGHT: Int = POSITION_IN_PARENT_RIGHT or POSITION_IN_PARENT_TOP
        val POSITION_IN_PARENT_BOTTOMRIGHT: Int = POSITION_IN_PARENT_RIGHT or POSITION_IN_PARENT_BOTTOM
        val POSITION_IN_PARENT_BOTTOMLEFT: Int = POSITION_IN_PARENT_LEFT or POSITION_IN_PARENT_BOTTOM
    }
}
