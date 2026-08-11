package org.osmdroid.views.overlay.infowindow

import android.util.Log
import android.view.View
import android.widget.ImageView
import org.osmdroid.api.IMapView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.OverlayWithIW

/**
 * [org.osmdroid.views.overlay.infowindow.MarkerInfoWindow] is the default
 * implementation of [InfoWindow] for a
 * [Marker].
 *
 *
 * It handles
 *
 *
 * R.id.bubble_title          = [OverlayWithIW.getTitle],
 * R.id.bubble_subdescription = [OverlayWithIW.getSubDescription],
 * R.id.bubble_description    = [OverlayWithIW.getSnippet],
 * R.id.bubble_image          = [Marker.getImage]
 *
 *
 * Description and sub-description interpret HTML tags (in the limits of the Html.fromHtml(String) API).
 * Clicking on the bubble will close it.
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='./doc-files/marker-infowindow-classes.png'></img>
 *
 * @author M.Kergall
 */
class MarkerInfoWindow
/**
 * @param layoutResId layout that must contain these ids: bubble_title,bubble_description,
 * bubble_subdescription, bubble_image
 * @param mapView
 */
    (layoutResId: Int, mapView: MapView) : BasicInfoWindow(layoutResId, mapView) {
    /**
     * reference to the Marker on which it is opened. Null if none.
     *
     * @return
     */
    var markerReference: Marker? = null //reference to the Marker on which it is opened. Null if none.
        protected set

    override fun onOpen(item: Any?) {
        super.onOpen(item)

        this.markerReference = item as Marker?
        if (mView == null) {
            Log.w(IMapView.LOGTAG, "Error trapped, MarkerInfoWindow.open, mView is null!")
            return
        }
        //handle image
        val view = mView ?: return
        val imageView = view.findViewById<View?>(BasicInfoWindow.mImageId /*R.id.image*/) as ImageView
        val image = markerReference!!.image
        if (image != null) {
            imageView.setImageDrawable(image) //or setBackgroundDrawable(image)?
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
            imageView.setVisibility(View.VISIBLE)
        } else imageView.setVisibility(View.GONE)
    }

    override fun onClose() {
        super.onClose()
        this.markerReference = null
        //by default, do nothing else
    }
}
