package org.osmdroid.samplefragments.animations

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.TargetApi
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.Property
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/* Copyright 2013 Google Inc.
  Licensed under Apache 2.0: http://www.apache.org/licenses/LICENSE-2.0.html */


object MarkerAnimation {
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    fun animateMarkerToGB(map: MapView, marker: Marker, finalPosition: GeoPoint, GeoPointInterpolator: GeoPointInterpolator) {
        val startPosition = marker.getPosition()
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val interpolator: Interpolator = AccelerateDecelerateInterpolator()
        val durationInMs = 3000f

        handler.post(object : Runnable {
            var elapsed: Long = 0
            var t: Float = 0f
            var v: Float = 0f

            override fun run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start
                t = elapsed / durationInMs
                v = interpolator.getInterpolation(t)

                marker.setPosition(GeoPointInterpolator.interpolate(v, startPosition, finalPosition))
                map.invalidate()
                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    fun animateMarkerToHC(map: MapView, marker: Marker, finalPosition: GeoPoint, GeoPointInterpolator: GeoPointInterpolator): ValueAnimator {
        val startPosition = marker.getPosition()

        val valueAnimator = ValueAnimator()
        valueAnimator.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                val v = animation.getAnimatedFraction()
                val newPosition = GeoPointInterpolator.interpolate(v, startPosition, finalPosition)
                marker.setPosition(newPosition)
                map.invalidate()
            }
        })
        valueAnimator.setFloatValues(0f, 1f) // Ignored.
        valueAnimator.setDuration(3000)
        valueAnimator.start()
        return valueAnimator
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun animateMarkerToICS(map: MapView?, marker: Marker?, finalPosition: GeoPoint?, GeoPointInterpolator: GeoPointInterpolator): ObjectAnimator {
        val typeEvaluator: TypeEvaluator<GeoPoint?> = object : TypeEvaluator<GeoPoint?> {
            override fun evaluate(fraction: Float, startValue: GeoPoint?, endValue: GeoPoint?): GeoPoint? {
                return GeoPointInterpolator.interpolate(fraction, requireNotNull(startValue), requireNotNull(endValue))
            }
        }
        val property = Property.of<Marker?, GeoPoint?>(Marker::class.java, GeoPoint::class.java, "position")
        val animator = ObjectAnimator.ofObject<Marker?, GeoPoint?>(marker, property, typeEvaluator, finalPosition)
        animator.setDuration(3000)
        animator.start()
        return animator
    }
}

