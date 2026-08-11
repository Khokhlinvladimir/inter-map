package org.osmdroid.samplefragments.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import org.osmdroid.library.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.milestones.MilestoneBitmapDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneManager
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer
import org.osmdroid.views.overlay.milestones.MilestonePixelDistanceLister
import kotlin.math.abs

/**
 * A very simple borrowed from Android's "Finger Page" example, modified to generate polylines that
 * are geopoint bound after finger up.
 * created on 1/13/2017.
 *
 * @author Alex O'Ree
 */
class CustomPaintingSurface(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    fun setMode(mode: Mode) {
        this.drawingMode = mode
    }

    private var drawingMode = Mode.Polyline

    enum class Mode {
        Polyline,
        Polygon,
        PolygonHole,
        PolylineAsPath
    }

    @JvmField
    var withArrows: Boolean = false
    private var mCanvas: Canvas? = null
    private val mPath: Path
    private var map: MapView? = null
    private val pts: MutableList<Point?> = ArrayList<Point?>()
    private val mPaint: Paint
    private var mX = 0f
    private var mY = 0f

    @Transient
    var lastPolygon: Polygon? = null


    init {
        mPath = Path()
        mPaint = Paint()
        mPaint.setAntiAlias(true)
        mPaint.setDither(true)
        mPaint.setColor(-0x10000)
        mPaint.setStyle(Paint.Style.STROKE)
        mPaint.setStrokeJoin(Paint.Join.ROUND)
        mPaint.setStrokeCap(Paint.Cap.ROUND)
        mPaint.setStrokeWidth(12f)
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(bitmap)
    }


    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(mPath, mPaint)
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
        mCanvas!!.drawPath(mPath, mPaint)
        // kill this so we don't double draw
        mPath.reset()
        if (map != null) {
            val mapView = map!!
            val projection = mapView.projection
            val geoPoints = ArrayList<GeoPoint>()
            val unrotatedPoint = Point()
            for (i in pts.indices) {
                projection.unrotateAndScalePoint(pts.get(i)!!.x, pts.get(i)!!.y, unrotatedPoint)
                val iGeoPoint = projection.fromPixels(unrotatedPoint.x, unrotatedPoint.y) as GeoPoint
                geoPoints.add(iGeoPoint)
            }

            if (geoPoints.size > 2) {
                //only plot a line unless there's at least one item
                when (drawingMode) {
                    Mode.Polyline, Mode.PolylineAsPath -> {
                        val asPath = drawingMode == Mode.PolylineAsPath
                        val color = Color.argb(100, 100, 100, 100)
                        val line = Polyline(mapView)
                        line.usePath(true)
                        line.setInfoWindow(
                            BasicInfoWindow(R.layout.bonuspack_bubble, mapView)
                        )
                        line.getOutlinePaint().setColor(color)
                        line.setTitle("This is a polyline" + (if (asPath) " as Path" else ""))
                        line.setPoints(geoPoints)
                        line.showInfoWindow()
                        line.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND)

                        //example below
                        /*
                        line.setOnClickListener(new Polyline.OnClickListener() {
                            @Override
                            public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                                Toast.makeText(mapView.getContext(), "polyline with " + polyline.getPoints().size() + "pts was tapped", Toast.LENGTH_LONG).show();
                                return false;
                            }
                        });
                        */
                        if (withArrows) {
                            val arrowPaint = Paint()
                            arrowPaint.setColor(color)
                            arrowPaint.setStrokeWidth(10.0f)
                            arrowPaint.setStyle(Paint.Style.FILL_AND_STROKE)
                            arrowPaint.setAntiAlias(true)
                            val arrowPath = Path() // a simple arrow towards the right
                            arrowPath.moveTo(-10f, -10f)
                            arrowPath.lineTo(10f, 0f)
                            arrowPath.lineTo(-10f, 10f)
                            arrowPath.close()
                            val managers: MutableList<MilestoneManager> = ArrayList()
                            managers.add(
                                MilestoneManager(
                                    MilestonePixelDistanceLister(50.0, 50.0),
                                    MilestonePathDisplayer(0.0, true, arrowPath, arrowPaint)
                                )
                            )
                            line.setMilestoneManagers(managers)
                        }
                        line.setSubDescription(line.getBounds().toString())
                        mapView.getOverlayManager().add(line)
                        lastPolygon = null
                    }

                    Mode.Polygon -> {
                        val polygon = Polygon(mapView)
                        polygon.setInfoWindow(
                            BasicInfoWindow(R.layout.bonuspack_bubble, mapView)
                        )
                        polygon.getFillPaint()!!.setColor(Color.argb(75, 255, 0, 0))
                        polygon.setPoints(geoPoints)
                        polygon.setTitle("A sample polygon")
                        polygon.showInfoWindow()
                        if (withArrows) {
                            val bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.round_navigation_white_48)
                            val managers: MutableList<MilestoneManager> = ArrayList()
                            managers.add(
                                MilestoneManager(
                                    MilestonePixelDistanceLister(20.0, 200.0),
                                    MilestoneBitmapDisplayer(90.0, true, bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2)
                                )
                            )
                            polygon.setMilestoneManagers(managers)
                        }
                        polygon.setOnClickListener(object : Polygon.OnClickListener {
                            override fun onClick(polygon: Polygon?, mapView: MapView?, eventPos: GeoPoint?): Boolean {
                                polygon ?: return false
                                lastPolygon = polygon
                                polygon.onClickDefault(polygon, mapView, eventPos)
                                Toast.makeText(
                                    mapView!!.getContext(),
                                    "polygon with " + polygon.getActualPoints().size + "pts was tapped",
                                    Toast.LENGTH_LONG
                                ).show()
                                return false
                            }
                        })
                        //polygon.setSubDescription(BoundingBox.fromGeoPoints(polygon.getPoints()).toString());
                        mapView.getOverlayManager().add(polygon)
                        lastPolygon = polygon
                    }

                    Mode.PolygonHole -> if (lastPolygon != null) {
                        val holes: MutableList<MutableList<GeoPoint>> = ArrayList()
                        holes.add(geoPoints)
                        lastPolygon!!.setHoles(holes)
                    }
                }

                mapView.invalidate()
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

    fun destroy() {
        map = null
        this.lastPolygon = null
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
    }
}
