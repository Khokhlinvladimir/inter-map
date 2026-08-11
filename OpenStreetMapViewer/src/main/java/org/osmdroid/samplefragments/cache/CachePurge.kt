package org.osmdroid.samplefragments.cache

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
 * Created by alex on 9/25/16.
 */
class CachePurge : BaseSampleFragment(), View.OnClickListener, Runnable {
    var btnCache: Button? = null

    override val sampleTitle: String
        get() = "How to purge the tile cache"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        mMapView = MapView(getActivity()!!)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache!!.setOnClickListener(this)
        btnCache!!.setText("Cache Purge (database)")

        return root
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.btnCache -> Thread(this).start()
        }
    }

    override fun run() {
        val tileWriter = mMapView!!.getTileProvider()!!.getTileWriter()
        if (tileWriter is SqlTileWriter) {
            val b = tileWriter.purgeCache()
            if (getActivity() != null) {
                getActivity()!!.runOnUiThread(object : Runnable {
                    override fun run() {
                        if (b) Toast.makeText(getActivity(), "Cache Purge successful", Toast.LENGTH_SHORT).show()
                        else Toast.makeText(getActivity(), "Cache Purge failed", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}
