package org.osmdroid.util

import android.graphics.Rect

/**
 * A class that will loop around all the map tiles in the given viewport.
 */
abstract class TileLooper @JvmOverloads constructor(horizontalWrapEnabled: Boolean = false, verticalWrapEnabled: Boolean = false) {
    protected val mTiles: Rect = Rect()
    protected var mTileZoomLevel: Int = 0
    var isHorizontalWrapEnabled: Boolean = true
    var isVerticalWrapEnabled: Boolean = true

    init {
        this.isHorizontalWrapEnabled = horizontalWrapEnabled
        this.isVerticalWrapEnabled = verticalWrapEnabled
    }

    protected fun loop(pZoomLevel: Double, pMercatorViewPort: RectL) {
        TileSystem.Companion.getTileFromMercator(pMercatorViewPort, TileSystem.Companion.getTileSize(pZoomLevel), mTiles)
        mTileZoomLevel = TileSystem.Companion.getInputTileZoomLevel(pZoomLevel)

        initialiseLoop()

        val mapTileUpperBound = 1 shl mTileZoomLevel

        /* Draw all the MapTiles (from the upper left to the lower right). */
        for (i in mTiles.left..mTiles.right) {
            for (j in mTiles.top..mTiles.bottom) {
                if ((this.isHorizontalWrapEnabled || (i >= 0 && i < mapTileUpperBound)) && (this.isVerticalWrapEnabled
                            || (j >= 0 && j < mapTileUpperBound))
                ) {
                    val tileX = MyMath.mod(i, mapTileUpperBound)
                    val tileY = MyMath.mod(j, mapTileUpperBound)
                    val tile = MapTileIndex.getTileIndex(mTileZoomLevel, tileX, tileY)
                    handleTile(tile, i, j)
                }
            }
        }

        finaliseLoop()
    }

    open fun initialiseLoop() {
    }

    abstract fun handleTile(pMapTileIndex: Long, pX: Int, pY: Int)

    open fun finaliseLoop() {
    }
}
