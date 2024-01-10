package org.osmdroid.util

interface MapTileContainer {
    operator fun contains(pTileIndex: Long): Boolean
}