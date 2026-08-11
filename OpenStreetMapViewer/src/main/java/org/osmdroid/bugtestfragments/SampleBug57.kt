package org.osmdroid.bugtestfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import org.osmdroid.ExtraSamplesActivity
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.MapView

/**
 * https://github.com/osmdroid/osmdroid/issues/57
 *
 *
 * load the map, then navigate to a different fragment, then hit the back button
 * Created by alex on 7/5/16.
 */
class SampleBug57 : BaseSampleFragment(), View.OnClickListener {
    override val sampleTitle: String
        get() = "Recovery from backstack"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)

        mMapView = MapView(getActivity()!!)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        val btn = root.findViewById<Button>(R.id.btnCache)
        btn.setOnClickListener(this)
        btn.setText("To Step 2")
        return root
    }

    override fun onClick(v: View?) {
        val fm = getFragmentManager()
        fm!!.beginTransaction().replace(R.id.samples_container, SampleBug57Step2(), ExtraSamplesActivity.SAMPLES_FRAGMENT_TAG)
            .addToBackStack(null).commit()
    }
}
