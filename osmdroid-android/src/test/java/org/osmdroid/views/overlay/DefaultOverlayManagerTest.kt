package org.osmdroid.views.overlay

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class DefaultOverlayManagerTest {
    @Test
    fun testOverlaysReversed() {
        val list = object : ListTest<Overlay>() {
            private val manager = DefaultOverlayManager(null)

            override fun add() {
                manager.add(object : Overlay() {})
            }

            override fun remove() {
                manager.removeAt(0)
            }

            override fun reverseOrder(): Iterable<Overlay?> = manager.overlaysReversed()

            override fun unprotectedReverseListIterator(): MutableListIterator<Overlay?> = throw IllegalArgumentException()

            override fun reverseIterator(): MutableListIterator<Overlay?> = throw IllegalArgumentException()
        }
        ListTester<Overlay>().test(list)
    }

    @Test
    fun testIntegerReversed() {
        val tester = ListTester<Int>()
        var list: ListTest<Int> = ListTest()
        tester.test(list)

        list = object : ListTest<Int>() {
            override fun remove() {
                synchronized(mList) { super.remove() }
            }

            override fun reverseIterator(): MutableListIterator<Int?> =
                synchronized(mList) { unprotectedReverseListIterator() }
        }
        tester.test(list)

        list = object : ListTest<Int>() {
            override fun reverseIterator(): MutableListIterator<Int?> =
                synchronized(mList) { unprotectedReverseListIterator() }
        }
        // Intentionally not executed: this half-synchronized variant is expected to crash.
    }

    private inner class ListTester<T> {
        private var mException: Exception? = null

        fun test(pList: ListTest<T>) {
            mException = null
            repeat(LOOPS) { pList.add() }
            val remove = Thread {
                try {
                    repeat(LOOPS) { pList.remove() }
                } catch (e: Exception) {
                    mException = e
                }
            }
            val loop = Thread {
                try {
                    repeat(LOOPS) {
                        for (item in pList.reverseOrder()) {
                            @Suppress("UNUSED_VARIABLE") val ignored = item
                        }
                    }
                } catch (e: Exception) {
                    mException = e
                }
            }
            val begin = System.currentTimeMillis()
            remove.start()
            loop.start()
            try {
                remove.join()
                loop.join()
            } catch (_: InterruptedException) {
            }
            println("duration: ${System.currentTimeMillis() - begin}")
            mException?.let { Assert.fail(it.message) }
        }
    }

    private open inner class ListTest<T> {
        protected val mList = CopyOnWriteArrayList<T?>()

        open fun add() {
            mList.add(null)
        }

        open fun remove() {
            mList.removeAt(0)
        }

        protected open fun unprotectedReverseListIterator(): MutableListIterator<T?> = mList.listIterator(mList.size)

        open fun reverseIterator(): MutableListIterator<T?> {
            while (true) {
                try {
                    return unprotectedReverseListIterator()
                } catch (_: IndexOutOfBoundsException) {
                }
            }
        }

        open fun reverseOrder(): Iterable<T?> = Iterable {
            val iterator = reverseIterator()
            object : MutableIterator<T?> {
                override fun hasNext(): Boolean = iterator.hasPrevious()
                override fun next(): T? = iterator.previous()
                override fun remove() = iterator.remove()
            }
        }
    }

    companion object {
        private const val LOOPS = 10000
    }
}
