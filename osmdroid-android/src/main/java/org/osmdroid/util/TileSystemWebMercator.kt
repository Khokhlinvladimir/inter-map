package org.osmdroid.util

import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

class TileSystemWebMercator : TileSystem() {
    override fun getX01FromLongitude(pLongitude: Double): Double {
        return (pLongitude - minLongitude) / (maxLongitude - minLongitude)
    }

    override fun getY01FromLatitude(pLatitude: Double): Double {
        val sinus = sin(pLatitude * Math.PI / 180)
        return 0.5 - ln((1 + sinus) / (1 - sinus)) / (4 * Math.PI)
    }

    override fun getLongitudeFromX01(pX01: Double): Double {
        return minLongitude + (maxLongitude - minLongitude) * pX01
    }

    override fun getLatitudeFromY01(pY01: Double): Double {
        return 90 - 360 * atan(exp((pY01 - 0.5) * 2 * Math.PI)) / Math.PI
    }

    override val minLatitude: Double
        get() = MinLatitude

    override val maxLatitude: Double
        get() = MaxLatitude

    override val minLongitude: Double
        get() = MinLongitude

    override val maxLongitude: Double
        get() = MaxLongitude

    companion object {
        val MinLatitude: Double = -85.05112877980658
        const val MaxLatitude: Double = 85.05112877980658
        val MinLongitude: Double = -180.0
        const val MaxLongitude: Double = 180.0
    }
}
