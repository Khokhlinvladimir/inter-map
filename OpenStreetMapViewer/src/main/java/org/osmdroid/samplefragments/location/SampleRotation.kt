package org.osmdroid.samplefragments.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

/**
 * created on 1/2/2017.
 *
 * @author Alex O'Ree
 */
class SampleRotation : BaseSampleFragment(), View.OnClickListener {
    var btnRotateLeft: ImageButton? = null
    var btnRotateRight: ImageButton? = null
    protected var textViewCurrentLocation: TextView? = null

    override val sampleTitle: String
        get() = "Map Rotation"

    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.map_with_locationbox_controls, null)
        mMapView = root.findViewById<MapView?>(R.id.mapview)
        btnRotateLeft = root.findViewById<ImageButton>(R.id.btnRotateLeft)
        btnRotateLeft!!.setOnClickListener(this)
        btnRotateRight = root.findViewById<ImageButton>(R.id.btnRotateRight)
        btnRotateRight!!.setOnClickListener(this)
        textViewCurrentLocation = root.findViewById<TextView?>(R.id.textViewCurrentLocation)
        textViewCurrentLocation!!.setText("0.0")
        return root
    }

    public override fun addOverlays() {
        super.addOverlays()

        val dm = getActivity()!!.getResources().getDisplayMetrics()
        val mRotationGestureOverlay = RotationGestureOverlay(mMapView)
        mRotationGestureOverlay.setEnabled(true)
        mMapView!!.getOverlays()!!.add(mRotationGestureOverlay)

        val mScaleBarOverlay = ScaleBarOverlay(mMapView)
        mScaleBarOverlay.setScaleBarOffset(0, (40 * dm.density).toInt())
        mScaleBarOverlay.setCentred(true)
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10)
        mMapView!!.getOverlays()!!.add(mScaleBarOverlay)
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.btnRotateLeft -> {
                var angle = mMapView!!.getMapOrientation() + 10
                if (angle > 360) angle = 360 - angle
                mMapView!!.setMapOrientation(angle)
            }

            R.id.btnRotateRight -> {
                var angle = mMapView!!.getMapOrientation() - 10
                if (angle < 0) angle += 360f
                mMapView!!.setMapOrientation(angle)
            }
        }
        textViewCurrentLocation!!.setText(mMapView!!.getMapOrientation().toString() + "")
    }
}
