package org.osmdroid.util

/**
 * A repository for integers
 *
 * @author Fabrice Fontaine
 * @since 6.2.0
 */
class IntegerAccepter(pSize: Int) {
    private val mValues: IntArray
    private var mIndex = 0

    init {
        mValues = IntArray(pSize)
    }

    fun init() {
        mIndex = 0
    }

    fun add(pInteger: Int) {
        mValues[mIndex++] = pInteger
    }

    fun getValue(pIndex: Int): Int {
        return mValues[pIndex]
    }

    fun end() {}
    fun flush() {
        mIndex = 0
    }
}