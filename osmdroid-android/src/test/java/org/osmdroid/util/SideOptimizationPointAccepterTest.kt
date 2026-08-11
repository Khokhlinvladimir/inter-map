package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test

class SideOptimizationPointAccepterTest {
    class SimpleAccepter : PointAccepter {
        val list: MutableList<PointL> = ArrayList()
        override fun init() = list.clear()
        override fun add(pX: Long, pY: Long) { list.add(PointL(pX, pY)) }
        override fun end() = Unit
    }

    @Test fun testNothing() = test(longArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), longArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))

    @Test
    fun testOneSideX() = test(
        longArrayOf(1, 2, 3, 4, 50, 6, 50, 20, 50, 4, 50, 15, 50, 18, 50, 5, 2, 12),
        longArrayOf(1, 2, 3, 4, 50, 6, 50, 4, 50, 20, 50, 5, 2, 12)
    )

    @Test
    fun testTwoSidesX() = test(
        longArrayOf(1, 2, 3, 4, 50, 6, 50, 20, 50, 4, 50, 15, 50, 18, 50, 5, 12, 2, 12, 78, 12, 3, 12, 1, 12, 1, 12, 4, 2, 12),
        longArrayOf(1, 2, 3, 4, 50, 6, 50, 4, 50, 20, 50, 5, 12, 2, 12, 1, 12, 78, 12, 4, 2, 12)
    )

    @Test
    fun testOneSideY() = test(
        longArrayOf(0, 1, 2, 3, 4, 50, 6, 50, 20, 50, 4, 50, 15, 50, 18, 50, 5, 2),
        longArrayOf(0, 1, 2, 3, 4, 50, 20, 50, 18, 50, 5, 2)
    )

    @Test
    fun testTwoSidesY() = test(
        longArrayOf(0, 1, 2, 3, 4, 50, 6, 50, 20, 50, 4, 50, 15, 50, 18, 50, 45, 10, 16, 10, 2, 10, 14, 10, 1, 10, 8, 10, 5, 2),
        longArrayOf(0, 1, 2, 3, 4, 50, 20, 50, 18, 50, 45, 10, 1, 10, 8, 10, 5, 2)
    )

    @Test
    fun testOneSideXManiac() = test(
        longArrayOf(
            1, 2, 3, 4, 50, 6, 50, 20, 50, 4,
            50, 15, 50, 6, 50, 23, 50, 15, 50, 6, 50, 23, 50, 15, 50, 6, 50, 23,
            50, 15, 50, 6, 50, 23, 50, 15, 50, 6, 50, 23, 50, 15, 50, 6, 50, 23,
            50, 15, 50, 6, 50, 23, 50, 18, 50, 5, 2, 12
        ),
        longArrayOf(1, 2, 3, 4, 50, 6, 50, 4, 50, 23, 50, 5, 2, 12)
    )

    @Test
    fun testRectangle() = test(
        longArrayOf(
            1, 2, 3, 4,
            50, 6, 50, 20, 50, 4, 50, 15, 50, 18, 50, 5,
            4, 50, 6, 50, 20, 50, 4, 50, 15, 50, 18, 50,
            12, 2, 12, 78, 12, 3, 12, 1, 12, 1, 12, 4,
            5, 5, 45, 10, 16, 10, 2, 10, 14, 10, 1, 10, 8, 10, 2, 12
        ),
        longArrayOf(
            1, 2, 3, 4, 50, 6, 50, 4, 50, 20, 50, 5,
            4, 50, 20, 50, 18, 50, 12, 2, 12, 1, 12, 78, 12, 4,
            5, 5, 45, 10, 1, 10, 8, 10, 2, 12
        )
    )

    private fun test(values: LongArray, expectedValues: LongArray) {
        val simpleAccepter = SimpleAccepter()
        val optim = SideOptimizationPointAccepter(simpleAccepter)
        optim.init()
        for (i in values.indices step 2) optim.add(values[i], values[i + 1])
        optim.end()
        val expected = ArrayList<PointL>()
        for (i in expectedValues.indices step 2) expected.add(PointL(expectedValues[i], expectedValues[i + 1]))
        Assert.assertEquals(expected, simpleAccepter.list)
    }
}
