package org.osmdroid.data

import org.osmdroid.util.BoundingBox

/**
 * Data about a geo region, including its ISO 3166, its name and its geo bounding box
 *
 * @author Fabrice Fontaine
 * @since 6.0.2
 */
class DataRegion(val iSO3166: String?, val name: String?, val box: BoundingBox?)
