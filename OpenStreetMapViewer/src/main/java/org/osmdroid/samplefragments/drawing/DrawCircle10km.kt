package org.osmdroid.samplefragments.drawing

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.api.IMapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.samplefragments.events.SampleMapEventListener
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

/**
 * created on 12/27/2017.
 *
 * @author Alex O'Ree
 */
class DrawCircle10km : BaseSampleFragment(), View.OnClickListener, OnLongClickListener {
    override val sampleTitle: String
        get() = "Draw a circle 10km (long press)"

    var painting: ImageButton? = null
    var panning: ImageButton? = null
    var textViewCurrentLocation: TextView? = null

    var btnRotateLeft: ImageButton? = null
    var btnRotateRight: ImageButton? = null


    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.layout_drawlines, null)
        btnRotateLeft = v.findViewById<ImageButton>(R.id.btnRotateLeft)
        btnRotateRight = v.findViewById<ImageButton>(R.id.btnRotateRight)
        btnRotateRight!!.setOnClickListener(this)
        btnRotateLeft!!.setOnClickListener(this)
        textViewCurrentLocation = v.findViewById<TextView>(R.id.textViewCurrentLocation)
        mMapView = v.findViewById<MapView?>(R.id.mapview)
        mMapView!!.setMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onScroll " + event.x + "," + event.y)
                //Toast.makeText(getActivity(), "onScroll", Toast.LENGTH_SHORT).show();
                updateInfo()
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                Log.i(IMapView.LOGTAG, System.currentTimeMillis().toString() + " onZoom " + event.zoomLevel)
                updateInfo()
                return true
            }
        })

        val mRotationGestureOverlay = RotationGestureOverlay(mMapView)
        mRotationGestureOverlay.setEnabled(true)
        mMapView!!.setMultiTouchControls(true)
        mMapView!!.getOverlayManager().add(mRotationGestureOverlay)
        mMapView!!.setOnLongClickListener(this)
        panning = v.findViewById<ImageButton>(R.id.enablePanning)
        panning!!.setVisibility(View.GONE)

        painting = v.findViewById<ImageButton>(R.id.enablePainting)
        painting!!.setVisibility(View.GONE)


        val plotter = CirclePlottingOverlay(100f)
        mMapView!!.getOverlayManager().add(plotter)

        return v
    }

    private fun updateInfo() {
        val mapCenter = mMapView!!.mapCenter
        textViewCurrentLocation!!.setText(
            (SampleMapEventListener.df.format(mapCenter!!.latitude) + "," +
                    SampleMapEventListener.df.format(mapCenter.longitude)
                    + ",zoom=" + mMapView!!.zoomLevelDouble + ",angle=" + mMapView!!.getMapOrientation())
        )
    }

    override fun onClick(v: View) {
        when (v.getId()) {
            R.id.btnRotateLeft -> {
                var angle = mMapView!!.getMapOrientation() + 10
                if (angle > 360) angle = 360 - angle
                mMapView!!.setMapOrientation(angle)
                updateInfo()
            }

            R.id.btnRotateRight -> {
                var angle = mMapView!!.getMapOrientation() - 10
                if (angle < 0) angle += 360f
                mMapView!!.setMapOrientation(angle)
                updateInfo()
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        return true
    }
}
