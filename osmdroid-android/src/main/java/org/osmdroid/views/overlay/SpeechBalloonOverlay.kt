package org.osmdroid.views.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.PointL
import org.osmdroid.util.RectL
import org.osmdroid.util.SpeechBalloonHelper
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection

/**
 * Overlay that display a title in a "speech balloon"
 *
 * @author Fabrice Fontaine
 * @since 6.1.1
 */
class SpeechBalloonOverlay : Overlay() {
    private val mHelper = SpeechBalloonHelper()
    private val mRect = RectL()
    private val mPoint = PointL()
    private val mIntersection1 = PointL()
    private val mIntersection2 = PointL()
    private val mPath = Path()
    private val mTextRect = Rect()
    private val mPixel = Point()

    private var mTitle: String? = null
    private var mGeoPoint: GeoPoint? = null
    private var mBackground: Paint? = null
    private var mForeground: Paint? = null
    private var mMargin = 0
    private var mRadius = 0.0
    private var mOffsetX = 0
    private var mOffsetY = 0

    private val mDraggable = true
    private var mIsDragged = false
    private var mDragStartX = 0f
    private var mDragStartY = 0f
    private var mDragDeltaX = 0f
    private var mDragDeltaY = 0f
    private var mDragBackground: Paint? = null
    private var mDragForeground: Paint? = null

    // TODO animation in / out
    // TODO paint border
    // TODO rounded corners
    // TODO option: no Geopoint, but a "fixed" pixel instead
    // TODO oriented
    fun setTitle(pTitle: String?) {
        mTitle = pTitle
    }

    fun setGeoPoint(pGeoPoint: GeoPoint?) {
        mGeoPoint = pGeoPoint
    }

    fun setBackground(pBackground: Paint?) {
        mBackground = pBackground
    }

    fun setForeground(pForeground: Paint?) {
        mForeground = pForeground
    }

    fun setDragBackground(pDragBackground: Paint?) {
        mDragBackground = pDragBackground
    }

    fun setDragForeground(pDragForeground: Paint?) {
        mDragForeground = pDragForeground
    }

    fun setMargin(pMargin: Int) {
        mMargin = pMargin
    }

    fun setRadius(pRadius: Long) {
        mRadius = pRadius.toDouble()
    }

    fun setOffset(pOffsetX: Int, pOffsetY: Int) {
        mOffsetX = pOffsetX
        mOffsetY = pOffsetY
    }

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        val background: Paint?
        val foreground: Paint?
        if (mIsDragged) {
            background = if (mDragBackground != null) mDragBackground else mBackground
            foreground = if (mDragForeground != null) mDragForeground else mForeground
        } else {
            background = mBackground
            foreground = mForeground
        }
        if (mGeoPoint == null) {
            return
        }
        if (mTitle == null || mTitle!!.trim { it <= ' ' }.length == 0) {
            return
        }
        if (foreground == null || background == null) {
            return
        }
        pProjection.toPixels(mGeoPoint, mPixel)
        val text = mTitle
        foreground.getTextBounds(text, 0, text!!.length, mTextRect)
        mPoint.set(mPixel.x.toLong(), mPixel.y.toLong())
        mTextRect.offset((mPoint.x + mOffsetX + mDragDeltaX).toInt(), (mPoint.y + mOffsetY + mDragDeltaY).toInt())
        mTextRect.top -= mMargin
        mTextRect.left -= mMargin
        mTextRect.right += mMargin
        mTextRect.bottom += mMargin
        mRect.set(mTextRect.left.toLong(), mTextRect.top.toLong(), mTextRect.right.toLong(), mTextRect.bottom.toLong())
        val corner = mHelper.compute(mRect, mPoint, mRadius, mIntersection1, mIntersection2)
        pCanvas.drawRect(mTextRect.left.toFloat(), mTextRect.top.toFloat(), mTextRect.right.toFloat(), mTextRect.bottom.toFloat(), background)
        if (corner != SpeechBalloonHelper.Companion.CORNER_INSIDE) {
            mPath.reset()
            mPath.moveTo(mPoint.x.toFloat(), mPoint.y.toFloat())
            mPath.lineTo(mIntersection1.x.toFloat(), mIntersection1.y.toFloat())
            mPath.lineTo(mIntersection2.x.toFloat(), mIntersection2.y.toFloat())
            mPath.close()
            pCanvas.drawPath(mPath, background)
        }
        pCanvas.drawText(text, (mTextRect.left + mMargin).toFloat(), (mTextRect.bottom - mMargin).toFloat(), foreground)
    }

    override fun onLongPress(event: MotionEvent, mapView: MapView?): Boolean {
        mapView ?: return false
        val touched = hitTest(event, mapView)
        if (touched) {
            if (mDraggable) {
                //starts dragging mode:
                mIsDragged = true
                mDragStartX = event.getX()
                mDragStartY = event.getY()
                mDragDeltaX = 0f
                mDragDeltaY = 0f
                mapView.invalidate()
            }
        }
        return touched
    }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView?): Boolean {
        mapView ?: return false
        if (mDraggable && mIsDragged) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                mDragDeltaX = event.getX() - mDragStartX
                mDragDeltaY = event.getY() - mDragStartY
                mOffsetX = (mOffsetX + mDragDeltaX).toInt()
                mOffsetY = (mOffsetY + mDragDeltaY).toInt()
                mDragDeltaX = 0f
                mDragDeltaY = 0f
                mIsDragged = false
                mapView.invalidate()
                return true
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mDragDeltaX = event.getX() - mDragStartX
                mDragDeltaY = event.getY() - mDragStartY
                mapView.invalidate()
                return true
            }
        }
        return false
    }

    private fun hitTest(event: MotionEvent, mapView: MapView?): Boolean {
        return mRect.contains(event.getX().toInt().toLong(), event.getY().toInt().toLong())
    }
}
