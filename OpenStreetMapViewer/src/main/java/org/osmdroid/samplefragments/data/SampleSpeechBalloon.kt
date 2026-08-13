package org.osmdroid.samplefragments.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.samplefragments.BaseSampleFragment
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.SpeechBalloonOverlay

/**
 * Demo around the new [SpeechBalloonOverlay] feature
 * 
 * @author Fabrice Fontaine
 * @since 6.1.1
 */
class SampleSpeechBalloon : BaseSampleFragment() {
    private val mGeoPoints: MutableList<GeoPoint> = ArrayList()
    private val mBackground = Paint()
    private val mForeground = Paint()
    private val mDragBackground = Paint()
    private val mDragForeground = Paint()

    override val sampleTitle: String?
        get() = "Speech Balloon"

    private var mBitmapDrawable: BitmapDrawable? = null

    public override fun addOverlays() {
        super.addOverlays()

        val radius = 10
        val bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.setStyle(Paint.Style.FILL)
        paint.setColor(Color.BLUE)
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), paint)
        mBitmapDrawable = BitmapDrawable(bitmap)

        mBackground.setStyle(Paint.Style.FILL)
        mBackground.setColor(Color.WHITE)
        mForeground.setStyle(Paint.Style.STROKE)
        mForeground.setColor(Color.BLACK)
        mForeground.setTextSize(30f)
        mForeground.setAntiAlias(true)
        mDragBackground.setStyle(Paint.Style.FILL)
        mDragBackground.setColor(Color.YELLOW)
        mDragForeground.setStyle(Paint.Style.STROKE)
        mDragForeground.setColor(Color.RED)
        mDragForeground.setTextSize(30f)
        mDragForeground.setAntiAlias(true)

        add(POI("Long click and drag me", GeoPoint(43.1677094, -1.23698415), -300, -90))
        add(POI("Roncesvalles", GeoPoint(43.01774243892033, -1.317764479899253)))
        add(POI("Urdániz", GeoPoint(42.9304266, -1.50463709)))
        add(POI("Pamplona", GeoPoint(42.81116477962334, -1.649884335366608), -200, -50))
        add(POI("Puente la Reina", GeoPoint(42.66585898113284, -1.815904950575316)))
        add(POI("Estella", GeoPoint(42.67372296488218, -2.025552547253327)))
        add(POI("Los Arcos", GeoPoint(42.5651743819995, -2.187210645317038)))
        add(POI("Logroño", GeoPoint(42.46552987114763, -2.445282148422933), 0, 90))
        add(POI("Nájera", GeoPoint(42.41652176456041, -2.732803767417607)))
        add(POI("Santo Domingo de la Calzada", GeoPoint(42.43229304715269, -2.952542527566706)))
        add(POI("Belorado", GeoPoint(42.4262676963629, -3.184220120411581)))
        add(POI("Agés", GeoPoint(42.369722, -3.4794)))
        add(POI("Burgos", GeoPoint(42.35092384897927, -3.685218770505309), -30, 90))
        add(POI("Hontanas", GeoPoint(42.316666, -4.033333)))
        add(POI("Boadilla del Camino", GeoPoint(42.25, -4.35)))
        add(POI("Carrion de los Condes", GeoPoint(42.33881483100247, -4.595917714974391)))
        add(POI("Terradillos de los Templarios", GeoPoint(42.362777, -4.8902777)))
        add(POI("El Burgo Ranero", GeoPoint(42.41746731921432, -5.218695473589733)))
        add(POI("León", GeoPoint(42.60054247433525, -5.572908186230237), 0, -100))
        add(POI("Villar de Mazarife", GeoPoint(42.483611, -5.7316666)))
        add(POI("Astorga", GeoPoint(42.44981716013144, -6.049581358750089)))
        add(POI("Foncebadón", GeoPoint(42.4916666, -6.3425)))
        add(POI("Ponferrada", GeoPoint(42.54629790350737, -6.578190951631911)))
        add(POI("Trabadelo", GeoPoint(42.6494444, -6.88194444)))
        add(POI("Fonfría", GeoPoint(42.73138888, -7.15694444)))
        add(POI("Barbadelo", GeoPoint(42.766666, -7.45)))
        add(POI("Hospital da Cruz", GeoPoint(42.840555, -7.735)))
        add(POI("Melide", GeoPoint(42.916666, -8.016666)))
        add(POI("Pedrouzo", GeoPoint(42.904444, -8.3625)))
        add(POI("Santiago de Compostela", GeoPoint(42.87968184500255, -8.545971242146521), 0, 150))

        val boundingBox = BoundingBox.fromGeoPoints(mGeoPoints)
        mMapView!!.post(object : Runnable {
            override fun run() {
                mMapView!!.zoomToBoundingBox(boundingBox, false, 50)
            }
        })
    }

    private fun add(pPOI: POI) {
        mGeoPoints.add(pPOI.mGeoPoint)
        addToDisplay(pPOI)
    }

    private fun addToDisplay(pPOI: POI) {
        val marker = Marker(mMapView!!)
        marker.setTitle(pPOI.mTitle)
        marker.position = pPOI.mGeoPoint
        marker.icon = mBitmapDrawable
        mMapView!!.getOverlays()!!.add(marker)
        if (pPOI.mSpeechBalloon) {
            val speechBalloonOverlay = SpeechBalloonOverlay()
            speechBalloonOverlay.setTitle(pPOI.mTitle)
            speechBalloonOverlay.setMargin(10)
            speechBalloonOverlay.setRadius(15)
            speechBalloonOverlay.setGeoPoint(GeoPoint(pPOI.mGeoPoint))
            speechBalloonOverlay.setOffset(pPOI.mOffsetX, pPOI.mOffsetY)
            speechBalloonOverlay.setForeground(mForeground)
            speechBalloonOverlay.setBackground(mBackground)
            speechBalloonOverlay.setDragForeground(mDragForeground)
            speechBalloonOverlay.setDragBackground(mDragBackground)
            mMapView!!.getOverlays()!!.add(speechBalloonOverlay)
        }
    }

    private inner class POI(
        val mTitle: String?,
        val mGeoPoint: GeoPoint,
        val mSpeechBalloon: Boolean,
        val mOffsetX: Int,
        val mOffsetY: Int
    ) {
        internal constructor(pTitle: String?, pGeoPoint: GeoPoint, pOffsetX: Int, pOffsetY: Int) : this(pTitle, pGeoPoint, true, pOffsetX, pOffsetY)

        internal constructor(pTitle: String?, pGeoPoint: GeoPoint) : this(pTitle, pGeoPoint, false, 0, 0)
    }
}
