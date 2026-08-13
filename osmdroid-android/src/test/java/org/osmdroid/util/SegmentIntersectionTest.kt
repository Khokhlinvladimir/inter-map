package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

class SegmentIntersectionTest {
    @Test
    fun test_intersection() {
        testIntersection(RectL(0, 500, 1000, 500), RectL(500, 0, 500, 1000), PointL(500, 500))
        testIntersection(RectL(0, 0, 100, 100), RectL(0, 50, 100, 50), PointL(50, 50))
        testIntersection(RectL(0, 0, 100, 0), RectL(0, 50, 100, 50), null)
        testIntersection(RectL(0, 0, 100, 100), RectL(50, 50, 1000, 1000), PointL(75, 75))
        testIntersection(RectL(0, 0, 100, 100), RectL(0, 500, 100, 500), null)
        testIntersection(
            RectL(0, 0, 1L shl 30, 1L shl 30),
            RectL(0, 1L shl 29, 1L shl 30, 1L shl 29),
            PointL(1L shl 29, 1L shl 29)
        )
        testIntersection(
            RectL(-33554178, 402653480, -33554178, 234881320),
            RectL(-268435456, 268435455, 268435455, 268435455),
            PointL(-33554178, 268435455)
        )
    }

    private fun testIntersection(segment1: RectL, segment2: RectL, expectedIntersection: PointL?) {
        testIntersectionHelper(segment1, segment2, expectedIntersection)
        testIntersectionHelper(segment2, segment1, expectedIntersection)
    }

    private fun testIntersectionHelper(segment1: RectL, segment2: RectL, expectedIntersection: PointL?) {
        val intersection = PointL()
        val result = SegmentIntersection.intersection(
            segment1.left.toDouble(), segment1.top.toDouble(), segment1.right.toDouble(), segment1.bottom.toDouble(),
            segment2.left.toDouble(), segment2.top.toDouble(), segment2.right.toDouble(), segment2.bottom.toDouble(),
            intersection
        )
        if (expectedIntersection == null) {
            Assert.assertFalse(result)
        } else {
            Assert.assertTrue(result)
            Assert.assertEquals(expectedIntersection.x, intersection.x)
            Assert.assertEquals(expectedIntersection.y, intersection.y)
        }
    }
}
