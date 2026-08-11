package org.osmdroid.views.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.DisplayMetrics
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection

/**///////////////////////////////////////////////////////////////////////////// */ //
//  Location - An Android location app.
//
//  Copyright (C) 2015	Bill Farmer
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
/**//////////////////////////////////////////////////////////////////////////// */


/**
 * CopyrightOverlay - uses the [ITileSource.getCopyrightNotice] text to paint on the screen
 *
 * [Original source](https://github.com/billthefarmer/location/blob/master/src/main/java/org/billthefarmer/location/CopyrightOverlay.java)
 *
 * [Issue 501](https://github.com/osmdroid/osmdroid/issues/501)
 *
 * [Open Street Map's guidance on attribution](http://www.openstreetmap.org/copyright/en)
 * created on 1/2/2017.
 *
 * @author billthefarmer@github
 * @author Alex O'Ree
 * @since 5.6.3
 */
class CopyrightOverlay(context: Context) : Overlay() {
    private val paint: Paint
    var xOffset: Int = 10
    var yOffset: Int = 10
    protected var mAlignBottom: Boolean = true
    protected var mAlignRight: Boolean = false
    val dm: DisplayMetrics
    private var mCopyrightNotice: String? = null

    // Constructor
    init {
        // Get the string
        val resources = context.getResources()

        // Get the display metrics
        dm = resources.getDisplayMetrics()

        // Get paint
        paint = Paint()
        paint.setAntiAlias(true)
        paint.setTextSize(dm.density * 12)
    }

    fun setTextSize(fontSize: Int) {
        paint.setTextSize(dm.density * fontSize)
    }

    fun setTextColor(color: Int) {
        paint.setColor(color)
    }

    // Set alignBottom
    fun setAlignBottom(alignBottom: Boolean) {
        mAlignBottom = alignBottom
    }

    // Set alignRight
    fun setAlignRight(alignRight: Boolean) {
        mAlignRight = alignRight
    }

    /**
     * Sets the screen offset. Values are in real pixels, not dip
     *
     * @param x horizontal screen offset, if aligh right is set, the offset is from the right, otherwise lift
     * @param y vertical screen offset, if align bottom is set, the offset is pixels from the bottom (not the top)
     */
    fun setOffset(x: Int, y: Int) {
        xOffset = x
        yOffset = y
    }

    override fun draw(canvas: Canvas, map: MapView, shadow: Boolean) {
        setCopyrightNotice(map.getTileProvider()?.getTileSource()?.copyrightNotice)
        draw(canvas, map.projection)
    }

    /**
     * @since 6.1.0
     */
    override fun draw(canvas: Canvas, pProjection: Projection) {
        if (mCopyrightNotice == null || mCopyrightNotice!!.length == 0) return

        val width = canvas.getWidth()
        val height = canvas.getHeight()

        var x = 0f
        var y = 0f

        if (mAlignRight) {
            x = (width - xOffset).toFloat()
            paint.setTextAlign(Paint.Align.RIGHT)
        } else {
            x = xOffset.toFloat()
            paint.setTextAlign(Paint.Align.LEFT)
        }

        if (mAlignBottom) y = (height - yOffset).toFloat()
        else y = paint.getTextSize() + yOffset

        // Draw the text
        pProjection.save(canvas, false, false)
        canvas.drawText(mCopyrightNotice!!, x, y, paint)
        pProjection.restore(canvas, false)
    }

    /**
     * @since 6.1.0
     */
    fun setCopyrightNotice(pCopyrightNotice: String?) {
        mCopyrightNotice = pCopyrightNotice
    }
}
