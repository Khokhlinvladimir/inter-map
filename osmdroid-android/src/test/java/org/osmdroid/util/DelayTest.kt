package org.osmdroid.util

import org.junit.Assert
import org.junit.Test

class DelayTest {
    @Test
    fun testDelayOne() {
        val millis = 5000L
        val delay = Delay(millis)
        repeat(5) {
            check(delay, millis)
            Assert.assertEquals(millis, delay.next())
        }
    }

    @Test
    fun testDelayMulti() {
        val millis = longArrayOf(500, 600, 800, 1000)
        val lastDuration = millis.last()
        val delay = Delay(millis)
        for (i in millis.indices) {
            check(delay, millis[i])
            Assert.assertEquals(if (i < millis.lastIndex) millis[i + 1] else lastDuration, delay.next())
        }
        check(delay, lastDuration)
        Assert.assertEquals(lastDuration, delay.next())
        check(delay, lastDuration)
    }

    private fun check(delay: Delay, millis: Long) {
        sleep(millis * 3 / 4)
        Assert.assertTrue(delay.shouldWait())
        sleep(millis / 2)
        Assert.assertFalse(delay.shouldWait())
    }

    private fun sleep(millis: Long) {
        try {
            Thread.sleep(millis, 0)
        } catch (_: InterruptedException) {
        }
    }
}
