package org.osmdroid.samplefragments.data

import android.graphics.Paint
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import org.osmdroid.samplefragments.events.SampleMapEventListener
import org.osmdroid.shape.ShapeConverter.convert
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.PolyOverlayWithIW
import org.osmdroid.views.overlay.Polygon
import java.io.File

/**
 * A simple how to for importing and display an ESRI shape file
 * created on 1/28/2018.
 *
 * @author Alex O'Ree
 */
class SampleShapeFile : SampleMapEventListener() {
    override val sampleTitle: String
        get() = "Shape File Import"

    val MENU_ADD_SHAPE: Int = Menu.FIRST
    val MENU_ADD_BOUNDS: Int = MENU_ADD_SHAPE + 1

    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_ADD_SHAPE, Menu.NONE, "Import a shape file")
        menu.add(0, MENU_ADD_BOUNDS, Menu.NONE, "Draw bounds")
        super.onCreateOptionsMenu(menu, inflater)
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            MENU_ADD_SHAPE -> {
                showPicker()
                return true
            }

            MENU_ADD_BOUNDS -> {
                val pts: MutableList<GeoPoint?> = ArrayList<GeoPoint?>()
                val boundingBox = mMapView!!.getBoundingBox()
                pts.add(GeoPoint(boundingBox!!.getLatNorth(), boundingBox.getLonEast()))
                pts.add(GeoPoint(boundingBox.getLatSouth(), boundingBox.getLonEast()))
                pts.add(GeoPoint(boundingBox.getLatSouth(), boundingBox.getLonWest()))
                pts.add(GeoPoint(boundingBox.getLatNorth(), boundingBox.getLonWest()))
                pts.add(GeoPoint(boundingBox.getLatNorth(), boundingBox.getLonEast()))
                val bounds = Polygon(mMapView)
                bounds.setPoints(pts)
                bounds.setSubDescription(boundingBox.toString())
                // bounds.setStrokeColor(Color.RED);
                mMapView!!.getOverlayManager().add(bounds)
                mMapView!!.invalidate()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun addOverlays() {
        super.addOverlays()
        mMapView!!.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mMapView!!.invalidate()
    }

    private fun showPicker() {
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)

        val registeredExtensions = ArchiveFileFactory.getRegisteredExtensions()
        registeredExtensions.add("shp")


        val ret = registeredExtensions.toTypedArray()
        properties.extensions = ret

        val dialog = FilePickerDialog(getContext(), properties)
        dialog.setTitle("Select a File")
        dialog.setDialogSelectionListener(object : DialogSelectionListener {
            override fun onSelectedFilePaths(files: Array<String?>) {
                //files is the array of the paths of files selected by the Application User.
                try {
                    val folder = convert(mMapView, File(files[0]))
                    for (item in folder) {
                        if (item is PolyOverlayWithIW) {
                            val poly = item
                            poly.setDowngradePixelSizes(50, 25)
                            poly.setDowngradeDisplay(true)
                            val paint = poly.getOutlinePaint()
                            paint.setStyle(Paint.Style.STROKE)
                            paint.setStrokeJoin(Paint.Join.ROUND)
                            paint.setStrokeCap(Paint.Cap.ROUND)
                        }
                    }
                    mMapView!!.getOverlayManager().addAll(folder)
                    mMapView!!.invalidate()
                } catch (e: Exception) {
                    Toast.makeText(getActivity(), "Error importing file: " + e.message, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "error importing file from " + files[0], e)
                }
            }
        })
        dialog.show()
    }
}
