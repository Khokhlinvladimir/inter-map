package org.osmdroid.sample.tiles

import android.content.res.AssetManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.io.IOException

class AssetsTileSource(
        private val assetManager: AssetManager,
        private val name: String = "customtiles",
        zoomMinLevel: Int,
        zoomMaxLevel: Int,
        tileSizePixels: Int,
        private val imageFilenameEnding: String
) : BitmapTileSourceBase(name, zoomMinLevel, zoomMaxLevel, tileSizePixels, imageFilenameEnding) {

    override fun getTileRelativeFilenameString(pMapTileIndex: Long): String {
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val z = MapTileIndex.getZoom(pMapTileIndex)
        return "$name/$z/$x/$y.$imageFilenameEnding"
    }

    override fun getDrawable(aFilePath: String?): Drawable? {
        if (aFilePath == null) return null
        return try {
            assetManager.open(aFilePath).use { inputStream ->
                Drawable.createFromStream(inputStream, null)
            }
        } catch (e: IOException) {
            null // Обработка ошибки или логирование
        }
    }
}

fun setupCustomTilesOverlays(mapView: MapView,  assetManager: AssetManager) {

    Configuration.instance?.cacheMapTileCount = 60000.toShort()
    Configuration.instance?.cacheMapTileOvershoot = 2000.toShort()
    Configuration.instance?.osmdroidTileCache = mapView.context.getExternalFilesDir(null)


    val customTileSource = AssetsTileSource(
            assetManager = assetManager,
            name = "customtiles",
            zoomMinLevel = 15,
            zoomMaxLevel = 19,
            tileSizePixels = 256,
            imageFilenameEnding = "png"
    )

    val tileProvider = MapTileProviderBasic(mapView.context).apply {
        tileSource = customTileSource
    }

    val tilesOverlay = TilesOverlay(tileProvider, mapView.context).apply {
        loadingBackgroundColor = Color.TRANSPARENT // Прозрачный фон при загрузке
    }

    tilesOverlay.loadingLineColor = Color.TRANSPARENT


     mapView.getOverlays()?.add(tilesOverlay)

    // Данный метод отключает мерцание, но удаляет тайлы по умолчанию.
    // mapView.setTileSource(customTileSource)
}
