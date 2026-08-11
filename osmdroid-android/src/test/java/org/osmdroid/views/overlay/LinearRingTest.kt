package org.osmdroid.views.overlay

import android.graphics.Path
import junit.framework.Assert
import org.junit.Test
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.util.TileSystemWebMercator
import java.util.Random
import kotlin.math.max
import kotlin.math.min

class LinearRingTest {
    private val mRandom = Random()

    @Test
    fun testGetCenter1DLatitudesOnly() = testGetCenter(true, false)

    @Test
    fun testGetCenter1DLongitudesOnly() = testGetCenter(false, true)

    @Test
    fun testGetCenter2D() = testGetCenter(true, true)

    private fun testGetCenter(pSeveralLatitudes: Boolean, pSeveralLongitudes: Boolean) {
        val iterations = 1000
        val delta = 1E-10
        val center = GeoPoint(0.0, 0.0)
        val linearRing = LinearRing(Path())
        var increasing = true
        repeat(iterations) {
            linearRing.clearPath()
            val latitudeStop = getRandomPositiveLatitude()
            val latitudeStart = if (pSeveralLatitudes) -latitudeStop else latitudeStop
            val longitude1 = getRandomLongitude()
            val longitude2 = if (pSeveralLongitudes) getRandomLongitude() else longitude1
            val longitudeStart = min(longitude1, longitude2)
            val longitudeStop = max(longitude1, longitude2)
            for (latitude in latitudeStart..latitudeStop) {
                increasing = !increasing
                for (j in 0..longitudeStop - longitudeStart) {
                    val longitude = if (increasing) longitudeStart + j else longitudeStop - j
                    linearRing.addPoint(GeoPoint(latitude.toDouble(), longitude.toDouble()))
                }
            }
            linearRing.getCenter(center)
            Assert.assertEquals((latitudeStart + latitudeStop) / 2.0, center.latitude, delta)
            Assert.assertEquals((longitudeStart + longitudeStop) / 2.0, center.longitude, delta)
        }
    }

    private fun getRandomPositiveLatitude(): Int = getRandom(0, tileSystem.maxLatitude.toInt())

    private fun getRandomLongitude(): Int = getRandom(tileSystem.minLongitude.toInt(), tileSystem.maxLongitude.toInt())

    private fun getRandom(min: Int, max: Int): Int = min + mRandom.nextInt(max - min)

    companion object {
        private val tileSystem: TileSystem = TileSystemWebMercator()
    }
}
