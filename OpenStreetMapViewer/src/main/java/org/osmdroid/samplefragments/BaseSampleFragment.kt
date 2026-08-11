package org.osmdroid.samplefragments

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnGenericMotionListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.osmdroid.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay

abstract class BaseSampleFragment : Fragment() {
    var gotoLocationDialog: AlertDialog? = null

    abstract val sampleTitle: String?

    // ===========================================================
    // Fields
    // ===========================================================
    @JvmField
    protected var mMapView: MapView? = null

    fun getmMapView(): MapView? {
        return mMapView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Log.d(TAG, "onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mMapView = MapView(inflater.getContext())
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
                                mMapView!!.controller!!.zoomIn()
                            }
                            return true
                        }
                    }
                }
                return false
            }
        })
        Log.d(TAG, "onCreateView")
        return mMapView
    }


    override fun onPause() {
        if (mMapView != null) {
            mMapView!!.onPause()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (mMapView != null) {
            mMapView!!.onResume()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated")

        if (mMapView != null) {
            addOverlays()

            val context: Context? = this.getActivity()
            val dm = context!!.getResources().getDisplayMetrics()

            val copyrightOverlay = CopyrightOverlay(getActivity())
            copyrightOverlay.setTextSize(10)

            mMapView!!.getOverlays()!!.add(copyrightOverlay)
            mMapView!!.setMultiTouchControls(true)
            mMapView!!.setTilesScaledToDpi(true)
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDetach")
        if (mMapView != null) mMapView!!.onDetach()
        mMapView = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        if (gotoLocationDialog != null) gotoLocationDialog!!.dismiss()
    }

    var MENU_VERTICAL_REPLICATION: Int = 0
    var MENU_HORIZTONAL_REPLICATION: Int = 0
    var MENU_ROTATE_CLOCKWISE: Int = 0
    var MENU_ROTATE_COUNTER_CLOCKWISE: Int = 0
    var MENU_SCALE_TILES: Int = 0
    var MENU_GOTO: Int = 0


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val add = menu.add("Run Tests")
        MENU_LAST_ID++
        MENU_VERTICAL_REPLICATION = MENU_LAST_ID
        menu.add(0, MENU_VERTICAL_REPLICATION, Menu.NONE, "Vertical Replication").setCheckable(true)
        MENU_LAST_ID++
        MENU_HORIZTONAL_REPLICATION = MENU_LAST_ID
        menu.add(0, MENU_HORIZTONAL_REPLICATION, Menu.NONE, "Horizontal Replication").setCheckable(true)

        MENU_LAST_ID++
        MENU_SCALE_TILES = MENU_LAST_ID
        menu.add(0, MENU_SCALE_TILES, Menu.NONE, "Scale Tiles").setCheckable(true)

        MENU_LAST_ID++
        MENU_GOTO = MENU_LAST_ID
        menu.add(0, MENU_GOTO, Menu.NONE, "Go To")

        MENU_LAST_ID++
        MENU_ROTATE_CLOCKWISE = MENU_LAST_ID
        menu.add(0, MENU_ROTATE_CLOCKWISE, Menu.NONE, "Rotate Clockwise")

        MENU_LAST_ID++
        MENU_ROTATE_COUNTER_CLOCKWISE = MENU_LAST_ID
        menu.add(0, MENU_ROTATE_COUNTER_CLOCKWISE, Menu.NONE, "Rotate Counter Clockwise")
        // Put overlay items first
        try {
            mMapView!!.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView)
        } catch (npe: NullPointerException) {
            //can happen during CI tests and very rapid fragment switching
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        try {
            var item = menu.findItem(MENU_VERTICAL_REPLICATION)
            item.setChecked(mMapView!!.isVerticalMapRepetitionEnabled())
            item = menu.findItem(MENU_HORIZTONAL_REPLICATION)
            item.setChecked(mMapView!!.isHorizontalMapRepetitionEnabled())

            item = menu.findItem(MENU_SCALE_TILES)
            item.setChecked(mMapView!!.isTilesScaledToDpi())
            mMapView!!.getOverlayManager().onPrepareOptionsMenu(menu, MENU_LAST_ID, mMapView)
        } catch (npe: NullPointerException) {
            //can happen during CI tests and very rapid fragment switching
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getTitle() != null && item.getTitle().toString() == "Run Tests") {
            Thread(object : Runnable {
                override fun run() {
                    try {
                        runTestProcedures()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }).start()
            return true
        } else if (item.getItemId() == MENU_HORIZTONAL_REPLICATION) {
            mMapView!!.setHorizontalMapRepetitionEnabled(!mMapView!!.isHorizontalMapRepetitionEnabled())
            mMapView!!.invalidate()
            return true
        } else if (item.getItemId() == MENU_VERTICAL_REPLICATION) {
            mMapView!!.setVerticalMapRepetitionEnabled(!mMapView!!.isVerticalMapRepetitionEnabled())
            mMapView!!.invalidate()
            return true
        } else if (item.getItemId() == MENU_SCALE_TILES) {
            mMapView!!.setTilesScaledToDpi(!mMapView!!.isTilesScaledToDpi())
            mMapView!!.invalidate()
            return true
        } else if (item.getItemId() == MENU_ROTATE_CLOCKWISE) {
            var currentRotation = mMapView!!.getMapOrientation() + 10
            if (currentRotation > 360) currentRotation = currentRotation - 360
            mMapView!!.setMapOrientation(currentRotation, true)

            return true
        } else if (item.getItemId() == MENU_ROTATE_COUNTER_CLOCKWISE) {
            var currentRotation = mMapView!!.getMapOrientation() - 10
            if (currentRotation < 0) currentRotation = currentRotation + 360
            mMapView!!.setMapOrientation(currentRotation, true)
            return true
        } else if (item.getItemId() == MENU_GOTO) {
            //TODO dialog with lat/lon prompt
            //prompt for input params
            val builder = AlertDialog.Builder(getActivity())

            val view = View.inflate(getActivity(), R.layout.gotolocation, null)

            val lat = view.findViewById<View?>(R.id.latlonPicker_latitude) as EditText
            val lon = view.findViewById<View?>(R.id.latlonPicker_longitude) as EditText
            val cancel = view.findViewById<View?>(R.id.latlonPicker_cancel) as Button
            cancel.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    gotoLocationDialog!!.dismiss()
                }
            })

            val ok = view.findViewById<View?>(R.id.latlonPicker_ok) as Button
            ok.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    gotoLocationDialog!!.dismiss()
                    try {
                        val latd = lat.getText().toString().toDouble()
                        if (latd < TileSystem.MinLatitude || latd > TileSystem.MaxLatitude) throw Exception()
                        val lond = lon.getText().toString().toDouble()
                        if (lond < TileSystem.MinLongitude || lond > TileSystem.MaxLongitude) throw Exception()
                        val pt = GeoPoint(latd, lond)
                        mMapView!!.controller!!.animateTo(pt)
                    } catch (ex: Exception) {
                        Toast.makeText(getActivity(), "Invalid input", Toast.LENGTH_SHORT).show()
                    }
                }
            })

            builder.setView(view)
            builder.setCancelable(true)
            builder.setOnCancelListener(object : DialogInterface.OnCancelListener {
                override fun onCancel(dialog: DialogInterface?) {
                    gotoLocationDialog!!.dismiss()
                }
            })
            gotoLocationDialog = builder.create()
            gotoLocationDialog!!.show()
        } else if (mMapView!!.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView)) {
            return true
        }
        return false
    }

    /**
     * An appropriate place to override and add overlays.
     */
    protected open fun addOverlays() {
    }

    open fun skipOnCiTests(): Boolean {
        return true
    }

    /**
     * optional place to put automated test procedures, used during the connectCheck tests
     * this is called OFF of the UI thread. block this method call util the test is done
     */
    @Throws(Exception::class)
    open fun runTestProcedures() {
    }

    companion object {
        private var MENU_LAST_ID = Menu.FIRST // Always set to last unused id
        const val TAG: String = "osmBaseFrag"
    }
}
