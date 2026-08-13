package org.osmdroid.views.overlay.gestures

import android.view.MotionEvent
import kotlin.math.atan2

/**
 * heads up, this class is used internally by osmdroid, you're welcome to use but it the interface
 * [RotationListener] will not fire as expected. It is used internally by osmdroid. If you want
 * to listen for rotation changes on the [org.osmdroid.views.MapView] then use [org.osmdroid.views.MapView.setMapListener]
 * and check for [MapView.getMapOrientation]. See [https://github.com/osmdroid/osmdroid/issues/628](https://github.com/osmdroid/osmdroid/issues/628)
 */
open class RotationGestureDetector(private val mListener: RotationListener) {
    /**
     * heads up, this class is used internally by osmdroid, you're welcome to use but it the interface
     * [RotationListener] will not fire as expected. It is used internally by osmdroid. If you want
     * to listen for rotation changes on the [org.osmdroid.views.MapView] then use [org.osmdroid.views.MapView.setMapListener]
     * and check for [MapView.getMapOrientation]
     * See [https://github.com/osmdroid/osmdroid/issues/628](https://github.com/osmdroid/osmdroid/issues/628)
     */
    interface RotationListener {
        fun onRotate(deltaAngle: Float)
    }

    protected var mRotation: Float = 0f
    open var isEnabled: Boolean = true

    open fun onTouch(e: MotionEvent) {
        if (e.getPointerCount() != 2) return

        if (e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            mRotation = rotation(e)
        }

        val rotation: Float = rotation(e)
        val delta = rotation - mRotation

        //we have to allow detector to capture and store the new rotation to avoid UI jump when
        //user enables the overlay again
        if (this.isEnabled) {
            mRotation += delta
            mListener.onRotate(delta)
        } else {
            mRotation = rotation
        }
    }

    companion object {
        private fun rotation(event: MotionEvent): Float {
            val delta_x = (event.getX(0) - event.getX(1)).toDouble()
            val delta_y = (event.getY(0) - event.getY(1)).toDouble()
            val radians = atan2(delta_y, delta_x)
            return Math.toDegrees(radians).toFloat()
        }
    }
}
