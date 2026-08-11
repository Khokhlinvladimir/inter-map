package org.osmdroid.samplefragments.drawing

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
 * created on 1/13/2017.
 *
 * @author Alex O'Ree
 */
open class SampleDrawPolyline : BaseSampleFragment(), View.OnClickListener {
    var painting: ImageButton? = null
    var panning: ImageButton? = null
    var textViewCurrentLocation: TextView? = null
    @JvmField
    var paint: CustomPaintingSurface? = null
    var btnRotateLeft: ImageButton? = null
    var btnRotateRight: ImageButton? = null

    override val sampleTitle: String?
        get() = "Draw a polyline on screen"


    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.layout_drawlines, null)
        btnRotateLeft = v.findViewById<ImageButton>(R.id.btnRotateLeft)
        btnRotateRight = v.findViewById<ImageButton>(R.id.btnRotateRight)
        btnRotateRight!!.setOnClickListener(this)
        btnRotateLeft!!.setOnClickListener(this)
        textViewCurrentLocation = v.findViewById<TextView>(R.id.textViewCurrentLocation)

        mMapView = v.findViewById<MapView?>(R.id.mapview)
        val mRotationGestureOverlay = RotationGestureOverlay(mMapView)
        mRotationGestureOverlay.setEnabled(true)
        mMapView!!.setMultiTouchControls(true)
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
        mMapView!!.getOverlayManager().add(mRotationGestureOverlay)
        panning = v.findViewById<ImageButton>(R.id.enablePanning)
        panning!!.setOnClickListener(this)
        panning!!.setBackgroundColor(Color.BLACK)
        painting = v.findViewById<ImageButton>(R.id.enablePainting)
        painting!!.setOnClickListener(this)
        paint = v.findViewById<CustomPaintingSurface>(R.id.paintingSurface)
        paint!!.init(mMapView)
        paint!!.setMode(CustomPaintingSurface.Mode.Polyline)
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
            R.id.enablePanning -> {
                paint!!.setVisibility(View.GONE)
                panning!!.setBackgroundColor(Color.BLACK)
                painting!!.setBackgroundColor(Color.TRANSPARENT)
            }

            R.id.enablePainting -> {
                paint!!.setVisibility(View.VISIBLE)
                painting!!.setBackgroundColor(Color.BLACK)
                panning!!.setBackgroundColor(Color.TRANSPARENT)
            }

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
}
