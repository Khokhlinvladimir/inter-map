package org.osmdroid.util

import junit.framework.Assert
import org.junit.Test
import java.util.Random

class ListPointLTest {
    @Test
    fun test() {
        val values = LongArray(200)
        val list = ListPointL()
        Assert.assertEquals(0, list.size())
        reload(values)
        check(values, list)
        list.clear()
        reload(values)
        check(values, list)
        list.clear()
        Assert.assertEquals(0, list.size())
    }

    private fun reload(values: LongArray) {
        for (i in values.indices) values[i] = random.nextInt().toLong()
    }

    private fun check(values: LongArray, list: ListPointL) {
        for (i in values.indices step 2) list.add(values[i], values[i + 1])
        var i = 0
        for (nullablePoint in list) {
            val point = nullablePoint!!
            Assert.assertEquals(values[i], point.x)
            Assert.assertEquals(values[i + 1], point.y)
            i += 2
        }
        Assert.assertEquals(values.size / 2, list.size())
    }

    companion object {
        private val random = Random()
    }
}
