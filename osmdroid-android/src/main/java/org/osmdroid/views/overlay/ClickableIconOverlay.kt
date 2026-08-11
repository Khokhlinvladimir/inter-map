/*
 * Copyright (c) 2015 by k3b.
 */
package org.osmdroid.views.overlay

import android.graphics.drawable.Drawable
import android.view.MotionEvent
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.MapView

/**
 * [org.osmdroid.views.overlay.ClickableIconOverlay] is a clickable icon item on the
 * [MapView] containing [IGeoPoint],
 * [unique id][ClickableIconOverlay.getID] and
 * [data][ClickableIconOverlay.getData].
 *
 *
 * Inspired by [Marker] but without the dependency to certain content and a popup-window
 *
 *
 * Created by k3b on 17.07.2015.
 */
abstract class ClickableIconOverlay<DataType> protected constructor(data: DataType?) : IconOverlay() {
    protected var mId: Int = 0
    private var mData: DataType? = null

    /**
     * save to be called in non-gui-thread
     */
    init {
        mData = data
    }

    /**
     * @return true if click was handeled.
     */
    protected abstract fun onMarkerClicked(mapView: MapView?, markerId: Int, makerPosition: IGeoPoint?, markerData: DataType?): Boolean

    /**
     * used to recycle this
     */
    fun set(id: Int, position: IGeoPoint?, icon: Drawable?, data: DataType?): ClickableIconOverlay<*> {
        set(position, icon)
        mId = id
        mData = data
        return this
    }

    /**
     * From [Marker.hitTest]
     *
     * @return true, if this marker was taped.
     */
    protected fun hitTest(event: MotionEvent, mapView: MapView): Boolean {
        val pj = mapView.projection

        // sometime at higher zoomlevels pj is null
        if ((mPosition == null) || (mPositionPixels == null) || (pj == null)) return false

        pj.toPixels(mPosition, mPositionPixels)
        val screenRect = pj.intrinsicScreenRect
        val x = -mPositionPixels.x + screenRect.left + event.getX().toInt()
        val y = -mPositionPixels.y + screenRect.top + event.getY().toInt()
        val hit = mIcon?.bounds?.contains(x, y) == true
        return hit
    }

    /**
     * @return true: tap handeled. No following overlay/map should handle the event.
     * false: tap not handeled. A following overlay/map should handle the event.
     */
    override fun onSingleTapConfirmed(event: MotionEvent, mapView: MapView?): Boolean {
        if (mapView == null) return false
        val touched = hitTest(event, mapView)
        if (touched) {
            return onMarkerClicked(mapView, mId, mPosition, mData)
        } else {
            return super.onSingleTapConfirmed(event, mapView)
        }
    }

    /**
     * By default does nothing (`return false`). If you handled the Event, return `true`
     * , otherwise return `false`. If you returned `true` none of the following Overlays
     * or the underlying [MapView] has the chance to handle this event.
     */
    override fun onLongPress(event: MotionEvent, mapView: MapView?): Boolean {
        if (mapView == null) return false
        val touched = hitTest(event, mapView)
        if (touched) {
            return onMarkerLongPress(mapView, mId, mPosition, mData)
        } else {
            return super.onLongPress(event, mapView)
        }
    }

    protected fun onMarkerLongPress(mapView: MapView?, markerId: Int, geoPosition: IGeoPoint?, data: Any?): Boolean {
        return false
    }

    fun getID(): Int = mId

    fun getData(): DataType? = mData

    companion object {
        fun find(list: MutableList<ClickableIconOverlay<*>?>, id: Int): ClickableIconOverlay<*>? {
            for (item in list) {
                if ((item != null) && (item.mId == id)) return item
            }
            return null
        }
    }
}
