package org.osmdroid.views.overlay

import android.graphics.Canvas
import android.graphics.Paint
import org.osmdroid.util.IntegerAccepter
import org.osmdroid.util.LineBuilder
import org.osmdroid.views.overlay.advancedpolyline.MonochromaticPaintList

/**
 * Created by Fabrice on 04/01/2018.
 *
 * @since 6.0.0
 */
open class LineDrawer(pMaxSize: Int) : LineBuilder(pMaxSize) {
    private var mIntegerAccepter: IntegerAccepter? = null
    private var mCanvas: Canvas? = null
    private var mPaintList: PaintList? = null

    fun setCanvas(pCanvas: Canvas) {
        mCanvas = pCanvas
    }

    fun setPaint(pPaint: Paint?) {
        setPaint(MonochromaticPaintList(pPaint))
    }

    fun setPaint(pPaintList: PaintList) {
        mPaintList = pPaintList
    }

    fun setIntegerAccepter(pIntegerAccepter: IntegerAccepter?) {
        mIntegerAccepter = pIntegerAccepter
    }

    override fun flush() {
        val nbSegments = size / 4
        if (nbSegments == 0) {
            additionalFlush()
            return
        }
        val lines = this.lines
        val paint = mPaintList!!.paint
        if (paint != null) { // monochromatic: that's enough
            val size: Int = compact(lines, nbSegments * 4)
            if (size > 0) {
                mCanvas!!.drawLines(lines, 0, size, paint)
            }
            additionalFlush()
            return
        }
        var i = 0
        while (i < nbSegments * 4) {
            val x0 = lines[i]
            val y0 = lines[i + 1]
            val x1 = lines[i + 2]
            val y1 = lines[i + 3]
            if (x0 == x1 && y0 == y1) {
                i += 4
                continue
            }
            val segmentIndex = mIntegerAccepter!!.getValue(i / 2)
            mCanvas!!.drawLine(x0, y0, x1, y1, mPaintList!!.getPaint(segmentIndex, x0, y0, x1, y1)!!)
            i += 4
        }
        additionalFlush()
    }

    private fun additionalFlush() {
        if (mIntegerAccepter != null) {
            mIntegerAccepter!!.flush()
        }
    }

    companion object {
        /**
         * @param pLines the input AND output array
         * @param pSize  the initial number of coordinates
         * @return the number of relevant coordinates
         * @since 6.2.0
         * Compact a float[] containing (x0,y0,x1,y1) segment coordinate quadruplets
         * by removing the single point cases (x0 == x1 && y0 == y1)
         */
        private fun compact(pLines: FloatArray, pSize: Int): Int {
            var dstIndex = 0
            var srcIndex = 0
            while (srcIndex < pSize) {
                val x0 = pLines[srcIndex]
                val y0 = pLines[srcIndex + 1]
                val x1 = pLines[srcIndex + 2]
                val y1 = pLines[srcIndex + 3]
                if (x0 == x1 && y0 == y1) {
                    srcIndex += 4
                    continue
                }
                if (srcIndex != dstIndex) {
                    System.arraycopy(pLines, srcIndex, pLines, dstIndex, 4)
                }
                dstIndex += 4
                srcIndex += 4
            }
            return dstIndex
        }
    }
}
