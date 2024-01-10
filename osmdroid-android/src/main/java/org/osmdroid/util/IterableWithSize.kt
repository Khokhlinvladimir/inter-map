package org.osmdroid.util

interface IterableWithSize<T> : Iterable<T> {
    fun size(): Int
}