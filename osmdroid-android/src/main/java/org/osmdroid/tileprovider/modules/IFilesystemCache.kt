package org.osmdroid.tileprovider.modules

import android.graphics.drawable.Drawable
import org.osmdroid.tileprovider.tilesource.ITileSource
import java.io.InputStream

/**
 * Represents a write-only interface into a file system cache.
 *
 * @author Marc Kurtz
 */
interface IFilesystemCache {
    /**
     * Save an InputStream as the specified tile in the file system cache for the specified tile
     * source.
     *
     * @param pTileSourceInfo a tile source
     * @param pMapTileIndex   a tile
     * @param pStream         an InputStream
     * @return
     */
    fun saveFile(pTileSourceInfo: ITileSource?, pMapTileIndex: Long,
                 pStream: InputStream?, pExpirationTime: Long?): Boolean

    /**
     * return true if the map file for download already exists
     *
     * @return
     */
    fun exists(pTileSourceInfo: ITileSource?, pMapTileIndex: Long): Boolean

    /**
     * Used when the map engine is shutdown, use it to perform any clean up activities and to terminate
     * any background threads
     *
     * @since 5.3
     */
    fun onDetach()

    /**
     * Removes a tile from the cache, see issue
     * https://github.com/osmdroid/osmdroid/issues/426
     *
     * @return true if it was removed, false otherwise
     * @since 5.4.2
     */
    fun remove(tileSource: ITileSource?, pMapTileIndex: Long): Boolean

    /**
     * Gets the cache expiration timestamp of a tile
     *
     * @return cache expiration timestamp in time since UTC epoch (in milliseconds),
     * or null if expiration timestamp is not supported or if the tile is not cached
     * @since 5.6.5
     */
    fun getExpirationTimestamp(pTileSource: ITileSource?, pMapTileIndex: Long): Long?

    /**
     * Gets the tile drawable
     *
     * @since 6.0.0
     */
    @Throws(Exception::class)
    fun loadTile(pTileSource: ITileSource?, pMapTileIndex: Long): Drawable?
}