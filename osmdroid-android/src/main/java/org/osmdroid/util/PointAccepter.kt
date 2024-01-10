package org.osmdroid.util

/**
 * Created by Fabrice on 23/12/2017.
 *
 * @since 6.0.0
 */
interface PointAccepter {
    fun init()
    fun add(pX: Long, pY: Long)
    fun end()
}