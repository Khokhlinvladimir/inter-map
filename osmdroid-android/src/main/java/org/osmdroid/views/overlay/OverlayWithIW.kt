package org.osmdroid.views.overlay

import android.content.Context
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * The [org.osmdroid.views.overlay.OverlayWithIW] is an [Overlay] that
 * contain data [title][.getTitle] ,
 * a [snippet or description][.getSnippet],
 * and optionally a [&quot;sub-description&quot;][.getSubDescription] and that
 * can be shown in a popup-[InfoWindow] (a bubble).
 *
 *
 * Handling tap event and showing the InfoWindow at a relevant position is let to sub-classes.
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='src=' .></img>/doc-files/marker-infowindow-classes.png' />
 *
 * @author M.Kergall
 * @see BasicInfoWindow
 */
abstract class OverlayWithIW() : Overlay() {
    //InfoWindow handling
    protected var mTitle: String? = null
    protected var mSnippet: String? = null
    protected var mSubDescription: String? = null
    protected var mInfoWindow: InfoWindow? = null
    protected var mRelatedObject: Any? = null
    protected var mId: String? = null

    /**
     * set the "sub-description", an optional text to be shown in the InfoWindow, below the snippet, in a smaller text size
     */
    fun setTitle(title: String?) {
        mTitle = title
    }

    fun getTitle(): String? = mTitle

    fun setSnippet(snippet: String?) {
        mSnippet = snippet
    }

    fun getSnippet(): String? = mSnippet

    fun setSubDescription(subDescription: String?) {
        mSubDescription = subDescription
    }

    fun getSubDescription(): String? = mSubDescription

    /**
     * By default, OverlayWithIW has no InfoWindow.
     * Usage: setInfoWindow(new BasicInfoWindow(layoutResId, mapView));
     *
     * @param infoWindow the InfoWindow to be opened when tapping the overlay.
     * This InfoWindow MUST be able to handle an OverlayWithIW (as BasicInfoWindow does).
     * Set it to null to remove an existing InfoWindow.
     */
    open fun setInfoWindow(infoWindow: InfoWindow?) {
        mInfoWindow = infoWindow
    }

    fun getInfoWindow(): InfoWindow? = mInfoWindow
    /**
     * @return the related object.
     */
    /**
     * Allows to link an Object (any Object) to this marker.
     * This is particularly useful to handle custom InfoWindow.
     */
    fun setRelatedObject(relatedObject: Any?) {
        mRelatedObject = relatedObject
    }

    fun getRelatedObject(): Any? = mRelatedObject
    /**
     * @return the user-defined id.
     */
    /**
     * Allows to set a user-defined id. Example: when drawing KML objects, can be the KML id.
     *
     * @param id the user-defined id, as a String. Can be null.
     */
    fun setId(id: String?) {
        mId = id
    }

    fun getId(): String? = mId

    /**
     * Use [.OverlayWithIW] instead
     */
    @Deprecated("")
    constructor(ctx: Context?) : this()

    fun closeInfoWindow() {
        if (mInfoWindow != null) mInfoWindow!!.close()
    }

    fun onDestroy() {
        if (mInfoWindow != null) {
            mInfoWindow!!.close()
            mInfoWindow!!.onDetach()
            mInfoWindow = null
            mRelatedObject = null
        }
    }

    fun isInfoWindowOpen(): Boolean = mInfoWindow?.isOpen == true
}
