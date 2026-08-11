package org.osmdroid.wms

import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.MapTileIndex
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sinh

/**
 * WMS Map Source. Use this in conjunction with the WMSParser.
 *
 *
 * This class is known to work with GeoServer, but only supports a subset of possible
 * WMS configurations.
 *
 * @author Alex O'Ree
 * @see WMSParser
 * https://github.com/osmdroid/osmdroid/issues/177
 *
 * @since 6.0.0
 * 11/5/15.
 */
open class WMSTileSource(aName: String?, aBaseUrl: Array<String?>?, layername: String?, version: String?, srs: String?, style: String?, size: Int) :
    OnlineTileSourceBase(aName, 0, 22, size, "png", aBaseUrl) {
    val WMS_FORMAT_STRING: String = "%s" +
            "&version=%s" +
            "&request=GetMap" +
            "&layers=%s" +
            "&bbox=%f,%f,%f,%f" +
            "&width=256" +
            "&height=256" +
            "&srs=%s" +
            "&format=image/png" +
            "&style=%s" +
            "&transparent=true"
    private var layer: String? = ""
    private var version: String? = "1.1.0"
    private var srs: String? = "EPSG:900913" //used by geo server
    private var style: String? = null
    open var isForceHttps: Boolean = false
    open var isForceHttp: Boolean = false

    /**
     * Constructor
     *
     * @param aName    a human-friendly name for this tile source
     * @param aBaseUrl the base url(s) of the tile server used when constructing the url to download the tiles http://sedac.ciesin.columbia.edu/geoserver/wms
     */
    init {
        Log.i(IMapView.LOGTAG, "WMS support is BETA. Please report any issues")
        this.layer = layername
        this.version = version
        if (srs != null) this.srs = srs
        this.style = style
    }

    // Return a web Mercator bounding box given tile x/y indexes and a zoom
    // level.
    open fun getBoundingBox(x: Int, y: Int, zoom: Int): DoubleArray {
        val tileSize: Double = MAP_SIZE / 2.0.pow(zoom.toDouble())
        val minx: Double = TILE_ORIGIN[ORIG_X] + x * tileSize
        val maxx: Double = TILE_ORIGIN[ORIG_X] + (x + 1) * tileSize
        val miny: Double = TILE_ORIGIN[ORIG_Y] - (y + 1) * tileSize
        val maxy: Double = TILE_ORIGIN[ORIG_Y] - y * tileSize

        val bbox = DoubleArray(4)
        bbox[MINX] = minx
        bbox[MINY] = miny
        bbox[MAXX] = maxx
        bbox[MAXY] = maxy

        return bbox
    }

    override fun getTileURLString(pMapTileIndex: Long): String {
        var baseUrl = this.baseUrl.orEmpty()
        if (this.isForceHttps) baseUrl = baseUrl.replace("http://", "https://")
        if (this.isForceHttp) baseUrl = baseUrl.replace("https://", "http://")
        val sb = StringBuilder(baseUrl)
        if (!baseUrl.endsWith("&")) sb.append("&")

        sb.append("request=GetMap&width=").append(tileSizePixels).append("&height=").append(tileSizePixels).append("&version=").append(version)
        sb.append("&layers=").append(layer)
        sb.append("&bbox=")
        if (srs == "EPSG:900913") {
            //geoserver style
            val bbox = getBoundingBox(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex))
            sb.append(bbox[MINX]).append(",")
            sb.append(bbox[MINY]).append(",")
            sb.append(bbox[MAXX]).append(",")
            sb.append(bbox[MAXY])
        } else {
            val boundingBox: BoundingBox =
                tile2boundingBox(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex))
            sb.append(boundingBox.lonWest).append(",")
            sb.append(boundingBox.latSouth).append(",")
            sb.append(boundingBox.lonEast).append(",")
            sb.append(boundingBox.latNorth)
        }
        sb.append("&srs=").append(srs)
        sb.append("&format=image/png&transparent=true")
        if (style != null) sb.append("&styles=").append(style)

        Log.i(IMapView.LOGTAG, sb.toString())
        return sb.toString()
    }

    companion object {
        // array indexes for array to hold bounding boxes.
        protected const val MINX: Int = 0
        protected const val MAXX: Int = 1
        protected const val MINY: Int = 2
        protected const val MAXY: Int = 3

        // Web Mercator n/w corner of the map.
        private val TILE_ORIGIN = doubleArrayOf(-20037508.34789244, 20037508.34789244)

        //array indexes for that data
        private const val ORIG_X = 0
        private const val ORIG_Y = 1 // "

        // Size of square world map in meters, using WebMerc projection.
        private val MAP_SIZE = 20037508.34789244 * 2
        @JvmStatic
        fun createFrom(endpoint: WMSEndpoint, layer: WMSLayer): WMSTileSource {
            var srs: String? = "EPSG:900913"
            if (!layer.srs.isEmpty()) {
                srs = layer.srs.get(0)
            }
            if (layer.styles.isEmpty()) {
                val r = WMSTileSource(
                    layer.name, arrayOf<String?>(endpoint.baseurl), layer.name,
                    endpoint.wmsVersion, srs, null, layer.pixelSize
                )
                return r
            }

            val r = WMSTileSource(
                layer.name, arrayOf<String?>(endpoint.baseurl), layer.name,
                endpoint.wmsVersion, srs, layer.styles[0], layer.pixelSize
            )
            return r
        }

        @JvmStatic
        fun tile2boundingBox(x: Int, y: Int, zoom: Int): BoundingBox {
            val bb = BoundingBox(tile2lat(y, zoom), tile2lon(x + 1, zoom), tile2lat(y + 1, zoom), tile2lon(x, zoom))
            return bb
        }

        @JvmStatic
        fun tile2lon(x: Int, z: Int): Double {
            return x / 2.0.pow(z.toDouble()) * 360.0 - 180
        }

        @JvmStatic
        fun tile2lat(y: Int, z: Int): Double {
            val n = Math.PI - (2.0 * Math.PI * y) / 2.0.pow(z.toDouble())
            return Math.toDegrees(atan(sinh(n)))
        }
    }
}
