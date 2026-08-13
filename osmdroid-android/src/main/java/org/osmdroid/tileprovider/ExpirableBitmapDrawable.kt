package org.osmdroid.tileprovider

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.osmdroid.util.MapTileIndex

/**
 * A [BitmapDrawable] for a [MapTileIndex] that has a state to indicate its relevancy:
 * up-to-date (not expired yet), expired, scaled (computed during zoom) and not found (default grey tile)
 */
open class ExpirableBitmapDrawable(pBitmap: Bitmap?) : BitmapDrawable(pBitmap) {
    private var mState: IntArray

    init {
        mState = IntArray(0)
    }

    override fun getState(): IntArray {
        return mState
    }

    override fun isStateful(): Boolean {
        return mState.size > 0
    }

    override fun setState(pStateSet: IntArray): Boolean {
        mState = pStateSet
        return true
    }

    companion object {
        val UP_TO_DATE: Int = -1 // should not be set manually, just leave an empty int[] state
        val EXPIRED: Int = -2
        val SCALED: Int = -3
        val NOT_FOUND: Int = -4
        private val defaultStatus: Int = UP_TO_DATE

        private val settableStatuses = intArrayOf(EXPIRED, SCALED, NOT_FOUND)

        @Deprecated("")
        fun isDrawableExpired(pTile: Drawable): Boolean {
            return getState(pTile) == EXPIRED
        }

        fun getState(pTile: Drawable): Int {
            for (statusItem in pTile.getState()) {
                for (statusReference in settableStatuses) {
                    if (statusItem == statusReference) {
                        return statusItem
                    }
                }
            }
            return defaultStatus
        }

        @Deprecated("use {@link #setState(Drawable, int)} instead")
        fun setDrawableExpired(pTile: Drawable) {
            setState(pTile, EXPIRED)
        }

        fun setState(pTile: Drawable, status: Int) {
            pTile.setState(intArrayOf(status))
        }
    }
}
