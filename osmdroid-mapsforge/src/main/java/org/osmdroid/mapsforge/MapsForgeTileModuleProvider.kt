package org.osmdroid.mapsforge

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.MapTileFileStorageProviderBase
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Adapted from code from here: https://github.com/MKergall/osmbonuspack, which is LGPL
 * http://www.salidasoftware.com/how-to-render-mapsforge-tiles-in-osmdroid/
 *
 * @author Salida Software
 * Adapted from code found here : http://www.sieswerda.net/2012/08/15/upping-the-developer-friendliness/
 */
open class MapsForgeTileModuleProvider
/**
 * Constructor
 *
 * @param receiverRegistrar
 * @param tileSource
 */(receiverRegistrar: IRegisterReceiver, @JvmField protected var tileSource: MapsForgeTileSource, @JvmField protected var tilewriter: IFilesystemCache) :
    MapTileFileStorageProviderBase(
        receiverRegistrar,
        instance!!.tileFileSystemThreads.toInt(),
        instance!!.tileFileSystemMaxQueueSize.toInt()
    ) {
    override fun getName(): String {
        return "MapsforgeTiles Provider"
    }

    override fun getThreadGroupName(): String {
        return "mapsforgetilesprovider"
    }

    override fun getTileLoader(): MapTileModuleProviderBase.TileLoader {
        return TileLoader()
    }

    override fun getUsesDataConnection(): Boolean {
        return false
    }

    override fun getMinimumZoomLevel(): Int {
        return tileSource.minimumZoomLevel
    }

    override fun getMaximumZoomLevel(): Int {
        return tileSource.maximumZoomLevel
    }

    override fun setTileSource(tileSource: ITileSource?) {
        //prevent re-assignment of tile source
        if (tileSource is MapsForgeTileSource) {
            this.tileSource = tileSource
        }
    }

    private inner class TileLoader : MapTileModuleProviderBase.TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            //TODO find a more efficient want to do this, seems overly complicated
            var dbgPrefix: String? = null
            if (instance!!.isDebugTileProviders) {
                dbgPrefix = "MapsForgeTileModuleProvider.TileLoader.loadTile(" + MapTileIndex.toString(pMapTileIndex) + "): "
                Log.d(IMapView.LOGTAG, dbgPrefix + "tileSource.renderTile")
            }
            val image = tileSource.renderTile(pMapTileIndex)
            if (image != null && image is BitmapDrawable) {
                val stream = ByteArrayOutputStream()
                image.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
                val bitmapdata = stream.toByteArray()
                try {
                    stream.close()
                } catch (e: IOException) {
                    //NO OP
                }

                if (instance!!.isDebugTileProviders) {
                    Log.d(
                        IMapView.LOGTAG, dbgPrefix +
                                "save tile " + bitmapdata.size +
                                " bytes to " + tileSource.getTileRelativeFilenameString(pMapTileIndex)
                    )
                }

                var bais: ByteArrayInputStream? = null
                try {
                    bais = ByteArrayInputStream(bitmapdata)
                    tilewriter.saveFile(tileSource, pMapTileIndex, bais, null)
                } catch (ex: Exception) {
                    Log.w(IMapView.LOGTAG, "forge error storing tile cache", ex)
                } finally {
                    if (bais != null) try {
                        bais.close()
                    } catch (e: IOException) {
                        //NO OP
                    }
                }
            }
            return image
        }
    }
}
