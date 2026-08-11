package org.osmdroid.samplefragments.data

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.osmdroid.api.IGeoPoint
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.OverlayItem

/**
 * @author Fabrice Fontaine
 * Sample on how to handle a click on overlapping [OverlayItem]s
 * @since 6.0.3
 */
class SampleItemizedOverlayMultiClick : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    private val mClicked: MutableList<OverlayItem> = ArrayList<OverlayItem>()

    override fun addOverlays() {
        super.addOverlays()

        val context: Context = requireActivity()

        val datas: MutableList<DataContainer> = data
        val items: MutableList<OverlayItem?> = ArrayList<OverlayItem?>()
        val geoPoints: MutableList<IGeoPoint> = ArrayList()
        for (data in datas) {
            geoPoints.add(requireNotNull(data.geoPoint))
            items.add(OverlayItem(data.title, data.snippet, data.geoPoint))
        }
        val box = BoundingBox.fromGeoPoints(geoPoints)

        mMapView!!.getOverlays()!!.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (mClicked.size == 0) {
                    return false
                }
                if (mClicked.size == 1) {
                    message(mClicked.get(0))
                    mClicked.clear()
                    return true
                }
                val titles = arrayOfNulls<String>(mClicked.size)
                val items = arrayOfNulls<OverlayItem>(titles.size)
                var i = 0
                for (item in mClicked) {
                    titles[i] = item.getTitle()
                    items[i] = item
                    i++
                }
                AlertDialog.Builder(getActivity()!!)
                    .setItems(titles, object : DialogInterface.OnClickListener {
                        override fun onClick(dialogInterface: DialogInterface?, i: Int) {
                            message(items[i]!!)
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show()
                mClicked.clear()
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }))

        val myLocationOverlay: ItemizedOverlayWithFocus<OverlayItem?>
        myLocationOverlay = ItemizedOverlayWithFocus<OverlayItem?>(
            items,
            object : OnItemGestureListener<OverlayItem?> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                    mClicked.add(item!!)
                    return false
                }

                override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                    return false
                }
            }, context
        )
        myLocationOverlay.setMarkerBackgroundColor(Color.BLUE)
        myLocationOverlay.setMarkerTitleForegroundColor(Color.WHITE)
        myLocationOverlay.setMarkerDescriptionForegroundColor(Color.WHITE)
        myLocationOverlay.setDescriptionBoxPadding(15)
        mMapView!!.getOverlays()!!.add(myLocationOverlay)

        mMapView!!.post(object : Runnable {
            override fun run() {
                mMapView!!.zoomToBoundingBox(box, false, 50)
            }
        })
    }

    private fun message(pItem: OverlayItem) {
        Toast.makeText(getActivity(), pItem.getTitle() + ": " + pItem.getSnippet(), Toast.LENGTH_LONG).show()
    }

    class DataContainer internal constructor(val title: String?, val snippet: String?, val geoPoint: IGeoPoint?)
    companion object {
        private const val SAMPLE_TITLE: String = "Overlapping ItemizedOverlays' click"

        @JvmStatic
        val data: MutableList<DataContainer>
            get() {
                val items: MutableList<DataContainer> = ArrayList<DataContainer>()
                items.add(
                    DataContainer(
                        "Bode Museum",
                        "The sculpture collection shows art of the Christian Orient, sculptures from "
                                + "Byzantium and Ravenna, sculptures of the Middle Ages, the Italian Gothic, and the early Renaissance.",
                        GeoPoint(52.521944, 13.394722)
                    )
                )
                items.add(
                    DataContainer(
                        "Altes Museum",
                        "It houses the Antikensammlung (antiquities collection) of the Berlin State Museums.",
                        GeoPoint(52.519444, 13.398333)
                    )
                )
                items.add(
                    DataContainer(
                        "Neues Museum",
                        "Exhibits include the Egyptian and Prehistory and Early History collections,"
                                + "as it did before the war. The artifacts it houses include the iconic bust of the Egyptian queen Nefertiti.",
                        GeoPoint(52.520555, 13.397777)
                    )
                )
                items.add(
                    DataContainer(
                        "Alte Nationalgalerie",
                        "The collection contains works of the Neoclassical and Romantic movements,"
                                + " of the Biedermeier, French Impressionism and early Modernism.",
                        GeoPoint(52.520833, 13.398055)
                    )
                )
                items.add(
                    DataContainer(
                        "Pergamon Museum",
                        ("The Pergamon Museum houses monumental buildings such as the Pergamon Altar,"
                                + " the Ishtar Gate of Babylon, the Market Gate of Miletus reconstructed from the ruins"
                                + " found in Anatolia, as well as the Mshatta Facade."),
                        GeoPoint(52.521, 13.396)
                    )
                )
                items.add(
                    DataContainer(
                        "Gemäldegalerie",
                        "It holds one of the world's leading collections of European paintings from the 13th to the 18th centuries.",
                        GeoPoint(52.508472, 13.365416)
                    )
                )
                items.add(
                    DataContainer(
                        "Kunstgewerbemuseum",
                        "It's an internationally important museum of the decorative arts.",
                        GeoPoint(52.5097, 13.3674)
                    )
                )
                items.add(
                    DataContainer(
                        "Musical Instrument Museum",
                        "The Museum holds over 3,500 musical instruments from the 16th century onward "
                                + "and is one of the largest and most representative musical instrument collections in Germany.",
                        GeoPoint(52.510277, 13.370833)
                    )
                )
                items.add(
                    DataContainer(
                        "Kupferstichkabinett",
                        "It is the largest museum of graphic art in Germany, with more than 500,000 prints"
                                + "and around 110,000 individual works on paper.",
                        GeoPoint(52.508333, 13.366944)
                    )
                )
                return items
            }
    }
}
