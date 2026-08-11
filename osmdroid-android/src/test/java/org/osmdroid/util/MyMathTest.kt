package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

class MyMathTest {
    @Test
    fun testGetAngleDifference() {
        Assert.assertEquals(20.0, MyMath.getAngleDifference(10.0, 30.0, null), DELTA)
        Assert.assertEquals(20.0, MyMath.getAngleDifference(10.0, 30.0, true), DELTA)
        Assert.assertEquals(-340.0, MyMath.getAngleDifference(10.0, 30.0, false), DELTA)
        Assert.assertEquals(-20.0, MyMath.getAngleDifference(30.0, 10.0, null), DELTA)
        Assert.assertEquals(340.0, MyMath.getAngleDifference(30.0, 10.0, true), DELTA)
        Assert.assertEquals(-20.0, MyMath.getAngleDifference(30.0, 10.0, false), DELTA)
        Assert.assertEquals(2.0, MyMath.getAngleDifference(179.0, -179.0, null), DELTA)
        Assert.assertEquals(2.0, MyMath.getAngleDifference(179.0, -179.0, true), DELTA)
        Assert.assertEquals(-358.0, MyMath.getAngleDifference(179.0, -179.0, false), DELTA)
        Assert.assertEquals(2.0, MyMath.getAngleDifference(359.0, 1.0, null), DELTA)
        Assert.assertEquals(2.0, MyMath.getAngleDifference(359.0, 1.0, true), DELTA)
        Assert.assertEquals(-358.0, MyMath.getAngleDifference(359.0, 1.0, false), DELTA)
    }

    @Test
    fun testComputeAngle() {
        val delta = 1E-10
        val value = 10L
        Assert.assertEquals(0.0, MyMath.computeAngle(0, 0, value, 0), delta)
        Assert.assertEquals(Math.PI, MyMath.computeAngle(0, 0, -value, 0), delta)
        Assert.assertEquals(-Math.PI / 2, MyMath.computeAngle(0, 0, 0, -value), delta)
        Assert.assertEquals(Math.PI / 2, MyMath.computeAngle(0, 0, 0, value), delta)
        Assert.assertEquals(-Math.PI / 4, MyMath.computeAngle(0, 0, value, -value), delta)
        Assert.assertEquals(Math.PI / 4, MyMath.computeAngle(0, 0, value, value), delta)
        Assert.assertEquals(-3 * Math.PI / 4, MyMath.computeAngle(0, 0, -value, -value), delta)
        Assert.assertEquals(3 * Math.PI / 4, MyMath.computeAngle(0, 0, -value, value), delta)
    }

    @Test
    fun testComputeCirclePoint() {
        val output = PointL()
        val radius = 10L
        MyMath.computeCirclePoint(0, 0, radius.toDouble(), Math.PI, output)
        Assert.assertEquals(-radius, output.x)
        Assert.assertEquals(0, output.y)
        MyMath.computeCirclePoint(0, 0, radius.toDouble(), 0.0, output)
        Assert.assertEquals(radius, output.x)
        Assert.assertEquals(0, output.y)
        MyMath.computeCirclePoint(0, 0, radius.toDouble(), Math.PI / 2, output)
        Assert.assertEquals(0, output.x)
        Assert.assertEquals(-radius, -output.y)
        MyMath.computeCirclePoint(0, 0, radius.toDouble(), -Math.PI / 2, output)
        Assert.assertEquals(0, output.x)
        Assert.assertEquals(radius, -output.y)
        MyMath.computeCirclePoint(0, 0, radius.toDouble(), Math.PI / 4, output)
        Assert.assertEquals((radius * Math.sqrt(2.0) / 2).toLong(), output.x)
        Assert.assertEquals((-radius * Math.sqrt(2.0) / 2).toLong(), -output.y)
    }

    companion object { private const val DELTA = 1E-20 }
}
