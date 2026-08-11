package org.osmdroid.bugtestfragments

import android.widget.Toast
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem

/**
 * created on 5/5/2019.
 *
 * @author Alex O'Ree
 */
class Bug1322 : BaseSampleFragment() {
    val description1: String = "Line1\nLine2\nLine3\nLine4\nLine5\nLine6\nLine7\nLine8\nLine9\nLine10\nLine11\nLine12\nLine13\nLine14\nLine15"
    val description2: String = ("Line01 Line02 Line03 Line04 Line05 Line06 Line07 Line08 Line09 Line10 Line11 "
            + "Line12 Line13 Line14 Line15 Line16 Line17 Line18 Line19 Line20 Line21 Line22 Line23")
    val description5: String =
        "Line1Line2Line3Line4Line5Line6Line7Line8Line9Line10Line11Line12Line13Line14Line15line16line17line18line19line20line21line22line23line24line25line26line27line28line29line30"
    val description6: String =
        "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
    val description7: String = ("BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC"
            + "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC"
            + "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC")
    val description3: String = ("0123456789012345678901234567890123456789012345678912345678901234"
            + "0123456789012345678901234567890123456789012345678912345678901234"
            + "0123456789012345678901234567890123456789012345678912345678901234")
    val description4: String = ("Line1\nLine2\n\nLine3\nLine4\n"
            + "BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC01234567890123456789012345678901234567890123456789123456789012340123456789012345678901234567890123456789012345678912345678901234")

    override val sampleTitle: String
        get() = "ItemizedOverlayWithFocus with long titles"

    private fun message(pItem: OverlayItem) {
        Toast.makeText(getActivity(), pItem.getTitle() + ": " + pItem.getSnippet(), Toast.LENGTH_LONG).show()
    }

    private val mClicked: MutableList<OverlayItem?> = ArrayList<OverlayItem?>()

    public override fun addOverlays() {
        super.addOverlays()
        val myLocationOverlay: ItemizedOverlayWithFocus<OverlayItem?>

        val items: MutableList<OverlayItem?> = ArrayList<OverlayItem?>()

        items.add(OverlayItem("Title1", "a small descripotion", GeoPoint(-3.0, -3.0)))
        items.add(OverlayItem("Title1", description1, GeoPoint(0.0, 0.0)))
        items.add(OverlayItem("Title2", description2, GeoPoint(3.0, 3.0)))
        items.add(OverlayItem("Title3", description3, GeoPoint(6.0, 6.0)))
        items.add(OverlayItem("Title4", description4, GeoPoint(9.0, 9.0)))
        items.add(OverlayItem("Title5", description5, GeoPoint(12.0, 12.0)))
        items.add(OverlayItem("Title6", description6, GeoPoint(15.0, 15.0)))
        items.add(OverlayItem("Title7", description7, GeoPoint(18.0, 18.0)))

        val mOverlay = ItemizedOverlayWithFocus<OverlayItem?>(
            items,
            object : OnItemGestureListener<OverlayItem?> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                    return true
                }

                override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                    return false
                }
            }, getContext()
        )
        mOverlay.setFocusItemsOnTap(true)
        mMapView!!.getOverlays()!!.add(mOverlay)

        /*mMapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (mClicked.size() == 0) {
                    return false;
                }
                if (mClicked.size() == 1) {
                    message(mClicked.get(0));
                    mClicked.clear();
                    return true;
                }
                final String[] titles = new String[mClicked.size()];
                final OverlayItem[] items = new OverlayItem[titles.length];
                int i = 0;
                for(final OverlayItem item : mClicked) {
                    titles[i] = item.getTitle();
                    items[i] = item;
                    i ++;
                }
                new AlertDialog.Builder(getActivity())
                    .setItems(titles, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            message(items[i]);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                mClicked.clear();
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));

        myLocationOverlay = new ItemizedOverlayWithFocus<>(items,
            new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                    mClicked.add(item);
                    return false;
                }

                @Override
                public boolean onItemLongPress(final int index, final OverlayItem item) {
                    return false;
                }
            }, getContext());
        myLocationOverlay.setMarkerBackgroundColor(Color.BLUE);
        myLocationOverlay.setMarkerTitleForegroundColor(Color.WHITE);
        myLocationOverlay.setMarkerDescriptionForegroundColor(Color.WHITE);
        myLocationOverlay.setDescriptionBoxPadding(15);
        mMapView.getOverlays().add(myLocationOverlay);
        */
    }

    override fun skipOnCiTests(): Boolean {
        return false
    }
}
