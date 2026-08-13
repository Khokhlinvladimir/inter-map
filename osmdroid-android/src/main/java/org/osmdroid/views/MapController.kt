package org.osmdroid.views

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.TargetApi
import android.graphics.Point
import android.os.Build
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import org.osmdroid.api.IGeoPoint
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration.instance
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MyMath
import org.osmdroid.views.MapView.Companion.getTileSystem
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.pow

open class MapController(mapView: MapView) : IMapController, OnFirstLayoutListener {
    protected val mMapView: MapView

    // Zoom animations
    private var mZoomInAnimationOld: ScaleAnimation? = null
    private var mZoomOutAnimationOld: ScaleAnimation? = null
    private var mTargetZoomLevel = 0.0

    private var mCurrentAnimator: Animator? = null
    private val mInterpolator: TimeInterpolator? = null

    // Keep track of calls before initial layout
    private val mReplayController: ReplayController

    init {
        mMapView = mapView

        // Keep track of initial layout
        mReplayController = ReplayController()
        if (!mMapView.isLayoutOccurred()) {
            mMapView.addOnFirstLayoutListener(this)
        }


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            val zoomAnimationListener = ZoomAnimationListener(this)
            mZoomInAnimationOld = ScaleAnimation(
                1f, 2f, 1f, 2f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            mZoomOutAnimationOld = ScaleAnimation(
                1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f
            )
            mZoomInAnimationOld!!.setDuration(instance!!.animationSpeedShort.toLong())
            mZoomOutAnimationOld!!.setDuration(instance!!.animationSpeedShort.toLong())
            mZoomInAnimationOld!!.setAnimationListener(zoomAnimationListener)
            mZoomOutAnimationOld!!.setAnimationListener(zoomAnimationListener)
        }
    }

    override fun onFirstLayout(v: View?, left: Int, top: Int, right: Int, bottom: Int) {
        mReplayController.replayCalls()
    }

    override fun zoomToSpan(latSpan: Double, lonSpan: Double) {
        if (latSpan <= 0 || lonSpan <= 0) {
            return
        }

        // If no layout, delay this call
        if (!mMapView.isLayoutOccurred()) {
            mReplayController.zoomToSpan(latSpan, lonSpan)
            return
        }

        val bb = this.mMapView.projection.boundingBox
        val curZoomLevel = this.mMapView.projection.zoomLevel

        val curLatSpan = bb.latitudeSpan
        val curLonSpan = bb.longitudeSpan

        val diffNeededLat = latSpan / curLatSpan // i.e. 600/500 = 1,2
        val diffNeededLon = lonSpan / curLonSpan // i.e. 300/400 = 0,75

        val diffNeeded = max(diffNeededLat, diffNeededLon) // i.e. 1,2

        if (diffNeeded > 1) { // Zoom Out
            this.mMapView.setZoomLevel(curZoomLevel - MyMath.getNextSquareNumberAbove(diffNeeded.toFloat()))
        } else if (diffNeeded < 0.5) { // Can Zoom in
            this.mMapView.setZoomLevel(
                curZoomLevel
                        + MyMath.getNextSquareNumberAbove(1 / diffNeeded.toFloat()) - 1
            )
        }
    }

    // TODO rework zoomToSpan
    override fun zoomToSpan(latSpanE6: Int, lonSpanE6: Int) {
        zoomToSpan(latSpanE6 * 1E-6, lonSpanE6 * 1E-6)
    }

    /**
     * Start animating the map towards the given point.
     */
    override fun animateTo(point: IGeoPoint?) {
        animateTo(point, null, null)
    }

    /**
     * @since 6.0.3
     */
    override fun animateTo(point: IGeoPoint?, pZoom: Double?, pSpeed: Long?, pOrientation: Float?) {
        animateTo(point, pZoom, pSpeed, pOrientation, null)
    }

