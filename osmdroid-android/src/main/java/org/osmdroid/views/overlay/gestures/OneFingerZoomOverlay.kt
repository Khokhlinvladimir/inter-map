package org.osmdroid.views.overlay.gestures

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.VelocityTracker
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.abs

@SuppressLint("NewApi")
open class OneFingerZoomOverlay : Overlay() {
    private var mIsDoubleClick = false
    private var mLastY = 0f

    override fun onDoubleTapEvent(event: MotionEvent, mapView: MapView?): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mIsDoubleClick = true
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mIsDoubleClick = false
        }
        return super.onDoubleTapEvent(event, mapView)
    }

    override fun onDoubleTap(event: MotionEvent, mapView: MapView?): Boolean {
        return true
    }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView?): Boolean {
        val activeMapView = mapView ?: return false
        if (mIsDoubleClick) {
            val velocityTracker = VelocityTracker.obtain()
            velocityTracker.addMovement(event)
            velocityTracker.computeCurrentVelocity(100, 1000f)
            val velocityY = abs(velocityTracker.getYVelocity()) / 1000
            if (mLastY > event.getY()) {
                activeMapView.controller!!.setZoom(activeMapView.zoomLevelDouble - velocityY)
            } else {
                activeMapView.controller!!.setZoom(activeMapView.zoomLevelDouble + velocityY)
            }
            mLastY = event.getY()
            velocityTracker.recycle()
        }
        return super.onTouchEvent(event, activeMapView)
    }
}
