package org.osmdroid.tileprovider.modules

import org.osmdroid.tileprovider.tilesource.ITileSource
import java.io.File
import java.io.InputStream

/**
 * The IArchiveFile is primary used to load tiles from a file archive. Generally, this should only
 * be used for archives that require little to no computation in order to provide a given tile.
 *
 *
 * For cases thereby the tiles are rendered or manipulated (such as from another projection)
 *
 * @see MapTileModuleProviderBase
 *
 * @see ArchiveFileFactory
 */
interface IArchiveFile {
    /**
     * initialize the file archive, such as performing initial scans, queries, opening a database, etc
     *
     * @param pFile
     * @throws Exception
     */
    @Throws(Exception::class)
    fun init(pFile: File?)

    /**
     * Get the input stream for the requested tile and tile source.
     *
     *
     * Also keep in mind that the tile source has an explicit tile size in pixels, and tile source name.
     *
     * @return the input stream, or null if the archive doesn't contain an entry for the requested tile.
     * @see org.osmdroid.tileprovider.tilesource.TileSourceFactory
     */
    fun getInputStream(tileSource: ITileSource?, pMapTileIndex: Long): InputStream?

    /**
     * Closes the archive file and releases resources.
     */
    fun close()

    /**
     * returns a list of tile source names that are available in the archive, if supported. If
     * not supported, return an empty set
     *
     * @return
     * @since 5.0
     */
    val tileSources: Set<String?>?

    /**
     * @since 6.0
     * If set to true, tiles from this archive will be loaded regardless of their associated tile source name
     */
    fun setIgnoreTileSource(pIgnoreTileSource: Boolean)
}