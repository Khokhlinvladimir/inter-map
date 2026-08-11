package org.osmdroid.samplefragments.models

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.osmdroid.R
import org.osmdroid.api.IMapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace

class SampleItemizedOverlay(pDefaultMarker: Drawable?, pContext: Context?) : ItemizedOverlay<SampleOverlayItem?>(pDefaultMarker),
    ItemizedOverlay.OnFocusChangeListener {
    private var mFocusChanged = false
    private var mPopupView: View? = null
    private var mContext: Context? = null

    init {
        populate()
        setOnFocusChangeListener(this)
        mContext = pContext
    }

    override fun createItem(i: Int): SampleOverlayItem {
        val item: SampleOverlayItem
        if (i == 0) item = SampleOverlayItem(
            "CentralPark", "Central Park",
            "Central Park in New York City", GeoPoint(40.7820, -73.9660), null,
            HotspotPlace.BOTTOM_CENTER
        )
        else item = SampleOverlayItem(
            "NorthCentralPark", "North Central Park",
            "North of Central Park in New York City", GeoPoint(41.7820, -73.9660),
            mContext!!.getResources().getDrawable(R.drawable.person), HotspotPlace.CENTER
        )
        return item
    }

    override fun onFocusChanged(overlay: ItemizedOverlay<*>?, newFocus: OverlayItem?) {
        mFocusChanged = true
    }

    override fun onTap(index: Int): Boolean {
        setFocus(getItem(index))
        return true
    }


    override fun draw(c: Canvas?, mapView: MapView, shadow: Boolean) {
        if (mFocusChanged) {
            mFocusChanged = false

            // Remove any current focus
            if (mPopupView != null) mapView.removeView(mPopupView)

            val item = this.getFocus()
            if (item != null) {
                mPopupView = getPopupView(mapView.getContext(), item)
                val lp = MapView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, item.getPoint(),
                    MapView.LayoutParams.TOP_CENTER, 0, 0
                )
                mapView.addView(mPopupView, lp)
            }
        }
        super.draw(c, mapView, shadow)
    }

    protected fun getPopupView(context: Context?, item: SampleOverlayItem): View {
        val tv = TextView(context)
        tv.setText(item.getTitle())
        tv.setBackgroundColor(Color.BLACK)
        return tv
    }

    override fun size(): Int {
        return 2
    }

    override fun onSnapToItem(arg0: Int, arg1: Int, arg2: Point?, arg3: IMapView?): Boolean {
        return false
    }
}
