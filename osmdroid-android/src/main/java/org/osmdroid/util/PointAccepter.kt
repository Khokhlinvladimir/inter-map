package org.osmdroid.util


interface PointAccepter {
    fun init()
    fun add(pX: Long, pY: Long)
    fun end()
}