    /**
     * @since 6.1.0
     */
    override fun animateTo(point: IGeoPoint?, pZoom: Double?, pSpeed: Long?, pOrientation: Float?, pClockwise: Boolean?) {
        // If no layout, delay this call
        if (!mMapView.isLayoutOccurred()) {
            mReplayController.animateTo(point, pZoom, pSpeed, pOrientation, pClockwise)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            val currentCenter: IGeoPoint = GeoPoint(mMapView.projection.currentCenter)
            val mapAnimatorListener =
                MapAnimatorListener(
                    this,
                    mMapView.zoomLevelDouble, pZoom,
                    currentCenter, point,
                    mMapView.getMapOrientation(), pOrientation, pClockwise
                )
            val mapAnimator = ValueAnimator.ofFloat(0f, 1f)
            mapAnimator.addListener(mapAnimatorListener)
            mapAnimator.addUpdateListener(mapAnimatorListener)
            if (pSpeed == null) {
                mapAnimator.setDuration(instance!!.animationSpeedDefault.toLong())
            } else {
                mapAnimator.setDuration(pSpeed)
            }

            mCurrentAnimator?.let(mapAnimatorListener::onAnimationCancel)
            mapAnimator.setInterpolator(mInterpolator)
            mCurrentAnimator = mapAnimator
            mapAnimator.start()
            return
        }
        // TODO handle the zoom and orientation parts for the .3% of the population below HONEYCOMB (Feb. 2018)
        val p = mMapView.projection.toPixels(point, null)
        animateTo(p!!.x, p.y)
    }

    /**
     * @since 6.0.2
     */
    override fun animateTo(pPoint: IGeoPoint?, pZoom: Double?, pSpeed: Long?) {
        animateTo(pPoint, pZoom, pSpeed, null)
    }

    /**
     * Start animating the map towards the given point.
     */
    override fun animateTo(x: Int, y: Int) {
        // If no layout, delay this call
        if (!mMapView.isLayoutOccurred()) {
            mReplayController.animateTo(x, y)
            return
        }

        if (!mMapView.isAnimating()) {
            mMapView.mIsFlinging = false
            val xStart = mMapView.getMapScrollX().toInt()
            val yStart = mMapView.getMapScrollY().toInt()

            val dx = x - mMapView.getWidth() / 2
            val dy = y - mMapView.getHeight() / 2

            if (dx != xStart || dy != yStart) {
                mMapView.getScroller()!!.startScroll(xStart, yStart, dx, dy, instance!!.animationSpeedDefault)
                mMapView.postInvalidate()
            }
        }
    }

    override fun scrollBy(x: Int, y: Int) {
        this.mMapView.scrollBy(x, y)
    }

    /**
     * Set the map view to the given center. There will be no animation.
     */
    override fun setCenter(point: IGeoPoint?) {
        // If no layout, delay this call
        if (!mMapView.isLayoutOccurred()) {
            mReplayController.setCenter(point)
            return
        }
        if (point != null) {
            mMapView.setExpectedCenter(point)
        }
    }

    override fun stopPanning() {
        mMapView.mIsFlinging = false
        mMapView.getScroller()!!.forceFinished(true)
    }

    /**
     * Stops a running animation.
     *
     * @param jumpToTarget
     */
    override fun stopAnimation(jumpToTarget: Boolean) {
        if (!mMapView.getScroller()!!.isFinished()) {
            if (jumpToTarget) {
                mMapView.mIsFlinging = false
                mMapView.getScroller()!!.abortAnimation()
            } else stopPanning()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            val currentAnimator = this.mCurrentAnimator
            if (mMapView.mIsAnimating.get()) {
                if (jumpToTarget) {
                    currentAnimator!!.end()
                } else {
                    currentAnimator!!.cancel()
                }
            }
        } else {
            if (mMapView.mIsAnimating.get()) {
                mMapView.clearAnimation()
            }
        }
    }

    override fun setZoom(zoomlevel: Int): Int {
        return setZoom(zoomlevel.toDouble()).toInt()
    }

    /**
     * @since 6.0
     */
    override fun setZoom(pZoomlevel: Double): Double {
        return mMapView.setZoomLevel(pZoomlevel)
    }

    /**
     * Zoom in by one zoom level.
     */
    override fun zoomIn(): Boolean {
        return zoomIn(null)
    }

    override fun zoomIn(animationSpeed: Long?): Boolean {
        return zoomTo(mMapView.zoomLevelDouble + 1, animationSpeed)
    }

