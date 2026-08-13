package org.osmdroid.mapsforge

import org.mapsforge.core.model.Tile
import org.mapsforge.map.layer.renderer.DirectRenderer.TileRefresher
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.util.MapTileIndex


/**
 * This lets you hook up multiple MapsForge files, it will render to the screen the first
 * image that's available.
 *
 *
 * Adapted from code from here: https://github.com/MKergall/osmbonuspack, which is LGPL
 * http://www.salidasoftware.com/how-to-render-mapsforge-tiles-in-osmdroid/
 *
 * @author Salida Software
 * Adapted from code found here : http://www.sieswerda.net/2012/08/15/upping-the-developer-friendliness/
 */
open class MapsForgeTileProvider(pRegisterReceiver: IRegisterReceiver, pTileSource: MapsForgeTileSource, cacheWriter: IFilesystemCache?) :
    MapTileProviderArray(pTileSource, pRegisterReceiver) {
    private var tileWriter: IFilesystemCache? = null

    /**
     * @param pRegisterReceiver
     */
    init {
        val fileSystemProvider = MapTileFilesystemProvider(
            pRegisterReceiver, pTileSource
        )
        mTileProviderList.add(fileSystemProvider)

        val archiveProvider = MapTileFileArchiveProvider(
            pRegisterReceiver, pTileSource
        )
        mTileProviderList.add(archiveProvider)


        if (cacheWriter != null) {
            tileWriter = cacheWriter
        } else {
            tileWriter = SqlTileWriter()
        }

        // Create the module provider; this class provides a TileLoader that
        // actually loads the tile from the map file.
        val moduleProvider = MapsForgeTileModuleProvider(pRegisterReceiver, (getTileSource() as MapsForgeTileSource?)!!, tileWriter!!)


        //this is detached by super


        // Add the module provider to the array of providers; mTileProviderList
        // is defined by the superclass.
        mTileProviderList.add(moduleProvider)

        // In mapsforge the tiles bitmap may need to be refreshed according to neighboring tiles' labels
        pTileSource.addTileRefresher(object : TileRefresher {
            override fun refresh(pTile: Tile) {
                val index = MapTileIndex.getTileIndex(pTile.zoomLevel.toInt(), pTile.tileX, pTile.tileY)
                expireInMemoryCache(index)
            }
        })
    }


    override fun detach() {
        if (tileWriter != null) tileWriter!!.onDetach()
        tileWriter = null
        super.detach()
    }
}
