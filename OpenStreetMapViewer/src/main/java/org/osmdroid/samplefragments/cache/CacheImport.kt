package org.osmdroid.samplefragments.cache

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.views.MapView

/**
 * An example of importing stored on disk cache produced by osmdroid < 5.4 using the older TileWriter
 * class
 *
 * @see org.osmdroid.tileprovider.modules.TileWriter
 *
 * @see SqlTileWriter
 * Created by alex on 9/25/16.
 */
class CacheImport : BaseSampleFragment(), View.OnClickListener, Runnable {
    var removeFromFileSystem: Boolean = true
    var btnCache: Button? = null

    override val sampleTitle: String
        get() = "Import the file system cache into the newer sql cache"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        mMapView = MapView(getActivity()!!)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache!!.setOnClickListener(this)
        btnCache!!.setText("Cache Filesystem Import")

        return root
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.btnCache -> {
                val dialogClickListener: DialogInterface.OnClickListener = object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {}
                            DialogInterface.BUTTON_NEGATIVE ->                                 //No button clicked
                                removeFromFileSystem = false
                        }
                        Thread(this@CacheImport).start()
                    }
                }

                val builder = AlertDialog.Builder(getActivity())
                builder.setMessage("Would you like to remove the tiles from the file system after importing into the cache database?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show()
            }
        }
    }

    override fun run() {
        val tileWriter = mMapView!!.getTileProvider()!!.getTileWriter()
        if (tileWriter is SqlTileWriter) {
            val b = tileWriter.importFromFileCache(removeFromFileSystem)
            if (getActivity() != null) {
                getActivity()!!.runOnUiThread(object : Runnable {
                    override fun run() {
                        Toast.makeText(
                            getActivity(),
                            "Cache Import success/failures/default/failres " + b[0] + "/" + b[1] + "/" + b[2] + "/" + b[3],
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }
        }
    }
}
