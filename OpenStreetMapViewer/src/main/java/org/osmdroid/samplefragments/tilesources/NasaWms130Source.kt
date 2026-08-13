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
class NasaWms130Source : SampleWMSSource() {
    override val sampleTitle: String?
        get() = "NASA WMS 1.3.0"

    override val defaultUrl: String
        get() = "https://neo.sci.gsfc.nasa.gov/wms/wms?version=1.3.0&service=WMS&request=GetCapabilities"
}
