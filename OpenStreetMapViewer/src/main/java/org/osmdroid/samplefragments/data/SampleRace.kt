package org.osmdroid.samplefragments.data

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import org.osmdroid.library.R
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.milestones.MilestoneBitmapDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneLineDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneLister
import org.osmdroid.views.overlay.milestones.MilestoneManager
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceLister
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceSliceLister
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneVertexLister

/**
 * Created by Fabrice on 28/12/2017.
 *
 * @since 6.0.0
 */
class SampleRace : BaseSampleFragment() {
    override val sampleTitle: String
        get() = SAMPLE_TITLE
    private var mAnimatedMetersSoFar = 0.0
    private var mAnimationEnded = false

    /**
     * @since 6.0.3
     */
    private val mGeoPoints: MutableList<GeoPoint?> = this.geoPoints

    public override fun onActivityCreated(savedInstanceState: Bundle?) {
        mMapView!!.post(object : Runnable {
            override fun run() {
                val boundingBox = BoundingBox.fromGeoPoints(mGeoPoints)
                mMapView!!.zoomToBoundingBox(boundingBox, false, 30)
            }
        })

        super.onActivityCreated(savedInstanceState)
    }

    override fun addOverlays() {
        super.addOverlays()

        val line = Polyline(mMapView)
        line.getOutlinePaint().setColor(COLOR_POLYLINE_STATIC)
        line.getOutlinePaint().setStrokeWidth(LINE_WIDTH_BIG)
        line.setPoints(mGeoPoints)
        line.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND)
        val managers: MutableList<MilestoneManager?> = ArrayList<MilestoneManager?>()
        val slicerForPath = MilestoneMeterDistanceSliceLister()
        val bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.next)
        val slicerForIcon = MilestoneMeterDistanceSliceLister()
        managers.add(getAnimatedPathManager(slicerForPath))
        managers.add(getAnimatedIconManager(slicerForIcon, bitmap))
        managers.add(this.halfKilometerManager)
        managers.add(this.kilometerManager)
        managers.add(getStartManager(bitmap))
        line.setMilestoneManagers(managers)
        mMapView!!.getOverlayManager().add(line)
        val percentageCompletion = ValueAnimator.ofFloat(0f, 10000f) // 10 kilometers
        percentageCompletion.setDuration(5000) // 5 seconds
        percentageCompletion.setStartDelay(1000) // 1 second
        percentageCompletion.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                mAnimatedMetersSoFar = (animation.getAnimatedValue() as Float).toDouble()
                slicerForPath.setMeterDistanceSlice(0.0, mAnimatedMetersSoFar)
                slicerForIcon.setMeterDistanceSlice(mAnimatedMetersSoFar, mAnimatedMetersSoFar)
                mMapView!!.invalidate()
            }
        })
        percentageCompletion.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mAnimationEnded = true
            }
        })
        percentageCompletion.start()
    }

    /**
     * @since 6.0.2
     */
    private fun getFillPaint(pColor: Int): Paint {
        val paint = Paint()
        paint.setColor(pColor)
        paint.setStyle(Paint.Style.FILL_AND_STROKE)
        return paint
    }

    /**
     * @since 6.0.2
     */
    private fun getStrokePaint(pColor: Int, pWidth: Float): Paint {
        val paint = Paint()
        paint.setStrokeWidth(pWidth)
        paint.setStyle(Paint.Style.STROKE)
        paint.setAntiAlias(true)
        paint.setColor(pColor)
        paint.setStrokeCap(Paint.Cap.ROUND)
        return paint
    }

    /**
     * @since 6.0.2
     */
    private fun getTextPaint(pColor: Int): Paint {
        val paint = Paint()
        paint.setColor(pColor)
        paint.setTextSize(TEXT_SIZE)
        paint.setAntiAlias(true)
        return paint
    }

    private val kilometerManager: MilestoneManager
        /**
         * Kilometer milestones
         *
         * @since 6.0.2
         */
        get() {
            val backgroundRadius = 20f
            val backgroundPaint1 = getFillPaint(COLOR_BACKGROUND)
            val backgroundPaint2 = getFillPaint(COLOR_POLYLINE_ANIMATED)
            val textPaint1 = getTextPaint(COLOR_POLYLINE_STATIC)
            val textPaint2 = getTextPaint(COLOR_BACKGROUND)
            val borderPaint = getStrokePaint(COLOR_BACKGROUND, 2f)
            return MilestoneManager(
                MilestoneMeterDistanceLister(1000.0),
                object : MilestoneDisplayer(0.0, false) {
                    override fun draw(pCanvas: Canvas, pParameter: Any?) {
                        val meters = pParameter as Double
                        val kilometers = Math.round(meters / 1000).toInt()
                        val checked = meters < mAnimatedMetersSoFar || (kilometers == 10 && mAnimationEnded)
                        val textPaint = if (checked) textPaint2 else textPaint1
                        val backgroundPaint = if (checked) backgroundPaint2 else backgroundPaint1
                        val text = "" + kilometers + "K"
                        val rect = Rect()
                        textPaint1.getTextBounds(text, 0, text.length, rect)
                        pCanvas.drawCircle(0f, 0f, backgroundRadius, backgroundPaint)
                        pCanvas.drawText(text, (-rect.left - rect.width() / 2).toFloat(), (rect.height() / 2 - rect.bottom).toFloat(), textPaint)
                        pCanvas.drawCircle(0f, 0f, backgroundRadius + 1, borderPaint)
                    }
                }
            )
        }

    private val halfKilometerManager: MilestoneManager
        /**
         * Half-kilometer milestones
         *
         * @since 6.0.2
         */
        get() {
            val arrowPath = Path() // a simple arrow towards the right
            arrowPath.moveTo(-5f, -5f)
            arrowPath.lineTo(5f, 0f)
            arrowPath.lineTo(-5f, 5f)
            arrowPath.close()
            val backgroundPaint = getFillPaint(COLOR_BACKGROUND)
            return MilestoneManager( // display an arrow at 500m every 1km
                MilestoneMeterDistanceLister(500.0),
                object : MilestonePathDisplayer(0.0, true, arrowPath, backgroundPaint) {
                    override fun draw(pCanvas: Canvas, pParameter: Any?) {
                        val halfKilometers = Math.round((pParameter as Double / 500)).toInt()
                        if (halfKilometers % 2 == 0) {
                            return
                        }
                        super.draw(pCanvas, pParameter)
                    }
                }
            )
        }

    /**
     * Animated path
     *
     * @since 6.0.2
     */
    private fun getAnimatedPathManager(pMilestoneLister: MilestoneLister): MilestoneManager {
        val slicePaint = getStrokePaint(COLOR_POLYLINE_ANIMATED, LINE_WIDTH_BIG)
        return MilestoneManager(pMilestoneLister, MilestoneLineDisplayer(slicePaint))
    }

    /**
     * Animated icon
     *
     * @since 6.0.2
     */
    private fun getAnimatedIconManager(
        pMilestoneLister: MilestoneLister,
        pBitmap: Bitmap
    ): MilestoneManager {
        return MilestoneManager(
            pMilestoneLister,
            MilestoneBitmapDisplayer(
                0.0, true, pBitmap,
                pBitmap.getWidth() / 2, pBitmap.getHeight() / 2
            )
        )
    }

    /**
     * Starting point
     *
     * @since 6.0.2
     */
    private fun getStartManager(pBitmap: Bitmap): MilestoneManager {
        return MilestoneManager(
            MilestoneVertexLister(),
            object : MilestoneBitmapDisplayer(
                0.0, true,
                pBitmap, pBitmap.getWidth() / 2, pBitmap.getHeight() / 2
            ) {
                override fun draw(pCanvas: Canvas, pParameter: Any?) {
                    if (0 != pParameter as Int) { // we only draw the start
                        return
                    }
                    super.draw(pCanvas, pParameter)
                }
            }
        )
    }

    private val geoPoints: MutableList<GeoPoint?>
        /**
         * @since 6.0.2
         * TODO get a real list of geo points instead of this lousy manual list
         */
        get() {
            val pts: MutableList<GeoPoint?> = ArrayList<GeoPoint?>()
            pts.add(GeoPoint(48.85546563875735, 2.359844067173981)) // saint paul
            pts.add(GeoPoint(48.85737826660179, 2.351524365470226)) // hôtel de ville
            pts.add(GeoPoint(48.86253652215784, 2.3354870181106264)) // louvre 1
            pts.add(GeoPoint(48.86292409137066, 2.3356209116511195)) // louvre 2
            pts.add(GeoPoint(48.86989982398147, 2.332474413449688)) // opéra loop 1
            pts.add(GeoPoint(48.87019045840439, 2.3327154218225985)) // opéra loop 2
            pts.add(GeoPoint(48.87100070303335, 2.332420856033508)) // opéra loop 3
            pts.add(GeoPoint(48.871987070089496, 2.3330367663197364)) // opéra loop 4
            pts.add(GeoPoint(48.87285012531207, 2.3319923967039813)) // opéra loop 5
            pts.add(GeoPoint(48.87270041271832, 2.33134970770962)) // opéra loop 6
            pts.add(GeoPoint(48.87166121883793, 2.330720408069368)) // opéra loop 7
            pts.add(GeoPoint(48.87096547527885, 2.331885281871564)) // opéra loop 8
            pts.add(GeoPoint(48.87003193074662, 2.3321932370146783)) // opéra loop 9
            pts.add(GeoPoint(48.86989982398147, 2.332474413449688)) // opéra loop 10
            pts.add(GeoPoint(48.864306984328245, 2.3350719481351234)) // rue de l'échelle 1
            pts.add(GeoPoint(48.86316191644713, 2.3338401275626666)) // rue de l'échelle 2
            pts.add(GeoPoint(48.866209500723855, 2.3235169355912433)) // rivoli
            pts.add(GeoPoint(48.866729156977776, 2.3223118937268623)) // concorde
            pts.add(GeoPoint(48.86901910330005, 2.3239721736289027)) // madeleine loop 1
            pts.add(GeoPoint(48.8691952486765, 2.3249897645366104)) // madeleine loop 2
            pts.add(GeoPoint(48.87022568670458, 2.325927019319977)) // madeleine loop 3
            pts.add(GeoPoint(48.870489898165346, 2.32583329384164)) // madeleine loop 4
            pts.add(GeoPoint(48.87073649426996, 2.3250165432446863)) // madeleine loop 5
            pts.add(GeoPoint(48.87075410823092, 2.3247085881016005)) // madeleine loop 6
            pts.add(GeoPoint(48.86957395913612, 2.323570493007452)) // madeleine loop 7
            pts.add(GeoPoint(48.86901910330005, 2.3239721736289027)) // madeleine loop 8
            pts.add(GeoPoint(48.86664988772853, 2.3224457872673554)) // concorde 1
            pts.add(GeoPoint(48.866183077380335, 2.3231420336778967)) // concorde 2
            pts.add(GeoPoint(48.865610568177935, 2.3231688123859726)) // concorde 3
            pts.add(GeoPoint(48.86398108306007, 2.321307692173235)) // concorde 4
            pts.add(GeoPoint(48.863531864319754, 2.3216022579623257)) // concorde 5
            pts.add(GeoPoint(48.86047157217769, 2.3306186871927252)) // pont césaire
            pts.add(GeoPoint(48.859105908108276, 2.336824405441064)) // mitterrand 1
            pts.add(GeoPoint(48.858679130445125, 2.3402407938844476)) // mitterrand 2
            pts.add(GeoPoint(48.85792514768071, 2.342640914879439)) // pont neuf
            pts.add(GeoPoint(48.8563361600739, 2.3489338967683864)) // pont notre dame
            pts.add(GeoPoint(48.85582206974299, 2.3509713700276507)) // pont d'arcole
            pts.add(GeoPoint(48.85403498622509, 2.3547049339593116)) // pont louis philippe
            pts.add(GeoPoint(48.85303073607055, 2.3575358780393856)) // pont marie
            pts.add(GeoPoint(48.852894107137885, 2.358500835434853)) // quai des célestins 1
            pts.add(GeoPoint(48.85275705072659, 2.3589590819111095)) // quai des célestins 2
            pts.add(GeoPoint(48.852639573503986, 2.3594411333991445)) // quai des célestins 3
            pts.add(GeoPoint(48.85244769344759, 2.3598755748636506)) // quai des célestins 4
            pts.add(GeoPoint(48.85215399805951, 2.360375480110463)) // quai des célestins 5
            return pts
        }

    companion object {
        private const val SAMPLE_TITLE: String = "10K race in Paris"

        private const val LINE_WIDTH_BIG = 12f
        private const val TEXT_SIZE = 20f
        private val COLOR_POLYLINE_STATIC = Color.BLUE
        private val COLOR_POLYLINE_ANIMATED = Color.GREEN
        private val COLOR_BACKGROUND = Color.WHITE
    }
}
