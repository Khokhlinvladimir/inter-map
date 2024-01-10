package org.osmdroid.tileprovider.tilesource

import android.graphics.drawable.Drawable
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException
import java.io.InputStream

interface ITileSource {
    /**
     * An ordinal identifier for this tile source
     *
     * @return the ordinal value
     */
    @Deprecated("")
    fun ordinal(): Int

    /**
     * A human-friendly name for this tile source
     *
     * @return the tile source name
     */
    fun name(): String?

    /**
     * Get a unique file path for the tile. This file path may be used to store the tile on a file
     * system and performance considerations should be taken into consideration. It can include
     * multiple paths. It should not begin with a leading path separator.
     *
     * @param pMapTileIndex the tile
     * @return the unique file path
     */
    fun getTileRelativeFilenameString(pMapTileIndex: Long): String?

    /**
     * Get a rendered Drawable from the specified file path.
     *
     * @param aFilePath a file path
     * @return the rendered Drawable
     */
    @Throws(LowMemoryException::class)
    fun getDrawable(aFilePath: String?): Drawable?

    /**
     * Get a rendered Drawable from the specified InputStream.
     *
     * @param aTileInputStream an InputStream
     * @return the rendered Drawable
     */
    @Throws(LowMemoryException::class)
    fun getDrawable(aTileInputStream: InputStream?): Drawable?

    /**
     * Get the minimum zoom level this tile source can provide.
     *
     * @return the minimum zoom level
     */
    val minimumZoomLevel: Int

    /**
     * Get the maximum zoom level this tile source can provide.
     *
     * @return the maximum zoom level
     */
    val maximumZoomLevel: Int

    /**
     * Get the tile size in pixels this tile source provides.
     *
     * @return the tile size in pixels
     */
    val tileSizePixels: Int

    /**
     * Returns an I18N sensitive string representing the copy right notice (if any) of the tile source
     *
     * @return a string or null
     * @since 5.6.1
     */
    val copyrightNotice: String?
}