package org.osmdroid.tileprovider.modules

import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration.instance
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.tileprovider.util.StreamUtils
import org.osmdroid.util.MapTileIndex
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.net.UnknownHostException
import java.util.Arrays
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
class TileDownloader {
    private var compatibilitySocketFactorySet = false

    @Throws(CantContinueException::class)
    fun downloadTile(
        pMapTileIndex: Long,
        pFilesystemCache: IFilesystemCache?, pTileSource: OnlineTileSourceBase
    ): Drawable? {
        return downloadTile(pMapTileIndex, 0, pTileSource.getTileURLString(pMapTileIndex), pFilesystemCache, pTileSource)
    }

    /**
     * downloads a tile and follows http redirects
     * Code used to be in MapTileDownloader.TileLoader.downloadTile
     */
    @Throws(CantContinueException::class)
    fun downloadTile(
        pMapTileIndex: Long, redirectCount: Int, targetUrl: String?,
        pFilesystemCache: IFilesystemCache?, pTileSource: OnlineTileSourceBase
    ): Drawable? {
        // prevent infinite looping of redirects, rare but very possible for misconfigured servers

        if (redirectCount > 3) {
            return null
        }

        var userAgent: String? = null
        if (pTileSource.tileSourcePolicy.normalizesUserAgent()) {
            userAgent = instance!!.normalizedUserAgent
        }
        if (userAgent == null) {
            userAgent = instance!!.userAgentValue
        }
        if (!pTileSource.tileSourcePolicy.acceptsUserAgent(userAgent)) {
            Log.e(IMapView.LOGTAG, "Please configure a relevant user agent; current value is: " + userAgent)
            return null
        }
        var `in`: InputStream? = null
        var out: OutputStream? = null
        var c: HttpURLConnection? = null
        var byteStream: ByteArrayInputStream? = null
        var dataStream: ByteArrayOutputStream? = null
        try {
            val tileURLString = targetUrl

            if (instance!!.isDebugMode) {
                Log.d(IMapView.LOGTAG, "Downloading Maptile from url: " + tileURLString)
            }

            if (TextUtils.isEmpty(tileURLString)) {
                return null
            }

            // Try to enable TLSv1.2 and/or disable SSLv3 on older devices
            // see:
            // https://stackoverflow.com/questions/33567596/android-https-web-service-communication-ssl-tls-1-2/33567745#33567745
            // https://stackoverflow.com/questions/26649389/how-to-disable-sslv3-in-android-for-httpsurlconnection#29946540
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH && !compatibilitySocketFactorySet) {
                val socketFactory: SSLSocketFactory = CompatibilitySocketFactory(
                    HttpsURLConnection.getDefaultSSLSocketFactory()
                )
                HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory)
                compatibilitySocketFactorySet = true
            }

            if (instance!!.httpProxy != null) {
                c = URL(tileURLString).openConnection(instance!!.httpProxy) as HttpURLConnection?
            } else {
                c = URL(tileURLString).openConnection() as HttpURLConnection?
            }
            c!!.setUseCaches(true)
            c.setRequestProperty(instance!!.userAgentHttpHeader, userAgent)
            for (entry in instance!!.additionalHttpRequestProperties!!.entries) {
                c.setRequestProperty(entry.key, entry.value)
            }
            c.connect()

            // Check to see if we got success
            if (c.getResponseCode() != 200) {
                when (c.getResponseCode()) {
                    301, 302, 307, 308 -> {
                        if (instance!!.isMapTileDownloaderFollowRedirects) {
                            //this is a redirect, check the header for a 'Location' header
                            var redirectUrl = c.getHeaderField("Location")
                            if (redirectUrl != null) {
                                if (redirectUrl.startsWith("/")) {
                                    //in this case we need to stitch together a full url
                                    val old = URL(targetUrl)
                                    var port = old.getPort()
                                    val secure = targetUrl!!.lowercase(Locale.getDefault()).startsWith("https://")
                                    if (port == -1) if (targetUrl.lowercase(Locale.getDefault()).startsWith("http://")) {
                                        port = 80
                                    } else {
                                        port = 443
                                    }

                                    redirectUrl = (if (secure) "https://" else "http") + old.getHost() + ":" + port + redirectUrl
                                }
                                Log.i(
                                    IMapView.LOGTAG,
                                    "Http redirect for MapTile: " + MapTileIndex.toString(pMapTileIndex) + " HTTP response: " + c.getResponseMessage() + " to url " + redirectUrl
                                )
                                return downloadTile(pMapTileIndex, redirectCount + 1, redirectUrl, pFilesystemCache, pTileSource)
                            }
                            // Match the Java switch break: continue after the response-code branch.
                        } //else follow through the normal path of aborting the download

                        run {
                            Log.w(
                                IMapView.LOGTAG,
                                "Problem downloading MapTile: " + MapTileIndex.toString(pMapTileIndex) + " HTTP response: " + c.getResponseMessage()
                            )
                            if (instance!!.isDebugMapTileDownloader) {
                                Log.d(IMapView.LOGTAG, tileURLString!!)
                            }
                            Counters.tileDownloadErrors++
                            `in` = c.getErrorStream() // in order to have the error stream purged by the finally block
                            return null
                        }
                    }

                    else -> {
                        Log.w(
                            IMapView.LOGTAG,
                            "Problem downloading MapTile: " + MapTileIndex.toString(pMapTileIndex) + " HTTP response: " + c.getResponseMessage()
                        )
                        if (instance!!.isDebugMapTileDownloader) {
                            Log.d(IMapView.LOGTAG, tileURLString!!)
                        }
                        Counters.tileDownloadErrors++
                        `in` = c.getErrorStream()
                        return null
                    }
                }
            }

