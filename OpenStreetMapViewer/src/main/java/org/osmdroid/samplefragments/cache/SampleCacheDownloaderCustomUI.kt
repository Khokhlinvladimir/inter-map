package org.osmdroid.samplefragments.cache

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.cachemanager.CacheManager.CacheManagerCallback
import org.osmdroid.tileprovider.cachemanager.CacheManager.CacheManagerTask
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

/**
 * Created by alex on 5/29/16.
 */
class SampleCacheDownloaderCustomUI : BaseSampleFragment(), View.OnClickListener, OnSeekBarChangeListener, TextWatcher, CacheManagerCallback {
    override val sampleTitle: String
        get() = "Cache Manager with custom UI"

    var progressBar: ProgressDialog? = null

    var btnCache: Button? = null
    var executeJob: Button? = null
    var zoom_min: SeekBar? = null
    var zoom_max: SeekBar? = null
    var cache_north: EditText? = null
    var cache_south: EditText? = null
    var cache_east: EditText? = null
    var cache_west: EditText? = null
    var cache_estimate: TextView? = null
    var mgr: CacheManager? = null
    var downloadPrompt: AlertDialog? = null
    var downloadingTask: CacheManagerTask? = null

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        //prevent the action bar/toolbar menu in order to prevent tile source changes.
        //if this is enabled, playstore users could actually download large volumes of tiles
        //from tile sources that do not allow it., causing our app to get banned, which would be
        //bad
        setHasOptionsMenu(false)

