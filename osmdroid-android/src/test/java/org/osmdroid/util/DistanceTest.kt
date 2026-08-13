package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

class DistanceTest {
    private val delta = 1E-10

    @Test
    fun test_getSquareDistanceToPoint() {
        val xA = 100.0
        val yA = 200.0
        val deltaX = 10.0
        val deltaY = 20.0
        Assert.assertEquals(0.0, Distance.getSquaredDistanceToPoint(xA, yA, xA, yA), delta)
        Assert.assertEquals(deltaX * deltaX, Distance.getSquaredDistanceToPoint(xA, yA, xA + deltaX, yA), delta)
        Assert.assertEquals(deltaY * deltaY, Distance.getSquaredDistanceToPoint(xA, yA, xA, yA + deltaY), delta)
        Assert.assertEquals(deltaX * deltaX + deltaY * deltaY, Distance.getSquaredDistanceToPoint(xA, yA, xA + deltaX, yA + deltaY), delta)
    }

    @Test
    fun test_getSquareDistanceToSegment() {
        val x = 100.0
        val y = 200.0
        Assert.assertEquals(0.0, Distance.getSquaredDistanceToSegment(x, y, x, y, x, y), delta)
        Assert.assertEquals(100.0, Distance.getSquaredDistanceToSegment(x, y, x + 10, y, x + 10, y), delta)
        Assert.assertEquals(400.0, Distance.getSquaredDistanceToSegment(x, y, x, y + 20, x, y + 20), delta)
        Assert.assertEquals(400.0, Distance.getSquaredDistanceToSegment(x, y + 20, x, y, x + 100, y), delta)
        Assert.assertEquals(1000.0, Distance.getSquaredDistanceToSegment(x - 10, y - 30, x, y, x + 100, y), delta)
        Assert.assertEquals(14900.0, Distance.getSquaredDistanceToSegment(x + 200, y - 70, x, y, x + 100, y), delta)
        Assert.assertEquals(49000000.0, Distance.getSquaredDistanceToSegment(x + 200, y - 7000, x, y, x + 200, y), delta)
        Assert.assertEquals(49000000.0, Distance.getSquaredDistanceToSegment(x + 200, y - 7000, x, y, x + 1000, y), delta)
    }

    @Test
    fun test_getProjectionFactorToLine() {
        val x = 100.0
        val y = 200.0
        Assert.assertEquals(0.0, Distance.getProjectionFactorToLine(x, y, x, y, x, y), delta)
        Assert.assertEquals(0.0, Distance.getProjectionFactorToLine(x, y, x + 10, y, x + 10, y), delta)
        Assert.assertEquals(0.0, Distance.getProjectionFactorToLine(x, y, x, y + 20, x, y + 20), delta)
        Assert.assertEquals(0.0, Distance.getProjectionFactorToLine(x, y + 20, x, y, x + 100, y), delta)
        Assert.assertEquals(-0.1, Distance.getProjectionFactorToLine(x - 10, y - 30, x, y, x + 100, y), delta)
        Assert.assertEquals(2.0, Distance.getProjectionFactorToLine(x + 200, y - 70, x, y, x + 100, y), delta)
        Assert.assertEquals(1.0, Distance.getProjectionFactorToLine(x + 200, y - 7000, x, y, x + 200, y), delta)
        Assert.assertEquals(0.2, Distance.getProjectionFactorToLine(x + 200, y - 7000, x, y, x + 1000, y), delta)
    }

    @Test
    fun test_getSquareDistanceToLine() {
        val x = 100.0
        val y = 200.0
        Assert.assertEquals(0.0, Distance.getSquaredDistanceToLine(x, y, x, y, x, y), delta)
        Assert.assertEquals(100.0, Distance.getSquaredDistanceToLine(x, y, x + 10, y, x + 10, y), delta)
        Assert.assertEquals(400.0, Distance.getSquaredDistanceToLine(x, y, x, y + 20, x, y + 20), delta)
        Assert.assertEquals(400.0, Distance.getSquaredDistanceToLine(x, y + 20, x, y, x + 100, y), delta)
        Assert.assertEquals(900.0, Distance.getSquaredDistanceToLine(x - 10, y - 30, x, y, x + 100, y), delta)
        Assert.assertEquals(4900.0, Distance.getSquaredDistanceToLine(x + 200, y - 70, x, y, x + 100, y), delta)
        Assert.assertEquals(49000000.0, Distance.getSquaredDistanceToLine(x + 200, y - 7000, x, y, x + 200, y), delta)
        Assert.assertEquals(49000000.0, Distance.getSquaredDistanceToLine(x + 200, y - 7000, x, y, x + 1000, y), delta)
    }
}
