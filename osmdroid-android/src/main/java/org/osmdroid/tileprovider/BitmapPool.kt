package org.osmdroid.tileprovider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.modules.ConfigurablePriorityThreadFactory
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BitmapPool  //singleton: begin
private constructor() {
    private val mPool = LinkedList<Bitmap>()
    private val mExecutor: ExecutorService = Executors.newFixedThreadPool(
        1,
        ConfigurablePriorityThreadFactory(Thread.MIN_PRIORITY, javaClass.name)
    )

    //singleton: end
    fun returnDrawableToPool(drawable: ReusableBitmapDrawable) {
        val b = drawable.tryRecycle()
        if (b != null && !b.isRecycled() && b.isMutable() && b.getConfig() != null) {
            synchronized(mPool) {
                mPool.addLast(b)
            }
        } else if (b != null) {
            Log.d(IMapView.LOGTAG, "Rejected bitmap from being added to BitmapPool.")
        }
    }

    @Deprecated(
        """As of 6.0.2, use
      {@link #applyReusableOptions(BitmapFactory.Options, int, int)} instead."""
    )
    fun applyReusableOptions(aBitmapOptions: BitmapFactory.Options) {
        // We can not guarantee a bitmap can be reused without knowing the dimensions, so always
        // return null in inBitmap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            aBitmapOptions.inBitmap = null
            aBitmapOptions.inSampleSize = 1
            aBitmapOptions.inMutable = true
        }
    }

    fun applyReusableOptions(aBitmapOptions: BitmapFactory.Options, width: Int, height: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // This could be optimized for KK and up, as from there on the only requirement is that
            // the reused bitmap's allocatedbytes are >= the size of new one. Since the pool is
            // almost only used for tiles of the same dimensions, the gains will probably be small.
            aBitmapOptions.inBitmap = obtainSizedBitmapFromPool(width, height)
            aBitmapOptions.inSampleSize = 1
            aBitmapOptions.inMutable = true
        }
    }

    @Deprecated(
        """As of 6.0.2, use
      {@link #obtainSizedBitmapFromPool(int, int)} instead."""
    )
    fun obtainBitmapFromPool(): Bitmap? {
        synchronized(mPool) {
            if (mPool.isEmpty()) {
                return null
            } else {
                val bitmap = mPool.removeFirst()
                if (bitmap.isRecycled()) {
                    return obtainBitmapFromPool() // recurse
                } else {
                    return bitmap
                }
            }
        }
    }

    fun obtainSizedBitmapFromPool(aWidth: Int, aHeight: Int): Bitmap? {
        synchronized(mPool) {
            if (mPool.isEmpty()) {
                return null
            } else {
                for (bitmap in mPool) {
                    if (bitmap.isRecycled()) {
                        mPool.remove(bitmap)
                        return obtainSizedBitmapFromPool(aWidth, aHeight) // recurse to prevent ConcurrentModificationException
                    } else if (bitmap.getWidth() == aWidth && bitmap.getHeight() == aHeight) {
                        mPool.remove(bitmap)
                        return bitmap
                    }
                }
            }
        }

        return null
    }

    fun clearBitmapPool() {
        synchronized(instance.mPool) {
            while (!instance.mPool.isEmpty()) {
                val bitmap: Bitmap = instance.mPool.remove()
                bitmap.recycle()
            }
        }
    }

    /**
     * @since 6.0.0
     * The same code was duplicated in many places: now there's a unique entry point and it's async
     */
    fun asyncRecycle(pDrawable: Drawable?) {
        if (pDrawable == null) {
            return
        }
        mExecutor.execute(object : Runnable {
            override fun run() {
                syncRecycle(pDrawable)
            }
        })
    }

    /**
     * @since 6.0.0
     */
    private fun syncRecycle(pDrawable: Drawable?) {
        if (pDrawable == null) {
            return
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            if (pDrawable is BitmapDrawable) {
                val bitmap = pDrawable.getBitmap()
                if (bitmap != null) {
                    bitmap.recycle()
                }
            }
        }
        if (pDrawable is ReusableBitmapDrawable) {
            returnDrawableToPool(pDrawable)
        }
    }

    companion object {
        val instance: BitmapPool = BitmapPool()
    }
}
