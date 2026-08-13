package org.osmdroid.views

import android.graphics.drawable.Drawable
import org.osmdroid.library.R
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

/**
 * Repository for a MapView
 * Designed for "singleton-like" objects that need a clean detach
 */
open class MapViewRepository(pMapView: MapView?) {
    private var mMapView: MapView?
    private var mDefaultMarkerInfoWindow: MarkerInfoWindow? = null
    private var mDefaultPolylineInfoWindow: BasicInfoWindow? = null
    private var mDefaultPolygonInfoWindow: BasicInfoWindow? = null
    private var mDefaultMarkerIcon: Drawable? = null
    private val mInfoWindowList: MutableSet<InfoWindow> = HashSet<InfoWindow>()

    init {
        mMapView = pMapView
    }

    fun add(pInfoWindow: InfoWindow?) {
        mInfoWindowList.add(pInfoWindow!!)
    }

    fun onDetach() {
        synchronized(mInfoWindowList) {
            for (infoWindow in mInfoWindowList) {
                infoWindow.onDetach()
            }
            mInfoWindowList.clear()
        }

        mMapView = null
        mDefaultMarkerInfoWindow = null
        mDefaultPolylineInfoWindow = null
        mDefaultPolygonInfoWindow = null
        mDefaultMarkerIcon = null
    }

    val defaultMarkerInfoWindow: MarkerInfoWindow
        get() {
            if (mDefaultMarkerInfoWindow == null) {
                mDefaultMarkerInfoWindow = MarkerInfoWindow(R.layout.bonuspack_bubble, requireNotNull(mMapView))
            }
            return mDefaultMarkerInfoWindow!!
        }

    val defaultPolylineInfoWindow: BasicInfoWindow
        get() {
            if (mDefaultPolylineInfoWindow == null) {
                mDefaultPolylineInfoWindow = BasicInfoWindow(R.layout.bonuspack_bubble, requireNotNull(mMapView))
            }
            return mDefaultPolylineInfoWindow!!
        }

    val defaultPolygonInfoWindow: BasicInfoWindow
        get() {
            if (mDefaultPolygonInfoWindow == null) {
                mDefaultPolygonInfoWindow = BasicInfoWindow(R.layout.bonuspack_bubble, requireNotNull(mMapView))
            }
            return mDefaultPolygonInfoWindow!!
        }

    val defaultMarkerIcon: Drawable?
        /**
         * note: it's possible for this to return null during certain lifecycle events. Such as
         * invoke this method after [.onDetach] has been called
         *
         * @return
         */
        get() {
            if (mDefaultMarkerIcon == null) {
                if (mMapView != null) {
                    val context = mMapView!!.getContext()
                    if (context != null) {
                        mDefaultMarkerIcon = context.getResources().getDrawable(R.drawable.marker_default)
                    }
                }
            }
            return mDefaultMarkerIcon
        }
}
