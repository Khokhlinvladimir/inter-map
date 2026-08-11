// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid

import android.R as AndroidR
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnGenericMotionListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MinimapOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.OneFingerZoomOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Default map view activity.
 *
 * @author Marc Kurtz
 * @author Manuel Stahl
 */
class StarterMapFragment : Fragment() {
    // ===========================================================
    // Fields
    // ===========================================================
    private var mPrefs: SharedPreferences? = null
    private var mMapView: MapView? = null
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private var mCompassOverlay: CompassOverlay? = null
    private var mMinimapOverlay: MinimapOverlay? = null
    private var mScaleBarOverlay: ScaleBarOverlay? = null
    private var mRotationGestureOverlay: RotationGestureOverlay? = null
    private var mCopyrightOverlay: CopyrightOverlay? = null
    private var mOneFingerZoomOverlay: OneFingerZoomOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        //Note! we are programmatically construction the map view
        //be sure to handle application lifecycle correct (see note in on pause)

        mMapView = MapView(inflater.getContext())
        mMapView!!.setDestroyMode(false)
        mMapView!!.setTag("mapView") // needed for OpenStreetMapViewTest

        mMapView!!.setOnGenericMotionListener(object : OnGenericMotionListener {
            /**
             * mouse wheel zooming ftw
             * http://stackoverflow.com/questions/11024809/how-can-my-view-respond-to-a-mousewheel
             * @param v
             * @param event
             * @return
             */
            override fun onGenericMotion(v: View?, event: MotionEvent): Boolean {
                if (0 != (event.getSource() and InputDevice.SOURCE_CLASS_POINTER)) {
                    when (event.getAction()) {
                        MotionEvent.ACTION_SCROLL -> {
                            if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) mMapView!!.controller!!.zoomOut()
                            else {
                                //this part just centers the map on the current mouse location before the zoom action occurs
                                val iGeoPoint = mMapView!!.projection.fromPixels(event.getX().toInt(), event.getY().toInt())
                                mMapView!!.controller!!.animateTo(iGeoPoint)
                                mMapView!!.controller!!.zoomIn()
                            }
                            return true
                        }
                    }
                }
                return false
            }
        })
        return mMapView!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context: Context? = this.getActivity()
        val dm = context!!.getResources().getDisplayMetrics()

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


        //My Location
        //note you have handle the permissions yourself, the overlay did not do it for you
        mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mMapView)
        mLocationOverlay!!.enableMyLocation()
        mMapView!!.getOverlays()!!.add(this.mLocationOverlay)


        //Mini map
        mMinimapOverlay = MinimapOverlay(context, mMapView!!.getTileRequestCompleteHandler())
        mMinimapOverlay!!.setWidth(dm.widthPixels / 5)
        mMinimapOverlay!!.setHeight(dm.heightPixels / 5)
        mMapView!!.getOverlays()!!.add(this.mMinimapOverlay)


        //Copyright overlay
        mCopyrightOverlay = CopyrightOverlay(context)
        //i hate this very much, but it seems as if certain versions of android and/or
        //device types handle screen offsets differently
        mMapView!!.getOverlays()!!.add(this.mCopyrightOverlay)


        //On screen compass
        mCompassOverlay = CompassOverlay(
            context, InternalCompassOrientationProvider(context),
            mMapView
        )
        mCompassOverlay!!.enableCompass()
        mMapView!!.getOverlays()!!.add(this.mCompassOverlay)


        //map scale
        mScaleBarOverlay = ScaleBarOverlay(mMapView)
        mScaleBarOverlay!!.setCentred(true)
        mScaleBarOverlay!!.setScaleBarOffset(dm.widthPixels / 2, 10)
        mMapView!!.getOverlays()!!.add(this.mScaleBarOverlay)


        //support for map rotation
        mRotationGestureOverlay = RotationGestureOverlay(mMapView)
        mRotationGestureOverlay!!.setEnabled(true)
        mMapView!!.getOverlays()!!.add(this.mRotationGestureOverlay)

        //support for one finger zoom
        mOneFingerZoomOverlay = OneFingerZoomOverlay()
        mMapView!!.getOverlays()!!.add(this.mOneFingerZoomOverlay)

        //needed for pinch zooms
        mMapView!!.setMultiTouchControls(true)

        //scales tiles to the current screen's DPI, helps with readability of labels
        mMapView!!.setTilesScaledToDpi(true)

        //the rest of this is restoring the last map location the user looked at
        val zoomLevel = mPrefs!!.getFloat(PREFS_ZOOM_LEVEL_DOUBLE, 1f)
        mMapView!!.controller!!.setZoom(zoomLevel.toDouble())
        val orientation = mPrefs!!.getFloat(PREFS_ORIENTATION, 0f)
        mMapView!!.setMapOrientation(orientation, false)
        val latitudeString: String = mPrefs!!.getString(PREFS_LATITUDE_STRING, "1.0")!!
        val longitudeString: String = mPrefs!!.getString(PREFS_LONGITUDE_STRING, "1.0")!!
        val latitude = latitudeString.toDouble()
        val longitude = longitudeString.toDouble()
        mMapView!!.setExpectedCenter(GeoPoint(latitude, longitude))

        setHasOptionsMenu(true)
    }

    override fun onPause() {
        //save the current location
        val edit = mPrefs!!.edit()
        edit.putString(PREFS_TILE_SOURCE, mMapView!!.getTileProvider()!!.getTileSource().name())
        edit.putFloat(PREFS_ORIENTATION, mMapView!!.getMapOrientation())
        edit.putString(PREFS_LATITUDE_STRING, mMapView!!.mapCenter!!.latitude.toString())
        edit.putString(PREFS_LONGITUDE_STRING, mMapView!!.mapCenter!!.longitude.toString())
        edit.putFloat(PREFS_ZOOM_LEVEL_DOUBLE, mMapView!!.zoomLevelDouble.toFloat())
        edit.commit()

        mMapView!!.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //this part terminates all of the overlays and background threads for osmdroid
        //only needed when you programmatically create the map
        mMapView!!.onDetach()
    }

    override fun onResume() {
        super.onResume()
        val tileSourceName: String = mPrefs!!.getString(
            StarterMapFragment.Companion.PREFS_TILE_SOURCE,
            TileSourceFactory.DEFAULT_TILE_SOURCE.name()
        )!!
        try {
            val tileSource = TileSourceFactory.getTileSource(tileSourceName)
            mMapView!!.setTileSource(tileSource)
        } catch (e: IllegalArgumentException) {
            mMapView!!.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        }

        mMapView!!.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Put overlay items first
        mMapView!!.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView)

        // Put "About" menu item last
        menu.add(0, MENU_ABOUT, Menu.CATEGORY_SECONDARY, R.string.about).setIcon(
            AndroidR.drawable.ic_menu_info_details
        )

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(pMenu: Menu) {
        mMapView!!.getOverlayManager().onPrepareOptionsMenu(pMenu, MENU_LAST_ID, mMapView)
        super.onPrepareOptionsMenu(pMenu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMapView!!.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView)) {
            return true
        }

        when (item.getItemId()) {
            MENU_ABOUT -> {
                val builder = AlertDialog.Builder(getActivity())
                    .setTitle(org.osmdroid.R.string.app_name).setMessage(org.osmdroid.R.string.about_message)
                    .setIcon(org.osmdroid.R.drawable.icon)
                    .setPositiveButton(android.R.string.ok, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, whichButton: Int) {
                            //
                        }
                    }
                    )
                builder.create().show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun zoomIn() {
        mMapView!!.controller!!.zoomIn()
    }

    fun zoomOut() {
        mMapView!!.controller!!.zoomOut()
    }

    // @Override
    // public boolean onTrackballEvent(final MotionEvent event) {
    // return this.mMapView.onTrackballEvent(event);
    // }
    fun invalidateMapView() {
        mMapView!!.invalidate()
    }

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private const val PREFS_NAME = "org.andnav.osm.prefs"
        private const val PREFS_TILE_SOURCE = "tilesource"
        private const val PREFS_LATITUDE_STRING = "latitudeString"
        private const val PREFS_LONGITUDE_STRING = "longitudeString"
        private const val PREFS_ORIENTATION = "orientation"
        private const val PREFS_ZOOM_LEVEL_DOUBLE = "zoomLevelDouble"

        private val MENU_ABOUT = Menu.FIRST + 1
        private val MENU_LAST_ID: Int = MENU_ABOUT + 1 // Always set to last unused id

        fun newInstance(): StarterMapFragment {
            return StarterMapFragment()
        }
    }
}

