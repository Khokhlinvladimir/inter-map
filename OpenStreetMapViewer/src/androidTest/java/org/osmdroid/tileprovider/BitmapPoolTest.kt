package org.osmdroid.tileprovider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BitmapPoolTest {
    private lateinit var bitmapPool: BitmapPool
    private lateinit var bitmap: Bitmap
    private lateinit var differentSizeBitmap: Bitmap

    @Before
    fun setUp() {
        bitmapPool = BitmapPool.instance
        bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        differentSizeBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    }

    @After
    fun tearDown() {
        bitmapPool.clearBitmapPool()
    }

    @Test
    fun testThatBitmapIsReusedForSameSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            bitmapPool.clearBitmapPool()
            bitmapPool.returnDrawableToPool(ReusableBitmapDrawable(bitmap))
            val options = BitmapFactory.Options()
            bitmapPool.applyReusableOptions(options, bitmap.width, bitmap.height)
            assertNotNull(options.inBitmap)
        }
    }

    @Test
    fun testThatBitmapIsNotReusedForDifferentSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            bitmapPool.clearBitmapPool()
            bitmapPool.returnDrawableToPool(ReusableBitmapDrawable(bitmap))
            val options = BitmapFactory.Options()
            bitmapPool.applyReusableOptions(options, differentSizeBitmap.width, differentSizeBitmap.height)
            assertNull(options.inBitmap)
        }
    }

    @Test
    fun testThatBitmapPoolIsCleared() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            bitmapPool.clearBitmapPool()
            bitmapPool.returnDrawableToPool(ReusableBitmapDrawable(bitmap))
            bitmapPool.clearBitmapPool()
            val options = BitmapFactory.Options()
            bitmapPool.applyReusableOptions(options, bitmap.width, bitmap.height)
            assertNull(options.inBitmap)
        }
    }
}
