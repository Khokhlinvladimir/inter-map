package org.osmdroid.tileprovider

import android.graphics.Bitmap

/**
 * A [ExpirableBitmapDrawable] class that allows keeping track of usage references. This
 * facilitates the ability to reuse the underlying Bitmaps if no references are active. To safely
 * use the Drawable first call [.beginUsingDrawable] and then check [.isBitmapValid]
 * to ensure that the Drawable is still valid. When done using the Drawable you must call
 * [.finishUsingDrawable] to release the reference and allow the Bitmap to be reused later.
 *
 * @author Marc Kurtz
 */
class ReusableBitmapDrawable(pBitmap: Bitmap?) : ExpirableBitmapDrawable(pBitmap) {
    private var mBitmapRecycled = false
    private var mUsageRefCount = 0

    fun beginUsingDrawable() {
        synchronized(this) {
            mUsageRefCount++
        }
    }

    fun finishUsingDrawable() {
        synchronized(this) {
            mUsageRefCount--
            check(mUsageRefCount >= 0) { "Unbalanced endUsingDrawable() called." }
        }
    }

    fun tryRecycle(): Bitmap? {
        synchronized(this) {
            if (mUsageRefCount == 0) {
                mBitmapRecycled = true
                return getBitmap()
            }
        }
        return null
    }

    val isBitmapValid: Boolean
        get() {
            synchronized(this) {
                return !mBitmapRecycled
            }
        }
}
