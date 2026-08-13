package org.osmdroid.views.overlay

import android.graphics.Color
import android.graphics.Paint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * A polyline is a list of points, where line segments are drawn between consecutive points.
 * Mimics the Polyline class from Google Maps Android API v2 as much as possible. Main differences:<br></br>
 * - Doesn't support Z-Index: drawing order is the order in map overlays<br></br>
 * - Supports InfoWindow (must be a BasicInfoWindow). <br></br>
 *
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='src=' .></img>/doc-files/marker-infowindow-classes.png' />
 *
 * @author M.Kergall
 * @see [Google Maps Polyline](http://developer.android.com/reference/com/google/android/gms/maps/model/Polyline.html)
 */
class Polyline @JvmOverloads constructor(pMapView: MapView?, pUsePath: Boolean, pClosePath: Boolean = false) :
    PolyOverlayWithIW(pMapView, pUsePath, pClosePath) {
    protected var mOnClickListener: OnClickListener? = null

    /**
     * If MapView is null, infowindow popup will not function unless you set it yourself.
     */
    /**
     * If MapView is not provided, infowindow popup will not function unless you set it yourself.
     */
    @JvmOverloads
    constructor(mapView: MapView? = null) : this(mapView, false)

    /**
     * @since 6.2.0
     */
    /**
     * @param pUsePath true if you want the drawing to use Path instead of Canvas.drawLines
     * Not recommended in all cases, given the performances.
     * Useful though if you want clean alpha vertices
     * cf. https://github.com/osmdroid/osmdroid/issues/1280
     * @since 6.1.0
     */
    init {
        //default as defined in Google API:
        mOutlinePaint.setColor(Color.BLACK)
        mOutlinePaint.setStrokeWidth(10.0f)
        mOutlinePaint.setStyle(Paint.Style.STROKE)
        mOutlinePaint.setAntiAlias(true)
    }

    @get:Deprecated("Use {@link #getActualPoints()} instead; copy the list if necessary")
    val points: ArrayList<GeoPoint>
        /**
         * @return a copy of the actual points
         */
        get() = ArrayList(getActualPoints())

    @get:Deprecated("Use {{@link #getOutlinePaint()}} instead")
    @set:Deprecated("Use {{@link #getOutlinePaint()}} instead")
    var color: Int
        get() = mOutlinePaint.getColor()
        set(color) {
            mOutlinePaint.setColor(color)
        }

    @get:Deprecated("Use {{@link #getOutlinePaint()}} instead")
    @set:Deprecated("Use {{@link #getOutlinePaint()}} instead")
    var width: Float
        get() = mOutlinePaint.getStrokeWidth()
        set(width) {
            mOutlinePaint.setStrokeWidth(width)
        }

    @get:Deprecated("Use {{@link #getOutlinePaint()}} instead")
    val paint: Paint?
        get() = getOutlinePaint()

    fun setOnClickListener(listener: OnClickListener?) {
        mOnClickListener = listener
    }

    /**
     * Internal method used to ensure that the infowindow will have a default position in all cases,
     * so that the user can call showInfoWindow even if no tap occured before.
     * Currently, set the position on the "middle" point of the polyline.
     */
    interface OnClickListener {
        fun onClick(polyline: Polyline?, mapView: MapView?, eventPos: GeoPoint?): Boolean
    }

    /**
     * default behaviour when no click listener is set
     */
    fun onClickDefault(polyline: Polyline, mapView: MapView?, eventPos: GeoPoint?): Boolean {
        polyline.setInfoWindowLocation(eventPos)
        polyline.showInfoWindow()
        return true
    }

    override fun onDetach(mapView: MapView?) {
        super.onDetach(mapView)
        mOnClickListener = null
    }

    /**
     * @return aggregate distance (in meters)
     * @since 6.0.3
     */
    override fun getDistance(): Double {
        return mOutline!!.getDistance()
    }

    /**
     * @since 6.2.0
     */
    override fun click(pMapView: MapView?, pEventPos: GeoPoint?): Boolean {
        if (mOnClickListener == null) {
            return onClickDefault(this, pMapView, pEventPos)
        }
        return mOnClickListener!!.onClick(this, pMapView, pEventPos)
    }
}
