package org.osmdroid.views.overlay.simplefastpoint

import android.graphics.Paint
import android.location.Location
import org.osmdroid.util.GeoPoint

/**
 * Created by miguel on 07-01-2018.
 */
open class StyledLabelledGeoPoint : LabelledGeoPoint {
    var pointStyle: Paint? = null
    var textStyle: Paint? = null

    constructor(aLatitude: Double, aLongitude: Double) : super(aLatitude, aLongitude)

    constructor(aLatitude: Double, aLongitude: Double, aAltitude: Double) : super(aLatitude, aLongitude, aAltitude)

    constructor(aLatitude: Double, aLongitude: Double, aAltitude: Double, aLabel: String?) : super(aLatitude, aLongitude, aAltitude, aLabel)

    constructor(aLocation: Location?) : super(requireNotNull(aLocation))

    constructor(aGeopoint: GeoPoint?) : super(requireNotNull(aGeopoint))

    constructor(aLatitude: Double, aLongitude: Double, aLabel: String?) : super(aLatitude, aLongitude, aLabel)

    constructor(aLatitude: Double, aLongitude: Double, aLabel: String?, pointStyle: Paint?, textStyle: Paint?) : super(
        aLatitude,
        aLongitude,
        aLabel
    ) {
        this.pointStyle = pointStyle
        this.textStyle = textStyle
    }

    constructor(aLatitude: Double, aLongitude: Double, aAltitude: Double, aLabel: String?, pointStyle: Paint?, textStyle: Paint?) : super(
        aLatitude,
        aLongitude,
        aAltitude,
        aLabel
    ) {
        this.pointStyle = pointStyle
        this.textStyle = textStyle
    }

    constructor(aLabelledGeopoint: LabelledGeoPoint) : super(aLabelledGeopoint)

    override fun clone(): StyledLabelledGeoPoint {
        return StyledLabelledGeoPoint(
            this.latitude, this.longitude, this.altitude,
            this.label, this.pointStyle, this.textStyle
        )
    }
}
