/*
 * WARNING, All test cases exist in osmdroid-android-it/src/main/java (maven project)
 *
 * During build time (with gradle), these tests are copied from osmdroid-android-it to OpenStreetMapViewer/src/androidTest/java
 * DO NOT Modify files in OpenSteetMapViewer/src/androidTest. You will loose your changes when building!
 *
 */
package org.osmdroid.views.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.test.rule.ActivityTestRule
import junit.framework.Assert
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.osmdroid.StarterMapActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.MapTileRequestState
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.ArrayList

class OpenStreetMapTileProviderDirectTest {
    @get:Rule
    val activityRule = ActivityTestRule(StarterMapActivity::class.java)

    private lateinit var provider: MapTileProviderBasic

    @Before
    fun setUp() {
        provider = MapTileProviderBasic(activityRule.activity)
    }

    @After
    fun tearDown() {
        provider.detach()
    }

    @Test
    fun test_getMapTile_not_found() {
        val tile = MapTileIndex.getTileIndex(29, 0, 0)
        assertNull("Expect tile to be null", provider.getMapTile(tile))
    }

    @Test
    fun test_getMapTile_found() {
        val tile = MapTileIndex.getTileIndex(2, 3, 3)
        if (Build.VERSION.SDK_INT >= 23) return

        var path = activityRule.activity.filesDir.absolutePath + File.separator + "osmdroid" + File.separator
        val cacheDirectory = File(path)
        if (!cacheDirectory.exists()) cacheDirectory.mkdirs()
        Configuration.instance!!.osmdroidTileCache = cacheDirectory

        path += "OpenStreetMapTileProviderTest.png"
        val file = File(path)
        if (file.exists()) file.delete()

        val firstBitmap = Bitmap.createBitmap(
            TileSourceFactory.MAPNIK.tileSizePixels,
            TileSourceFactory.MAPNIK.tileSizePixels,
            Bitmap.Config.ARGB_8888
        )
        firstBitmap.eraseColor(Color.YELLOW)
        Canvas(firstBitmap).drawText("test", 10f, 20f, Paint())

        try {
            file.createNewFile()
            FileOutputStream(path).use { output ->
                firstBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } catch (error: Exception) {
            error.printStackTrace()
            Assert.fail("unable to write temp tile $error")
        }

        val state = MapTileRequestState(
            tile,
            ArrayList<MapTileModuleProviderBase?>(),
            provider
        )
        provider.mapTileRequestCompleted(state, TileSourceFactory.MAPNIK.getDrawable(path))

        val drawable = provider.getMapTile(tile)
        if (file.exists()) file.delete()
        assertNotNull("Expect tile to be not null from path $path", drawable)
        assertTrue("Expect instance of BitmapDrawable", drawable is BitmapDrawable)
        val secondBitmap = (drawable as BitmapDrawable).bitmap
        assertNotNull("Expect tile to be not null", secondBitmap)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
            assertEquals("Compare config", firstBitmap.config, secondBitmap.config)
        }
        assertEquals("Compare width", firstBitmap.width, secondBitmap.width)
        assertEquals("Compare height", firstBitmap.height, secondBitmap.height)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
            val firstBuffer = ByteBuffer.allocate(firstBitmap.width * firstBitmap.height * 4)
            firstBitmap.copyPixelsToBuffer(firstBuffer)
            val secondBuffer = ByteBuffer.allocate(secondBitmap.width * secondBitmap.height * 4)
            secondBitmap.copyPixelsToBuffer(secondBuffer)
            assertEquals("Compare pixels", firstBuffer, secondBuffer)
        }
    }
}
