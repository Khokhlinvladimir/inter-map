package org.osmdroid.samplefragments.geopackage

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnGenericMotionListener
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import org.osmdroid.R
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.gpkg.tiles.raster.GeoPackageProvider
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.events.SampleMapEventListener
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.util.StorageUtils
import org.osmdroid.views.MapView
import java.io.File
import java.io.FileFilter
import java.util.Collections
import java.util.Locale

/**
 * One way for viewing geopackage tiles to the osmdroid view
 *
 *
 * created on 1/12/2017.
 *
 * @author Alex O'Ree
 * @since 5.6.3
 */
class GeopackageSample : BaseSampleFragment() {
    var textViewCurrentLocation: TextView? = null

    var currentSource: XYTileSource? = null
    var geoPackageProvider: GeoPackageProvider? = null
    var alertDialog: AlertDialog? = null

    override val sampleTitle: String
        get() = "Geopackage Raster Tiles"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Log.d(TAG, "onCreate")
    }


    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.map_with_locationbox, container, false)
        mMapView = root.findViewById<MapView?>(R.id.mapview)

        mMapView!!.setOnGenericMotionListener(object : OnGenericMotionListener {
            /**
             * mouse wheel zooming ftw
             * http://stackoverflow.com/questions/11024809/how-can-my-view-respond-to-a-mousewheel
             * @param v
             * @param event
             * @return
             */
            override fun onGenericMotion(v: View?, event: MotionEvent): Boolean {
                if (0 != (event.getSource() and InputDevice.SOURCE_CLASS_POINTER)) {
                    when (event.getAction()) {
                        MotionEvent.ACTION_SCROLL -> {
                            if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) mMapView!!.controller!!.zoomOut()
                            else {
                                mMapView!!.controller!!.zoomIn()
                            }
                            return true
                        }
                    }
                }
                return false
            }
        })

        textViewCurrentLocation = root.findViewById<TextView>(R.id.textViewCurrentLocation)
        return root
    }


    public override fun addOverlays() {
        super.addOverlays()
        //first let's up our map source, geopackage needs you to explicitly specify which map files to load
        //this bit does some basic file system scanning
        val mapfiles = findMapFiles()
        //do a simple scan of local storage for .gpkg files.
        val maps = mapfiles.toTypedArray()
        if (maps.size == 0) {
            //show a warning that no map files were found
            val alertDialogBuilder = AlertDialog.Builder(
                getContext()
            )

            // set title
            alertDialogBuilder.setTitle("No Geopackage files found")

            // set dialog message
            alertDialogBuilder
                .setMessage(
                    "In order to render map tiles, you'll need to either create or obtain .gpkg files. See http://www.geopackage.org/ for more info. Place them in "
                            + instance!!.osmdroidBasePath!!.getAbsolutePath()
                )
                .setCancelable(false)
                .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, id: Int) {
                        if (alertDialog != null) {
                            alertDialog!!.hide()
                            alertDialog!!.dismiss()
                        }
                    }
                })


            // create alert dialog
            alertDialog = alertDialogBuilder.create()

            // show it
            alertDialog!!.show()
        } else {
            Toast.makeText(getContext(), "Loaded " + maps.size + " map files", Toast.LENGTH_LONG).show()
            geoPackageProvider = GeoPackageProvider(maps, this.getContext()!!)
            mMapView!!.setTileProvider(geoPackageProvider)
            var sourceSet = false

            val tileSources = geoPackageProvider!!.geoPackageMapTileModuleProvider().getTileSources()
            if (!tileSources.isEmpty()) {
                mMapView!!.setTileSource(tileSources.get(0)!!)

                mMapView!!.zoomToBoundingBox(tileSources.get(0)!!.bounds, true)
                mMapView!!.controller!!.setZoom(tileSources.get(0)!!.minimumZoomLevel)
                sourceSet = true
            }


            if (!sourceSet) {
                Toast.makeText(getContext(), "No tile source is available, get your geopackages for 'tiles' tables", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(getContext(), "Tile source set to " + mMapView!!.getTileProvider()!!.getTileSource().name(), Toast.LENGTH_LONG).show()
            }
        }

        mMapView!!.setMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onScroll " + event.x + "," + event.y)
                updateInfo()
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onZoom " + event.zoomLevel)
                updateInfo()
                return true
            }
        })
        updateInfo()
    }

    public override fun onPause() {
        super.onPause()
        if (alertDialog != null) {
            alertDialog!!.hide()
            alertDialog!!.dismiss()
        }
        alertDialog = null
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (alertDialog != null) {
            alertDialog!!.hide()
            alertDialog!!.dismiss()
        }
        alertDialog = null
        this.currentSource = null
        if (geoPackageProvider != null) geoPackageProvider!!.detach()
    }


    private fun updateInfo() {
        val sb = StringBuilder()
        val mapCenter = mMapView!!.mapCenter
        sb.append(
            (SampleMapEventListener.Companion.df.format(mapCenter!!.latitude) + "," +
                    SampleMapEventListener.Companion.df.format(mapCenter.longitude)
                    + ",zoom=" + mMapView!!.zoomLevelDouble)
        )

        if (currentSource != null) {
            sb.append("\n")
            sb.append(currentSource!!.name() + "," + currentSource!!.getBaseUrl())
        }


        textViewCurrentLocation!!.setText(sb.toString())
    }

    /**
     * simple function to scan for paths that match /something/osmdroid/ *.map to find database files
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
                return pathname.getName().lowercase(Locale.getDefault()).endsWith(".gpkg")
            }
        })
        if (files != null) {
            Collections.addAll<File?>(ret, *files)
        }
        return ret
    }
}
