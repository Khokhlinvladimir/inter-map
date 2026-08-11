package org.osmdroid.samplefragments.milstd2525

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.math.abs

/**
 * A very simple borrowed from Android's "Finger Page" example, modified to generate polylines that
 * are geopoint bound after finger up.
 * created on 1/13/2017.
 *
 * @author Alex O'Ree
 */
class MilStdCustomPaintingSurface(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var symbol: SimpleSymbol? = null

    fun setSymbol(symbol: SimpleSymbol?) {
        this.symbol = symbol
    }


    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mPath: Path
    private var map: MapView? = null
    private val pts: MutableList<Point?> = ArrayList<Point?>()
    private var mPaint: Paint? = null
    private var mX = 0f
    private var mY = 0f

    init {
        mPath = Path()
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap!!)
    }


    override fun onDraw(canvas: Canvas) {
        mCanvas = Canvas(mBitmap!!)
        mPaint = Paint()
        mPaint!!.setAntiAlias(true)
        mPaint!!.setDither(true)
        mPaint!!.setColor(-0x10000)
        mPaint!!.setStyle(Paint.Style.STROKE)
        mPaint!!.setStrokeJoin(Paint.Join.ROUND)
        mPaint!!.setStrokeCap(Paint.Cap.ROUND)
        mPaint!!.setStrokeWidth(12f)

        canvas.drawPath(mPath, mPaint!!)
    }

    fun init(mapView: MapView?) {
        map = mapView
    }

    private fun touch_start(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun touch_move(x: Float, y: Float) {
        val dx = abs(x - mX)
        val dy = abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
        }
    }

    private fun touch_up() {
        mPath.lineTo(mX, mY)
        // commit the path to our offscreen
        mCanvas!!.drawPath(mPath, mPaint!!)
        // kill this so we don't double draw
        mPath.reset()
        if (map != null) {
            val projection = map!!.projection


            if (symbol != null && symbol!!.minPoints <= pts.size) {
                val inputGeoPoints = ArrayList<GeoPoint?>()
                val unrotatedPoint = Point()
                for (i in pts.indices) {
                    projection.unrotateAndScalePoint(pts.get(i)!!.x, pts.get(i)!!.y, unrotatedPoint)
                    val iGeoPoint = projection.fromPixels(unrotatedPoint.x, unrotatedPoint.y) as GeoPoint?
                    inputGeoPoints.add(iGeoPoint)
                }

                val overlay = MilStdMultipointOverlay(requireNotNull(symbol), inputGeoPoints)
                map!!.getOverlayManager().add(overlay)
                map!!.invalidate()
            }
        }

        pts.clear()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.getX()
        val y = event.getY()
        pts.add(Point(x.toInt(), y.toInt()))
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                touch_move(x, y)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()
            }
        }
        return true
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
    }
}