        mMapView = MapView(getActivity()!!)
        mMapView!!.setTileSource(TileSourceFactory.USGS_SAT)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache!!.setOnClickListener(this)
        mgr = CacheManager(mMapView!!)
        return root
    }

    public override fun addOverlays() {
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.executeJob -> updateEstimate(true)
            R.id.btnCache -> showCacheManagerDialog()
        }
    }


    private fun showCacheManagerDialog() {
        val alertDialogBuilder = AlertDialog.Builder(
            getActivity()
        )


        // set title
        alertDialogBuilder.setTitle(R.string.cache_manager)

        //.setMessage(R.string.cache_manager_description);

        // set dialog message
        alertDialogBuilder.setItems(
            arrayOf<CharSequence>(
                getResources().getString(R.string.cache_current_size),
                getResources().getString(R.string.cache_download),
                getResources().getString(R.string.cancelall),
                getResources().getString(R.string.showpendingjobs),
                getResources().getString(R.string.close)
            ), object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    when (which) {
                        0 -> showCurrentCacheInfo()
                        1 -> downloadJobAlert()
                        2 -> {
                            mgr!!.cancelAllJobs()
                            Toast.makeText(getActivity(), "Jobs Canceled", Toast.LENGTH_LONG).show()
                        }

                        3 -> Toast.makeText(getActivity(), "Pending Jobs: " + mgr!!.pendingJobs, Toast.LENGTH_LONG).show()
                    }
                    dialog.dismiss()
                }
            }
        )


        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()


        //mgr.possibleTilesInArea(mMapView.boundingBox, 0, 18);
        // mgr.
    }

    private fun downloadJobAlert() {
        //prompt for input params
        val builder = AlertDialog.Builder(getActivity())

        val view = View.inflate(getActivity(), R.layout.sample_cachemgr_input, null)

        val boundingBox = mMapView!!.getBoundingBox()!!
        zoom_max = view.findViewById<SeekBar?>(R.id.slider_zoom_max)
        zoom_max!!.setMax(mMapView!!.maxZoomLevel.toInt())
        zoom_max!!.setOnSeekBarChangeListener(this@SampleCacheDownloaderCustomUI)


        zoom_min = view.findViewById<SeekBar?>(R.id.slider_zoom_min)
        zoom_min!!.setMax(mMapView!!.maxZoomLevel.toInt())
        zoom_min!!.setProgress(mMapView!!.getMinZoomLevel().toInt())
        zoom_min!!.setOnSeekBarChangeListener(this@SampleCacheDownloaderCustomUI)
        cache_east = view.findViewById<EditText?>(R.id.cache_east)
        cache_east!!.setText(boundingBox!!.lonEast.toString() + "")
        cache_north = view.findViewById<EditText?>(R.id.cache_north)
        cache_north!!.setText(boundingBox.latNorth.toString() + "")
        cache_south = view.findViewById<EditText?>(R.id.cache_south)
        cache_south!!.setText(boundingBox.latSouth.toString() + "")
        cache_west = view.findViewById<EditText?>(R.id.cache_west)
        cache_west!!.setText(boundingBox.lonWest.toString() + "")
        cache_estimate = view.findViewById<TextView?>(R.id.cache_estimate)

        //change listeners for both validation and to trigger the download estimation
        cache_east!!.addTextChangedListener(this)
        cache_north!!.addTextChangedListener(this)
        cache_south!!.addTextChangedListener(this)
        cache_west!!.addTextChangedListener(this)
        executeJob = view.findViewById<Button>(R.id.executeJob)
        executeJob!!.setOnClickListener(this)
        builder.setView(view)
        builder.setCancelable(true)
        builder.setOnCancelListener(object : DialogInterface.OnCancelListener {
            override fun onCancel(dialog: DialogInterface?) {
                cache_east = null
                cache_south = null
                cache_estimate = null
                cache_north = null
                cache_west = null
                executeJob = null
                zoom_min = null
                zoom_max = null
            }
        })
        downloadPrompt = builder.create()
        downloadPrompt!!.show()
    }

    /**
     * if true, start the job
     * if false, just update the dialog box
     */
    private fun updateEstimate(startJob: Boolean) {
        try {
            if (cache_east != null && cache_west != null && cache_north != null && cache_south != null && zoom_max != null && zoom_min != null) {
                val n = cache_north!!.getText().toString().toDouble()
                val s = cache_south!!.getText().toString().toDouble()
                val e = cache_east!!.getText().toString().toDouble()
                val w = cache_west!!.getText().toString().toDouble()

                val zoommin = zoom_min!!.getProgress()
                val zoommax = zoom_max!!.getProgress()
                //nesw
                val bb = BoundingBox(n, e, s, w)
                val tilecount = mgr!!.possibleTilesInArea(bb, zoommin, zoommax)
                cache_estimate!!.setText(tilecount.toString() + " tiles")
                if (startJob) {
                    if (downloadPrompt != null) {
                        downloadPrompt!!.dismiss()
                        downloadPrompt = null
                    }


                    // prepare for a progress bar dialog ( do this first! )
                    progressBar = ProgressDialog(this@SampleCacheDownloaderCustomUI.getActivity())
                    progressBar!!.setCancelable(true)
                    progressBar!!.setMessage("Downloading ...")
                    progressBar!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    progressBar!!.setProgress(0)
                    progressBar!!.setCancelable(true)
                    progressBar!!.setOnDismissListener(object : DialogInterface.OnDismissListener {
                        override fun onDismiss(dialog: DialogInterface?) {
                            //cancel the job wit the dialog is closed
                            downloadingTask!!.cancel(true)
                            println("Pending jobs " + mgr!!.pendingJobs)
                        }
                    })
                    //this triggers the download
                    downloadingTask = mgr!!.downloadAreaAsyncNoUI(requireActivity(), bb, zoommin, zoommax, this@SampleCacheDownloaderCustomUI)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun showCurrentCacheInfo() {
        Toast.makeText(getActivity(), "Calculating...", Toast.LENGTH_SHORT).show()
        Thread(object : Runnable {
            override fun run() {
                val alertDialogBuilder = AlertDialog.Builder(
                    getActivity()
                )


                // set title
                alertDialogBuilder.setTitle(R.string.cache_manager)
                    .setMessage(
                        "Cache Capacity (bytes): " + mgr!!.cacheCapacity() + "\n" +
                                "Cache Usage (bytes): " + mgr!!.currentCacheUsage()
                    )

                // set dialog message
                alertDialogBuilder.setItems(
                    arrayOf<CharSequence>(
                        getResources().getString(R.string.cancel)
                    ), object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            dialog.dismiss()
                        }
                    }
                )


                getActivity()!!.runOnUiThread(object : Runnable {
                    override fun run() {
                        // show it
                        // create alert dialog
                        val alertDialog = alertDialogBuilder.create()
                        alertDialog.show()
                    }
                })
            }
        }).start()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        updateEstimate(false)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        updateEstimate(false)
    }

    override fun afterTextChanged(s: Editable?) {
    }


    //cache manager callback
    override fun onTaskComplete() {
        progressBar!!.dismiss()
        progressBar = null
        Toast.makeText(getActivity(), "Download complete!", Toast.LENGTH_LONG).show()
    }

    //cache manager callback
    override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
        if (progressBar != null) {
            progressBar!!.setProgress(progress)
        }
    }


    //cache manager callback
    override fun downloadStarted() {
        if (progressBar != null) {
            progressBar!!.show()
        }
    }

    //cache manager callback
    override fun setPossibleTilesInArea(total: Int) {
        if (progressBar != null) {
            progressBar!!.setMax(total)
        }
    }

    override fun onTaskFailed(errors: Int) {
        if (progressBar != null) progressBar!!.dismiss()
        progressBar = null
        Toast.makeText(getActivity(), "Download complete with " + errors + " errors", Toast.LENGTH_LONG).show()
    }
}
