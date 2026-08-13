package org.osmdroid.samplefragments.animations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.animations.GeoPointInterpolator.Spherical
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Marker animation based on Google's sample code for animating a marker
 * API 9+
 * created on 9/2/2017.
 *
 * @author Alex O'Ree
 */
class AnimatedMarkerHandler : BaseSampleFragment(), View.OnClickListener {
    override val sampleTitle: String
        get() = "Marker Animation (Handler)"

    var btnCache: Button? = null
    var marker: Marker? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.sample_cachemgr, container, false)
        mMapView = MapView(getActivity()!!)
        (root.findViewById<View?>(R.id.mapview) as LinearLayout).addView(mMapView)
        btnCache = root.findViewById<Button>(R.id.btnCache)
        btnCache!!.setOnClickListener(this)
        btnCache!!.setText("Start/Stop Animation")

        marker = Marker(mMapView!!)
        marker!!.setTitle("An animated marker")
        marker!!.position = GeoPoint(0.0, 0.0)
        mMapView!!.getOverlayManager().add(marker)


        return root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }


    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.btnCache -> {
                val random = GeoPoint((Math.random() * 180) - 90, (Math.random() * 360) - 180)
                MarkerAnimation.animateMarkerToGB(mMapView!!, marker!!, random, Spherical())
            }
        }
    }
}
