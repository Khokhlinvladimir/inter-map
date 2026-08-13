package org.osmdroid.samplefragments.data

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import org.osmdroid.R
import org.osmdroid.data.DataCountry
import org.osmdroid.data.DataCountryLoader
import org.osmdroid.samplefragments.events.SampleMapEventListener
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.milestones.MilestoneDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneLineDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneLister
import org.osmdroid.views.overlay.milestones.MilestoneManager
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceLister
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceSliceLister

/**
 * Demo with the new "non repetitive milestones" feature - a map of all capitals of the EU
 *
 * @author Fabrice Fontaine
 * @since 6.0.3
 */
class SampleMilestonesNonRepetitive : SampleMapEventListener() {
    private var mAnimatedMetersSoFar = 0.0
    private var mAnimationEnded = false
    private var mPercentageCompletion: ValueAnimator? = null
    private val mOrder: Array<String?> = arrayOf<String?>( // arbitrary order
        "FRA", "LUX", "BEL", "NLD",
        "GBR", "IRL",
        "PRT", "ESP",
        "MLT", "ITA", "HRV", "SVN",
        "DEU", "DNK", "SWE", "FIN",
        "EST", "LVA", "LTU", "POL", "CZE", "AUT", "SVK", "HUN",
        "ROU", "BGR", "GRC", "CYP"
    )

    // source https://en.wikipedia.org/wiki/Flag_of_Europe#Colours
    private val COLOR_BLUE = Color.rgb(0, 51, 153)
    private val COLOR_GOLD = Color.rgb(255, 204, 0)
    private val mLineWidth = 6
    private val mDiskRadius = 18

    override val sampleTitle: String
        get() = "Milestones with non repetitive values"

    public override fun addOverlays() {
        super.addOverlays()

        val mList: LinkedHashMap<String?, DataCountry?>
        try {
            mList = DataCountryLoader(requireActivity(), R.raw.data_country).list
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }
        val polyline = Polyline()
        val capitals: MutableList<GeoPoint> = ArrayList(mOrder.size)
        val distances: DoubleArray? = DoubleArray(mOrder.size)
        var distancesIndex = 0
        var distance1 = 0.0
        var previous: GeoPoint? = null
        for (country in mOrder) {
            val capital = GeoPoint(mList.get(country)!!.capitalGeoPoint)
            if (distancesIndex == 0) {
                distance1 = 0.0
            } else {
                distance1 += previous!!.distanceToAsDouble(capital)
            }
            distances!![distancesIndex++] = distance1
            previous = GeoPoint(capital)
            capitals.add(capital)
        }
        val boundingBox = BoundingBox.fromGeoPoints(capitals)
        polyline.setPoints(capitals)
        polyline.getOutlinePaint().setColor(Color.TRANSPARENT)
        val managers: MutableList<MilestoneManager> = ArrayList()
        val slicerForPath = MilestoneMeterDistanceSliceLister()
        managers.add(getAnimatedPathManager(slicerForPath))

        val backgroundPaint = getFillPaint(COLOR_BLUE)
        val starPaint = getFillPaint(COLOR_GOLD)
        managers.add(
            MilestoneManager(
                MilestoneMeterDistanceLister(distances),
                object : MilestoneDisplayer(0.0, false) {
                    private val mPath = Path()

                    override fun draw(pCanvas: Canvas, pParameter: Any?) {
                        val meters = pParameter as Double
                        val checked = meters < mAnimatedMetersSoFar || mAnimationEnded
                        if (!checked) {
                            return
                        }

                        pCanvas.drawCircle(0f, 0f, mDiskRadius.toFloat(), backgroundPaint)

                        // drawing a star
                        // inspired by https://stackoverflow.com/questions/7007429/android-how-to-draw-triangle-star-square-heart-on-the-canvas
                        mPath.reset()
                        // top left
                        mPath.moveTo(mDiskRadius * -.5f, mDiskRadius * -.16f)
                        // top right
                        mPath.lineTo(mDiskRadius * .5f, mDiskRadius * -.16f)
                        // bottom left
                        mPath.lineTo(mDiskRadius * -.32f, mDiskRadius * .45f)
                        // top tip
                        mPath.lineTo(0f, mDiskRadius * -.5f)
                        // bottom right
                        mPath.lineTo(mDiskRadius * .32f, mDiskRadius * .45f)
                        mPath.close()
                        pCanvas.drawPath(mPath, starPaint)
                    }
                }
            ))

        polyline.setMilestoneManagers(managers)

        mMapView!!.getOverlayManager().add(polyline)
        val distance = polyline.getDistance().toFloat()
        val fraction = 1f / 10 // fraction of the polyline to be displayed
        val percentageCompletion = ValueAnimator.ofFloat(0f, distance)
        mPercentageCompletion = percentageCompletion
        percentageCompletion.setDuration(5000) // 5 seconds
        percentageCompletion.setStartDelay(500) // .5 second
        percentageCompletion.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                mAnimatedMetersSoFar = (animation.getAnimatedValue() as Float).toDouble()
                if (mAnimatedMetersSoFar < distance * fraction) {
                    slicerForPath.setMeterDistanceSlice(0.0, mAnimatedMetersSoFar)
                } else if (mAnimatedMetersSoFar > distance * (1 - fraction)) {
                    slicerForPath.setMeterDistanceSlice(mAnimatedMetersSoFar - (distance - mAnimatedMetersSoFar), mAnimatedMetersSoFar)
                } else {
                    slicerForPath.setMeterDistanceSlice(mAnimatedMetersSoFar - distance * fraction, mAnimatedMetersSoFar)
                }
                mMapView?.invalidate()
            }
        })
        percentageCompletion.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mAnimationEnded = true
                mMapView?.invalidate()
            }
        })
        percentageCompletion.start()

        mMapView!!.post(object : Runnable {
            override fun run() {
                mMapView!!.zoomToBoundingBox(boundingBox, false, 30)
            }
        })
    }

    override fun onDestroyView() {
        mPercentageCompletion?.cancel()
        mPercentageCompletion = null
        super.onDestroyView()
    }

    private fun getAnimatedPathManager(pMilestoneLister: MilestoneLister): MilestoneManager {
        val paint = Paint()
        paint.setStrokeWidth(mLineWidth.toFloat())
        paint.setStyle(Paint.Style.STROKE)
        paint.setAntiAlias(true)
        paint.setColor(COLOR_GOLD)
        paint.setStrokeCap(Paint.Cap.ROUND)
        return MilestoneManager(pMilestoneLister, MilestoneLineDisplayer(paint))
    }

    private fun getFillPaint(pColor: Int): Paint {
        val paint = Paint()
        paint.setColor(pColor)
        paint.setStyle(Paint.Style.FILL_AND_STROKE)
        return paint
    }
}
