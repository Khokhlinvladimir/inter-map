package org.osmdroid.tileprovider.modules

import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.IMapTileProviderCallback
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import java.io.File

/**
 * Causes Osmdroid to load from tiles from only the referenced file sources and
 * no where else. online sources are not even attempted.
 *
 * @since 5.0 Created by alex on 6/14/2015.
 */
class OfflineTileProvider(pRegisterReceiver: IRegisterReceiver, source: Array<File>) :
    MapTileProviderArray(FileBasedTileSource.Companion.getSource(source[0].name), pRegisterReceiver), IMapTileProviderCallback {
    var archives: Array<IArchiveFile>
        private set

    /**
     * Creates a [MapTileProviderBasic].
     * throws with the source[] is null or empty
     */
    init {
        val files: MutableList<IArchiveFile> = ArrayList()

        for (file in source) {
            val temp = ArchiveFileFactory.getArchiveFile(file)
            if (temp != null) files.add(temp)
            else {
                Log.w(IMapView.LOGTAG, "Skipping " + file + ", no tile provider is registered to handle the file extension")
            }
        }
        archives = files.toTypedArray()
        val mapTileFileArchiveProvider = MapTileFileArchiveProvider(pRegisterReceiver, getTileSource(), archives)
        mTileProviderList.add(mapTileFileArchiveProvider)

        val approximationProvider = MapTileApproximater()
        mTileProviderList.add(approximationProvider)
        approximationProvider.addProvider(mapTileFileArchiveProvider)
    }

    override fun detach() {
        for (file in archives) {
            file.close()
        }
        super.detach()
    }

    override fun isDowngradedMode(pMapTileIndex: Long): Boolean {
        return true
    }
}
