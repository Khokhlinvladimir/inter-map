package org.osmdroid.samplefragments.data

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

/**
 * icons generated from https://github.com/missioncommand/mil-sym-java
 * demonstrates one way to show custom icons for a given point on the map
 * (itemized icon)
 *
 * @author alex
 */
class SampleMilitaryIconsItemizedIcons : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    // ===========================================================
    // Fields
    // ===========================================================
    private var itemOverlay: ItemizedOverlayWithFocus<OverlayItem?>? = null
    private val icons: MutableList<Drawable?> = ArrayList<Drawable?>(4)

    // ===========================================================
    // Constructors
    // ===========================================================
    override fun addOverlays() {
        super.addOverlays()

        val context: Context = requireActivity()


        icons.add(getResources().getDrawable(R.drawable.sfgpuci))
        icons.add(getResources().getDrawable(R.drawable.shgpuci))
        icons.add(getResources().getDrawable(R.drawable.sngpuci))
        icons.add(getResources().getDrawable(R.drawable.sugpuci))

        /* Itemized Overlay */
        run {
            /* OnTapListener for the Markers, shows a simple Toast. */
            itemOverlay = ItemizedOverlayWithFocus<OverlayItem?>(
                ArrayList<OverlayItem?>(),
                object : OnItemGestureListener<OverlayItem?> {
                    override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                        Toast.makeText(
                            context,
                            ("Item '" + item!!.getTitle() + "' (index=" + index
                                    + ") got single tapped up"), Toast.LENGTH_LONG
                        ).show()
                        return true
                    }

                    override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                        Toast.makeText(
                            context,
                            ("Item '" + item!!.getTitle() + "' (index=" + index
                                    + ") got long pressed"), Toast.LENGTH_LONG
                        ).show()
                        return true
                    }
                }, context
            )
            itemOverlay!!.setFocusItemsOnTap(true)
            itemOverlay!!.setFocusedItem(0)

            //generates 50 randomized points
            addIcons(50)

            mMapView!!.getOverlays()!!.add(itemOverlay)

            val mRotationGestureOverlay: RotationGestureOverlay
            mRotationGestureOverlay = RotationGestureOverlay(mMapView)
            mRotationGestureOverlay.setEnabled(false)
            mMapView!!.getOverlays()!!.add(mRotationGestureOverlay)
        }

        /* MiniMap */
        run {}

        // Zoom and center on the focused item.
        mMapView!!.controller!!.setZoom(3.0)
        val geoPoint = itemOverlay!!.focusedItem!!.getPoint()
        mMapView!!.controller!!.animateTo(geoPoint)

        setHasOptionsMenu(true)
        Toast.makeText(context, "Icon selection and location are random!", Toast.LENGTH_SHORT).show()
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Put overlay items first
        mMapView!!.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView)

        menu.add(0, MENU_ZOOMIN_ID, Menu.NONE, "ZoomIn")
        menu.add(0, MENU_ZOOMOUT_ID, Menu.NONE, "ZoomOut")
        menu.add(0, MENU_ZOOMOUT_ID, Menu.NONE, "ZoomOut")
        menu.add(0, MENU_ADDICONS_ID, Menu.NONE, "AddIcons")

        super.onCreateOptionsMenu(menu, inflater)
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        mMapView!!.getOverlayManager().onPrepareOptionsMenu(menu, MENU_LAST_ID, mMapView)
        super.onPrepareOptionsMenu(menu)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMapView!!.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView)) {
            return true
        }

        when (item.getItemId()) {
            MENU_ZOOMIN_ID -> {
                mMapView!!.controller!!.zoomIn()
                return true
            }

            MENU_ZOOMOUT_ID -> {
                mMapView!!.controller!!.zoomOut()
                return true
            }

            MENU_ADDICONS_ID -> {
                addIcons(500)
                return true
            }
        }
        return false
    }

    private fun addIcons(count: Int) {
        /* Create a static ItemizedOverlay showing some Markers on various cities. */
        val items = ArrayList<OverlayItem?>()
        for (i in 0 until count) {
            val random_lon = (Math.random() * 360) - 180
            val random_lat = (Math.random() * 180) - 90
            val overlayItem: OverlayItem
            overlayItem = OverlayItem(
                "A random point", "SampleDescription", GeoPoint(
                    random_lat,
                    random_lon
                )
            )
            var index = (Math.random() * (icons.size)).toInt()
            if (index == icons.size) {
                index--
            }
            overlayItem.setMarker(icons.get(index))
            items.add(overlayItem)
        }
        itemOverlay!!.addItems(items)
        mMapView!!.invalidate()
        Toast.makeText(getActivity(), count.toString() + " icons added! Current size: " + itemOverlay!!.size(), Toast.LENGTH_SHORT).show()
    }


    public override fun onDestroyView() {
        //itemOverlay.onDetach(mMapView);
        super.onDestroyView()
    } // ===========================================================
    // Methods
    // ===========================================================
    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================


    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private const val SAMPLE_TITLE: String = "Military Icons using Itemized Icons"

        private val MENU_ZOOMIN_ID = Menu.FIRST
        private val MENU_ZOOMOUT_ID: Int = MENU_ZOOMIN_ID + 1
        private val MENU_ADDICONS_ID: Int = MENU_ZOOMOUT_ID + 1
        private val MENU_LAST_ID: Int = MENU_ADDICONS_ID + 1 // Always set to last unused id
    }
}
