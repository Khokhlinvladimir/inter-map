package org.osmdroid.samplefragments.bookmarks

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import org.osmdroid.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger


/**
 * created on 2/11/2018.
 * TODO it would be nice to have the ability to select an icon for the location
 *
 * @author Alex O'Ree
 */
class BookmarkSample : BaseSampleFragment(), LocationListener {
    private var lm: LocationManager? = null
    private var datastore: BookmarkDatastore? = null
    private var mMyLocationOverlay: MyLocationNewOverlay? = null
    private var currentLocation: Location? = null


    override val sampleTitle: String
        get() = "Bookmark Sample"


    var addBookmark: AlertDialog? = null

    public override fun addOverlays() {
        super.addOverlays()
        if (datastore == null) datastore = BookmarkDatastore()
        //add all our bookmarks to the view
        mMapView!!.getOverlayManager().addAll(datastore!!.getBookmarksAsMarkers(mMapView))

        this.mMyLocationOverlay = MyLocationNewOverlay(mMapView!!)
        mMyLocationOverlay!!.setEnabled(true)


        this.mMapView!!.getOverlays()!!.add(mMyLocationOverlay)

        //support long press to add a bookmark

        //TODO menu item to
        val events = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                showDialog(requireNotNull(p))
                return true
            }
        })
        mMapView!!.getOverlayManager().add(events)
    }

    private fun showDialog(p: GeoPoint) {
        if (addBookmark != null) addBookmark!!.dismiss()

        //TODO prompt for user input
        val builder = AlertDialog.Builder(getContext())

        val view = View.inflate(getContext(), R.layout.bookmark_add_dialog, null)
        builder.setView(view)
        val lat = view.findViewById<EditText>(R.id.bookmark_lat)
        lat.setText(p.latitude.toString() + "")
        val lon = view.findViewById<EditText>(R.id.bookmark_lon)
        lon.setText(p.longitude.toString() + "")
        val title = view.findViewById<EditText>(R.id.bookmark_title)
        val description = view.findViewById<EditText>(R.id.bookmark_description)

        view.findViewById<View?>(R.id.bookmark_cancel).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                addBookmark!!.dismiss()
            }
        })
        view.findViewById<View?>(R.id.bookmark_ok).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var valid = true
                var latD = 0.0
                var lonD = 0.0
                //basic validate input
                try {
                    latD = lat.getText().toString().toDouble()
                } catch (ex: Exception) {
                    valid = false
                }
                try {
                    lonD = lon.getText().toString().toDouble()
                } catch (ex: Exception) {
                    valid = false
                }

                if (!getTileSystem().isValidLatitude(latD)) valid = false
                if (!getTileSystem().isValidLongitude(lonD)) valid = false

                if (valid) {
                    val m = Marker(mMapView!!)
                    m.setId(UUID.randomUUID().toString())
                    m.setTitle(title.getText().toString())
                    m.setSubDescription(description.getText().toString())

                    m.position = GeoPoint(latD, lonD)
                    m.setSnippet(m.position.toDoubleString())
                    datastore!!.addBookmark(m)
                    mMapView!!.getOverlayManager().add(m)
                    mMapView!!.invalidate()
                }
                addBookmark!!.dismiss()
            }
        })

        addBookmark = builder.show()
    }

    public override fun onPause() {
        super.onPause()
        try {
            lm!!.removeUpdates(this)
        } catch (ex: Exception) {
        }
    }

    public override fun onResume() {
        super.onResume()
        lm = getActivity()!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            //this fails on AVD 19s, even with the appcompat check, says no provided named gps is available
            lm!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        } catch (ex: Exception) {
        }

        try {
            lm!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, this)
        } catch (ex: Exception) {
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (datastore != null) datastore!!.close()
        datastore = null
        if (addBookmark != null) addBookmark!!.dismiss()
        addBookmark = null
    }


    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_BOOKMARK_MY_LOCATION, Menu.NONE, "Bookmark Current Location").setCheckable(false)
        MENU_LAST_ID++
        menu.add(0, MENU_BOOKMARK_IMPORT, Menu.NONE, "Import from CSV").setCheckable(false)
        MENU_LAST_ID++
        menu.add(0, MENU_BOOKMARK_EXPORT, Menu.NONE, "Export to CSV").setCheckable(false)
        MENU_LAST_ID++
        try {
            mMapView!!.getOverlayManager().onCreateOptionsMenu(menu, MENU_BOOKMARK_MY_LOCATION + 1, mMapView)
        } catch (npe: NullPointerException) {
            //can happen during CI tests and very rapid fragment switching
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        try {
            mMapView!!.getOverlayManager().onPrepareOptionsMenu(menu, MENU_LAST_ID, mMapView)
        } catch (npe: NullPointerException) {
            //can happen during CI tests and very rapid fragment switching
        }
        super.onPrepareOptionsMenu(menu)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == MENU_BOOKMARK_MY_LOCATION) {
            //TODO
            if (currentLocation != null) {
                val pt = GeoPoint(currentLocation!!.getLatitude(), currentLocation!!.getLongitude())
                showDialog(pt)
                return true
            }
        } else if (item.getItemId() == MENU_BOOKMARK_IMPORT) {
            //TODO
            showFilePicker()
            return true
        } else if (item.getItemId() == MENU_BOOKMARK_EXPORT) {
            //TODO
            showFileExportPicker()
            return true
        } else if (mMapView!!.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView)) {
            return true
        }
        return false
    }


    override fun onLocationChanged(location: Location) {
        currentLocation = location
        //mMyLocationOverlay.setLocation(new GeoPoint(location.getLatitude(), location.getLongitude()));
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }

    private fun showFileExportPicker() {
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.DIR_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)

        val dialog = FilePickerDialog(getContext(), properties)
        dialog.setTitle("Save CSV File")
        dialog.setDialogSelectionListener(object : DialogSelectionListener {
            override fun onSelectedFilePaths(files: Array<String?>) {
                //files is the array of the paths of files selected by the Application User.
                if (files.size == 1) {
                    //now prompt for a file name

                    val builder = AlertDialog.Builder(this@BookmarkSample.getContext())
                    builder.setTitle("Enter file name (.csv)")

                    // Set up the input
                    val input = EditText(this@BookmarkSample.getContext())
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                    input.setLines(1)
                    input.setText("export.csv")

                    builder.setView(input)

                    // Set up the buttons
                    builder.setPositiveButton("OK", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            //save the file here.
                            if (input.getText() == null) return
                            Thread(object : Runnable {
                                override fun run() {
                                    var file = input.getText().toString()
                                    if (!file.lowercase(Locale.getDefault()).endsWith(".csv")) {
                                        file = file + ".csv"
                                    }
                                    exportToCsv(File(files[0] + File.separator + file))
                                }
                            }).start()
                        }
                    })
                    builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            dialog.cancel()
                        }
                    })

                    builder.show()
                }
            }
        })
        dialog.show()
    }

    private fun showFilePicker() {
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)

        val registeredExtensions = ArchiveFileFactory.registeredExtensions

        registeredExtensions.add("csv")


        val ret = registeredExtensions.toTypedArray()
        properties.extensions = ret

        val dialog = FilePickerDialog(getContext(), properties)
        dialog.setTitle("Select a CSV File")
        dialog.setDialogSelectionListener(object : DialogSelectionListener {
            override fun onSelectedFilePaths(files: Array<String?>) {
                //files is the array of the paths of files selected by the Application User.
                if (files.size == 1) Thread(object : Runnable {
                    override fun run() {
                        importFromCsv(File(files[0]))
                    }
                }).start()
            }
        })
        dialog.show()
    }

    private var exportStatus = true

    /**
     * call me from a background thread
     */
    private fun exportToCsv(exportFile: File?) {
        var fileWriter: FileWriter? = null
        exportStatus = true
        try {
            fileWriter = FileWriter(exportFile)
            val writer = CSVWriter(fileWriter)
            val markers: List<Marker> = datastore!!.getBookmarksAsMarkers(getmMapView()).filterNotNull()
            val headers: Array<String?> = arrayOf<String?>("Latitude", "Longitude", "Description", "Title")
            writer.writeNext(headers)
            for (m in markers) {
                val items = arrayOfNulls<String>(4)
                items[0] = m.position.latitude.toString() + ""
                items[1] = m.position.longitude.toString() + ""
                items[2] = m.getSubDescription()
                items[3] = m.getTitle()
                writer.writeNext(items)
            }
        } catch (ex: Exception) {
            exportStatus = false
            ex.printStackTrace()
        } finally {
            if (fileWriter != null) try {
                fileWriter.close()
            } catch (ex: Exception) {
            }
        }
        val act: Activity? = getActivity()
        if (act != null) {
            act.runOnUiThread(object : Runnable {
                override fun run() {
                    if (exportStatus) {
                        Toast.makeText(act, "Export Complete", Toast.LENGTH_LONG).show()
                    } else Toast.makeText(act, "Export Failed", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    /**
     * call me from a background thread
     */
    private fun importFromCsv(importFile: File?) {
        val imported = AtomicInteger()
        val failed = AtomicInteger()
        var fileReader: FileReader? = null
        try {
            fileReader = FileReader(importFile)
            val reader = CSVReader(fileReader)
            var nextLine = reader.readNext()
            while ((reader.readNext().also { nextLine = it }) != null) {
                // nextLine[] is an array of values from the line
                try {
                    val lat = nextLine!![0]
                    val lon = nextLine[1]
                    val description: String? = nextLine[2]
                    val title: String? = nextLine[3]
                    val m = Marker(getmMapView()!!)
                    m.setTitle(title)
                    m.setSubDescription(description)
                    m.position = GeoPoint(lat.toDouble(), lon.toDouble())
                    datastore!!.addBookmark(m)
                    getmMapView()!!.getOverlayManager().add(m)
                    imported.getAndIncrement()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    failed.getAndIncrement()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            if (fileReader != null) try {
                fileReader.close()
            } catch (ex: Exception) {
            }
        }

        val act: Activity? = getActivity()
        if (act != null) {
            act.runOnUiThread(object : Runnable {
                override fun run() {
                    Toast.makeText(act, "Import Complete: " + imported.get() + "/" + failed.get() + "(imported/failed)", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    companion object {
        private val MENU_BOOKMARK_MY_LOCATION = Menu.FIRST
        private val MENU_BOOKMARK_IMPORT: Int = MENU_BOOKMARK_MY_LOCATION + 1
        private val MENU_BOOKMARK_EXPORT: Int = MENU_BOOKMARK_IMPORT + 1
        private var MENU_LAST_ID = Menu.FIRST
    }
}
