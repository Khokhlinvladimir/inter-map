package org.osmdroid.views

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Canvas
import android.os.Build
import android.view.MotionEvent
import android.view.animation.LinearInterpolator

class CustomZoomButtonsController(private val mMapView: MapView) {
    enum class Visibility {
        ALWAYS, NEVER, SHOW_AND_FADEOUT
    }

    private val mThreadSync = Any()
    private var mFadeOutAnimation: ValueAnimator? = null
    val display: CustomZoomButtonsDisplay = CustomZoomButtonsDisplay(mMapView)
    private var mListener: OnZoomListener? = null
    private var mZoomInEnabled = false
    private var mZoomOutEnabled = false
    private var mAlpha01 = 0f
    private var detached = false
    private var mVisibility = Visibility.NEVER
    private var mFadeOutAnimationDurationInMillis = 500
    private var mShowDelayInMillis = 3500
    private var mJustActivated = false
    private var mLatestActivation: Long = 0
    private var mThread: Thread? = null
    private val mRunnable: Runnable

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

                mFadeOutAnimation = ValueAnimator.ofFloat(0f, 1f)
                (mFadeOutAnimation as? ValueAnimator)?.let {
                it.interpolator = LinearInterpolator()
                    it.duration = mFadeOutAnimationDurationInMillis.toLong()
                    it.addUpdateListener(
                        AnimatorUpdateListener { valueAnimator ->
                            if (detached) {
                                it.cancel()
                                return@AnimatorUpdateListener
                            }
                            mAlpha01 = 1 - valueAnimator.animatedValue as Float
                            invalidate()
                        }
                )
            }
        } else {
            mFadeOutAnimation = null
        }
        mRunnable = Runnable {
            while (true) {
                val pending = mLatestActivation + mShowDelayInMillis - nowInMillis()
                if (pending <= 0) {
                    break
                }
                try {
                    Thread.sleep(pending, 0)
                } catch (e: InterruptedException) {
                    //
                }
            }
            startFadeOut()
        }
    }

    fun setZoomInEnabled(pEnabled: Boolean) {
        mZoomInEnabled = pEnabled
    }

    fun setZoomOutEnabled(pEnabled: Boolean) {
        mZoomOutEnabled = pEnabled
    }

    fun setOnZoomListener(pListener: OnZoomListener?) {
        mListener = pListener
    }

    fun setVisibility(pVisibility: Visibility) {
        mVisibility = pVisibility
        mAlpha01 = when (mVisibility) {
            Visibility.ALWAYS -> 1f
            Visibility.NEVER, Visibility.SHOW_AND_FADEOUT -> 0f
        }
    }

    fun setShowFadeOutDelays(pShowDelayInMillis: Int,
                             pFadeOutAnimationDurationInMillis: Int) {
        mShowDelayInMillis = pShowDelayInMillis
        mFadeOutAnimationDurationInMillis = pFadeOutAnimationDurationInMillis
    }

    fun onDetach() {
        detached = true
        stopFadeOut()
    }

    private fun nowInMillis(): Long {
        return System.currentTimeMillis()
    }

    private fun startFadeOut() {
        if (detached) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mFadeOutAnimation!!.startDelay = 0
            mMapView.post { mFadeOutAnimation!!.start() }
        } else {
            mAlpha01 = 0f
            invalidate()
        }
    }

    private fun stopFadeOut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mFadeOutAnimation!!.cancel()
        }
    }

    private fun invalidate() {
        if (detached) {
            return
        }
        mMapView.postInvalidate()
    }

    fun activate() {
        if (detached) {
            return
        }
        if (mVisibility != Visibility.SHOW_AND_FADEOUT) {
            return
        }
        val alpha = mAlpha01
        mJustActivated = if (!mJustActivated) {
            alpha == 0f
        } else {
            false
        }
        stopFadeOut()
        mAlpha01 = 1f
        mLatestActivation = nowInMillis()
        invalidate()
        if (mThread == null || mThread!!.state == Thread.State.TERMINATED) {
            synchronized(mThreadSync) {
                if (mThread == null || mThread!!.state == Thread.State.TERMINATED) {
                    mThread = Thread(mRunnable)
                    mThread!!.name = this.javaClass.name + "#active"
                    mThread!!.start()
                }
            }
        }
    }

    private fun checkJustActivated(): Boolean {
        if (mJustActivated) {
            mJustActivated = false
            return true
        }
        return false
    }

    fun isTouched(pMotionEvent: MotionEvent): Boolean {
        if (mAlpha01 == 0f) {
            return false
        }
        if (checkJustActivated()) {
            return false
        }
        if (display.isTouched(pMotionEvent, true)) {
            if (mZoomInEnabled && mListener != null) {
                mListener!!.onZoom(true)
            }
            return true
        }
        if (display.isTouched(pMotionEvent, false)) {
            if (mZoomOutEnabled && mListener != null) {
                mListener!!.onZoom(false)
            }
            return true
        }
        return false
    }

    interface OnZoomListener {
        fun onVisibilityChanged(b: Boolean)
        fun onZoom(b: Boolean)
    }

    @Deprecated("")
    fun onSingleTapConfirmed(pMotionEvent: MotionEvent): Boolean {
        return isTouched(pMotionEvent)
    }

    @Deprecated("")
    fun onLongPress(pMotionEvent: MotionEvent): Boolean {
        return isTouched(pMotionEvent)
    }

    fun draw(pCanvas: Canvas) {
        display.draw(pCanvas, mAlpha01, mZoomInEnabled, mZoomOutEnabled)
    }
}