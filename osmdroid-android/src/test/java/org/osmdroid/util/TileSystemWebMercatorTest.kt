package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

class TileSystemWebMercatorTest {
    private val tileSystem = TileSystemWebMercator()

    @Test
    fun test01() {
        test01Lon(tileSystem.maxLongitude, 1.0)
        test01Lon(tileSystem.minLongitude, 0.0)
        test01Lat(tileSystem.maxLatitude, 0.0)
        test01Lat(tileSystem.minLatitude, 1.0)
    }

    private fun test01Lon(longitude: Double, expected: Double) =
        test01Value("longitude:$longitude", tileSystem.getX01FromLongitude(longitude), expected)

    private fun test01Lat(latitude: Double, expected: Double) =
        test01Value("latitude:$latitude", tileSystem.getY01FromLatitude(latitude), expected)

    private fun test01Value(text: String, value: Double, expected: Double) {
        val delta = 1E-10
        Assert.assertTrue(text, value >= 0)
        Assert.assertTrue(text, value <= 1)
        Assert.assertEquals(text, expected, value, delta)
    }
}
