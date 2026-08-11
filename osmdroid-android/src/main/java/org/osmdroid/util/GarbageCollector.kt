package org.osmdroid.util

import java.util.concurrent.atomic.AtomicBoolean

/**
 * "Garbage Collector" tool
 * The principles are:
 * * it runs smoothly and asynchronously
 * * only one execution at the same time
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
class GarbageCollector(private val mAction: Runnable) {
    private val mRunning = AtomicBoolean(false)

    fun gc(): Boolean {
        if (mRunning.getAndSet(true)) {
            return false
        }
        val thread = Thread(object : Runnable {
            override fun run() {
                try {
                    mAction.run()
                } finally {
                    mRunning.set(false)
                }
            }
        })
        thread.setName("GarbageCollector")
        thread.setPriority(Thread.MIN_PRIORITY)
        thread.start()
        return true
    }

    val isRunning: Boolean
        get() = mRunning.get()
}
