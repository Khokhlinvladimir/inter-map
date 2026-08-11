package org.osmdroid.tileprovider.tilesource

import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.util.CloudmadeUtil
import org.osmdroid.util.MapTileIndex

class CloudmadeTileSource(
    pName: String?,
    pZoomMinLevel: Int, pZoomMaxLevel: Int, pTileSizePixels: Int,
    pImageFilenameEnding: String?, pBaseUrl: Array<out String?>?
) : OnlineTileSourceBase(
    pName, pZoomMinLevel, pZoomMaxLevel, pTileSizePixels,
    pImageFilenameEnding, pBaseUrl
), IStyledTileSource<Int?> {
    private var mStyle: Int? = 1

    override fun pathBase(): String? {
        if (mStyle == null || mStyle!! <= 1) {
            return mName
        } else {
            return mName + mStyle
        }
    }

    override fun getTileURLString(pMapTileIndex: Long): String {
        val key = CloudmadeUtil.cloudmadeKey
        if (key.length == 0) {
            Log.e(IMapView.LOGTAG, "CloudMade key is not set. You should enter it in the manifest and call CloudmadeUtil.retrieveCloudmadeKey()")
        }
        val token = CloudmadeUtil.cloudmadeToken
        return String.format(
            baseUrl.orEmpty(), key, mStyle, tileSizePixels, MapTileIndex.getZoom(pMapTileIndex),
            MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), mImageFilenameEnding, token
        )
    }

    override fun setStyle(pStyle: Int?) {
        mStyle = pStyle
    }

    override fun setStyle(pStyle: String?) {
        if (pStyle == null) return
        try {
            mStyle = pStyle.toInt()
        } catch (e: NumberFormatException) {
            Log.e(IMapView.LOGTAG, "Error setting integer style: " + pStyle)
        }
    }

    override fun getStyle(): Int? {
        return mStyle
    }
}