    /**
     * @param xPixel
     * @param yPixel
     * @param zoomAnimation if null, the default is used
     * @return
     */
    override fun zoomInFixing(xPixel: Int, yPixel: Int, zoomAnimation: Long?): Boolean {
        return zoomToFixing(mMapView.zoomLevelDouble + 1, xPixel, yPixel, zoomAnimation)
    }

    override fun zoomInFixing(xPixel: Int, yPixel: Int): Boolean {
        return zoomInFixing(xPixel, yPixel, null)
    }

    override fun zoomOut(animationSpeed: Long?): Boolean {
        return zoomTo(mMapView.zoomLevelDouble - 1, animationSpeed)
    }

    /**
     * Zoom out by one zoom level.
     */
    override fun zoomOut(): Boolean {
        return zoomOut(null)
    }

    @Deprecated("")
    override fun zoomOutFixing(xPixel: Int, yPixel: Int): Boolean {
        return zoomToFixing(mMapView.zoomLevelDouble - 1, xPixel, yPixel, null)
    }

    override fun zoomTo(zoomLevel: Int): Boolean {
        return zoomTo(zoomLevel, null)
    }

    /**
     * @since 6.0
     */
    override fun zoomTo(zoomLevel: Int, animationSpeed: Long?): Boolean {
        return zoomTo(zoomLevel.toDouble(), animationSpeed)
    }

    /**
     * @param zoomLevel
     * @param xPixel
     * @param yPixel
     * @param zoomAnimationSpeed time in milliseconds, if null, the default settings will be used
     * @return
     * @since 6.0.0
     */
    override fun zoomToFixing(zoomLevel: Int, xPixel: Int, yPixel: Int, zoomAnimationSpeed: Long?): Boolean {
        return zoomToFixing(zoomLevel.toDouble(), xPixel, yPixel, zoomAnimationSpeed)
    }

    override fun zoomTo(pZoomLevel: Double, animationSpeed: Long?): Boolean {
        return zoomToFixing(pZoomLevel, mMapView.getWidth() / 2, mMapView.getHeight() / 2, animationSpeed)
    }

    override fun zoomTo(pZoomLevel: Double): Boolean {
        return zoomTo(pZoomLevel, null)
    }


    override fun zoomToFixing(zoomLevel: Double, xPixel: Int, yPixel: Int, zoomAnimationSpeed: Long?): Boolean {
        var zoomLevel = zoomLevel
        zoomLevel = if (zoomLevel > mMapView.maxZoomLevel) mMapView.maxZoomLevel else zoomLevel
        zoomLevel = if (zoomLevel < mMapView.getMinZoomLevel()) mMapView.getMinZoomLevel() else zoomLevel

        val currentZoomLevel = mMapView.zoomLevelDouble
        val canZoom = zoomLevel < currentZoomLevel && mMapView.canZoomOut() ||
                zoomLevel > currentZoomLevel && mMapView.canZoomIn()

        if (!canZoom) {
            return false
        }
        if (mMapView.mIsAnimating.getAndSet(true)) {
            // TODO extend zoom (and return true)
            return false
        }
        var event: ZoomEvent? = null
        for (mapListener in mMapView.mListners) {
            mapListener.onZoom(if (event != null) event else (ZoomEvent(mMapView, zoomLevel).also { event = it }))
        }
        mMapView.setMultiTouchScaleInitPoint(xPixel.toFloat(), yPixel.toFloat())
        mMapView.startAnimation()

        val end = 2.0.pow(zoomLevel - currentZoomLevel).toFloat()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            val zoomAnimatorListener = MapAnimatorListener(
                this,
                currentZoomLevel, zoomLevel,
                null, null,
                null, null, null
            )
            val zoomToAnimator = ValueAnimator.ofFloat(0f, 1f)
            zoomToAnimator.addListener(zoomAnimatorListener)
            zoomToAnimator.addUpdateListener(zoomAnimatorListener)
            if (zoomAnimationSpeed == null) {
                zoomToAnimator.setDuration(instance!!.animationSpeedShort.toLong())
            } else {
                zoomToAnimator.setDuration(zoomAnimationSpeed)
            }
            zoomToAnimator.setInterpolator(mInterpolator)
            mCurrentAnimator = zoomToAnimator

            zoomToAnimator.start()
            return true
        }
        mTargetZoomLevel = zoomLevel
        if (zoomLevel > currentZoomLevel) mMapView.startAnimation(mZoomInAnimationOld)
        else mMapView.startAnimation(mZoomOutAnimationOld)
        val scaleAnimation: ScaleAnimation?

