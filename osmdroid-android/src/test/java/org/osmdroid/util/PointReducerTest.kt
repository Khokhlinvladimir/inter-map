package org.osmdroid.util

import org.junit.Assert
import org.junit.Test

class PointReducerTest {
    @Test
    fun testReducer() {
        val points = arrayListOf(
            GeoPoint(45.0, -74.0),
            GeoPoint(45.0009, -74.0009),
            GeoPoint(45.0018, -74.0018)
        )
        val geoPoints = PointReducer.reduceWithTolerance(points, 0.5 / 312)
        Assert.assertTrue(geoPoints.isNotEmpty())
        Assert.assertTrue(geoPoints.size.toString(), geoPoints.size == 2)
    }
}
