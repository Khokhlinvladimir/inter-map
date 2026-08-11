package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import org.osmdroid.api.IMapView
import org.osmdroid.library.R
import org.osmdroid.views.MapView
import kotlin.math.min

open class ItemizedIconOverlay<Item : OverlayItem?>(
    pList: MutableList<Item?>?,
    pDefaultMarker: Drawable?,
    pOnItemGestureListener: OnItemGestureListener<Item?>?,
    pContext: Context?
) : ItemizedOverlay<Item?>(requireNotNull(pDefaultMarker)) {
    protected var mItemList: MutableList<Item?>?
    protected var mOnItemGestureListener: OnItemGestureListener<Item?>?

    init {
        this.mItemList = pList
        this.mOnItemGestureListener = pOnItemGestureListener
        populate()
    }

    constructor(
        pList: MutableList<Item?>?,
        pOnItemGestureListener: OnItemGestureListener<Item?>?,
        pContext: Context
    ) : this(
        pList, pContext.getResources().getDrawable(R.drawable.marker_default), pOnItemGestureListener,
        pContext
    )

    constructor(
        pContext: Context,
        pList: MutableList<Item?>?,
        pOnItemGestureListener: OnItemGestureListener<Item?>?
    ) : this(
        pList, pContext.getResources().getDrawable(R.drawable.marker_default),
        pOnItemGestureListener, pContext
    )

    override fun onDetach(mapView: MapView?) {
        if (mItemList != null) mItemList!!.clear()
        mItemList = null
        mOnItemGestureListener = null
    }

    override fun onSnapToItem(pX: Int, pY: Int, pSnapPoint: Point, pMapView: IMapView?): Boolean {
        // TODO Implement this!
        return false
    }

    override fun createItem(index: Int): Item? {
        return mItemList!!.get(index)
    }

    override fun size(): Int {
        return min(mItemList!!.size, drawnItemsLimit)
    }

    fun addItem(item: Item?): Boolean {
        val result = mItemList!!.add(item)
        populate()
        return result
    }

    fun addItem(location: Int, item: Item?) {
        mItemList!!.add(location, item)
        populate()
    }

    fun addItems(items: MutableList<Item?>): Boolean {
        val result = mItemList!!.addAll(items)
        populate()
        return result
    }

    @JvmOverloads
    fun removeAllItems(withPopulate: Boolean = true) {
        mItemList!!.clear()
        if (withPopulate) {
            populate()
        }
    }

    fun removeItem(item: Item?): Boolean {
        val result = mItemList!!.remove(item)
        populate()
        return result
    }

    fun removeItem(position: Int): Item? {
        val result = mItemList!!.removeAt(position)
        populate()
        return result
    }

    /**
     * Each of these methods performs a item sensitive check. If the item is located its
     * corresponding method is called. The result of the call is returned.
     *
     *
     * Helper methods are provided so that child classes may more easily override behavior without
     * resorting to overriding the ItemGestureListener methods.
     */
    override fun onSingleTapConfirmed(event: MotionEvent, mapView: MapView?): Boolean {
        return if (activateSelectedItems(event, mapView, object : ActiveItem {
                override fun run(index: Int): Boolean {
                    val that = this@ItemizedIconOverlay
                    if (that.mOnItemGestureListener == null) {
                        return false
                    }
                    return onSingleTapUpHelper(index, that.mItemList!!.get(index), mapView)
                }
            })) true else super.onSingleTapConfirmed(event, mapView)
    }

    protected open fun onSingleTapUpHelper(index: Int, item: Item?, mapView: MapView?): Boolean {
        return this.mOnItemGestureListener!!.onItemSingleTapUp(index, item)
    }

    override fun onLongPress(event: MotionEvent, mapView: MapView?): Boolean {
        return if (activateSelectedItems(event, mapView, object : ActiveItem {
                override fun run(index: Int): Boolean {
                    val that = this@ItemizedIconOverlay
                    if (that.mOnItemGestureListener == null) {
                        return false
                    }
                    return onLongPressHelper(index, getItem(index))
                }
            })) true else super.onLongPress(event, mapView)
    }

    protected fun onLongPressHelper(index: Int, item: Item?): Boolean {
        return this.mOnItemGestureListener!!.onItemLongPress(index, item)
    }

    /**
     * When a content sensitive action is performed the content item needs to be identified. This
     * method does that and then performs the assigned task on that item.
     *
     * @param event
     * @param mapView
     * @param task
     * @return true if event is handled false otherwise
     */
    private fun activateSelectedItems(
        event: MotionEvent, mapView: MapView?,
        task: ActiveItem
    ): Boolean {
        val eventX = Math.round(event.getX())
        val eventY = Math.round(event.getY())
        for (i in this.mItemList!!.indices) {
            if (isEventOnItem(getItem(i), eventX, eventY, mapView)) {
                if (task.run(i)) {
                    return true
                }
            }
        }
        return false
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
    /**
     * When the item is touched one of these methods may be invoked depending on the type of touch.
     *
     *
     * Each of them returns true if the event was completely handled.
     */
    interface OnItemGestureListener<T> {
        fun onItemSingleTapUp(index: Int, item: T?): Boolean

        fun onItemLongPress(index: Int, item: T?): Boolean
    }

    interface ActiveItem {
        fun run(aIndex: Int): Boolean
    }
}
