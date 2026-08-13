package org.osmdroid.util

/**
 * Created by Fabrice on 03/01/2018.
 *
 * @since 6.0.0
 */
abstract class LineBuilder(pMaxSize: Int) : PointAccepter {
    val lines: FloatArray
    var size: Int = 0
        private set

    init {
        this.lines = FloatArray(pMaxSize)
    }

    override fun init() {
        this.size = 0
    }

    override fun add(pX: Long, pY: Long) {
        this.lines[this.size++] = pX.toFloat()
        this.lines[this.size++] = pY.toFloat()
        if (this.size >= lines.size) {
            innerFlush()
        }
    }

    override fun end() {
        innerFlush()
    }

    private fun innerFlush() {
        if (this.size > 0) {
            flush()
        }
        this.size = 0
    }

    abstract fun flush()
}
