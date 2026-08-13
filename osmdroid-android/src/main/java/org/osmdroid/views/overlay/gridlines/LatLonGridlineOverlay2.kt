package org.osmdroid.views.overlay.gridlines

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.LinearRing
import org.osmdroid.views.overlay.Overlay
import java.text.DecimalFormat
import kotlin.math.sqrt

class LatLonGridlineOverlay2 : Overlay() {
    protected var mDecimalFormatter: DecimalFormat = DecimalFormat("#.#####")

    //used to adjust the number of grid lines displayed on screen
    protected var mMultiplier: Float = 1f
    protected var mLinePaint: Paint = Paint()
    protected var mTextBackgroundPaint: Paint = Paint()
    /**
     * getter for the Paint object. I'd suggest using the setter methods first or subclassing this class
     * if you need to do something else but this will get you access to the live instance of the paint object
     * which is used for drawing text labels
     */
    /**
     * if for some reason there's missing setter for this class and you don't want to subclass it,
     * you can override the paint object with this method. Only used for the text painter
     * @param paint
     */
    var textPaint: Paint = Paint()
    protected var mOptimizationGeoPoint: GeoPoint = GeoPoint(0.0, 0.0)
    protected var mOptimizationPoint: Point = Point()

    init {
        mLinePaint.setAntiAlias(true)
        mLinePaint.setStyle(Paint.Style.STROKE)
        mTextBackgroundPaint.setStyle(Paint.Style.FILL)
        textPaint.setAntiAlias(true)
        textPaint.setStyle(Paint.Style.STROKE)
        textPaint.setTypeface(Typeface.DEFAULT_BOLD)
        textPaint.setTextAlign(Paint.Align.CENTER)
        setLineColor(Color.BLACK)
        setFontColor(Color.WHITE)
        setBackgroundColor(Color.BLACK)
        setLineWidth(1f)
        setFontSizeDp(32.toShort())
    }

