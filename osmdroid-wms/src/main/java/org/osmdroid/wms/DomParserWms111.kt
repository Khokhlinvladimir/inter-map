package org.osmdroid.wms

import android.util.Log
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView.Companion.getTileSystem
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Provides parsing for WMS 1.1.1 and 1.3.0. The schema's are close enough that the
 * parsing we do will work with both cases.
 * This is primarily for internal use only. See WMSParser
 * https://github.com/osmdroid/osmdroid/issues/177
 * created on 8/25/2017.
 *
 * @author Alex O'Ree
 * @see WMSLayer
 *
 * @see WMSParser
 *
 * @see WMSEndpoint
 *
 * @since 6.0.0
 */
object DomParserWms111 {
    const val TAG: String = "osmdroidwms"

    @JvmStatic
    @Throws(Exception::class)
    fun parse(element: Element): WMSEndpoint {
        //  WMTMSCapabilities ret = new WMTMSCapabilities();


        val rets = WMSEndpoint()
        rets.wmsVersion = element.getAttribute("version")

        //check the version attribute
        for (i in 0 until element.getChildNodes().getLength()) {
            val e = element.getChildNodes().item(i)
            if (e.getNodeName().contains("Service")) {
                extractService(e, rets)

                //                ret.setService(parseService(e));
            } else if (e.getNodeName().contains("Capability")) {
                extractCapability(e, rets)

                //           ret.setCapability(parseCapability(e));
            }
        }
        val deleteme: MutableList<WMSLayer?> = ArrayList<WMSLayer?>()
        for (i in rets.layers.indices) {
            if (rets.layers.get(i).name == null) deleteme.add(rets.layers.get(i))
            else {
                if (rets.layers.get(i).title == null) rets.layers.get(i).title = rets.layers.get(i).name
            }
        }
        rets.layers.removeAll(deleteme)


        return rets
    }

    private fun extractCapability(element: Node, rets: WMSEndpoint): WMSEndpoint {
        //   Capability ret = new Capability();


        for (i in 0 until element.getChildNodes().getLength()) {
            val e = element.getChildNodes().item(i)
            val name = e.getNodeName()
            //System.out.println("parseCapabilties/" + name);
            // Starts by looking for the entry tag
            if (name.contains("Request")) {
                parseRequest(e, rets)
            } else if (name.contains("Exception")) {
            } else if (name.contains("Layer")) {
                rets.layers.addAll(parseLayers(e))

                //TODO
            } else {
            }
        }
        return rets
    }

