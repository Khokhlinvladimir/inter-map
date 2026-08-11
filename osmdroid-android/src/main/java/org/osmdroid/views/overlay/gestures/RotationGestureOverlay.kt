package org.osmdroid.views.overlay.gestures

import android.R
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.IOverlayMenuProvider
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureDetector.RotationListener

open class RotationGestureOverlay(private var mMapView: MapView?) : Overlay(), RotationListener, IOverlayMenuProvider {
    private val mRotationDetector: RotationGestureDetector
    override var isOptionsMenuEnabled: Boolean = true

    /**
     * use [.RotationGestureOverlay] instead.
     */
    @Deprecated("")
    constructor(context: Context?, mapView: MapView?) : this(mapView)

    override fun onTouchEvent(event: MotionEvent, mapView: MapView?): Boolean {
        mRotationDetector.onTouch(event)
        return super.onTouchEvent(event, mapView)
    }

    var timeLastSet: Long = 0L
    val deltaTime: Long = 25L
    var currentAngle: Float = 0f

    init {
        mRotationDetector = RotationGestureDetector(this)
    }

    override fun onRotate(deltaAngle: Float) {
        currentAngle += deltaAngle
        if (System.currentTimeMillis() - deltaTime > timeLastSet) {
            timeLastSet = System.currentTimeMillis()
            mMapView!!.setMapOrientation(mMapView!!.getMapOrientation() + currentAngle)
        }
    }

    override fun onDetach(map: MapView?) {
        mMapView = null
    }

    override fun onCreateOptionsMenu(pMenu: Menu?, pMenuIdOffset: Int, pMapView: MapView?): Boolean {
        val menu = pMenu ?: return false
        menu.add(0, MENU_ENABLED + pMenuIdOffset, Menu.NONE, "Enable rotation").setIcon(
            R.drawable.ic_menu_info_details
        )
        if (SHOW_ROTATE_MENU_ITEMS) {
            menu.add(
                0, MENU_ROTATE_CCW + pMenuIdOffset, Menu.NONE,
                "Rotate maps counter clockwise"
            ).setIcon(R.drawable.ic_menu_rotate)
            menu.add(0, MENU_ROTATE_CW + pMenuIdOffset, Menu.NONE, "Rotate maps clockwise")
                .setIcon(R.drawable.ic_menu_rotate)
        }
        return true
    }

    override fun onOptionsItemSelected(pItem: MenuItem?, pMenuIdOffset: Int, pMapView: MapView?): Boolean {
        val item = pItem ?: return false
        if (item.getItemId() == MENU_ENABLED + pMenuIdOffset) {
            if (this.isEnabled()) {
                mMapView!!.setMapOrientation(0f)
                this.setEnabled(false)
            } else {
                this.setEnabled(true)
                return true
            }
        } else if (item.getItemId() == MENU_ROTATE_CCW + pMenuIdOffset) {
            mMapView!!.setMapOrientation(mMapView!!.getMapOrientation() - 10)
        } else if (item.getItemId() == MENU_ROTATE_CW + pMenuIdOffset) {
            mMapView!!.setMapOrientation(mMapView!!.getMapOrientation() + 10)
        }

        return false
    }

    override fun onPrepareOptionsMenu(pMenu: Menu?, pMenuIdOffset: Int, pMapView: MapView?): Boolean {
        val menu = pMenu ?: return false
        menu.findItem(MENU_ENABLED + pMenuIdOffset).setTitle(
            if (this.isEnabled()) "Disable rotation" else "Enable rotation"
        )
        return false
    }

    override fun setEnabled(pEnabled: Boolean) {
        mRotationDetector.isEnabled = pEnabled
        super.setEnabled(pEnabled)
    }

    companion object {
        private const val SHOW_ROTATE_MENU_ITEMS = false

        private val MENU_ENABLED: Int = Overlay.getSafeMenuId()
        private val MENU_ROTATE_CCW: Int = Overlay.getSafeMenuId()
        private val MENU_ROTATE_CW: Int = Overlay.getSafeMenuId()
    }
}
