package org.osmdroid.samplefragments.layouts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.MapView


class SampleFragmentXmlLayout : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.activity_starter_mapview, null)
        mMapView = v.findViewById<MapView?>(R.id.mapview)
        return v
    }

    companion object {
        // ===========================================================
        // Fields
        // ===========================================================
        private const val SAMPLE_TITLE: String = "MapView in XML layout"
    }
}