    private fun parseRequest(element: Node, rets: WMSEndpoint) {
        //   Capability ret = new Capability();

        for (i in 0 until element.getChildNodes().getLength()) {
            val e = element.getChildNodes().item(i)
            val name = e.getNodeName()
            //System.out.println("parseCapabilties/" + name);
            // Starts by looking for the entry tag
            if (name.contains("GetCapabilities")) {
                for (i2 in 0 until e.getChildNodes().getLength()) {
                    val e3 = e.getChildNodes().item(i2)
                    val name3 = e3.getNodeName()

                    if (name3.contains("DCPType")) {
                        for (i4 in 0 until e3.getChildNodes().getLength()) {
                            val e4 = e3.getChildNodes().item(i4)
                            val name4 = e4.getNodeName()
                            if (name4.contains("HTTP")) {
                                for (i5 in 0 until e4.getChildNodes().getLength()) {
                                    val e5 = e4.getChildNodes().item(i5)
                                    val name5 = e5.getNodeName()
                                    if (name5.contains("Get")) {
                                        for (i6 in 0 until e5.getChildNodes().getLength()) {
                                            val e6 = e5.getChildNodes().item(i6)
                                            val name6 = e6.getNodeName()
                                            if (name6.contains("OnlineResource")) {
                                                val href = e6.getAttributes().getNamedItem("href")
                                                val href2 = e6.getAttributes().getNamedItem("xlink:href")
                                                val href3 = e6.getAttributes().getNamedItemNS("http://www.w3.org/1999/xlink", "href")

                                                if (href != null) {
                                                    rets.baseurl = href.getNodeValue()
                                                } else if (href2 != null) {
                                                    rets.baseurl = href2.getNodeValue()
                                                } else if (href3 != null) {
                                                    rets.baseurl = href3.getNodeValue()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //find format
                //find http
                //find get
                //find OnlineResource/href
            } else {
            }
        }
    }

    //e is "Layer"
    private fun parseLayers(element: Node): MutableCollection<WMSLayer> {
        val tileSystem = getTileSystem()
        val rets: MutableList<WMSLayer> = ArrayList()
        val ret = WMSLayer()

        var north: Double? = null
        var south: Double? = null
        var east: Double? = null
        var west: Double? = null
        for (i in 0 until element.getChildNodes().getLength()) {
            val e = element.getChildNodes().item(i)
            val name = e.getNodeName()
            if (name.contains("Name")) {
                ret.name = e.getTextContent()
            } else if (name.contains("Title")) {
                ret.title = e.getTextContent()
            } else if (name.contains("Abstract")) {
                ret.description = e.getTextContent()
            } else if (name.contains("SRS")) {
                ret.srs.add(e.getTextContent())
            } else if (name.contains("CRS")) {
                ret.srs.add(e.getTextContent())
            } else if (name.contains("LatLonBoundingBox")) {
                //TODO need some handling for crs here
                south = (e.getAttributes().getNamedItem("miny").getNodeValue().toDouble())
                if (south < tileSystem.minLatitude) south = tileSystem.minLatitude
                north = (e.getAttributes().getNamedItem("maxy").getNodeValue().toDouble())

                if (north > tileSystem.maxLatitude) north = tileSystem.maxLatitude
                west = (e.getAttributes().getNamedItem("maxx").getNodeValue().toDouble())
                east = (e.getAttributes().getNamedItem("minx").getNodeValue().toDouble())
                ret.bbox = BoundingBox(north, east, south, west)
            } else if (name.contains("BoundingBox") && ret.bbox == null) {
                //need to check CRS first if it's valid
                //<BoundingBox CRS="CRS:84" minx="-179.999996" miny="-89.000000" maxx="179.999996" maxy="89.000000"/>
                //coordinates reversed?
                //<BoundingBox CRS="EPSG:4326" minx="-89.000000" miny="-179.999996" maxx="89.000000" maxy="179.999996"/>
                val crs = e.getAttributes().getNamedItem("CRS")
                if (crs != null && crs.getAttributes() != null) {
                    val maxx = crs.getAttributes().getNamedItem("maxx")
                    val maxy = crs.getAttributes().getNamedItem("maxy")
                    val miny = crs.getAttributes().getNamedItem("miny")
                    val minx = crs.getAttributes().getNamedItem("minx")

                    val ok = maxx != null && maxy != null && minx != null && miny != null
                    if (ok) {
                        if ("EPSG:4326" == crs.getNodeValue()) {
                            south = (minx!!.getNodeValue().toDouble())
                            north = (maxx!!.getNodeValue().toDouble())
                            west = (maxy!!.getNodeValue().toDouble())
                            east = (miny!!.getNodeValue().toDouble())
                            ret.bbox = BoundingBox(north, east, south, west)
                        } else if ("CRS:84" == crs.getNodeValue()) {
                            south = (miny!!.getNodeValue().toDouble())
                            north = (maxy!!.getNodeValue().toDouble())
                            west = (maxx!!.getNodeValue().toDouble())
                            east = (minx!!.getNodeValue().toDouble())
                            ret.bbox = BoundingBox(north, east, south, west)
                        } else {
                            Log.w(TAG, "warn, unhandled CRS/SRS " + crs.getNodeValue())
                        }
                    }
                }
            } else if (name.contains("Style")) {
                for (k in 0 until e.getChildNodes().getLength()) {
                    val e2 = e.getChildNodes().item(k)
                    if ("Name" == e2.getNodeName()) {
                        ret.styles.add(e2.getTextContent())
                    }
                }
            } else if (name.contains("Layer")) {
                rets.addAll(parseLayers(e))
            } else {
            }
        }

        val pixelx = element.getAttributes().getNamedItem("fixedHeight")
        val pixely = element.getAttributes().getNamedItem("fixedWidth")
        if (pixely != null && pixelx != null) {
            if (pixelx.getNodeValue() == pixely.getNodeValue()) {
                ret.pixelSize = pixelx.getNodeValue().toInt()
            } else {
                Log.w(TAG, "Layer excluded due to non-equal height,width tile sizes")
                return rets
            }
        } //else the image size wasn't defined,


        rets.add(ret)
        return rets
    }


    private fun extractService(element: Node, ret: WMSEndpoint): WMSEndpoint {
        for (i in 0 until element.getChildNodes().getLength()) {
            val e = element.getChildNodes().item(i)
            val name = e.getNodeName()
            // Starts by looking for the entry tag
            if (name.contains("Name")) {
                ret.name = e.getTextContent()
            } else if (name.contains("Title")) {
                ret.title = e.getTextContent()
            } else if (name.contains("Abstract")) {
                ret.description = e.getTextContent()
            } else if (name.contains("OnlineResource")) {
                val namedItem = e.getAttributes().getNamedItem("xlink:href")
                val namedItem2 = e.getAttributes().getNamedItem("href")
                var baseUrl: String? = null
                if (namedItem != null) baseUrl = namedItem.getNodeValue()
                if (namedItem2 != null) baseUrl = namedItem2.getNodeValue()
                if (baseUrl != null) {
                    ret.baseurl = baseUrl
                }
            }
        }
        return ret
    }
}
