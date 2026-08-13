package org.osmdroid.samplefragments.tilesources

/**
 * A simple demo work for working with WMS endpoints. Tested and functional
 * NASA 1.1.1 WMS
 * created on 8/20/2017.
 *
 * @author Alex O'Ree
 * @see WMSLayer
 *
 * @see WMSParser
 *
 * @see WMSEndpoint
 *
 * @since 5.6.5
 */
class NasaWmsSrs : SampleWMSSource() {
    override val sampleTitle: String?
        get() = "NASA WMS SRS"

    override val defaultUrl: String
        get() = "https://svs.gsfc.nasa.gov/cgi-bin/wms?version=1.1.1&service=WMS&request=GetCapabilities"
}