    override fun draw(c: Canvas, pProjection: Projection) {
        if (!isEnabled()) return

        val incrementor = getIncrementor(pProjection.zoomLevel.toInt())
        val mapCenter = pProjection.currentCenter
        val startLongitude = incrementor * Math.round(mapCenter.longitude / incrementor)
        val startLatitude = computeStartLatitude(mapCenter.latitude, incrementor)
        val worldMapSize = pProjection.worldMapSize
        val screenWidth = pProjection.width.toFloat()
        val screenHeight = pProjection.height.toFloat()
        val screenCenterX = screenWidth / 2
        val screenCenterY = screenHeight / 2
        val screenDiagonal = sqrt((screenWidth * screenWidth + screenHeight * screenHeight).toDouble()).toFloat()
        val screenRadius = (screenDiagonal / 2).toDouble()
        val squaredScreenRadius = screenRadius * screenRadius
        val textOffsetX = screenWidth / 5
        val textOffsetY = screenHeight / 5
        val textBaseline = -textPaint.ascent() + 0.5f
        val textDescent = textPaint.descent() + 0.5f
        val textHeight = textBaseline + textDescent

        for (lineOrText in 0..1) { // draw lines first, then texts
            for (latOrLon in 0..1) { // latitude then longitude lines
                val orientation = -pProjection.orientation + (if (latOrLon == 0) 0 else 90)
                for (increaseOrDecrease in 0..1) { // in both directions
                    val delta = if (increaseOrDecrease == 0) incrementor else -incrementor
                    var latest =
                        if (latOrLon == 0) Math.round(screenCenterY) else Math.round(screenCenterX) // as close to the screen center as possible
                    var stillVisible = true
                    var longitude = startLongitude
                    var latitude = startLatitude
                    var i = 0
                    while (stillVisible) {
                        if (i > 0) {
                            if (latOrLon == 1) {
                                longitude += delta
                                while (longitude < -180) {
                                    longitude += 360.0
                                }
                                while (longitude > 180) {
                                    longitude -= 360.0
                                }
                            } else {
                                latitude += delta
                                if (latitude > getTileSystem().maxLatitude) {
                                    latitude = computeStartLatitude(getTileSystem().minLatitude, incrementor)
                                } else if (latitude < getTileSystem().minLatitude) {
                                    latitude = computeStartLatitude(getTileSystem().maxLatitude, incrementor)
                                }
                            }
                        }
                        mOptimizationGeoPoint.setCoords(latitude, longitude)
                        pProjection.toPixels(mOptimizationGeoPoint, mOptimizationPoint)
                        if (latOrLon == 0) {
                            mOptimizationPoint.y =
                                Math.round(LinearRing.getCloserValue(latest.toDouble(), mOptimizationPoint.y.toDouble(), worldMapSize))
                                    .toInt()
                            // low zoom fix
                            if (i > 0) {
                                if (delta < 0) { // when decreasing the degrees, we should find increased Y
                                    while (mOptimizationPoint.y < latest) { // if not, let's add the world
                                        mOptimizationPoint.y = (mOptimizationPoint.y + worldMapSize).toInt()
                                    }
                                } else {
                                    while (mOptimizationPoint.y > latest) {
                                        mOptimizationPoint.y = (mOptimizationPoint.y - worldMapSize).toInt()
                                    }
                                }
                            }
                            latest = mOptimizationPoint.y
                        } else {
                            mOptimizationPoint.x =
                                Math.round(LinearRing.getCloserValue(latest.toDouble(), mOptimizationPoint.x.toDouble(), worldMapSize))
                                    .toInt()
                            latest = mOptimizationPoint.x
                        }
                        if (i == 0 && increaseOrDecrease == 1) { // special case: already done with i=0,increaseOrDecrease=0
                            i++
                            continue
                        }
                        val xA: Float
                        val yA: Float
                        val xB: Float
                        val yB: Float
                        val squaredDistanceToCenter: Double
                        if (latOrLon == 0) {
                            yB = mOptimizationPoint.y.toFloat()
                            yA = yB
                            xA = screenCenterX - screenDiagonal
                            xB = screenCenterX + screenDiagonal
                            squaredDistanceToCenter = ((mOptimizationPoint.y - screenCenterY) * (mOptimizationPoint.y - screenCenterY)).toDouble()
                        } else {
                            xB = mOptimizationPoint.x.toFloat()
                            xA = xB
                            yA = screenCenterY - screenDiagonal
                            yB = screenCenterY + screenDiagonal
                            squaredDistanceToCenter = ((mOptimizationPoint.x - screenCenterX) * (mOptimizationPoint.x - screenCenterX)).toDouble()
                        }
                        stillVisible = squaredDistanceToCenter <= squaredScreenRadius
                        if (stillVisible) {
                            if (lineOrText == 0) { // draw lines
                                c.drawLine(xA, yA, xB, yB, mLinePaint)
                            } else { // draw text
                                val text = formatCoordinate(if (latOrLon == 0) latitude else longitude, latOrLon == 0)
                                val textCenterX = if (latOrLon == 0) textOffsetX else xA
                                val textCenterY = if (latOrLon == 0) yA else screenHeight - textOffsetY
                                val textWidth = textPaint.measureText(text) + 0.5f

                                if (orientation != 0f) {
                                    c.save()
                                    c.rotate(orientation, textCenterX, textCenterY)
                                }
                                c.drawRect(
                                    textCenterX - textWidth / 2f, textCenterY - textHeight / 2f,
                                    textCenterX + textWidth / 2f, textCenterY + textHeight / 2f,
                                    mTextBackgroundPaint
                                )
                                c.drawText(text, textCenterX, textCenterY + textHeight / 2 - textDescent, this.textPaint)
                                if (orientation != 0f) {
                                    c.restore()
                                }
                            }
                        }
                        i++
                    }
                }
            }
        }
    }

    fun setDecimalFormatter(df: DecimalFormat) {
        this.mDecimalFormatter = df
    }

