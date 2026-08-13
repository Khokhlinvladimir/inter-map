package org.osmdroid.data

import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint

/**
 * Data about a country, including its ISO 3166-1 alpha-3, its name and its capital (name + lan/lon)
 *
 * @author Fabrice Fontaine
 * @since 6.0.3
 */
class DataCountry(
    val iSO3166_1_alpha_3: String?, val name: String?,
    val capitalName: String?,
    pCapitalLatitude: Double, pCapitalLongitude: Double
) {
    val capitalGeoPoint: IGeoPoint

    init {
        this.capitalGeoPoint = GeoPoint(pCapitalLatitude, pCapitalLongitude)
    }
}
