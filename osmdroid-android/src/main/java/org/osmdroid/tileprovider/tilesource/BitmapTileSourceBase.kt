package org.osmdroid.tileprovider.tilesource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.BitmapPool
import org.osmdroid.tileprovider.ReusableBitmapDrawable
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.io.InputStream
import java.util.Random

abstract class BitmapTileSourceBase @JvmOverloads constructor(
    aName: String?,
    aZoomMinLevel: Int, aZoomMaxLevel: Int, aTileSizePixels: Int,
    aImageFilenameEnding: String?, aCopyrightNotice: String? = null
) : ITileSource {
    open override val minimumZoomLevel: Int
    open override val maximumZoomLevel: Int

    private val mOrdinal: Int
    protected var mName: String?
    open override val copyrightNotice: String? = aCopyrightNotice
    protected val mImageFilenameEnding: String?
    protected val random: Random = Random()

    open override val tileSizePixels: Int

    /**
     * Constructor
     *
     * @param aName                a human-friendly name for this tile source. this name is also used on the file system, to keep the characters linux file system friendly
     * @param aZoomMinLevel        the minimum zoom level this tile source can provide
     * @param aZoomMaxLevel        the maximum zoom level this tile source can provide
     * @param aTileSizePixels      the tile size in pixels this tile source provides
     * @param aImageFilenameEnding the file name extension used when constructing the filename
     */
    //private final string mResourceId;
    /**
     * Constructor
     *
     * @param aName                a human-friendly name for this tile source. this name is also used on the file system, to keep the characters linux file system friendly
     * @param aZoomMinLevel        the minimum zoom level this tile source can provide
     * @param aZoomMaxLevel        the maximum zoom level this tile source can provide
     * @param aTileSizePixels      the tile size in pixels this tile source provides
     * @param aImageFilenameEnding the file name extension used when constructing the filename
     */
    init {
        mOrdinal = globalOrdinal++
        mName = aName
        this.minimumZoomLevel = aZoomMinLevel
        this.maximumZoomLevel = aZoomMaxLevel
        this.tileSizePixels = aTileSizePixels
        mImageFilenameEnding = aImageFilenameEnding
    }

    override fun ordinal(): Int {
        return mOrdinal
    }

    override fun name(): String? {
        return mName
    }

    open fun pathBase(): String? {
        return mName
    }

    fun imageFilenameEnding(): String? {
        return mImageFilenameEnding
    }

    override fun toString(): String {
        return name().orEmpty()
    }

    @Throws(LowMemoryException::class)
    override fun getDrawable(aFilePath: String?): Drawable? {
        if (aFilePath == null) return null
        //Log.d(IMapView.LOGTAG, aFilePath + " attempting to load bitmap");
        try {
            // We need to determine the real tile size first..
            // Otherwise, if mTileSizePixel is not correct, we will never be able to reuse bitmaps
            // from the pool, as we request them with mTileSizePixels, while they are stored with
            // their real size
            val optSize = BitmapFactory.Options()
            optSize.inJustDecodeBounds = true
            BitmapFactory.decodeFile(aFilePath, optSize)
            val realSize = optSize.outHeight

            // default implementation will load the file as a bitmap and create
            // a BitmapDrawable from it
            val bitmapOptions = BitmapFactory.Options()
            BitmapPool.instance.applyReusableOptions(
                bitmapOptions, realSize, realSize
            )
            val bitmap: Bitmap?
            //fix for API 15 see https://github.com/osmdroid/osmdroid/issues/227
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) bitmap = BitmapFactory.decodeFile(aFilePath)
            else bitmap = BitmapFactory.decodeFile(aFilePath, bitmapOptions)
            if (bitmap != null) {
                return ReusableBitmapDrawable(bitmap)
            } else {
                val bmp = File(aFilePath)
                if (bmp.exists()) {
                    // if we couldn't load it then it's invalid - delete it
                    Log.d(IMapView.LOGTAG, aFilePath + " is an invalid image file, deleting...")
                    try {
                        File(aFilePath).delete()
                    } catch (e: Throwable) {
                        Log.e(IMapView.LOGTAG, "Error deleting invalid file: " + aFilePath, e)
                    }
                } else Log.d(IMapView.LOGTAG, "Request tile: " + aFilePath + " does not exist")
            }
        } catch (e: OutOfMemoryError) {
            Log.e(IMapView.LOGTAG, "OutOfMemoryError loading bitmap: " + aFilePath)
            System.gc()
            throw LowMemoryException(e)
        } catch (e: Exception) {
            Log.e(IMapView.LOGTAG, "Unexpected error loading bitmap: " + aFilePath, e)
            Counters.tileDownloadErrors++
            System.gc()
        }
        return null
    }

    override fun getTileRelativeFilenameString(pMapTileIndex: Long): String? {
        val sb = StringBuilder()
        sb.append(pathBase())
        sb.append('/')
        sb.append(MapTileIndex.getZoom(pMapTileIndex))
        sb.append('/')
        sb.append(MapTileIndex.getX(pMapTileIndex))
        sb.append('/')
        sb.append(MapTileIndex.getY(pMapTileIndex))
        sb.append(imageFilenameEnding())
        return sb.toString()
    }

    @Throws(LowMemoryException::class)
    override fun getDrawable(aFileInputStream: InputStream?): Drawable? {
        if (aFileInputStream == null) return null
        try {
            // We need to determine the real tile size first..
            // Otherwise, if mTileSizePixel is not correct, we will never be able to reuse bitmaps
            // from the pool, as we request them with mTileSizePixels, while they are stored with
            // their real size
            var realSize = this.tileSizePixels
            if (aFileInputStream.markSupported()) {
                aFileInputStream.mark(1024 * 1024)
                val optSize = BitmapFactory.Options()
                optSize.inJustDecodeBounds = true
                BitmapFactory.decodeStream(aFileInputStream, null, optSize)
                realSize = optSize.outHeight

                aFileInputStream.reset()
            }


            // default implementation will load the file as a bitmap and create
            // a BitmapDrawable from it
            val bitmapOptions = BitmapFactory.Options()
            BitmapPool.instance.applyReusableOptions(
                bitmapOptions, realSize, realSize
            )
            val bitmap = BitmapFactory.decodeStream(aFileInputStream, null, bitmapOptions)
            if (bitmap != null) {
                return ReusableBitmapDrawable(bitmap)
            }
        } catch (e: OutOfMemoryError) {
            Log.e(IMapView.LOGTAG, "OutOfMemoryError loading bitmap")
            System.gc()
            throw LowMemoryException(e)
        } catch (ex: Exception) {
            Log.w(IMapView.LOGTAG, "#547 Error loading bitmap" + pathBase(), ex)
        }
        return null
    }

    class LowMemoryException : Exception {
        constructor(pDetailMessage: String?) : super(pDetailMessage)

        constructor(pThrowable: Throwable?) : super(pThrowable)

        companion object {
            private const val serialVersionUID = 146526524087765134L
        }
    }

    companion object {
        private var globalOrdinal = 0
    }
}
