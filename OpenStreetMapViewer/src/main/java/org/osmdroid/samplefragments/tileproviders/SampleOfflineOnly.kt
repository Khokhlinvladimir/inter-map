package org.osmdroid.samplefragments.tileproviders

import android.os.Environment
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import java.io.File
import java.util.Locale

/**
 * An example on how to setup osmdroid to only use offline map archives, how to
 * query the map archives for the available tile sources
 *
 * @author alex
 * @since 5.0
 */
class SampleOfflineOnly : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Offline Only Tiles with custom 404 image"

    public override fun addOverlays() {
        //not even needed since we are using the offline tile provider only
        this.mMapView!!.setUseDataConnection(false)

        //https://github.com/osmdroid/osmdroid/issues/330
        //custom image placeholder for files that aren't available
        mMapView!!.getTileProvider()!!.setTileLoadFailureImage(getResources().getDrawable(R.drawable.notfound))


        //first we'll look at the default location for tiles that we support
        val f = File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/osmdroid/")
        if (f.exists()) {
            val list = f.listFiles()
            if (list != null) {
                for (i in list.indices) {
                    if (list[i]!!.isDirectory()) {
                        continue
                    }
                    var name = list[i]!!.getName().lowercase(Locale.getDefault())
                    if (!name.contains(".")) {
                        continue  //skip files without an extension
                    }
                    name = name.substring(name.lastIndexOf(".") + 1)
                    if (name.length == 0) {
                        continue
                    }
                    if (ArchiveFileFactory.isFileExtensionRegistered(name)) {
                        try {
                            //ok found a file we support and have a driver for the format, for this demo, we'll just use the first one

                            //create the offline tile provider, it will only do offline file archives
                            //again using the first file

                            val tileProvider = OfflineTileProvider(
                                SimpleRegisterReceiver(getActivity()),
                                arrayOf(list[i]!!)
                            )

                            //tell osmdroid to use that provider instead of the default rig which is (asserts, cache, files/archives, online
                            mMapView!!.setTileProvider(tileProvider)

                            //this bit enables us to find out what tiles sources are available. note, that this action may take some time to run
                            //and should be ran asynchronously. we've put it inline for simplicity
                            var source = ""
                            val archives = tileProvider.archives
                            if (archives.size > 0) {
                                //cheating a bit here, get the first archive file and ask for the tile sources names it contains
                                val tileSources: Set<String?>? = archives[0]!!.tileSources
                                //presumably, this would be a great place to tell your users which tiles sources are available
                                val sourceName = tileSources?.firstOrNull { it != null }
                                if (sourceName != null) {
                                    //ok good, we found at least one tile source, create a basic file based tile source using that name
                                    //and set it. If we don't set it, osmdroid will attempt to use the default source, which is "MAPNIK",
                                    //which probably won't match your offline tile source, unless it's MAPNIK
                                    source = sourceName
                                    this.mMapView!!.setTileSource(FileBasedTileSource.getSource(source))
                                } else {
                                    this.mMapView!!.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                                }
                            } else {
                                this.mMapView!!.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                            }

                            Snackbar.make(getView()!!, "Using " + list[i]!!.getAbsolutePath() + " " + source, Snackbar.LENGTH_SHORT).show()
                            this.mMapView!!.invalidate()
                            return
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
            Toast.makeText(getActivity(), f.getAbsolutePath() + " did not have any files I can open! Try using MOBAC", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getActivity(), f.getAbsolutePath() + " dir not found!", Toast.LENGTH_SHORT).show()
        }
    }
}
