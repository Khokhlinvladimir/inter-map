package org.osmdroid.tileprovider.tilesource.bing

import android.content.Context
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.tilesource.IStyledTileSource
import org.osmdroid.tileprovider.tilesource.QuadTreeTileSource
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource.Companion.IMAGERYSET_AERIAL
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource.Companion.IMAGERYSET_AERIALWITHLABELS
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource.Companion.IMAGERYSET_ROAD
import org.osmdroid.tileprovider.util.ManifestUtil
import org.osmdroid.tileprovider.util.StreamUtils
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * BingMap tile source used with OSMDroid<br></br>
 *
 *
 * This class builds the Bing REST services url to be requested to get a tile image.<br></br>
 *
 *
 * Before to be used, the static method [.retrieveBingKey] must be invoked.<br></br>
 *
 *
 * See
 * [http://msdn.microsoft.com/en-us/library/ff701721.aspx](http://msdn.microsoft.com/en-us/library/ff701721.aspx)
 * for details on the Bing API.
 */
class BingMapTileSource(aLocale: String?) : QuadTreeTileSource("BingMaps", 0, 19, 256, FILENAME_ENDING, null), IStyledTileSource<String?> {
    private var mStyle: String = IMAGERYSET_ROAD

    // object storing imagery meta data
    private var mImageryData: ImageryMetaDataResource? = ImageryMetaDataResource.Companion.defaultInstance

    // local used for set BingMap REST culture parameter
    private var mLocale: String?

    // baseURl used for OnlineTileSourceBase override
    private var mBaseUrl: String? = null

    // tile's image resolved url pattern
    private var mUrl: String? = null

    /**
     * Constructor.<br></br> **Warning, the static method [.retrieveBingKey] should have been invoked once before constructor invocation**
     *
     * @param aLocale The language used with BingMap REST service to retrieve tiles.<br></br> If null, the system default locale is used.
     */
    init {
        mLocale = aLocale
        if (mLocale == null) {
            mLocale = Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry()
        }
    }

    /*-------------- overrides OnlineTileSourceBase ---------------------*/
    override val baseUrl: String?
        get() {
            if (!mImageryData!!.m_isInitialised) {
                initMetaData()
            }
            return mBaseUrl
        }

    override fun getTileURLString(pMapTileIndex: Long): String {
        if (!mImageryData!!.m_isInitialised) {
            initMetaData()
        }
        return String.format(mUrl!!, quadTree(pMapTileIndex))
    }

    override val minimumZoomLevel: Int
        /**
         * get minimum zoom level
         *
         * @return minimum zoom level supported by Bing Map for current map view mode
         */
        get() = mImageryData!!.m_zoomMin

    override val maximumZoomLevel: Int
        /**
         * get maximum zoom level
         *
         * @return maximum zoom level supported by Bing Map for current map view mode
         */
        get() = mImageryData!!.m_zoomMax

    override val tileSizePixels: Int
        /**
         * get tile size in pixel
         *
         * @return tile size in pixel supported by Bing Map for current map view mode
         */
        get() = mImageryData!!.m_imageHeight


    /**
     * get the base path used for caching purpose
     *
     * @return a base path built on name given as constructor parameter and current style name
     */
    override fun pathBase(): String {
        return mName + mStyle
    }

    override val copyrightNotice: String?
        get() = mImageryData!!.copyright

    /*--------------- IStyledTileSource --------------------*/
    /**
     * Set the map style.
     * @param aStyle The map style.<br></br>
     * Should be one of [IMAGERYSET_AERIAL], [IMAGERYSET_AERIALWITHLABELS] or [IMAGERYSET_ROAD]
     */
    override fun setStyle(pStyle: String?) {
        if (pStyle == null) return
        if (pStyle != mStyle) {
            // flag to re-read imagery data
            synchronized(mStyle) {
                mUrl = null
                mBaseUrl = null
                mImageryData!!.m_isInitialised = false
            }
        }
        mStyle = pStyle
        mName = pathBase()
    }

    override fun getStyle(): String {
        return mStyle
    }

    /**
     * Fire this after you've set up your prefered tile styles and locale
     * if you forget, it should fire on the first request for tiles.
     *
     *
     * See issue [https://github.com/osmdroid/osmdroid/issues/383](https://github.com/osmdroid/osmdroid/issues/383)
     * It was made public since v5.3
     *
     * @return
     * @since 5.3
     */
    fun initMetaData(): ImageryMetaDataResource {
        if (!mImageryData!!.m_isInitialised) {
            synchronized(this) {
                if (!mImageryData!!.m_isInitialised) {
                    val imageryData = this.metaData
                    if (imageryData != null) {
                        mImageryData = imageryData
                        updateBaseUrl()
                    }
                }
            }
        }
        return mImageryData!!
    }

    private val metaData: ImageryMetaDataResource?
        /**
         * Gets the imagery meta from the REST service, or null if it fails
         */
        get() {
            Log.d(IMapView.LOGTAG, "getMetaData")

            var returnValue: ImageryMetaDataResource? = null

            var `in`: InputStream? = null
            var client: HttpURLConnection? = null
            var dataStream: ByteArrayOutputStream? = null
            var out: BufferedOutputStream? = null
            try {
                client =
                    (URL(String.format(BASE_URL_PATTERN, mStyle, bingKey))
                        .openConnection()) as HttpURLConnection?
                Log.d(IMapView.LOGTAG, "make request " + client!!.getURL().toString().toString())
                client.setRequestProperty(
                    instance!!.userAgentHttpHeader,
                    instance!!.userAgentValue
                )
                for (entry in instance!!.additionalHttpRequestProperties!!.entries) {
                    client.setRequestProperty(entry.key, entry.value)
                }
                client.connect()

                if (client.getResponseCode() != 200) {
                    Log.e(
                        IMapView.LOGTAG,
                        "Cannot get response for url " + client.getURL().toString() + " " + client.getResponseMessage()
                    )
                } else {
                    `in` = client.getInputStream()
                    dataStream = ByteArrayOutputStream()
                    out = BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE)
                    StreamUtils.copy(`in`, out)
                    out.flush()

                    returnValue = ImageryMetaData.getInstanceFromJSON(dataStream.toString())
                }
            } catch (e: Exception) {
                Log.e(IMapView.LOGTAG, "Error getting imagery meta data", e)
            } finally {
                if (client != null) try {
                    client.disconnect()
                } catch (e: Exception) {
                    Log.d(IMapView.LOGTAG, "end getMetaData", e)
                }
                if (`in` != null) try {
                    `in`.close()
                } catch (e: Exception) {
                    Log.d(IMapView.LOGTAG, "end getMetaData", e)
                }
                if (dataStream != null) try {
                    dataStream.close()
                } catch (e: Exception) {
                    Log.d(IMapView.LOGTAG, "end getMetaData", e)
                }
                if (out != null) try {
                    out.close()
                } catch (e: Exception) {
                    Log.d(IMapView.LOGTAG, "end getMetaData", e)
                }
                Log.d(IMapView.LOGTAG, "end getMetaData")
            }
            return returnValue
        }

    /**
     * Resolves url patterns to update urls with current map view mode and available sub domain.<br></br>
     * When several subdomains are available, change current sub domain in a cycle manner
     */
    protected fun updateBaseUrl() {
        Log.d(IMapView.LOGTAG, "updateBaseUrl")
        val subDomain = mImageryData!!.subDomain
        val imageUrl = mImageryData!!.m_imageUrl ?: return
        val idx = imageUrl.lastIndexOf("/")
        if (idx > 0) {
            mBaseUrl = imageUrl.substring(0, idx)
        } else {
            mBaseUrl = imageUrl
        }

        mUrl = imageUrl
        if (subDomain != null) {
            mBaseUrl = String.format(mBaseUrl!!, subDomain)
            mUrl = String.format(mUrl!!, subDomain, "%s", mLocale)
        }
        Log.d(IMapView.LOGTAG, "updated url = " + mUrl)
        Log.d(IMapView.LOGTAG, "end updateBaseUrl")
    }

    companion object {
        /**
         * the meta data key in the manifest
         */
        private const val BING_KEY = "BING_KEY"

        //Constant used for imagerySet parameter
        /**
         * Aerial imagery mode
         */
        const val IMAGERYSET_AERIAL: String = "Aerial"

        /**
         * Aerial imagery with road overlay mode
         */
        const val IMAGERYSET_AERIALWITHLABELS: String = "AerialWithLabels"

        /**
         * Roads imagery mode
         */
        const val IMAGERYSET_ROAD: String = "Road"

        // Bing Map REST services return jpeg images
        private const val FILENAME_ENDING = ".jpeg"

        // URL used to get imageryData. It is requested in order to get tiles url patterns
        private const val BASE_URL_PATTERN =
            "https://dev.virtualearth.net/REST/V1/Imagery/Metadata/%s?mapVersion=v1&output=json&uriScheme=https&key=%s"

        /**
         * Bing Map key set by user.
         *
         * @see [http://msdn.microsoft.com/en-us/library/ff428642.aspx](http://msdn.microsoft.com/en-us/library/ff428642.aspx)
         */
        var bingKey: String? = ""

        /**
         * Read the API key from the manifest.<br></br>
         * This method should be invoked before class instantiation.<br></br>
         */
        fun retrieveBingKey(aContext: Context) {
            // get the key from the manifest

            bingKey = ManifestUtil.retrieveKey(aContext, BING_KEY)
        }
    }
}
