package org.osmdroid.util

import android.graphics.Rect
import kotlin.math.cos
import kotlin.math.sin

/**
 * A [Rect] with corners in long type instead of int
 */
class RectL {
    @JvmField
    var left: Long = 0

    @JvmField
    var top: Long = 0

    @JvmField
    var right: Long = 0

    @JvmField
    var bottom: Long = 0

    constructor()

    constructor(pLeft: Long, pTop: Long, pRight: Long, pBottom: Long) {
        set(pLeft, pTop, pRight, pBottom)
    }

    constructor(pOther: RectL) {
        set(pOther)
    }

    fun set(pLeft: Long, pTop: Long, pRight: Long, pBottom: Long) {
        left = pLeft
        top = pTop
        right = pRight
        bottom = pBottom
    }

    fun set(pOther: RectL) {
        left = pOther.left
        top = pOther.top
        right = pOther.right
        bottom = pOther.bottom
    }

    fun union(x: Long, y: Long) {
        if (x < left) {
            left = x
        } else if (x > right) {
            right = x
        }
        if (y < top) {
            top = y
        } else if (y > bottom) {
            bottom = y
        }
    }

    /**
     * Returns true if (x,y) is inside the rectangle. Left and top coordinates are considered
     * inside the bounds, while right and bottom are not.
     */
    fun contains(x: Long, y: Long): Boolean {
        return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom
    }

    fun inset(dx: Long, dy: Long) {
        left += dx
        top += dy
        right -= dx
        bottom -= dy
    }

    fun width(): Long {
        return right - left
    }

    fun height(): Long {
        return bottom - top
    }

    override fun toString(): String {
        return "RectL(" + left + ", " + top + " - " + right + ", " + bottom + ")"
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val r = o as RectL
        return left == r.left && top == r.top && right == r.right && bottom == r.bottom
    }

    override fun hashCode(): Int {
        var result = left
        result = 31 * result + top
        result = 31 * result + right
        result = 31 * result + bottom
        return (result % Int.Companion.MAX_VALUE).toInt()
    }

    fun offset(pDx: Long, pDy: Long) {
        left += pDx
        top += pDy
        right += pDx
        bottom += pDy
    }

    fun union(pLeft: Long, pTop: Long, pRight: Long, pBottom: Long) {
        if ((pLeft < pRight) && (pTop < pBottom)) {
            if ((left < right) && (top < bottom)) {
                if (left > pLeft) left = pLeft
                if (top > pTop) top = pTop
                if (right < pRight) right = pRight
                if (bottom < pBottom) bottom = pBottom
            } else {
                left = pLeft
                top = pTop
                right = pRight
                bottom = pBottom
            }
        }
    }

    fun union(pRect: RectL) {
        union(pRect.left, pRect.top, pRect.right, pRect.bottom)
    }

    fun centerX(): Long {
        return (left + right) / 2
    }

    fun centerY(): Long {
        return (top + bottom) / 2
    }

