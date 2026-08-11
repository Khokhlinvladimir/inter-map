package org.osmdroid.debug.model

/**
 * Provide source, database key and expiration date to map tile.
 * created on 12/20/2016.
 * There use to be a `MapTile`. Not anymore, we use [org.osmdroid.util.MapTileIndex] instead.
 *
 * @author Alex O'Ree
 * @since 5.6.2
 */
class MapTileExt {
    var source: String? = null
    var key: Long = 0
    var expires: Long? = null
}
