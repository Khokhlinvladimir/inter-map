package org.osmdroid.util

/**
 * A [android.graphics.Point] with coordinates in long type instead of int
 */
class PointL {
    @JvmField
    var x: Long = 0

    @JvmField
    var y: Long = 0

    constructor()

    constructor(pX: Long, pY: Long) {
        x = pX
        y = pY
    }

    constructor(pOther: PointL) {
        set(pOther)
    }

    fun set(pOther: PointL) {
        x = pOther.x
        y = pOther.y
    }

    fun set(pX: Long, pY: Long) {
        x = pX
        y = pY
    }

    fun offset(dx: Long, dy: Long) {
        x += dx
        y += dy
    }

    override fun toString(): String {
        return "PointL(" + x + ", " + y + ")"
    }

    override fun equals(`object`: Any?): Boolean {
        if (this === `object`) {
            return true
        }
        if (`object` !is PointL) {
            return false
        }
        val other = `object`
        return x == other.x && y == other.y
    }
}
