package org.osmdroid.samplefragments.tileproviders

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import mil.nga.geopackage.GeoPackageFactory
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.osmdroid.R
import org.osmdroid.gpkg.tiles.raster.GeoPackageMapTileModuleProvider
import org.osmdroid.gpkg.tiles.raster.GeoPackageProvider
import org.osmdroid.gpkg.tiles.raster.GeopackageRasterTileSource
import org.osmdroid.mapsforge.MapsForgeTileModuleProvider
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.mapsforge.MapsForgeTileSource.Companion.createFromFiles
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.IArchiveFile
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.modules.MapTileAssetsProvider
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.views.MapView
import java.io.File

/**
 * lets you pick one or more offline tile archives/providers
 * then a named tile source which will have tiles in at least one archive or provider
 * created on 8/20/2017.
 *
 * @author Alex O'Ree
 */
class OfflinePickerSample : BaseSampleFragment(), View.OnClickListener {
    private var btnArchives: Button? = null
    private var btnSource: Button? = null
    private val tileSources: MutableSet<ITileSource?> = HashSet<ITileSource?>()

    override val sampleTitle: String
        get() = "Offline Only Tiles with picker"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_map_two_button, container, false)

        mMapView = root.findViewById<MapView?>(R.id.mapview)
        btnArchives = root.findViewById<Button>(R.id.button1)
        btnArchives!!.setOnClickListener(this)
        btnArchives!!.setText("Pick Files")

        btnSource = root.findViewById<Button>(R.id.button2)
        btnSource!!.setOnClickListener(this)
        btnSource!!.setText("Pick Tile Source")
        return root
    }

    public override fun addOverlays() {
        //not even needed since we are using the offline tile provider only
        this.mMapView!!.setUseDataConnection(true)
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (tileWriter != null) tileWriter!!.onDetach()
    }

    /**
     * step 1, users selects files
     */
    private fun promptForFiles() {
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.MULTI_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)

        val registeredExtensions = ArchiveFileFactory.getRegisteredExtensions()
        //api check
        if (Build.VERSION.SDK_INT >= 14) registeredExtensions.add("gpkg")
        registeredExtensions.add("map")

        properties.extensions = registeredExtensions.toTypedArray()

        val dialog = FilePickerDialog(getContext(), properties)
        dialog.setTitle("Select a File")
        dialog.setDialogSelectionListener(object : DialogSelectionListener {
            override fun onSelectedFilePaths(files: Array<String?>?) {
                //files is the array of the paths of files selected by the Application User.
                setProviderConfig(files)
            }
        })
        dialog.show()
    }

    var tileWriter: IFilesystemCache? = null

    /**
     * step two, configure our offline tile provider
     *
     * @param files
     */
    private fun setProviderConfig(files: Array<String?>?) {
        if (files == null || files.size == 0) return
        val simpleRegisterReceiver = SimpleRegisterReceiver(getContext())
        if (tileWriter != null) tileWriter!!.onDetach()

        tileWriter = SqlTileWriter()

        tileSources.clear()
        val providers: MutableList<MapTileModuleProviderBase?> = ArrayList<MapTileModuleProviderBase?>()
        providers.add(MapTileAssetsProvider(simpleRegisterReceiver, getContext()!!.getAssets()))

        val geopackages: MutableList<File?> = ArrayList<File?>()
        val forgeMaps: MutableList<File?> = ArrayList<File?>()
        val archives: MutableList<IArchiveFile?> = ArrayList<IArchiveFile?>()
        //this part seperates the geopackage and maps forge stuff since they are handled differently
        for (i in files.indices) {
            val archive = File(files[i])
            if (archive.getName().endsWith("gpkg")) {
                geopackages.add(archive)
            } else if (archive.getName().endsWith("map")) {
                forgeMaps.add(archive)
            } else {
                val temp = ArchiveFileFactory.getArchiveFile(archive)
                if (temp != null) {
                    val tileSources: Set<String?> = temp.tileSources ?: emptySet()
                    val iterator = tileSources.iterator()
                    while (iterator.hasNext()) {
                        this.tileSources.add(FileBasedTileSource.getSource(iterator.next()))
                        archives.add(temp)
                    }
                }
            }
        }

        //setup the standard osmdroid-android library supported offline tile providers
        val archArray = archives.toTypedArray()
        val mapTileFileArchiveProvider = MapTileFileArchiveProvider(simpleRegisterReceiver, TileSourceFactory.DEFAULT_TILE_SOURCE, archArray)


        var geopackage: GeoPackageMapTileModuleProvider? = null
        var provider: GeoPackageProvider? = null
        //geopackages
        if (!geopackages.isEmpty()) {
            val maps = geopackages.toTypedArray()

            val manager = GeoPackageFactory.getManager(getContext())

            // Import database
            for (f in maps) {
                try {
                    val imported = manager.importGeoPackage(f)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            provider = GeoPackageProvider(maps, getContext()!!)
            geopackage = provider.geoPackageMapTileModuleProvider()
            providers.add(geopackage)
            val geotileSources: MutableList<GeopackageRasterTileSource?> = ArrayList<GeopackageRasterTileSource?>()
            geotileSources.addAll(geopackage.getTileSources())
            tileSources.addAll(geotileSources)
            //TODO add feature tiles here too
        }

        var moduleProvider: MapsForgeTileModuleProvider? = null
        if (!forgeMaps.isEmpty()) {
            //fire up the forge maps...
            var theme: XmlRenderTheme? = null
            try {
                theme = AssetsRenderTheme(getContext()!!.getApplicationContext().getAssets(), "renderthemes/", "rendertheme-v4.xml")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            val forge = forgeMaps.toTypedArray()
            val fromFiles = createFromFiles(forge, theme, "rendertheme-v4")
            tileSources.add(fromFiles)
            // Create the module provider; this class provides a TileLoader that
            // actually loads the tile from the map file.
            moduleProvider = MapsForgeTileModuleProvider(simpleRegisterReceiver, fromFiles, tileWriter!!)
        }


        val approximationProvider = MapTileApproximater()
        approximationProvider.addProvider(mapTileFileArchiveProvider)

        if (geopackage != null) {
            providers.add(geopackage)
            approximationProvider.addProvider(geopackage)
        }
        if (moduleProvider != null) {
            providers.add(moduleProvider)
            approximationProvider.addProvider(moduleProvider)
        }

        providers.add(mapTileFileArchiveProvider)
        providers.add(approximationProvider)
        val providerArray = arrayOfNulls<MapTileModuleProviderBase>(providers.size)
        for (i in providers.indices) {
            providerArray[i] = providers.get(i)
        }


        val obj = MapTileProviderArray(TileSourceFactory.DEFAULT_TILE_SOURCE, simpleRegisterReceiver, providerArray)
        mMapView!!.setTileProvider(obj)

        //ok everything is setup, we now have 0 or many tile sources available, ask the user
        promptForTileSource()
    }

    /**
     * step 3 ask for the tile source
     */
    private fun promptForTileSource() {
        val builderSingle = AlertDialog.Builder(getContext())
        builderSingle.setIcon(R.drawable.icon)
        builderSingle.setTitle("Select Offline Tile source:-")

        val arrayAdapter = ArrayAdapter<ITileSource?>(getContext()!!, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(tileSources)

        builderSingle.setNegativeButton("cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.dismiss()
            }
        })

        builderSingle.setAdapter(arrayAdapter, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val strName = arrayAdapter.getItem(which)
                val builderInner = AlertDialog.Builder(getContext())
                builderInner.setMessage(strName!!.name())
                builderInner.setTitle("Your Selected Item is")
                builderInner.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        mMapView!!.setTileSource(strName) //new XYTileSource(strName, 0, 22, 256, "png", new String[0]));
                        //on tile sources that are supported, center the map an area that's within bounds
                        if (strName is MapsForgeTileSource) {
                            val src = strName
                            mMapView!!.post(object : Runnable {
                                override fun run() {
                                    mMapView!!.controller!!.setZoom(src.minimumZoomLevel)
                                    mMapView!!.setMinZoomLevel(src.minimumZoomLevel.toDouble())
                                    mMapView!!.setMaxZoomLevel(src.maximumZoomLevel.toDouble())

                                    mMapView!!.invalidate()
                                    mMapView!!.zoomToBoundingBox(src.boundsOsmdroid, true)
                                }
                            })
                        } else if (strName is GeopackageRasterTileSource) {
                            val src = strName
                            mMapView!!.post(object : Runnable {
                                override fun run() {
                                    mMapView!!.controller!!.setZoom(src.minimumZoomLevel)
                                    mMapView!!.setMinZoomLevel(src.minimumZoomLevel.toDouble())
                                    mMapView!!.setMaxZoomLevel(src.maximumZoomLevel.toDouble())
                                    mMapView!!.invalidate()
                                    mMapView!!.zoomToBoundingBox(src.bounds, true)
                                }
                            })
                        }

                        dialog.dismiss()
                    }
                })
                builderInner.show()
            }
        })
        builderSingle.show()
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.button1 ->                 //pick files
                promptForFiles()

            R.id.button2 ->                 //pick source
                promptForTileSource()
        }
    }
}
