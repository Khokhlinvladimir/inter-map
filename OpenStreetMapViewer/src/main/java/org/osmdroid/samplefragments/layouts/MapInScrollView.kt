package org.osmdroid.samplefragments.layouts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.views.MapView

/**
 * created on 1/3/2017.
 *
 * @author Alex O'Ree
 */
class MapInScrollView : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Map in a scroll view"


    public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.map_scoll, null)
        mMapView = v.findViewById<MapView?>(R.id.mapview)

        mMapView!!.setOnTouchListener(object : OnTouchListener {
            // Setting on Touch Listener for handling the touch inside ScrollView
            override fun onTouch(v: View, event: MotionEvent?): Boolean {
                // Disallow the touch request for parent scroll on touch of child view
                Log.d(TAG, "onTouch")
                v.getParent().requestDisallowInterceptTouchEvent(true)
                return false
            }
        })
        Log.d(TAG, "onCreateView")
        return v
    }


    public override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDetach")
    }

    public override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}
