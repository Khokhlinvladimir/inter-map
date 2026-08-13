package org.osmdroid.views.overlay.advancedpolyline

import org.junit.Assert
import org.junit.Test

class ColorMappingVariationTest {
    @Test
    fun testAllZero() {
        objTest.init(0f, 0f, 0f, 0f)
        verify(0.0, 0f)
        verify(0.0, -1f)
        verify(0.0, 5f)
    }

    @Test
    fun testSimple() {
        objTest.init(0f, 100f, 0f, 100f)
        verify(0.0, 0f)
        verify(50.0, 50f)
        verify(100.0, 100f)
        verify(0.0, -4f)
        verify(100.0, 400f)
    }

    @Test
    fun testScalarOffset() {
        objTest.init(20f, 100f, 0f, 100f)
        verify(0.0, 0f)
        verify(37.5, 50f)
        verify(100.0, 100f)
        verify(0.0, -4f)
        verify(100.0, 400f)
    }

    @Test
    fun testScalarNegative() {
        objTest.init(-200f, 1000f, 0f, 100f)
        verify(16.6667, 0f)
        verify(58.3333, 500f)
        verify(100.0, 1000f)
        verify(100.0, 1010f)
        verify(0.0, -300f)
    }

    @Test
    fun testInverse() {
        objTest.init(-200f, 1000f, 300f, -100f)
        verify(300.0, -200f)
        verify(233.333, 0f)
        verify(-100.0, 1000f)
        verify(-100.0, 1010f)
        verify(300.0, -300f)
    }

    private fun verify(expected: Double, scalar: Float) {
        Assert.assertEquals(expected, objTest.mapScalar(scalar).toDouble(), DELTA)
    }

    companion object {
        private const val DELTA = 1E-3
        private val objTest = object : ColorMappingVariation() {
            override fun getHue(pScalar: Float): Float = 0f
            override fun getSaturation(pScalar: Float): Float = 0f
            override fun getLuminance(pScalar: Float): Float = 0f
        }
    }
}
