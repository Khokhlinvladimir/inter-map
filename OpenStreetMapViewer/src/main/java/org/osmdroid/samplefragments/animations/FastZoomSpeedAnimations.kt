package org.osmdroid.samplefragments.animations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.config.Configuration.instance
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.MapView

/**
 * How to override animation speeds for zoom in/out<br></br>
 * Implementation notes:
 *
 *  * When using the build in zoom controls (android supplied, lower part of the view, the only way to override this speed is via preference. It
 * is only checked when the mapview is created. Screen double tap to zoom is also affected by this.
 *  * If using custom zoom in/out buttons, this can be changed using the example below.
 *
 * created on 8/11/2017.
 *
 * @author Alex O'Ree
 */
class FastZoomSpeedAnimations : BaseSampleFragment(), View.OnClickListener {
    override val sampleTitle: String
        get() = "Super fast zoom speed"


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        //overrides the default animation speeds
        //note: the mapview creates the default double tap to zoom in animator when the map view is created
        //this we have to set the desired zoom speed here before the mapview is created/inflated

        instance!!.animationSpeedShort = 100
        instance!!.animationSpeedDefault = 100

        val root = inflater.inflate(R.layout.map_with_locationbox_controls, container, false)

        mMapView = root.findViewById<MapView?>(R.id.mapview)
        val textViewCurrentLocation = root.findViewById<TextView>(R.id.textViewCurrentLocation)
        textViewCurrentLocation.setText("Animation Speed Test")
        var btn = root.findViewById<ImageButton>(R.id.btnRotateLeft)
        btn.setOnClickListener(this)

        btn = root.findViewById<ImageButton>(R.id.btnRotateRight)
        btn.setOnClickListener(this)
        return root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        //maybe restore the old animation settings here
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.btnRotateLeft -> mMapView!!.controller!!.zoomIn(100L)
            R.id.btnRotateRight -> mMapView!!.controller!!.zoomOut(100L)
        }
    }
}
