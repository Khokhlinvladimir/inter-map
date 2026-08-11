package org.osmdroid.bugtestfragments

import android.graphics.drawable.Drawable
import org.osmdroid.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem

/**
 * created on 12/6/2019.
 *
 * @author Alex O'Ree
 */
class Issue1444 : BaseSampleFragment() {
    override val sampleTitle: String
        get() = "Issue 1444 stuck label with itemized icon overlay"

    private val icons: MutableList<Drawable?> = ArrayList<Drawable?>(4)


    public override fun addOverlays() {
        super.addOverlays()

        icons.add(getResources().getDrawable(R.drawable.sfgpuci))
        icons.add(getResources().getDrawable(R.drawable.shgpuci))
        icons.add(getResources().getDrawable(R.drawable.sngpuci))
        icons.add(getResources().getDrawable(R.drawable.sugpuci))
        val myGeoPoint = GeoPoint(32.0, -74.0)

        val MY_OverlayItem = OverlayItem("1", "LABEL", "", myGeoPoint)
        MY_OverlayItem.setMarker(icons.get(1))

        val ARRAY_Of_OverlayItems = ArrayList<OverlayItem?>()
        ARRAY_Of_OverlayItems.add(MY_OverlayItem)

        val myItemizedOverlayWithFocus = ItemizedOverlayWithFocus<OverlayItem?>(
            ARRAY_Of_OverlayItems,
            object : OnItemGestureListener<OverlayItem?> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                    return false
                }

                override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                    return false
                }
            }, getContext()
        )

        myItemizedOverlayWithFocus.setFocusItemsOnTap(true)
        myItemizedOverlayWithFocus.setFocusedItem(0)
        mMapView!!.getOverlays()!!.add(myItemizedOverlayWithFocus)
        mMapView!!.invalidate()
    }
}