    companion object {
        @JvmStatic
        fun intersects(a: RectL, b: RectL): Boolean {
            return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
        }

        /**
         * Rough computation of the smaller [RectL] that contains a rotated [RectL]
         * Emulating [Canvas.getClipBounds] after a canvas rotation
         * The code is supposed to be exactly the same as the Rect version, except for int/long
         */
        @JvmStatic
        fun getBounds(
            pIn: RectL,
            pCenterX: Long, pCenterY: Long, pDegrees: Double,
            pReuse: RectL?
        ): RectL {
            val out = if (pReuse != null) pReuse else RectL()
            if (pDegrees == 0.0) { // optimization
                out.top = pIn.top
                out.left = pIn.left
                out.bottom = pIn.bottom
                out.right = pIn.right
                return out
            }
            val radians = pDegrees * Math.PI / 180.0
            val cos = cos(radians)
            val sin = sin(radians)
            var inputX: Long
            var inputY: Long
            var outputX: Long
            var outputY: Long
            inputX = pIn.left // corner 1
            inputY = pIn.top
            outputX = getRotatedX(inputX, inputY, pCenterX, pCenterY, cos, sin)
            outputY = getRotatedY(inputX, inputY, pCenterX, pCenterY, cos, sin)
            out.bottom = outputY
            out.top = out.bottom
            out.right = outputX
            out.left = out.right
            inputX = pIn.right // corner 2
            inputY = pIn.top
            outputX = getRotatedX(inputX, inputY, pCenterX, pCenterY, cos, sin)
            outputY = getRotatedY(inputX, inputY, pCenterX, pCenterY, cos, sin)
            if (out.top > outputY) {
                out.top = outputY
            }
            if (out.bottom < outputY) {
                out.bottom = outputY
            }
            if (out.left > outputX) {
                out.left = outputX
            }
            if (out.right < outputX) {
                out.right = outputX
            }
            inputX = pIn.right // corner 3
            inputY = pIn.bottom
            outputX = getRotatedX(inputX, inputY, pCenterX, pCenterY, cos, sin)
            outputY = getRotatedY(inputX, inputY, pCenterX, pCenterY, cos, sin)
            if (out.top > outputY) {
                out.top = outputY
            }
            if (out.bottom < outputY) {
                out.bottom = outputY
            }
            if (out.left > outputX) {
                out.left = outputX
            }
            if (out.right < outputX) {
                out.right = outputX
            }
            inputX = pIn.left // corner 4
            inputY = pIn.bottom
            outputX = getRotatedX(inputX, inputY, pCenterX, pCenterY, cos, sin)
            outputY = getRotatedY(inputX, inputY, pCenterX, pCenterY, cos, sin)
            if (out.top > outputY) {
                out.top = outputY
            }
            if (out.bottom < outputY) {
                out.bottom = outputY
            }
            if (out.left > outputX) {
                out.left = outputX
            }
            if (out.right < outputX) {
                out.right = outputX
            }
            return out
        }

        /**
         * Rough computation of the smaller [Rect] that contains a rotated [Rect]
         * Emulating [Canvas.getClipBounds] after a canvas rotation
         * The code is supposed to be exactly the same as the RectL version, except for int/long
         * The code is written to run as fast as possible because it's constantly used when drawing markers
         */
        @JvmStatic
        fun getBounds(
            pIn: Rect,
            pCenterX: Int, pCenterY: Int, pDegrees: Double,
            pReuse: Rect?
        ): Rect {
            val out = if (pReuse != null) pReuse else Rect()
            if (pDegrees == 0.0) { // optimization
                out.top = pIn.top
                out.left = pIn.left
                out.bottom = pIn.bottom
                out.right = pIn.right
                return out
            }
            val radians = pDegrees * Math.PI / 180.0
            val cos = cos(radians)
            val sin = sin(radians)
            var inputX: Int
            var inputY: Int
            var outputX: Int
            var outputY: Int
            inputX = pIn.left // corner 1
            inputY = pIn.top
            outputX = getRotatedX(inputX.toLong(), inputY.toLong(), pCenterX.toLong(), pCenterY.toLong(), cos, sin).toInt()
            outputY = getRotatedY(inputX.toLong(), inputY.toLong(), pCenterX.toLong(), pCenterY.toLong(), cos, sin).toInt()
            out.bottom = outputY
            out.top = out.bottom
            out.right = outputX
            out.left = out.right
            inputX = pIn.right // corner 2
            inputY = pIn.top
            outputX = getRotatedX(inputX.toLong(), inputY.toLong(), pCenterX.toLong(), pCenterY.toLong(), cos, sin).toInt()
            outputY = getRotatedY(inputX.toLong(), inputY.toLong(), pCenterX.toLong(), pCenterY.toLong(), cos, sin).toInt()
            if (out.top > outputY) {
                out.top = outputY
            }
            if (out.bottom < outputY) {
                out.bottom = outputY
            }
            if (out.left > outputX) {
                out.left = outputX
            }
            if (out.right < outputX) {
                out.right = outputX
            }
            inputX = pIn.right // corner 3
            inputY = pIn.bottom
            outputX = getRotatedX(inputX.toLong(), inputY.toLong(), pCenterX.toLong(), pCenterY.toLong(), cos, sin).toInt()
            outputY = getRotatedY(inputX.toLong(), inputY.toLong(), pCenterX.toLong(), pCenterY.toLong(), cos, sin).toInt()
            if (out.top > outputY) {
                out.top = outputY
            }
            if (out.bottom < outputY) {
                out.bottom = outputY
            }
            if (out.left > outputX) {
                out.left = outputX
            }
            if (out.right < outputX) {
                out.right = outputX
            }
            inputX = pIn.left // corner 4
            inputY = pIn.bottom
            outputX = getRotatedX(inputX.toLong(), inputY.toLong(), pCenterX.toLong(), pCenterY.toLong(), cos, sin).toInt()
            outputY = getRotatedY(inputX.toLong(), inputY.toLong(), pCenterX.toLong(), pCenterY.toLong(), cos, sin).toInt()
            if (out.top > outputY) {
                out.top = outputY
            }
            if (out.bottom < outputY) {
                out.bottom = outputY
            }
            if (out.left > outputX) {
                out.left = outputX
            }
            if (out.right < outputX) {
                out.right = outputX
            }
            return out
        }

        /**
         * Apply a rotation on a point and get the resulting X
         */
        @JvmStatic
        fun getRotatedX(
            pX: Long, pY: Long,
            pDegrees: Double, pCenterX: Long, pCenterY: Long
        ): Long {
            if (pDegrees == 0.0) { // optimization
                return pX
            }
            val radians = pDegrees * Math.PI / 180.0
            return getRotatedX(pX, pY, pCenterX, pCenterY, cos(radians), sin(radians))
        }

        /**
         * Apply a rotation on a point and get the resulting Y
         */
        @JvmStatic
        fun getRotatedY(
            pX: Long, pY: Long,
            pDegrees: Double, pCenterX: Long, pCenterY: Long
        ): Long {
            if (pDegrees == 0.0) { // optimization
                return pY
            }
            val radians = pDegrees * Math.PI / 180.0
            return getRotatedY(pX, pY, pCenterX, pCenterY, cos(radians), sin(radians))
        }

        /**
         * Apply a rotation on a point and get the resulting X
         */
        @JvmStatic
        fun getRotatedX(
            pX: Long, pY: Long,
            pCenterX: Long, pCenterY: Long,
            pCos: Double, pSin: Double
        ): Long {
            return pCenterX + Math.round((pX - pCenterX) * pCos - (pY - pCenterY) * pSin)
        }

        /**
         * Apply a rotation on a point and get the resulting Y
         */
        @JvmStatic
        fun getRotatedY(
            pX: Long, pY: Long,
            pCenterX: Long, pCenterY: Long,
            pCos: Double, pSin: Double
        ): Long {
            return pCenterY + Math.round((pX - pCenterX) * pSin + (pY - pCenterY) * pCos)
        }
    }
}
