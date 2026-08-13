package org.osmdroid.views.drawing

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import org.osmdroid.util.PointL
import org.osmdroid.views.Projection

@Deprecated("")
class OsmBitmapShader(bitmap: Bitmap, tileX: TileMode, tileY: TileMode) : BitmapShader(bitmap, tileX, tileY) {
    private val mMatrix = Matrix()
    private val mBitmapWidth: Int
    private val mBitmapHeight: Int

    init {
        mBitmapWidth = bitmap.getWidth()
        mBitmapHeight = bitmap.getHeight()
    }

    fun onDrawCycle(projection: Projection) {
        projection.toMercatorPixels(0, 0, sPoint)
        mMatrix.setTranslate((-sPoint.x % mBitmapWidth).toFloat(), (-sPoint.y % mBitmapHeight).toFloat())
        setLocalMatrix(mMatrix)
    }

    companion object {
        private val sPoint = PointL()
    }
}
