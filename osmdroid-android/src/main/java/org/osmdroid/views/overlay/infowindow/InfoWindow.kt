package org.osmdroid.views.overlay.infowindow

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.osmdroid.api.IGeoPoint
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.drawing.MapSnapshot

/**
 * [InfoWindow] is a (pop-up-) View that can
 * be displayed on an [MapView], associated to a
 * [IGeoPoint].
 *
 *
 * Typical usage: cartoon-like bubbles displayed when clicking an overlay item (i.e. a
 * [org.osmdroid.views.overlay.Marker]).
 * It mimics the InfoWindow class of Google Maps JavaScript API V3.
 * Main differences are:
 *
 *  * Structure and content of the view is let to the responsibility of the caller.
 *  * The same InfoWindow can be associated to many items.
 *
 *
 *
 * This is an abstract class.
 *
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='./doc-files/marker-infowindow-classes.png'></img>
 *
 * @author M.Kergall
 * @see MarkerInfoWindow
 */
abstract class InfoWindow {
    protected var mView: View?
    var isOpen: Boolean
        protected set

    /**
     * may return null if the info window hasn't been attached yet
     *
     * @return
     */
    var mapView: MapView?
        protected set
    /**
     * @return the related object.
     */
    /**
     * Allows to link an Object (any Object) to this marker.
     * This is particularly useful to handle custom InfoWindow.
     */
    var relatedObject: Any? = null
    private var mPosition: GeoPoint? = null
    private var mOffsetX = 0
    private var mOffsetY = 0

    /**
     * @param layoutResId the id of the view resource.
     * @param mapView     the mapview on which is hooked the view
     */
    constructor(layoutResId: Int, mapView: MapView) {
        this.mapView = mapView
        mapView.getRepository().add(this)
        this.isOpen = false
        val parent = mapView.getParent() as ViewGroup?
        val context = mapView.getContext()
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mView = inflater.inflate(layoutResId, parent, false)
        mView!!.setTag(this)
    }

    constructor(v: View?, mapView: MapView?) {
        this.mapView = mapView
        this.isOpen = false
        mView = v
        mView!!.setTag(this)
    }

    val view: View?
        /**
         * Returns the Android view. This allows to set its content.
         *
         * @return the Android view
         */
        get() = (mView)

    /**
     * open the InfoWindow at the specified GeoPosition + offset.
     * If it was already opened, close it before reopening.
     *
     * @param object   the graphical object on which is hooked the view
     * @param position to place the window on the map
     * @param offsetX  (&offsetY) the offset of the view to the position, in pixels.
     * This allows to offset the view from the object position.
     */
    fun open(`object`: Any?, position: GeoPoint?, offsetX: Int, offsetY: Int) {
        close() //if it was already opened
        this.relatedObject = `object`
        mPosition = position
        mOffsetX = offsetX
        mOffsetY = offsetY
        onOpen(`object`)
        val lp = MapView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            mPosition, MapView.LayoutParams.BOTTOM_CENTER,
            mOffsetX, mOffsetY
        )

        if (this.mapView != null && mView != null) {
            mapView!!.addView(mView, lp)
            this.isOpen = true
        } else {
            Log.w(
                IMapView.LOGTAG,
                "Error trapped, InfoWindow.open mMapView: " + (if (this.mapView == null) "null" else "ok") + " mView: " + (if (mView == null) "null" else "ok")
            )
        }
    }

    /**
     * refresh the infowindow drawing. Must be called every time the view changes (drag, zoom,...).
     * Best practice is to call this method in the draw method of its overlay.
     */
    fun draw() {
        if (!this.isOpen) return
        try {
            val lp = MapView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                mPosition, MapView.LayoutParams.BOTTOM_CENTER,
                mOffsetX, mOffsetY
            )
            mapView!!.updateViewLayout(mView, lp) // supposed to work only on the UI Thread
        } catch (e: Exception) {
            if (MapSnapshot.isUIThread) {
                throw e
            }
            // in order to avoid a CalledFromWrongThreadException crash
        }
    }

    /**
     * hides the info window, which triggers another render of the map
     */
    fun close() {
        if (this.isOpen) {
            this.isOpen = false
            (mView!!.getParent() as ViewGroup).removeView(mView)
            onClose()
        }
    }

    /**
     * this destroys the window and all references to views
     */
    fun onDetach() {
        close()
        if (mView != null) mView!!.setTag(null)
        mView = null
        this.mapView = null
        if (instance!!.isDebugMode) Log.d(IMapView.LOGTAG, "Marked detached")
    }

    //Abstract methods to implement in sub-classes:
    abstract fun onOpen(item: Any?)

    abstract fun onClose()

    companion object {
        /**
         * close all InfoWindows currently opened on this MapView
         *
         * @param mapView
         */
        fun closeAllInfoWindowsOn(mapView: MapView) {
            val opened: ArrayList<InfoWindow> = getOpenedInfoWindowsOn(mapView)
            for (infoWindow in opened) {
                infoWindow.close()
            }
        }

        /**
         * return all InfoWindows currently opened on this MapView
         *
         * @param mapView
         * @return
         */
        fun getOpenedInfoWindowsOn(mapView: MapView): ArrayList<InfoWindow> {
            val count = mapView.getChildCount()
            val opened = ArrayList<InfoWindow>(count)
            for (i in 0 until count) {
                val child = mapView.getChildAt(i)
                val tag = child.getTag()
                if (tag != null && tag is InfoWindow) {
                    val infoWindow = tag
                    opened.add(infoWindow)
                }
            }
            return opened
        }
    }
}