            val mime = c.getHeaderField("Content-Type")
            if (instance!!.isDebugMapTileDownloader) {
                Log.d(IMapView.LOGTAG, tileURLString + " success, mime is " + mime)
            }
            if (mime != null && !mime.lowercase(Locale.getDefault()).contains("image")) {
                Log.w(IMapView.LOGTAG, tileURLString + " success, however the mime type does not appear to be an image " + mime)
            }

            val input = c.getInputStream()
            `in` = input

            dataStream = ByteArrayOutputStream()
            out = BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE)
            val expirationTime = pTileSource.tileSourcePolicy.computeExpirationTime(
                c, System.currentTimeMillis()
            )
            StreamUtils.copy(input, out)
            out.flush()
            val data = dataStream.toByteArray()
            byteStream = ByteArrayInputStream(data)

            // Save the data to the cache
            // this is the only point in which we insert tiles to the db or local file system.
            if (pFilesystemCache != null) {
                pFilesystemCache.saveFile(pTileSource, pMapTileIndex, byteStream, expirationTime)
                byteStream.reset()
            }
            return pTileSource.getDrawable(byteStream)
        } catch (e: UnknownHostException) {
            Log.w(IMapView.LOGTAG, "UnknownHostException downloading MapTile: " + MapTileIndex.toString(pMapTileIndex) + " : " + e)
            Counters.tileDownloadErrors++
        } catch (e: LowMemoryException) {
            // low memory so empty the queue
            Counters.countOOM++
            Log.w(IMapView.LOGTAG, "LowMemoryException downloading MapTile: " + MapTileIndex.toString(pMapTileIndex) + " : " + e)
            throw CantContinueException(e)
        } catch (e: FileNotFoundException) {
            Counters.tileDownloadErrors++
            Log.w(IMapView.LOGTAG, "Tile not found: " + MapTileIndex.toString(pMapTileIndex) + " : " + e)
        } catch (e: IOException) {
            Counters.tileDownloadErrors++
            Log.w(IMapView.LOGTAG, "IOException downloading MapTile: " + MapTileIndex.toString(pMapTileIndex) + " : " + e)
        } catch (e: Throwable) {
            Counters.tileDownloadErrors++
            Log.e(IMapView.LOGTAG, "Error downloading MapTile: " + MapTileIndex.toString(pMapTileIndex), e)
        } finally {
            StreamUtils.closeStream(`in`)
            StreamUtils.closeStream(out)
            StreamUtils.closeStream(byteStream)
            StreamUtils.closeStream(dataStream)
            try {
                c!!.disconnect()
            } catch (ex: Exception) {
            }
        }

        return null
    }

    /**
     * @return the Epoch timestamp corresponding to the http header (in milliseconds), or null
     * @since 6.0.3
     */
    @Deprecated("Use {@link TileSourcePolicy#getHttpExpiresTime(String)} instead")
    fun getHttpExpiresTime(pHttpExpiresHeader: String?): Long? {
        if (pHttpExpiresHeader != null && pHttpExpiresHeader.length > 0) {
            try {
                val dateExpires = instance!!.httpHeaderDateTimeFormat!!.parse(pHttpExpiresHeader)
                return dateExpires!!.getTime()
            } catch (ex: Exception) {
                if (instance!!.isDebugMapTileDownloader) Log.d(
                    IMapView.LOGTAG,
                    "Unable to parse expiration tag for tile, server returned " + pHttpExpiresHeader,
                    ex
                )
            }
        }
        return null
    }

    /**
     * @return the max-age corresponding to the http header (in seconds), or null
     * @since 6.0.3
     */
    @Deprecated("Use {@link TileSourcePolicy#getHttpCacheControlDuration(String)} instead")
    fun getHttpCacheControlDuration(pHttpCacheControlHeader: String?): Long? {
        if (pHttpCacheControlHeader != null && pHttpCacheControlHeader.length > 0) {
            try {
                val parts = pHttpCacheControlHeader.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val maxAge = "max-age="
                for (part in parts) {
                    val pos = part.indexOf(maxAge)
                    if (pos == 0) {
                        val durationString = part.substring(maxAge.length)
                        return durationString.toLong()
                    }
                }
            } catch (ex: Exception) {
                if (instance!!.isDebugMapTileDownloader) Log.d(
                    IMapView.LOGTAG,
                    "Unable to parse cache control tag for tile, server returned " + pHttpCacheControlHeader, ex
                )
            }
        }
        return null
    }

    /**
     * @return the expiration time (as Epoch timestamp in milliseconds)
     * @since 6.0.3
     */
    @Deprecated("Use {@link TileSourcePolicy#computeExpirationTime(HttpURLConnection, long)} instead")
    fun computeExpirationTime(pHttpExpiresHeader: String?, pHttpCacheControlHeader: String?, pNow: Long): Long {
        val override = instance!!.expirationOverrideDuration
        if (override != null) {
            return pNow + override
        }

        val extension = instance!!.expirationExtendedDuration
        val cacheControlDuration = getHttpCacheControlDuration(pHttpCacheControlHeader)
        if (cacheControlDuration != null) {
            return pNow + cacheControlDuration * 1000 + extension
        }

        val httpExpiresTime = getHttpExpiresTime(pHttpExpiresHeader)
        if (httpExpiresTime != null) {
            return httpExpiresTime + extension
        }

        return pNow + OpenStreetMapTileProviderConstants.DEFAULT_MAXIMUM_CACHED_FILE_AGE + extension
    }

    /**
     * Proxy for [SSLSocketFactory] that tries to enable TLSv1.2 and/or disable SSLv3 on
     * older devices to improve security and compatibility with modern https server configurations
     *
     * @since 6.1.7
     */
    private class CompatibilitySocketFactory(sslSocketFactory: SSLSocketFactory) : SSLSocketFactory() {
        var sslSocketFactory: SSLSocketFactory

        init {
            this.sslSocketFactory = sslSocketFactory
        }

        override fun getDefaultCipherSuites(): Array<String?>? {
            return sslSocketFactory.getDefaultCipherSuites()
        }

        override fun getSupportedCipherSuites(): Array<String?>? {
            return sslSocketFactory.getSupportedCipherSuites()
        }

        @Throws(IOException::class)
        override fun createSocket(): Socket {
            val socket = sslSocketFactory.createSocket() as SSLSocket
            return upgradeTlsAndRemoveSsl(socket)
        }

        @Throws(IOException::class)
        override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
            val socket = sslSocketFactory.createSocket(s, host, port, autoClose) as SSLSocket
            return upgradeTlsAndRemoveSsl(socket)
        }

        @Throws(IOException::class, UnknownHostException::class)
        override fun createSocket(host: String?, port: Int): Socket {
            val socket = sslSocketFactory.createSocket(host, port) as SSLSocket
            return upgradeTlsAndRemoveSsl(socket)
        }

        @Throws(IOException::class, UnknownHostException::class)
        override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
            val socket = sslSocketFactory.createSocket(host, port, localHost, localPort) as SSLSocket
            return upgradeTlsAndRemoveSsl(socket)
        }

        @Throws(IOException::class)
        override fun createSocket(host: InetAddress?, port: Int): Socket {
            val socket = sslSocketFactory.createSocket(host, port) as SSLSocket
            return upgradeTlsAndRemoveSsl(socket)
        }

        @Throws(IOException::class)
        override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
            val socket = sslSocketFactory.createSocket(address, port, localAddress, localPort) as SSLSocket
            return upgradeTlsAndRemoveSsl(socket)
        }

        fun upgradeTlsAndRemoveSsl(socket: SSLSocket): SSLSocket {
            val supportedProtocols = socket.getSupportedProtocols()
            val enabledProtocols = socket.getEnabledProtocols()
            val newEnabledProtocols: Array<String>

            // If TLS 1.2 is supported just set it as the only enabled protocol an be done with it,
            // as it's guaranteed to be the most modern protocol on devices on API<21 (1.3 only
            // exists since August 2018)
            if (Arrays.binarySearch(supportedProtocols, "TLSv1.2") >= 0) {
                newEnabledProtocols = arrayOf("TLSv1.2")
            } else {
                val sslEnabled = Arrays.binarySearch(enabledProtocols, "SSLv3")
                if (sslEnabled >= 0) {
                    newEnabledProtocols = Array(enabledProtocols.size - 1) { "" }
                    System.arraycopy(enabledProtocols, 0, newEnabledProtocols, 0, sslEnabled)
                    if (newEnabledProtocols.size > sslEnabled) {
                        System.arraycopy(
                            enabledProtocols, sslEnabled + 1,
                            newEnabledProtocols, sslEnabled,
                            newEnabledProtocols.size - sslEnabled
                        )
                    }
                } else {
                    newEnabledProtocols = enabledProtocols
                }
            }

            socket.setEnabledProtocols(newEnabledProtocols)
            return socket
        }
    }
}