    fun setLineColor(lineColor: Int) {
        mLinePaint.setColor(lineColor)
    }

    fun setFontColor(fontColor: Int) {
        textPaint.setColor(fontColor)
    }

    fun setFontSizeDp(fontSizeDp: Short) {
        textPaint.setTextSize(fontSizeDp.toFloat())
    }

    /**
     * sets the text label paint styler
     * see https://github.com/osmdroid/osmdroid/issues/1723
     * @param paint
     */
    fun setTextStyle(paint: Paint.Style?) {
        textPaint.setStyle(paint)
    }


    /**
     * background color for the text labels
     */
    fun setBackgroundColor(backgroundColor: Int) {
        mTextBackgroundPaint.setColor(backgroundColor)
    }

    fun setLineWidth(lineWidth: Float) {
        mLinePaint.setStrokeWidth(lineWidth)
    }

    /**
     * default is 1, larger number = more lines on screen. This comes at a performance penalty though
     */
    fun setMultiplier(multiplier: Float) {
        this.mMultiplier = multiplier
    }

    /**
     * this gets the distance in decimal degrees in between each line on the grid based on zoom level.
     * i had had it at more logical increments (90, 45, 30, etc) but changing to factors of 90 helps visualization
     * (i.e. when you zoom in on a particular crosshair, the crosshair is still there at the next zoom level, for the most part
     *
     * @param zoom mapview's osm zoom level
     * @return a double indicating the distance in degrees/decimal from which to place the gridlines on screen
     */
    protected fun getIncrementor(zoom: Int): Double {
        when (zoom) {
            0, 1 -> return 30.0 * mMultiplier
            2 -> return 15.0 * mMultiplier
            3 -> return 9.0 * mMultiplier
            4 -> return 6.0 * mMultiplier
            5 -> return 3.0 * mMultiplier
            6 -> return 2.0 * mMultiplier
            7 -> return 1.0 * mMultiplier
            8 -> return 0.5 * mMultiplier
            9 -> return 0.25 * mMultiplier
            10 -> return 0.1 * mMultiplier
            11 -> return 0.05 * mMultiplier
            12 -> return 0.025 * mMultiplier
            13 -> return 0.0125 * mMultiplier
            14 -> return 0.00625 * mMultiplier
            15 -> return 0.003125 * mMultiplier
            16 -> return 0.0015625 * mMultiplier
            17 -> return 0.00078125 * mMultiplier
            18 -> return 0.000390625 * mMultiplier
            19 -> return 0.0001953125 * mMultiplier
            20 -> return 0.00009765625 * mMultiplier
            21 -> return 0.000048828125 * mMultiplier
            22 -> return 0.0000244140625 * mMultiplier
            23 -> return 0.00001220703125 * mMultiplier
            24 -> return 0.000006103515625 * mMultiplier
            25 -> return 0.0000030517578125 * mMultiplier
            26 -> return 0.00000152587890625 * mMultiplier
            27 -> return 0.000000762939453125 * mMultiplier
            28 -> return 0.0000003814697265625 * mMultiplier
            29 -> return 0.00000019073486328125 * mMultiplier
            else -> return 0.00000019073486328125 * mMultiplier
        }
    }

    /**
     * Computes the start latitude when dealing with a latitude and an incrementor
     * Special focus on the "beyond possible" values of latitudes
     */
    private fun computeStartLatitude(pLatitude: Double, pIncrementor: Double): Double {
        var result = pIncrementor * Math.round(pLatitude / pIncrementor)
        while (result > getTileSystem().maxLatitude) {
            result -= pIncrementor
        }
        while (result < getTileSystem().minLatitude) {
            result += pIncrementor
        }
        return result
    }


    private fun formatCoordinate(pValue: Double, pLatitudeOrLongitude: Boolean): String {
        return mDecimalFormatter.format(pValue) + (if (pValue == 0.0) "" else if (pValue > 0) (if (pLatitudeOrLongitude) "N" else "E") else (if (pLatitudeOrLongitude) "S" else "W"))
    }
}
