package org.osmdroid.wms

/**
 * Simple data model for represeting a WMS server
 * https://github.com/osmdroid/osmdroid/issues/177
 *
 * @author Alex O'Ree
 * 1/10/16.
 * @since 6.0.0
 */
open class WMSEndpoint {
    open var name: String? = null
    open var description: String? = null
    open var title: String? = null
    open var wmsVersion: String? = "1.1.0"

    //capability/getmap/HTTP/Get/OnlineResource
    open var baseurl: String? = null
    open var layers: MutableList<WMSLayer> = ArrayList()
}
