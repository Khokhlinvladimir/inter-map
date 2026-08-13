package org.osmdroid.util

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GarbageCollectorTest {
    private val count = AtomicInteger(0)

    @Test
    fun testInit() {
        val garbageCollector = GarbageCollector(action())
        count.set(0)
        Assert.assertFalse(garbageCollector.isRunning)
        Assert.assertEquals(0, count.get())
    }

    @Test
    fun testFirst() {
        val garbageCollector = GarbageCollector(action())
        count.set(0)
        garbageCollector.gc()
        sleepFactor(.5)
        Assert.assertEquals(1, count.get())
        Assert.assertTrue(garbageCollector.isRunning)
        sleepFactor(2.0)
        Assert.assertFalse(garbageCollector.isRunning)
        Assert.assertEquals(1, count.get())
    }

    @Test
    fun testSecond() {
        val garbageCollector = GarbageCollector(action())
        count.set(0)
        garbageCollector.gc()
        sleepFactor(.5)
        Assert.assertEquals(1, count.get())
        Assert.assertTrue(garbageCollector.isRunning)
        sleepFactor(2.0)
        Assert.assertFalse(garbageCollector.isRunning)
        Assert.assertEquals(1, count.get())
        garbageCollector.gc()
        sleepFactor(.5)
        Assert.assertEquals(2, count.get())
        Assert.assertTrue(garbageCollector.isRunning)
        sleepFactor(2.0)
        Assert.assertFalse(garbageCollector.isRunning)
        Assert.assertEquals(2, count.get())
    }

    @Test
    fun testMulti() {
        val garbageCollector = GarbageCollector(action())
        count.set(0)
        repeat(4) { garbageCollector.gc() }
        sleepFactor(.5)
        Assert.assertEquals(1, count.get())
        Assert.assertTrue(garbageCollector.isRunning)
        sleepFactor(2.0)
        Assert.assertFalse(garbageCollector.isRunning)
        Assert.assertEquals(1, count.get())
    }

    private fun action() = Runnable {
        count.incrementAndGet()
        sleepFactor(1.0)
    }

    private fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (_: InterruptedException) {
        }
    }

    private fun sleepFactor(factor: Double) = sleep(Math.round(ACTION_MILLISECONDS * factor))

    companion object {
        private const val ACTION_MILLISECONDS = 500L
    }
}
