package org.osmdroid.views.overlay.infowindow

import android.content.Context
import android.text.Html
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import org.osmdroid.api.IMapView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.OverlayWithIW

/**
 * [org.osmdroid.views.overlay.infowindow.BasicInfoWindow] is the default
 * implementation of [InfoWindow] for an
 * [OverlayWithIW].
 *
 *
 * It handles a title, a description and a sub-description.
 * Clicking on the bubble will close it.
 *
 * <img alt="Class diagram around Marker class" width="686" height="413" src='./doc-files/marker-infowindow-classes.png'></img>
 *
 * @author M.Kergall
 * @see Marker
 */
open class BasicInfoWindow(layoutResId: Int, mapView: MapView) : InfoWindow(layoutResId, mapView) {
    init {
        if (mTitleId == UNDEFINED_RES_ID) setResIds(mapView.getContext())

        //default behavior: close it when clicking on the bubble:
        mView!!.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, e: MotionEvent): Boolean {
                if (e.getAction() == MotionEvent.ACTION_UP) close()
                return true
            }
        })
    }

    override fun onOpen(item: Any?) {
        val overlay = item as OverlayWithIW
        var title = overlay.getTitle()
        if (title == null) title = ""
        if (mView == null) {
            Log.w(IMapView.LOGTAG, "Error trapped, BasicInfoWindow.open, mView is null!")
            return
        }
        val view = mView ?: return
        val temp = view.findViewById<View?>(mTitleId /*R.id.title*/) as TextView?

        if (temp != null) temp.setText(title)

        var snippet = overlay.getSnippet()
        if (snippet == null) snippet = ""
        val snippetHtml = Html.fromHtml(snippet)
        (view.findViewById<View?>(mDescriptionId /*R.id.description*/) as TextView).setText(snippetHtml)

        //handle sub-description, hidding or showing the text view:
        val subDescText = view.findViewById<View?>(mSubDescriptionId) as TextView
        val subDesc = overlay.getSubDescription()
        if (subDesc != null && !("" == subDesc)) {
            subDescText.setText(Html.fromHtml(subDesc))
            subDescText.setVisibility(View.VISIBLE)
        } else {
            subDescText.setVisibility(View.GONE)
        }
    }

    override fun onClose() {
        //by default, do nothing
    }

    companion object {
        /**
         * resource id value meaning "undefined resource id"
         */
        const val UNDEFINED_RES_ID: Int = 0

        var mTitleId: Int = UNDEFINED_RES_ID
        var mDescriptionId: Int = UNDEFINED_RES_ID
        var mSubDescriptionId: Int = UNDEFINED_RES_ID
        var mImageId: Int = UNDEFINED_RES_ID //resource ids

        private fun setResIds(context: Context) {
            val packageName = context.getPackageName() //get application package name
            mTitleId = context.getResources().getIdentifier("id/bubble_title", null, packageName)
            mDescriptionId = context.getResources().getIdentifier("id/bubble_description", null, packageName)
            mSubDescriptionId = context.getResources().getIdentifier("id/bubble_subdescription", null, packageName)
            mImageId = context.getResources().getIdentifier("id/bubble_image", null, packageName)
            if (mTitleId == UNDEFINED_RES_ID || mDescriptionId == UNDEFINED_RES_ID || mSubDescriptionId == UNDEFINED_RES_ID || mImageId == UNDEFINED_RES_ID) {
                Log.e(IMapView.LOGTAG, "BasicInfoWindow: unable to get res ids in " + packageName)
            }
        }
    }
}
