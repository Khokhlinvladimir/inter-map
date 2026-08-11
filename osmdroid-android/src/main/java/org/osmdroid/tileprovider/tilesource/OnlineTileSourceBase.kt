package org.osmdroid.tileprovider.tilesource

import java.util.concurrent.Semaphore

abstract class OnlineTileSourceBase(
    pName: String?,
    pZoomMinLevel: Int, pZoomMaxLevel: Int, pTileSizePixels: Int,
    pImageFilenameEnding: String?, pBaseUrl: Array<out String?>?, pCopyright: String?,
    pTileSourcePolicy: TileSourcePolicy
) : BitmapTileSourceBase(
    pName, pZoomMinLevel, pZoomMaxLevel, pTileSizePixels,
    pImageFilenameEnding, pCopyright
) {
    private val mBaseUrls: Array<out String?>?

    /**
     * @since 6.1.0
     */
    private val mSemaphore: Semaphore?

    /**
     * @since 6.1.0
     */
    /**
     * @since 6.1.0
     */
    val tileSourcePolicy: TileSourcePolicy

    @JvmOverloads
    constructor(
        aName: String?,
        aZoomMinLevel: Int, aZoomMaxLevel: Int, aTileSizePixels: Int,
        aImageFilenameEnding: String?, aBaseUrl: Array<out String?>?, copyyright: String? = null
    ) : this(
        aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels,
        aImageFilenameEnding, aBaseUrl, copyyright, TileSourcePolicy()
    )

    /**
     * @param pName                a human-friendly name for this tile source
     * @param pZoomMinLevel        the minimum zoom level this tile source can provide
     * @param pZoomMaxLevel        the maximum zoom level this tile source can provide
     * @param pTileSizePixels      the tile size in pixels this tile source provides
     * @param pImageFilenameEnding the file name extension used when constructing the filename
     * @param pBaseUrl             the base url(s) of the tile server used when constructing the url to download the tiles
     * @param pCopyright           the source copyright
     * @param pTileSourcePolicy    tile source policy
     * @since 6.1.0
     */
    init {
        mBaseUrls = pBaseUrl
        this.tileSourcePolicy = pTileSourcePolicy
        if (tileSourcePolicy.maxConcurrent > 0) {
            mSemaphore = Semaphore(tileSourcePolicy.maxConcurrent, true)
        } else {
            mSemaphore = null
        }
    }

    abstract fun getTileURLString(pMapTileIndex: Long): String?

    open val baseUrl: String?
        /**
         * Get the base url, which will be a random one if there are more than one.
         * <br></br>
         * Updated around 6.1.1, if base url list is null or empty, empty string is returned
         */
        get() {
            if (mBaseUrls != null && mBaseUrls.size > 0) return mBaseUrls[random.nextInt(mBaseUrls.size)]
            return ""
        }

    /**
     * @since 6.1.0
     */
    @Throws(InterruptedException::class)
    fun acquire() {
        if (mSemaphore == null) {
            return
        }
        mSemaphore.acquire()
    }

    /**
     * @since 6.1.0
     */
    fun release() {
        if (mSemaphore == null) {
            return
        }
        mSemaphore.release()
    }
}
