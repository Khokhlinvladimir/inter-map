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
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.util.Random

/**
 * icons generated from https://github.com/missioncommand/mil-sym-java
 * demonstrates one way to show custom icons for a given point on the map
 * (Marker)
 *
 * @author alex
 */
class SampleMilitaryIconsMarker : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    // ===========================================================
    // Fields
    // ===========================================================
    private var mRotationGestureOverlay: RotationGestureOverlay? = null
    private val icons: MutableList<Drawable?> = ArrayList<Drawable?>(4)
    private val mRandom = Random()

    // ===========================================================
    // Constructors
    // ===========================================================
    override fun addOverlays() {
        super.addOverlays()

        val context: Context? = getActivity()


        icons.add(getResources().getDrawable(R.drawable.sfgpuci))
        icons.add(getResources().getDrawable(R.drawable.shgpuci))
        icons.add(getResources().getDrawable(R.drawable.sngpuci))
        icons.add(getResources().getDrawable(R.drawable.sugpuci))


        //generates 50 randomized points
        addIcons(50)

        mRotationGestureOverlay = RotationGestureOverlay(mMapView)
        mRotationGestureOverlay!!.setEnabled(false)
        mMapView!!.getOverlays()!!.add(mRotationGestureOverlay)

        // Zoom and center on the focused item.
        mMapView!!.controller!!.setZoom(3)

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
        for (i in 0 until count) {
            val random_lon = getTileSystem().getRandomLongitude(mRandom.nextDouble())
            val random_lat = getTileSystem().getRandomLatitude(mRandom.nextDouble())
            val m = Marker(mMapView)
            m.setPosition(GeoPoint(random_lat, random_lon))
            val index = mRandom.nextInt(icons.size)
            m.setSnippet("A random point")
            m.setSubDescription("location: " + random_lat + "," + random_lon)
            m.setIcon(icons.get(index))
            mMapView!!.getOverlayManager().add(m)
        }

        mMapView!!.invalidate()
        Toast.makeText(getActivity(), count.toString() + " icons added! Current size: " + mMapView!!.getOverlayManager().size, Toast.LENGTH_SHORT)
            .show()
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
        private const val SAMPLE_TITLE: String = "Military Icons using Markers"

        private val MENU_ZOOMIN_ID = Menu.FIRST
        private val MENU_ZOOMOUT_ID: Int = MENU_ZOOMIN_ID + 1
        private val MENU_ADDICONS_ID: Int = MENU_ZOOMOUT_ID + 1
        private val MENU_LAST_ID: Int = MENU_ADDICONS_ID + 1 // Always set to last unused id
    }
}
