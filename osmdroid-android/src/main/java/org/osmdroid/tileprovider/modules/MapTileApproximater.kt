package org.osmdroid.tileprovider.modules

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.BitmapPool
import org.osmdroid.tileprovider.ExpirableBitmapDrawable
import org.osmdroid.tileprovider.ReusableBitmapDrawable
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.TileSystem
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

/**
 * The MapTileApproximater computes approximation of tiles.
 * The approximation is based on the tiles of the same region, but on lower zoom level tiles.
 * An obvious use is in offline mode: it's better to display an approximation than an empty grey square.
 */
class MapTileApproximater @JvmOverloads constructor(
    pThreadPoolSize: Int = instance!!.tileFileSystemThreads.toInt(),
    pPendingQueueSize: Int = instance!!.tileFileSystemMaxQueueSize.toInt()
) : MapTileModuleProviderBase(pThreadPoolSize, pPendingQueueSize) {
    private val mProviders: MutableList<MapTileModuleProviderBase> = CopyOnWriteArrayList<MapTileModuleProviderBase>()
    private var minZoomLevel = 0

    fun addProvider(pProvider: MapTileModuleProviderBase?) {
        mProviders.add(pProvider!!)
        computeZoomLevels()
    }

    private fun computeZoomLevels() {
        var first = true
        minZoomLevel = OpenStreetMapTileProviderConstants.MINIMUM_ZOOM_LEVEL
        for (provider in mProviders) {
            val otherMin = provider.getMinimumZoomLevel()

            if (first) {
                first = false
                minZoomLevel = otherMin
            } else {
                minZoomLevel = min(minZoomLevel, otherMin)
            }
        }
    }

    override fun getUsesDataConnection(): Boolean {
        return false
    }

    override fun getName(): String {
        return "Offline Tile Approximation Provider"
    }

    override fun getThreadGroupName(): String {
        return "approximater"
    }

    override fun getTileLoader(): MapTileModuleProviderBase.TileLoader {
        return TileLoader()
    }

    override fun getMinimumZoomLevel(): Int {
        return minZoomLevel
    }

    override fun getMaximumZoomLevel(): Int {
        return TileSystem.maximumZoomLevel
    }

    @Deprecated("")
    override fun setTileSource(pTileSource: ITileSource?) {
        // not relevant
    }

    protected inner class TileLoader : MapTileModuleProviderBase.TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            val bitmap = approximateTileFromLowerZoom(pMapTileIndex)
            if (bitmap != null) {
                val drawable = BitmapDrawable(bitmap)
                ExpirableBitmapDrawable.Companion.setState(drawable, ExpirableBitmapDrawable.Companion.SCALED)
                return drawable
            }
            return null
        }
    }

    /**
     * Approximate a tile from a lower zoom level
     *
     * @param pMapTileIndex Destination tile, for the same place on the planet as the source, but on a higher zoom
     */
    fun approximateTileFromLowerZoom(pMapTileIndex: Long): Bitmap? {
        var zoomDiff = 1
        while (MapTileIndex.getZoom(pMapTileIndex) - zoomDiff >= OpenStreetMapTileProviderConstants.MINIMUM_ZOOM_LEVEL) {
            val bitmap = approximateTileFromLowerZoom(pMapTileIndex, zoomDiff)
            if (bitmap != null) {
                return bitmap
            }
            zoomDiff++
        }
        return null
    }

    /**
     * Approximate a tile from a lower zoom level
     *
     * @param pMapTileIndex Destination tile, for the same place on the planet as the source, but on a higher zoom
     * @param pZoomDiff     Zoom level difference between the destination and the source; strictly positive
     */
    fun approximateTileFromLowerZoom(pMapTileIndex: Long, pZoomDiff: Int): Bitmap? {
        for (provider in mProviders) {
            val bitmap: Bitmap? = approximateTileFromLowerZoom(provider, pMapTileIndex, pZoomDiff)
            if (bitmap != null) {
                return bitmap
            }
        }
        return null
    }

    override fun detach() {
        super.detach()
        mProviders.clear()
    }

    companion object {
        /**
         * Approximate a tile from a lower zoom level
         *
         * @param pProvider     Source tile provider
         * @param pMapTileIndex Destination tile, for the same place on the planet as the source, but on a higher zoom
         * @param pZoomDiff     Zoom level difference between the destination and the source; strictly positive
         */
        fun approximateTileFromLowerZoom(
            pProvider: MapTileModuleProviderBase,
            pMapTileIndex: Long, pZoomDiff: Int
        ): Bitmap? {
            if (pZoomDiff <= 0) {
                return null
            }
            val srcZoomLevel = MapTileIndex.getZoom(pMapTileIndex) - pZoomDiff
            if (srcZoomLevel < pProvider.getMinimumZoomLevel()) {
                return null
            }
            if (srcZoomLevel > pProvider.getMaximumZoomLevel()) {
                return null
            }
            val srcTile = MapTileIndex.getTileIndex(
                srcZoomLevel,
                MapTileIndex.getX(pMapTileIndex) shr pZoomDiff,
                MapTileIndex.getY(pMapTileIndex) shr pZoomDiff
            )
            try {
                val srcDrawable = pProvider.getTileLoader().loadTileIfReachable(srcTile)
                if (srcDrawable !is BitmapDrawable) {
                    return null
                }
                return approximateTileFromLowerZoom(srcDrawable, pMapTileIndex, pZoomDiff)
            } catch (e: Exception) {
                return null
            }
        }

        /**
         * Approximate a tile from a lower zoom level
         *
         * @param pSrcDrawable  Source tile bitmap
         * @param pMapTileIndex Destination tile, for the same place on the planet as the source, but on a higher zoom
         * @param pZoomDiff     Zoom level difference between the destination and the source; strictly positive
         */
        fun approximateTileFromLowerZoom(
            pSrcDrawable: BitmapDrawable,
            pMapTileIndex: Long, pZoomDiff: Int
        ): Bitmap? {
            if (pZoomDiff <= 0) {
                return null
            }
            val tileSizePixels = pSrcDrawable.getBitmap().getWidth()
            val bitmap: Bitmap = getTileBitmap(tileSizePixels)
            val canvas = Canvas(bitmap)
            val isReusable = pSrcDrawable is ReusableBitmapDrawable
            val reusableBitmapDrawable = if (isReusable) pSrcDrawable as ReusableBitmapDrawable else null
            var success = false
            if (isReusable) {
                reusableBitmapDrawable!!.beginUsingDrawable()
            }
            try {
                if (!isReusable || reusableBitmapDrawable!!.isBitmapValid) {
                    val srcSize = tileSizePixels shr pZoomDiff
                    if (srcSize == 0) {
                        success = false
                    } else {
                        val srcX = (MapTileIndex.getX(pMapTileIndex) % (1 shl pZoomDiff)) * srcSize
                        val srcY = (MapTileIndex.getY(pMapTileIndex) % (1 shl pZoomDiff)) * srcSize
                        val srcRect = Rect(srcX, srcY, srcX + srcSize, srcY + srcSize)
                        val dstRect = Rect(0, 0, tileSizePixels, tileSizePixels)
                        canvas.drawBitmap(pSrcDrawable.getBitmap(), srcRect, dstRect, null)
                        success = true
                    }
                }
            } finally {
                if (isReusable) reusableBitmapDrawable!!.finishUsingDrawable()
            }
            if (!success) {
                return null
            }
            return bitmap
        }

        /**
         * Try to get a tile bitmap from the pool, otherwise allocate a new one
         */
        fun getTileBitmap(pTileSizePx: Int): Bitmap {
            val bitmap: Bitmap? = BitmapPool.instance.obtainSizedBitmapFromPool(pTileSizePx, pTileSizePx)
            if (bitmap != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    // without that, the retrieved bitmap forgets it allowed transparency
                    bitmap.setHasAlpha(true)
                }
                // without that, the bitmap keeps its previous contents when transparent content is copied on it
                bitmap.eraseColor(Color.TRANSPARENT)
                return bitmap
            }
            return Bitmap.createBitmap(pTileSizePx, pTileSizePx, Bitmap.Config.ARGB_8888)
        }
    }
}
