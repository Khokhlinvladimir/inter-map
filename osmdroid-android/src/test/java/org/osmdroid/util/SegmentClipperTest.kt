package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

class SegmentClipperTest {
    private fun accepter(points: MutableList<PointL>) = object : PointAccepter {
        override fun init() = points.clear()
        override fun add(pX: Long, pY: Long) { points.add(PointL(pX, pY)) }
        override fun end() = Unit
    }

    @Test
    fun test_clip_with_path() {
        val points = ArrayList<PointL>()
        val clippable = accepter(points)
        val segmentClipper = SegmentClipper()
        segmentClipper.set(-600, -600, 1400, 1400, clippable, true)

        clippable.init(); segmentClipper.clip(-2146, -2152, -145, -141)
        check(points, PointL(-600, -600), PointL(-600, -598), PointL(-145, -141))
        clippable.init(); segmentClipper.clip(-145, -141, 855, -1150)
        check(points, PointL(-145, -141), PointL(310, -600), PointL(855, -600))
        clippable.init(); segmentClipper.clip(1856, 267, -2146, 9434)
        check(points, PointL(1400, 267), PointL(1400, 1312), PointL(1361, 1400), PointL(-600, 1400))
        clippable.init(); segmentClipper.clip(-30, 500, 700, 800)
        check(points, PointL(-30, 500), PointL(700, 800))
        clippable.init(); segmentClipper.clip(-1000, -10000, 10000, 10000)
        check(points, PointL(-600, -600), PointL(1400, -600), PointL(1400, 1400))
        clippable.init(); segmentClipper.clip(-10000, -1000, 10000, 10000)
        check(points, PointL(-600, -600), PointL(-600, 1400), PointL(1400, 1400))
    }

    @Test
    fun test_clip_without_path() {
        val points = ArrayList<PointL>()
        val clippable = accepter(points)
        val segmentClipper = SegmentClipper()
        segmentClipper.set(-600, -600, 1400, 1400, clippable, false)

        clippable.init(); segmentClipper.clip(-2146, -2152, -145, -141)
        check(points, PointL(-600, -598), PointL(-145, -141))
        clippable.init(); segmentClipper.clip(-145, -141, 855, -1150)
        check(points, PointL(-145, -141), PointL(310, -600))
        clippable.init(); segmentClipper.clip(1856, 267, -2146, 9434)
        check(points, PointL(1400, 1312), PointL(1361, 1400))
        clippable.init(); segmentClipper.clip(-30, 500, 700, 800)
        check(points, PointL(-30, 500), PointL(700, 800))
        clippable.init(); segmentClipper.clip(-1000, -10000, 10000, 10000)
        check(points)
        clippable.init(); segmentClipper.clip(-10000, -1000, 10000, 10000)
        check(points)
    }

    private fun check(actual: List<PointL>, vararg expected: PointL) {
        Assert.assertEquals(expected.size, actual.size)
        expected.forEachIndexed { index, point -> Assert.assertEquals(point, actual[index]) }
    }
}
