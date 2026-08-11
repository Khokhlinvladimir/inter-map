package org.osmdroid.util

/**
 * Tools about 2D distance computation
 * Optimized code: we compute the square of the distance.
 * If you really want to know the distance, apply Math.sqrt
 *
 * @author Fabrice Fontaine
 * @since 6.0.0
 */
object Distance {
    /**
     * Square of the distance between two points
     */
    @JvmStatic
    fun getSquaredDistanceToPoint(
        pFromX: Double, pFromY: Double, pToX: Double, pToY: Double
    ): Double {
        val dX = pFromX - pToX
        val dY = pFromY - pToY
        return dX * dX + dY * dY
    }

    /**
     * Square of the distance between a point and line AB
     */
    @JvmStatic
    fun getSquaredDistanceToLine(
        pFromX: Double, pFromY: Double,
        pAX: Double, pAY: Double, pBX: Double, pBY: Double
    ): Double {
        return getSquaredDistanceToProjection(
            pFromX, pFromY, pAX, pAY, pBX, pBY,
            getProjectionFactorToLine(pFromX, pFromY, pAX, pAY, pBX, pBY)
        )
    }

    /**
     * Square of the distance between a point and segment AB
     */
    @JvmStatic
    fun getSquaredDistanceToSegment(
        pFromX: Double, pFromY: Double,
        pAX: Double, pAY: Double, pBX: Double, pBY: Double
    ): Double {
        return getSquaredDistanceToProjection(
            pFromX, pFromY, pAX, pAY, pBX, pBY,
            getProjectionFactorToSegment(pFromX, pFromY, pAX, pAY, pBX, pBY)
        )
    }

    /**
     * @since 6.0.3
     * Square of the distance between a point and its projection on line AB
     */
    @JvmStatic
    fun getSquaredDistanceToProjection(
        pFromX: Double, pFromY: Double,
        pAX: Double, pAY: Double, pBX: Double, pBY: Double,
        pProjectionFactor: Double
    ): Double {
        val projectedX = pAX + (pBX - pAX) * pProjectionFactor
        val projectedY = pAY + (pBY - pAY) * pProjectionFactor
        return getSquaredDistanceToPoint(pFromX, pFromY, projectedX, projectedY)
    }

    /**
     * @return 0 if projected to A, 1 if projected to B, [0,1] if projected inside segment [A,B],
     * &lt; 0 or &gt; 1 if projected outside of the segment
     * @since 6.0.3
     * Projection factor on line AB from a point
     */
    @JvmStatic
    fun getProjectionFactorToLine(
        pFromX: Double, pFromY: Double,
        pAX: Double, pAY: Double, pBX: Double, pBY: Double
    ): Double {
        if (pAX == pBX && pAY == pBY) {
            return 0.0
        }
        return (dotProduct(pAX, pAY, pBX, pBY, pFromX, pFromY)
                / getSquaredDistanceToPoint(pAX, pAY, pBX, pBY))
    }

    /**
     * @return [0, 1]; 0 if projected to A, 1 if projected to B
     * @since 6.0.3
     * Projection factor on segment AB from a point
     */
    @JvmStatic
    fun getProjectionFactorToSegment(
        pFromX: Double, pFromY: Double,
        pAX: Double, pAY: Double, pBX: Double, pBY: Double
    ): Double {
        val result = getProjectionFactorToLine(pFromX, pFromY, pAX, pAY, pBX, pBY)
        if (result < 0) {
            return 0.0
        }
        if (result > 1) {
            return 1.0
        }
        return result
    }

    /**
     * Compute the dot product AB x AC
     */
    private fun dotProduct(
        pAX: Double, pAY: Double, pBX: Double, pBY: Double,
        pCX: Double, pCY: Double
    ): Double {
        return (pBX - pAX) * (pCX - pAX) + (pBY - pAY) * (pCY - pAY)
    }
}
