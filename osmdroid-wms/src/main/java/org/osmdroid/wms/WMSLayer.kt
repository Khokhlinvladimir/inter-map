package org.osmdroid.wms

import org.osmdroid.util.BoundingBox

/**
 * A simple data model for WMS layers
 * 1/10/16.
 *
 * @author Alex O'Ree
 * @since 6.0.0
 */
open class WMSLayer {
    open var pixelSize: Int = 256

    /**
     * the name goes in the url and is machine intrepretable
     */
    open var name: String? = null

    /**
     * human readable title
     */
    open var title: String? = null

    //maps to 'abstract' wms element
    open var description: String? = null

    //TODO replace with osmdroid boundingbox
    open var bbox: BoundingBox? = null
    open val srs: MutableList<String> = ArrayList()

    open var styles: MutableList<String> = ArrayList()
}
