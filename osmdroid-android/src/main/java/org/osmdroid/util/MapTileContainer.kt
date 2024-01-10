package org.osmdroid.util

/**
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
interface MapTileContainer {
    operator fun contains(pTileIndex: Long): Boolean
}