        scaleAnimation = ScaleAnimation(
            1f, end,  //X
            1f, end,  //Y
            Animation.RELATIVE_TO_SELF, 0.5f,  //Pivot X
            Animation.RELATIVE_TO_SELF, 0.5f
        ) //Pivot Y
        if (zoomAnimationSpeed == null) {
            scaleAnimation.setDuration(instance!!.animationSpeedShort.toLong())
        } else {
            scaleAnimation.setDuration(zoomAnimationSpeed)
        }
        scaleAnimation.setAnimationListener(ZoomAnimationListener(this))
        return true
    }

    /**
     * @since 6.0
     */
    override fun zoomToFixing(zoomLevel: Double, xPixel: Int, yPixel: Int): Boolean {
        return zoomToFixing(zoomLevel, xPixel, yPixel, null)
    }

    override fun zoomToFixing(zoomLevel: Int, xPixel: Int, yPixel: Int): Boolean {
        return zoomToFixing(zoomLevel, xPixel, yPixel, null)
    }


    protected fun onAnimationStart() {
        mMapView.mIsAnimating.set(true)
    }

    protected fun onAnimationEnd() {
        mMapView.mIsAnimating.set(false)
        mMapView.resetMultiTouchScale()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mCurrentAnimator = null
        } else { // Fix for issue 477
            mMapView.clearAnimation()
            mZoomInAnimationOld!!.reset()
            mZoomOutAnimationOld!!.reset()
            setZoom(mTargetZoomLevel)
        }
        mMapView.invalidate()
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private class MapAnimatorListener
        (
        pMapController: MapController,
        pZoomStart: Double, pZoomEnd: Double?,
        pCenterStart: IGeoPoint?, pCenterEnd: IGeoPoint?,
        pOrientationStart: Float?, pOrientationEnd: Float?,
        pClockwise: Boolean?
    ) : Animator.AnimatorListener, AnimatorUpdateListener {
        private val mCenter = GeoPoint(0.0, 0.0)
        private val mMapController: MapController
        private val mZoomStart: Double
        private val mZoomEnd: Double?
        private val mCenterStart: IGeoPoint?
        private val mCenterEnd: IGeoPoint?
        private val mOrientationStart: Float?
        private val mOrientationSpan: Float?

        init {
            mMapController = pMapController
            mZoomStart = pZoomStart
            mZoomEnd = pZoomEnd
            mCenterStart = pCenterStart
            mCenterEnd = pCenterEnd
            if (pOrientationEnd == null) {
                mOrientationStart = null
                mOrientationSpan = null
            } else {
                mOrientationStart = pOrientationStart
                mOrientationSpan = MyMath.getAngleDifference(mOrientationStart!!.toDouble(), pOrientationEnd.toDouble(), pClockwise).toFloat()
            }
        }

        override fun onAnimationStart(animator: Animator) {
            mMapController.onAnimationStart()
        }

        override fun onAnimationEnd(animator: Animator) {
            mMapController.onAnimationEnd()
        }

        override fun onAnimationCancel(animator: Animator) {
            mMapController.onAnimationEnd()
        }

        override fun onAnimationRepeat(animator: Animator) {
            //noOp
        }

        override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
            val value = valueAnimator.getAnimatedValue() as Float
            if (mZoomEnd != null) {
                val zoom = mZoomStart + (mZoomEnd - mZoomStart) * value
                //map events listeners are triggered by this call
                mMapController.mMapView.setZoomLevel(zoom)
            }
            if (mOrientationSpan != null) {
                val orientation = mOrientationStart!! + mOrientationSpan * value
                //map events listeners are triggered by this call
                mMapController.mMapView.setMapOrientation(orientation)
            }
            if (mCenterEnd != null) {
                val tileSystem = getTileSystem()
                val longitudeStart = tileSystem.cleanLongitude(mCenterStart!!.longitude)
                val longitudeEnd = tileSystem.cleanLongitude(mCenterEnd.longitude)
                val longitude = tileSystem.cleanLongitude(longitudeStart + (longitudeEnd - longitudeStart) * value)
                val latitudeStart = tileSystem.cleanLatitude(mCenterStart.latitude)
                val latitudeEnd = tileSystem.cleanLatitude(mCenterEnd.latitude)
                val latitude = tileSystem.cleanLatitude(latitudeStart + (latitudeEnd - latitudeStart) * value)
                mCenter.setCoords(latitude, longitude)
                mMapController.mMapView.setExpectedCenter(mCenter)
            }
            mMapController.mMapView.invalidate()
        }
    }

    protected class ZoomAnimationListener(mapController: MapController) : Animation.AnimationListener {
        private val mMapController: MapController

        init {
            mMapController = mapController
        }

        override fun onAnimationStart(animation: Animation?) {
            mMapController.onAnimationStart()
        }

        override fun onAnimationEnd(animation: Animation?) {
            mMapController.onAnimationEnd()
        }

        override fun onAnimationRepeat(animation: Animation?) {
            //noOp
        }
    }

    private enum class ReplayType {
        ZoomToSpanPoint, AnimateToPoint, AnimateToGeoPoint, SetCenterPoint
    }

    private inner class ReplayController {
        private val mReplayList: LinkedList<ReplayClass> = LinkedList<ReplayClass>()

        fun animateTo(
            geoPoint: IGeoPoint?,
            pZoom: Double?, pSpeed: Long?, pOrientation: Float?, pClockwise: Boolean?
        ) {
            mReplayList.add(
                ReplayClass(
                    ReplayType.AnimateToGeoPoint, null, geoPoint,
                    pZoom, pSpeed, pOrientation, pClockwise
                )
            )
        }

        fun animateTo(x: Int, y: Int) {
            mReplayList.add(ReplayClass(ReplayType.AnimateToPoint, Point(x, y), null))
        }

        fun setCenter(geoPoint: IGeoPoint?) {
            mReplayList.add(ReplayClass(ReplayType.SetCenterPoint, null, geoPoint))
        }

        fun zoomToSpan(x: Int, y: Int) {
            mReplayList.add(ReplayClass(ReplayType.ZoomToSpanPoint, Point(x, y), null))
        }

        fun zoomToSpan(x: Double, y: Double) {
            mReplayList.add(ReplayClass(ReplayType.ZoomToSpanPoint, Point((x * 1E6).toInt(), (y * 1E6).toInt()), null))
        }


        fun replayCalls() {
            for (replay in mReplayList) {
                when (replay.mReplayType) {
                    ReplayType.AnimateToGeoPoint -> if (replay.mGeoPoint != null) this@MapController.animateTo(
                        replay.mGeoPoint,
                        replay.mZoom,
                        replay.mSpeed,
                        replay.mOrientation,
                        replay.mClockwise
                    )

                    ReplayType.AnimateToPoint -> if (replay.mPoint != null) this@MapController.animateTo(replay.mPoint.x, replay.mPoint.y)
                    ReplayType.SetCenterPoint -> if (replay.mGeoPoint != null) this@MapController.setCenter(replay.mGeoPoint)
                    ReplayType.ZoomToSpanPoint -> if (replay.mPoint != null) this@MapController.zoomToSpan(replay.mPoint.x, replay.mPoint.y)
                }
            }
            mReplayList.clear()
        }

        private inner class ReplayClass(
            pReplayType: ReplayType, pPoint: Point?, pGeoPoint: IGeoPoint?,
            pZoom: Double?, pSpeed: Long?, pOrientation: Float?, pClockwise: Boolean?
        ) {
            val mReplayType: ReplayType
            val mPoint: Point?
            val mGeoPoint: IGeoPoint?
            val mSpeed: Long?
            val mZoom: Double?
            val mOrientation: Float?
            val mClockwise: Boolean?

            constructor(mReplayType: ReplayType, mPoint: Point?, mGeoPoint: IGeoPoint?) : this(mReplayType, mPoint, mGeoPoint, null, null, null, null)

            /**
             * @since 6.0.2
             */
            init {
                mReplayType = pReplayType
                mPoint = pPoint
                mGeoPoint = pGeoPoint
                mSpeed = pSpeed
                mZoom = pZoom
                mOrientation = pOrientation
                mClockwise = pClockwise
            }
        }
    }
}
