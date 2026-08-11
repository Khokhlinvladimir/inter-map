package org.osmdroid.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.util.constants.GeoConstants
import org.osmdroid.views.util.constants.MathConstants
import java.util.Random
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

class GeoPointTest {
    @Test
    fun test_distanceTo_itself() {
        repeat(100) {
            val target = GeoPoint(randomLatitude(), randomLongitude())
            val other = GeoPoint(target)
            assertEquals("distance to self is zero for $target", 0.0, target.distanceToAsDouble(other), 0.0)
            assertEquals("reverse distance to self is zero for $other", 0.0, other.distanceToAsDouble(target), 0.0)
        }
    }

    @Test
    fun test_distanceTo_Equator() {
        repeat(100) {
            val longitude1 = randomLongitude()
            val longitude2 = randomLongitude()
            val target = GeoPoint(0.0, longitude1)
            val other = GeoPoint(0.0, longitude2)
            val expected = GeoConstants.RADIUS_EARTH_METERS * cleanLongitudeDiff(longitude1, longitude2) * MathConstants.DEG2RAD
            if (expected >= MINIMUM_DISTANCE) {
                assertEquals("distance between $target and $other", expected, target.distanceToAsDouble(other), expected * 1E-10)
            }
        }
    }

    @Test
    fun test_distanceTo_Equator_Smaller() {
        var longitudeIncrement = 1.0
        repeat(10) {
            val longitude1 = randomLongitude()
            val longitude2 = longitude1 + longitudeIncrement
            longitudeIncrement /= 10.0
            val target = GeoPoint(0.0, longitude1)
            val other = GeoPoint(0.0, longitude2)
            val expected = GeoConstants.RADIUS_EARTH_METERS * cleanLongitudeDiff(longitude1, longitude2) * MathConstants.DEG2RAD
            if (expected >= MINIMUM_DISTANCE) {
                assertEquals("distance between $target and $other", expected, target.distanceToAsDouble(other), expected * 1E-5)
            }
        }
    }

    @Test
    fun test_distanceTo_Parallels() {
        repeat(100) {
            val latitude = randomLatitude()
            val longitude1 = randomLongitude()
            val longitude2 = randomLongitude()
            val target = GeoPoint(latitude, longitude1)
            val other = GeoPoint(latitude, longitude2)
            val diff = cleanLongitudeDiff(longitude1, longitude2)
            val expected = GeoConstants.RADIUS_EARTH_METERS * 2 * asin(
                cos(latitude * MathConstants.DEG2RAD) * sin(diff * MathConstants.DEG2RAD / 2)
            )
            if (expected >= MINIMUM_DISTANCE) {
                assertEquals("distance between $target and $other", expected, target.distanceToAsDouble(other), expected * 1E-5)
            }
        }
    }

    @Test
    fun test_bearingTo_north() = assertBearing("directly north", 0L, GeoPoint(10.0, 0.0))

    @Test
    fun test_bearingTo_east() = assertBearing("directly east", 90L, GeoPoint(0.0, 10.0))

    @Test
    fun test_bearingTo_south() = assertBearing("directly south", 180L, GeoPoint(-10.0, 0.0))

    @Test
    fun test_bearingTo_west() = assertBearing("directly west", 270L, GeoPoint(0.0, -10.0))

    @Test
    fun test_bearingTo_north_west() = assertBearing("north west", 225L, GeoPoint(-10.0, -10.0))

    private fun assertBearing(message: String, expected: Long, other: GeoPoint) {
        assertEquals(message, expected, Math.round(GeoPoint(0.0, 0.0).bearingTo(other)))
    }

    @Test
    fun test_destinationPoint_north_west_here() {
        val start = GeoPoint(52.387524, 4.891604)
        val end = GeoPoint(52.3906999098817, 4.886399738626785)
        assertEquals("destinationPoint north west", end, start.destinationPoint(500.0, -45.0))
    }

    @Test
    fun test_toFromString_withoutAltitude() {
        val input = GeoPoint(52387524, 4891604)
        assertEquals("toFromString without altitude", input, GeoPoint.fromIntString("52387524,4891604"))
    }

    @Test
    fun test_toFromString_withAltitude() {
        val input = GeoPoint(52387524, 4891604, 12345)
        System.out.println("GeoPoint to intString " + input.toIntString())
        System.out.println("GeoPoint to doubleString " + input.toDoubleString())
        System.out.println("GeoPoint to toString " + input)
        assertEquals("toFromString with altitude", input, GeoPoint.fromIntString(input.toIntString()))
    }

    @Test
    fun test_toFromDoubleString_withoutAltitude() {
        val input = GeoPoint(-117.123, 33.123)
        assertEquals("toFromString without altitude", input, GeoPoint.fromDoubleString("-117.123,33.123", ','))
    }

    @Test
    fun test_toFromDoubleString_withAltitude() {
        val input = GeoPoint(-117.123, 33.123, 12345.0)
        assertEquals("toFromString with altitude", input, GeoPoint.fromDoubleString(input.toDoubleString(), ','))
    }

    @Test
    fun test_toFromInvertedDoubleString_withoutAltitude() {
        val input = GeoPoint(-117.123, 33.123)
        assertEquals("toFromString without altitude", input, GeoPoint.fromInvertedDoubleString("33.123,-117.123", ','))
    }

    @Test
    fun test_toFromInvertedDoubleString_withAltitude() {
        val input = GeoPoint(-117.123, 33.123, 12345.0)
        assertEquals("toFromString with altitude", input, GeoPoint.fromInvertedDoubleString(input.toInvertedDoubleString(), ','))
    }

    private fun cleanLongitudeDiff(longitude1: Double, longitude2: Double): Double {
        var diff = abs(longitude1 - longitude2)
        if (diff > tileSystem.maxLongitude) {
            diff = tileSystem.maxLongitude - tileSystem.minLongitude - diff
        }
        return diff
    }

    private fun randomLongitude() = tileSystem.getRandomLongitude(random.nextDouble())

    private fun randomLatitude() = tileSystem.getRandomLatitude(random.nextDouble(), tileSystem.minLatitude)

    companion object {
        private const val MINIMUM_DISTANCE = 1E-2
        private val random = Random()
        private val tileSystem: TileSystem = TileSystemWebMercator()
    }
}
