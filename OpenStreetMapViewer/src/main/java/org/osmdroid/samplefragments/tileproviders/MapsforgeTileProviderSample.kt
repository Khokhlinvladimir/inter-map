package org.osmdroid.samplefragments.tileproviders

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.osmdroid.config.Configuration.instance
import org.osmdroid.mapsforge.MapsForgeTileProvider
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.mapsforge.MapsForgeTileSource.Companion.createFromFiles
import org.osmdroid.mapsforge.MapsForgeTileSource.Companion.createInstance
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.tileprovider.util.StorageUtils
import java.io.File
import java.io.FileFilter
import java.util.Collections
import java.util.Locale

/**
 * An example of using MapsForge in osmdroid
 * created on 1/12/2017.
 *
 * @author Alex O'Ree
 */
class MapsforgeTileProviderSample : BaseSampleFragment() {
    var fromFiles: MapsForgeTileSource? = null
    var forge: MapsForgeTileProvider? = null
    var alertDialog: AlertDialog? = null

    override val sampleTitle: String
        get() = "Mapsforge tiles"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false) //turn off the menu to prevent accidential tile source changes
        Log.d(TAG, "onCreate")

        /**
         * super important to configure some of the mapsforge settings first
         */
        createInstance(this.getActivity()!!.getApplication())

        /*
        not sure how important these are....
        MapFile.wayFilterEnabled = true;
        MapFile.wayFilterDistance = 20;
        MapWorkerPool.DEBUG_TIMING = true;
        MapWorkerPool.NUMBER_OF_THREADS = MapWorkerPool.DEFAULT_NUMBER_OF_THREADS;
*/
    }


    public override fun addOverlays() {
        super.addOverlays()
        //first let's up our map source, mapsforge needs you to explicitly specify which map files to load
        //this bit does some basic file system scanning
        val mapfiles = findMapFiles()
        //do a simple scan of local storage for .map files.
        val maps = mapfiles.toTypedArray()
        if (maps.isEmpty()) {
            //show a warning that no map files were found
            val alertDialogBuilder = AlertDialog.Builder(
                getContext()
            )

            // set title
            alertDialogBuilder.setTitle("No Mapsforge files found")

            // set dialog message
            alertDialogBuilder
                .setMessage(
                    "In order to render map tiles, you'll need to either create or obtain mapsforge .map files. See https://github.com/mapsforge/mapsforge for more info. Store them in "
                            + instance!!.osmdroidBasePath!!.getAbsolutePath()
                )
                .setCancelable(false)
                .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, id: Int) {
                        if (alertDialog != null) alertDialog!!.dismiss()
                    }
                })


            // create alert dialog
            alertDialog = alertDialogBuilder.create()

            // show it
            alertDialog!!.show()
        } else {
            Toast.makeText(getContext(), "Loaded " + maps.size + " map files", Toast.LENGTH_LONG).show()

            //this creates the forge provider and tile sources

            //protip: when changing themes, you should also change the tile source name to prevent cached tiles

            //null is ok here, uses the default rendering theme if it's not set
            var theme: XmlRenderTheme? = null
            try {
                theme = AssetsRenderTheme(getContext()!!.getApplicationContext().getAssets(), "renderthemes/", "rendertheme-v4.xml")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            fromFiles = createFromFiles(maps, theme, "rendertheme-v4")
            forge = MapsForgeTileProvider(
                SimpleRegisterReceiver(getContext()),
                fromFiles!!, null
            )

            // with value of .5F the map tiles more closely resemble that of native MapsForge basic map
            // fromFiles.setUserScaleFactor(.5F);
            mMapView!!.setTileProvider(forge)


            //now for a magic trick
            //since we have no idea what will be on the
            //user's device and what geographic area it is, this will attempt to center the map
            //on whatever the map data provides
            mMapView!!.controller!!.setZoom(fromFiles!!.minimumZoomLevel)
            mMapView!!.zoomToBoundingBox(fromFiles!!.boundsOsmdroid, true)
        }
    }

    public override fun onPause() {
        super.onPause()
        if (alertDialog != null) alertDialog!!.dismiss()
        alertDialog = null
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (alertDialog != null) {
            alertDialog!!.hide()
            alertDialog!!.dismiss()
            alertDialog = null
        }
        if (fromFiles != null) fromFiles!!.dispose()
        if (forge != null) forge!!.detach()
        AndroidGraphicFactory.clearResourceMemoryCache()
    }

    /**
     * simple function to scan for paths that match /something/osmdroid/ *.map to find mapforge database files
     *
     * @return
     */
    protected fun findMapFiles(): MutableSet<File?> {
        val maps: MutableSet<File?> = HashSet<File?>()
        val storageList = StorageUtils.getStorageList(getActivity())
        for (i in storageList.indices) {
            val f = File(storageList.get(i)!!.path + File.separator + "osmdroid" + File.separator)
            if (f.exists()) {
                maps.addAll(scan(f))
            }
        }
        return maps
    }

    private fun scan(f: File): MutableCollection<out File?> {
        val ret: MutableList<File?> = ArrayList<File?>()
        val files = f.listFiles(object : FileFilter {
            override fun accept(pathname: File): Boolean {
                return pathname.getName().lowercase(Locale.getDefault()).endsWith(".map")
            }
        })
        if (files != null) {
            Collections.addAll<File?>(ret, *files)
        }
        return ret
    }
}
