package org.osmdroid.util

import android.graphics.Rect
import org.junit.Assert
import org.junit.Test
import java.util.Random
import kotlin.math.max
import kotlin.math.min

class RectLTest {
    @Test
    fun testGetRotatedCenter() = repeat(iterations) {
        val x = getRandomCoordinate(); val y = getRandomCoordinate(); val degrees = getRandomAngle()
        Assert.assertEquals(x, RectL.getRotatedX(x, y, degrees, x, y))
        Assert.assertEquals(y, RectL.getRotatedY(x, y, degrees, x, y))
    }

    @Test
    fun testGetRotated0() = repeat(iterations) {
        val x = getRandomCoordinate(); val y = getRandomCoordinate()
        val centerX = getRandomCoordinate(); val centerY = getRandomCoordinate()
        Assert.assertEquals(x, RectL.getRotatedX(x, y, 0.0, centerX, centerY))
        Assert.assertEquals(y, RectL.getRotatedY(x, y, 0.0, centerX, centerY))
    }

    @Test
    fun testGetRotated90() = repeat(iterations) {
        val x = getRandomCoordinate(); val y = getRandomCoordinate()
        val centerX = getRandomCoordinate(); val centerY = getRandomCoordinate()
        Assert.assertEquals(centerX - (y - centerY), RectL.getRotatedX(x, y, 90.0, centerX, centerY))
        Assert.assertEquals(centerY + (x - centerX), RectL.getRotatedY(x, y, 90.0, centerX, centerY))
    }

    @Test
    fun testGetRotated180() = repeat(iterations) {
        val x = getRandomCoordinate(); val y = getRandomCoordinate()
        val centerX = getRandomCoordinate(); val centerY = getRandomCoordinate()
        Assert.assertEquals(centerX - (x - centerX), RectL.getRotatedX(x, y, 180.0, centerX, centerY))
        Assert.assertEquals(centerY - (y - centerY), RectL.getRotatedY(x, y, 180.0, centerX, centerY))
    }

    @Test
    fun testGetRotated270() = repeat(iterations) {
        val x = getRandomCoordinate(); val y = getRandomCoordinate()
        val centerX = getRandomCoordinate(); val centerY = getRandomCoordinate()
        Assert.assertEquals(centerX + (y - centerY), RectL.getRotatedX(x, y, 270.0, centerX, centerY))
        Assert.assertEquals(centerY - (x - centerX), RectL.getRotatedY(x, y, 270.0, centerX, centerY))
    }

    @Test
    fun testGetBounds0() {
        val input = RectL(); val output = RectL()
        repeat(iterations) {
            input.top = getRandomCoordinate(); input.left = getRandomCoordinate()
            input.bottom = getRandomCoordinate(); input.right = getRandomCoordinate()
            RectL.getBounds(input, getRandomCoordinate(), getRandomCoordinate(), 0.0, output)
            Assert.assertEquals(input.top, output.top); Assert.assertEquals(input.left, output.left)
            Assert.assertEquals(input.bottom, output.bottom); Assert.assertEquals(input.right, output.right)
        }
    }

    @Test
    fun testGetBounds180() {
        val input = RectL(); val output = RectL()
        repeat(iterations) {
            input.top = getRandomCoordinate(); input.left = getRandomCoordinate()
            input.bottom = getRandomCoordinate(); input.right = getRandomCoordinate()
            val centerX = getRandomCoordinate(); val centerY = getRandomCoordinate()
            RectL.getBounds(input, centerX, centerY, 180.0, output)
            val top = centerY - (input.top - centerY); val bottom = centerY - (input.bottom - centerY)
            val left = centerX - (input.left - centerX); val right = centerX - (input.right - centerX)
            Assert.assertEquals(min(top, bottom), output.top); Assert.assertEquals(min(left, right), output.left)
            Assert.assertEquals(max(top, bottom), output.bottom); Assert.assertEquals(max(left, right), output.right)
        }
    }

    @Test
    fun testGetBoundsSamplesRectL() {
        val input = RectL(); val output = RectL()
        input.set(0, 0, 4, 6)
        RectL.getBounds(input, 0, 0, 180.0, output)
        Assert.assertEquals(-6, output.top); Assert.assertEquals(0, output.bottom)
        Assert.assertEquals(-4, output.left); Assert.assertEquals(0, output.right)
        input.set(0, 0, 5, 7)
        RectL.getBounds(input, 0, 0, 90.0, output)
        Assert.assertEquals(0, output.top); Assert.assertEquals(-7, output.left)
        Assert.assertEquals(5, output.bottom); Assert.assertEquals(0, output.right)
        input.set(0, 0, 8, 8)
        RectL.getBounds(input, 0, 0, 45.0, output)
        Assert.assertEquals(0, output.top)
        Assert.assertEquals(-Math.round(8 * Math.sqrt(2.0) / 2.0), output.left)
        Assert.assertEquals(Math.round(8 * Math.sqrt(2.0)), output.bottom)
        Assert.assertEquals(Math.round(8 * Math.sqrt(2.0) / 2.0), output.right)
    }

    @Test
    fun testGetBoundsSamplesRect() {
        val input = Rect(); val output = Rect()
        input.top = 0; input.left = 0; input.bottom = 6; input.right = 4
        RectL.getBounds(input, 0, 0, 180.0, output)
        Assert.assertEquals(-6, output.top); Assert.assertEquals(0, output.bottom)
        Assert.assertEquals(-4, output.left); Assert.assertEquals(0, output.right)
        input.bottom = 7; input.right = 5
        RectL.getBounds(input, 0, 0, 90.0, output)
        Assert.assertEquals(0, output.top); Assert.assertEquals(-7, output.left)
        Assert.assertEquals(5, output.bottom); Assert.assertEquals(0, output.right)
        input.bottom = 8; input.right = 8
        RectL.getBounds(input, 0, 0, 45.0, output)
        Assert.assertEquals(0, output.top)
        Assert.assertEquals(-Math.round(8 * Math.sqrt(2.0) / 2.0).toInt(), output.left)
        Assert.assertEquals(Math.round(8 * Math.sqrt(2.0)).toInt(), output.bottom)
        Assert.assertEquals(Math.round(8 * Math.sqrt(2.0) / 2.0).toInt(), output.right)
    }

    private fun getRandomCoordinate() = random.nextInt(maxCoordinate).toLong() * if (random.nextBoolean()) 1 else -1
    private fun getRandomAngle() = random.nextInt(360).toDouble()

    companion object {
        private val random = Random()
        private const val maxCoordinate = 2000
        private const val iterations = 100
    }
}
